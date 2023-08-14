package com.example.myapplication.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.ConditionFromFileJson
import com.example.myapplication.data.WeatherRepositoryImpl
import com.example.myapplication.domain.AddFavouritePlaceUseCase
import com.example.myapplication.domain.CheckFavouritePlaceUseCase
import com.example.myapplication.domain.DeleteFavouritePlaceUseCase
import com.example.myapplication.domain.FavouritePlace
import com.example.myapplication.domain.GetAllFavouritePlacesUseCase
import com.example.myapplication.domain.GetWeatherByLocationUseCase
import com.example.myapplication.domain.UpdateFavouritePlaceUseCase
import com.example.myapplication.domain.WeatherJson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.io.Writer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepositoryImpl(application)

    private val getWeatherByLocationUseCase = GetWeatherByLocationUseCase(repository)
    private val addFavouritePlaceUseCase = AddFavouritePlaceUseCase(repository)
    private val checkFavouritePlaceUseCase = CheckFavouritePlaceUseCase(repository)
    private val getAllFavouritePlacesUseCase = GetAllFavouritePlacesUseCase(repository)
    private val updateFavouritePlaceUseCase = UpdateFavouritePlaceUseCase(repository)
    private val deleteFavouritePlaceUseCase = DeleteFavouritePlaceUseCase(repository)

    val conditionsCodes by lazy { getJsonObject(R.raw.conditions) }

    val languageCode by lazy { getApplication<Application>().resources.configuration.locales[0].language }

    private val weatherApiState: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)
    val weatherApiStateLivedata = weatherApiState.asLiveData()

    private val placeStatusFlow: MutableStateFlow<PlaceStatus> =
        MutableStateFlow(PlaceStatus.NotFavourite)
    val placeStatusLivedata = placeStatusFlow.asLiveData()

    private val favouritePlacesFlow = getAllFavouritePlacesUseCase()
    val favouritePlacesLivedata = favouritePlacesFlow.asLiveData()

    init {
        viewModelScope.launch {
            getAllFavouritePlacesUseCase().collect { list ->
                list.forEach { favouritePlace ->
                    val location = "${favouritePlace.latitude},${favouritePlace.longitude}"
                    getWeatherByLocationUseCase(location)
                        .catch { e ->
                            weatherApiState.value = ApiState.Failure(e)
                        }
                        .collect { weather ->
                            val temp = getApplication<Application>()
                                .applicationContext
                                .getString(
                                    R.string.degrees,
                                    weather.current.temp_c.roundToInt()
                                )
                            val dayOrNight = getDayOrNightForCurrentTime(weather)
                            val condition = conditionsCodes.first {
                                it.code == weather.current.condition.code
                            }
                            val languageJson = condition.languages.first {
                                it.lang_iso == languageCode
                            }
                            val conditionForecast =
                                if (dayOrNight == "day") {
                                    languageJson.day_text
                                } else {
                                    languageJson.night_text
                                }
                            val conditionIconId =
                                getConditionIconId(weather.current.condition.code, dayOrNight)
                            val updatePlace = favouritePlace.copy(
                                current_temp = temp,
                                conditionText = conditionForecast,
                                conditionIconId = conditionIconId
                            )
                            updateFavouritePlaceUseCase(updatePlace)
                        }
                }

            }
        }
    }

    fun getWeatherByLocation(location: String) {
        viewModelScope.launch {
            weatherApiState.value = ApiState.Loading
            getWeatherByLocationUseCase(location)
                .catch { e ->
                    weatherApiState.value = ApiState.Failure(e)
                }.collect { data ->
                    weatherApiState.value = ApiState.Success(data)
                    val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
                    decimalFormatSymbols.decimalSeparator = '.'
                    val latString =
                        DecimalFormat("#.##", decimalFormatSymbols).format(data.location.lat)
                    val lonString =
                        DecimalFormat("#.##", decimalFormatSymbols).format(data.location.lon)
                    if (checkFavouritePlaceUseCase(latString, lonString)) {
                        placeStatusFlow.value = PlaceStatus.Favourite
                    } else {
                        placeStatusFlow.value = PlaceStatus.NotFavourite
                    }
                }
        }
    }

    fun addFavouritePlace(place: FavouritePlace) {
        viewModelScope.launch {
            val check = checkFavouritePlaceUseCase(place.latitude, place.longitude)
            if (check) {
                placeStatusFlow.value = PlaceStatus.Favourite
            } else {
                val ret = addFavouritePlaceUseCase(place)
                if (ret != FavouritePlace.UNDEFINED_ID) {
                    placeStatusFlow.value = PlaceStatus.Favourite
                } else {
                    placeStatusFlow.value = PlaceStatus.NotFavourite
                }
            }

        }
    }

    fun deleteFavouritePlace(place: FavouritePlace) {
        viewModelScope.launch {
            deleteFavouritePlaceUseCase(place)
        }
    }

    fun getConditionIconId(conditionCode: Int, day_or_night: String): Int {
        val conditionIcon = conditionsCodes.first {
            it.code == conditionCode
        }.icon
        return getResId(
            "${day_or_night}_${conditionIcon}",
            R.drawable::class.java
        )
    }

    private fun getJsonObject(resourcesRawFile: Int): List<ConditionFromFileJson> {
        val inputStream = getApplication<Application>().resources.openRawResource(resourcesRawFile)
        val writer: Writer = StringWriter()
        val buffer = CharArray(1024)
        inputStream.use { inSt ->
            val reader = BufferedReader(InputStreamReader(inSt, "UTF-8"))
            var n: Int
            while (reader.read(buffer).also { n = it } != -1) {
                writer.write(buffer, 0, n)
            }
        }
        val jsonString = writer.toString()
        val gson = GsonBuilder()
            .create()
        return gson.fromJson(jsonString, Array<ConditionFromFileJson>::class.java).toList()
    }

    private fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun getDayOrNightForCurrentTime(weatherJson: WeatherJson): String {
        val timeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-ddhh:mm a")
        val zoneId = ZoneId.of(weatherJson.location.tz_id)
        val today = weatherJson.forecast.forecastdays[0]
        val sunriseToday =
            today.date + today.astro.sunrise
        val sunriseTodayEpoch =
            LocalDateTime.parse(sunriseToday, timeFormatter).atZone(zoneId).toEpochSecond()
        val sunsetToday =
            today.date + today.astro.sunset
        val sunsetTodayEpoch =
            LocalDateTime.parse(sunsetToday, timeFormatter).atZone(zoneId).toEpochSecond()

        return if (weatherJson.current.last_updated_epoch in (sunriseTodayEpoch + 1) until sunsetTodayEpoch) "day" else "night"
    }

}