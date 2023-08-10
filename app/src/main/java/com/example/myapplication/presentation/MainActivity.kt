package com.example.myapplication.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.ConditionFromFileJson
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.domain.WeatherJson
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.io.Writer
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 222
    }

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val conditionsCodes by lazy { getJsonObject(R.raw.conditions) }


    private val timeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-ddhh:mm a")
    private val fromDateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    private val toDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM HH:mm")
    private val fromDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val toDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd")
    private val toTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val languageCode by lazy { resources.configuration.locales[0].language }

    private val hourlyWeatherAdapter by lazy {
        HourlyWeatherAdapter()
    }
    private val daysForecastAdapter by lazy {
        DaysForecastAdapter()
    }

    private var searchMode = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUi()
        observeViewModel()

        initLocationObjects()

        if (checkLocationPermissions()) {
            getLocationUpdate()
        } else {
            requestLocationPermissions()
        }

        //viewModel.getWeatherByLocation("Moscow")
    }

    private fun initLocationObjects() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 60
        ).setMinUpdateIntervalMillis(30)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                if(currentLocation != null) {
                    if (abs(p0.lastLocation!!.longitude - currentLocation!!.longitude) < 0.01) {
                        currentLocation = p0.lastLocation!!
                        stopLocationUpdate()
                    }
                }
                currentLocation = p0.lastLocation!!
            }
        }

        locationSettingsRequest =
            LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build()
    }

    private fun stopLocationUpdate() {

        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener {
                val lon = currentLocation!!.longitude //DecimalFormat("#.####").format(currentLocation!!.longitude)
                val lat = currentLocation!!.latitude//DecimalFormat("#.####").format(currentLocation!!.latitude)
                Log.d("MainActivity", "$lat,$lon")
                viewModel.getWeatherByLocation("$lat,$lon")
            }
    }

    private fun getLocationUpdate() {

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(
                this
            ) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@addOnSuccessListener
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
            }
            .addOnFailureListener {
                Log.d("mainActiviy", "Failure")
            }
    }

    private fun requestLocationPermissions() {
        val shouldProvideRationaleFine = shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val shouldProvideRationaleCoarse = shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (shouldProvideRationaleFine && shouldProvideRationaleCoarse) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Location permissions are needed for app functionality",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Ok") {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            }.show()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isEmpty()) {
                Log.d("onRequestPermissions", "Request was cancelled")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                getLocationUpdate()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Turn on location in settings",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("Settings") {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        packageName,
                        null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }.show()
            }

        }
    }

    private fun checkLocationPermissions(): Boolean {

        val permissionFineState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permissionCoarseState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return (permissionFineState == PackageManager.PERMISSION_GRANTED &&
            permissionCoarseState == PackageManager.PERMISSION_GRANTED)
    }

    private fun observeViewModel() {
        viewModel.weatherApiStateLivedata.observe(this) {
            when (it) {
                is ApiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.motionLayout.visibility = View.GONE
                }

                is ApiState.Failure -> {
                    it.e.printStackTrace()
                    Log.d("MainActivity", it.e.toString())
                    binding.progressBar.visibility = View.GONE
                    binding.motionLayout.visibility = View.VISIBLE
                }

                is ApiState.Success -> {
                    //  binding.swipeRefresh.isRefreshing = false
                    val weatherJson = it.data as WeatherJson
                    // Log.d("Success", myObj.toString())
                    embedWeatherJson(weatherJson)
                    binding.progressBar.visibility = View.GONE
                    binding.motionLayout.visibility = View.VISIBLE
                }

                is ApiState.Empty -> {
                    println("Empty...")
                }

                else -> {
                    println("Empty...")
                }
            }
        }
    }

    private fun initUi() {
        binding.hourlyForecastRecyclerView.adapter = hourlyWeatherAdapter
        binding.forecastDayRecyclerView.adapter = daysForecastAdapter
        binding.forecastDayRecyclerView.visibility = View.GONE
        binding.daysImageView.setOnClickListener {
            binding.daysImageView.setImageResource(R.color.primary80)
            binding.todayImageView.setImageResource(R.color.white)
            binding.forecastDayRecyclerView.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
            Log.d("MainActivity", "${binding.forecastDayRecyclerView.visibility}")
        }
        binding.todayImageView.setOnClickListener {
            binding.daysImageView.setImageResource(R.color.white)
            binding.todayImageView.setImageResource(R.color.primary80)
            binding.forecastDayRecyclerView.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
        }
        binding.searchImage.setOnClickListener {
            if (!searchMode) {
                binding.searchEditText.text.clear()
                binding.searchEditText.visibility = View.VISIBLE
                searchMode = true
            } else {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                binding.searchEditText.visibility = View.GONE
                binding.searchEditText.clearFocus()

                viewModel.getWeatherByLocation(binding.searchEditText.text.toString())
                searchMode = false
            }
        }
    }

    fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun embedWeatherJson(weatherJson: WeatherJson) {

        val zoneId = ZoneId.of(weatherJson.location.tz_id)

        with(binding) {
            cityNameWithCountry.text = getString(
                R.string.city_country,
                weatherJson.location.name,
                weatherJson.location.country
            )
            degreesTextView.text = getString(R.string.degrees, weatherJson.current.temp_c.roundToInt())
            feelsLikeTextView.text =
                getString(R.string.feels_like, weatherJson.current.feelslike_c.roundToInt())


            val today = weatherJson.forecast.forecastdays[0]
            val sunrise =
                today.date + today.astro.sunrise
            val sunriseEpoch =
                LocalDateTime.parse(sunrise, timeFormatter).atZone(zoneId).toEpochSecond()
            val sunset =
                today.date + today.astro.sunset
            val sunsetEpoch =
                LocalDateTime.parse(sunset, timeFormatter).atZone(zoneId).toEpochSecond()

            val day_or_night =
                getDayOrNight(weatherJson.current.last_updated_epoch, sunriseEpoch, sunsetEpoch)

            val condition = conditionsCodes.first {
                it.code == weatherJson.current.condition.code
            }
            val languageJson = condition.languages.first {
                it.lang_iso == languageCode
            }
            conditionTextView.text =
                if (day_or_night == "day") languageJson.day_text else languageJson.night_text

            conditionImageView.setImageResource(
                getConditionIconId(weatherJson.current.condition.code, day_or_night)
            )

            dateTimeTextView.text = LocalDateTime
                .parse(weatherJson.current.last_updated, fromDateTimeFormatter)
                .format(toDateTimeFormatter)


            val windSpeedMS = DecimalFormat("#.#").format(weatherJson.current.wind_kph / 3.6)
            windSpeedTextView.text = getString(R.string.wind_speed_ms, windSpeedMS)

            falloutTextView.text = getString(
                R.string.chance_of_rain,
                weatherJson.forecast.forecastdays[0].day.daily_chance_of_rain
            )

            val pressureMMHg = (weatherJson.current.pressure_mb * 0.75006).roundToInt()
            pressureTextView.text = getString(R.string.pressure_mmhg, pressureMMHg)

            val uv = DecimalFormat("#.#").format(weatherJson.current.uv)
            uvTextView.text = uv

            hourlyWeatherAdapter.submitList(getHourlyForecast(weatherJson))

            val rainChanceList = getRainChanceHourly(weatherJson)
            rainTime1TextView.text = rainChanceList[0].time
            rainChance1progressBar.progress = rainChanceList[0].chance
            rainChance1TextView.text = getString(R.string.degrees,rainChanceList[0].chance)
            rainTime2TextView.text = rainChanceList[1].time
            rainChance2progressBar.progress = rainChanceList[1].chance
            rainChance2TextView.text = getString(R.string.degrees,rainChanceList[1].chance)
            rainTime3TextView.text = rainChanceList[2].time
            rainChance3progressBar.progress = rainChanceList[2].chance
            rainChance3TextView.text = getString(R.string.degrees,rainChanceList[2].chance)
            rainTime4TextView.text = rainChanceList[3].time
            rainChance4progressBar.progress = rainChanceList[3].chance
            rainChance4TextView.text = getString(R.string.degrees,rainChanceList[3].chance)

            sunRiseTextView.text = LocalDateTime.parse(sunrise, timeFormatter).format(toTimeFormatter)
            sunSetTextView.text = LocalDateTime.parse(sunset, timeFormatter).format(toTimeFormatter)

            daysForecastAdapter.submitList(getDayForecastList(weatherJson))
        }
    }

    private fun getConditionIconId(conditionCode: Int, day_or_night: String): Int {
        val conditionIcon = conditionsCodes.first {
            it.code == conditionCode
        }.icon
        return getResId(
            "${day_or_night}_${conditionIcon}",
            R.drawable::class.java
        )
    }

    private fun getDayOrNight(timeEpoch: Long, sunriseEpoch: Long, sunsetEpoch: Long): String {

        return if (timeEpoch in (sunriseEpoch + 1) until sunsetEpoch) "day" else "night"
    }

    private fun getHourlyForecast(weatherJson: WeatherJson): List<HourForecast> {

        val resultList = mutableListOf<HourForecast>()

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

        val day_or_night = getDayOrNight(
            weatherJson.current.last_updated_epoch,
            sunriseTodayEpoch,
            sunsetTodayEpoch
        )
        val currentConditionIconId =
            getConditionIconId(weatherJson.current.condition.code, day_or_night)

        val currentHour = HourForecast(
            time = "Now",
            conditionIcon = currentConditionIconId,
            temp = getString(R.string.degrees, weatherJson.current.temp_c.roundToInt())
        )
        resultList.add(currentHour)

        val todayHours = weatherJson.forecast.forecastdays[0].hour

        val startIdx = todayHours.indexOfFirst {
            it.time_epoch > weatherJson.current.last_updated_epoch
        }

        todayHours.forEachIndexed { index, hourJson ->
            if (index >= startIdx) {
                val dayOrNightHour =
                    getDayOrNight(hourJson.time_epoch, sunriseTodayEpoch, sunsetTodayEpoch)
                val conditionIconId = getConditionIconId(hourJson.condition.code, dayOrNightHour)
                val time = LocalDateTime
                    .parse(hourJson.time, fromDateTimeFormatter)
                    .format(toTimeFormatter)
                val hour = HourForecast(
                    time = time,
                    conditionIcon = conditionIconId,
                    temp = getString(R.string.degrees, hourJson.temp_c.roundToInt())
                )
                resultList.add(hour)
            }
        }

        val listCount = resultList.count()

        if (listCount < 24) {

            val tomorrow = weatherJson.forecast.forecastdays[1]
            val sunriseTomorrow =
                tomorrow.date + tomorrow.astro.sunrise
            val sunriseTomorrowEpoch =
                LocalDateTime.parse(sunriseTomorrow, timeFormatter).atZone(zoneId).toEpochSecond()
            val sunsetTomorrow =
                tomorrow.date + tomorrow.astro.sunset
            val sunsetTomorrowEpoch =
                LocalDateTime.parse(sunsetTomorrow, timeFormatter).atZone(zoneId).toEpochSecond()

            val tomorrowHours = weatherJson.forecast.forecastdays[1].hour
            for (index in 0 until 24 - listCount) {
                val hourJson = tomorrowHours[index]
                val dayOrNightHour = getDayOrNight(
                    hourJson.time_epoch,
                    sunriseTomorrowEpoch,
                    sunsetTomorrowEpoch
                )
                val conditionIconId = getConditionIconId(hourJson.condition.code, dayOrNightHour)
                val time = LocalDateTime
                    .parse(hourJson.time, fromDateTimeFormatter)
                    .format(toTimeFormatter)
                val hour = HourForecast(
                    time = time,
                    conditionIcon = conditionIconId,
                    temp = getString(R.string.degrees, hourJson.temp_c.roundToInt())
                )
                resultList.add(hour)
            }
        }
        return resultList.toList()
    }

    private fun getRainChanceHourly(weatherJson: WeatherJson): List<RainChanceHourly> {

        val resultList = mutableListOf<RainChanceHourly>()

        val todayHours = weatherJson.forecast.forecastdays[0].hour
        val startIdx = todayHours.indexOfFirst {
            it.time_epoch > weatherJson.current.last_updated_epoch
        }

        todayHours.forEachIndexed { index, hourJson ->
            if(index >= startIdx) {
                val time = LocalDateTime
                    .parse(hourJson.time, fromDateTimeFormatter)
                    .format(toTimeFormatter)
                val rainChanceHourly = RainChanceHourly(
                    time = time,
                    chance = hourJson.chance_of_rain
                )
                resultList.add(rainChanceHourly)
            }
        }

        if(resultList.count() < 4){
            val tomorrowHours = weatherJson.forecast.forecastdays[1].hour
            for (index in 0 until 4 - resultList.count()) {
                val hourJson = tomorrowHours[index]
                val time = LocalDateTime
                    .parse(hourJson.time, fromDateTimeFormatter)
                    .format(toTimeFormatter)
                val rainChanceHourly = RainChanceHourly(
                    time = time,
                    chance = hourJson.chance_of_rain
                )
                resultList.add(rainChanceHourly)
            }
        }
        return resultList.toList()
    }

    private fun getDayForecastList(weatherJson: WeatherJson): List<DayForecast>{

        val resultList = mutableListOf<DayForecast>()

        val daysForecastList = weatherJson.forecast.forecastdays

        daysForecastList.forEachIndexed { index, dayWeatherJson ->
            val dateForecast = if(index == 0) {
                getString(R.string.today)
            } else {
                LocalDate
                    .parse(dayWeatherJson.date, fromDateFormatter)
                    .format(toDateFormatter)
                    .replaceFirstChar { it.uppercase() }
            }

            val condition = conditionsCodes.first {
                it.code == dayWeatherJson.day.condition.code
            }
            val languageJson = condition.languages.first {
                it.lang_iso == languageCode
            }
            val conditionForecast = languageJson.day_text
            val conditionIconId = getConditionIconId(condition.code, "day")
            val maxTemp = getString(R.string.degrees,dayWeatherJson.day.maxtemp_c.roundToInt())
            val minTemp = getString(R.string.degrees,dayWeatherJson.day.mintemp_c.roundToInt())

            val dayForecast = DayForecast(
                day = dateForecast,
                conditionText = conditionForecast,
                conditionIconId = conditionIconId,
                maxTemp = maxTemp,
                minTemp = minTemp
            )

            resultList.add(dayForecast)
        }

        return resultList.toList()
    }

    private fun getJsonObject(resourcesRawFile: Int): List<ConditionFromFileJson> {
        val inputStream = resources.openRawResource(resourcesRawFile)
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
}