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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.ConditionFromFileJson
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.domain.HourJson
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
import java.text.DecimalFormatSymbols
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

            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        initUi()
        observeViewModel()

        initLocationObjects()

        if (checkLocationPermissions()) {
            getLocationUpdate()
        } else {
            requestLocationPermissions()
        }
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
                if (currentLocation != null) {
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
                val lon =
                    currentLocation!!.longitude //DecimalFormat("#.####").format(currentLocation!!.longitude)
                val lat =
                    currentLocation!!.latitude//DecimalFormat("#.####").format(currentLocation!!.latitude)
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
                binding.progressBar.visibility = View.GONE
                binding.motionLayout.visibility = View.VISIBLE
                Toast.makeText(this, "Use search button for find location", Toast.LENGTH_LONG)
                    .show()
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

    private fun getResId(resName: String, c: Class<*>): Int {
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
            degreesTextView.text =
                getString(R.string.degrees, weatherJson.current.temp_c.roundToInt())
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

            val dayOrNight =
                getDayOrNight(weatherJson.current.last_updated_epoch, sunriseEpoch, sunsetEpoch)

            val condition = conditionsCodes.first {
                it.code == weatherJson.current.condition.code
            }
            val languageJson = condition.languages.first {
                it.lang_iso == languageCode
            }
            conditionTextView.text =
                if (dayOrNight == "day") languageJson.day_text else languageJson.night_text

            conditionImageView.setImageResource(
                getConditionIconId(weatherJson.current.condition.code, dayOrNight)
            )

            dateTimeTextView.text = LocalDateTime
                .parse(weatherJson.current.last_updated, fromDateTimeFormatter)
                .format(toDateTimeFormatter)

            val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
            decimalFormatSymbols.decimalSeparator = '.'
            val windSpeedMS = DecimalFormat("#.#", decimalFormatSymbols).format(weatherJson.current.wind_kph / 3.6)
            windSpeedTextView.text = getString(R.string.wind_speed_ms, windSpeedMS)

            val dailyChanceOfRain = weatherJson.forecast.forecastdays[0].day.daily_chance_of_rain
            val dailyChanceOfSnow = weatherJson.forecast.forecastdays[0].day.daily_chance_of_snow
            val falloutChance = if(dailyChanceOfRain > 0){
                dailyChanceOfRain
            } else if(dailyChanceOfSnow > 0){
                dailyChanceOfSnow
            } else {
                0
            }
            falloutTextView.text = getString(
                R.string.chance_of_rain,
                falloutChance
            )

            val pressureMMHg = (weatherJson.current.pressure_mb * 0.75006).roundToInt()
            pressureTextView.text = getString(R.string.pressure_mmhg, pressureMMHg)

            uvTextView.text = weatherJson.current.uv.toString()

            hourlyWeatherAdapter.submitList(getHourlyForecast(weatherJson))

            val falloutChanceList = getFalloutChanceHourly(weatherJson)
            falloutTime1TextView.text = falloutChanceList[0].time
            falloutChance1ProgressBar.progress = falloutChanceList[0].chance
            falloutChance1TextView.text = getString(R.string.chance_of_rain, falloutChanceList[0].chance)
            falloutTime2TextView.text = falloutChanceList[1].time
            falloutChance2ProgressBar.progress = falloutChanceList[1].chance
            falloutChance2TextView.text = getString(R.string.chance_of_rain, falloutChanceList[1].chance)
            falloutTime3TextView.text = falloutChanceList[2].time
            falloutChance3ProgressBar.progress = falloutChanceList[2].chance
            falloutChance3TextView.text = getString(R.string.chance_of_rain, falloutChanceList[2].chance)
            falloutTime4TextView.text = falloutChanceList[3].time
            falloutChance4ProgressBar.progress = falloutChanceList[3].chance
            falloutChance4TextView.text = getString(R.string.chance_of_rain, falloutChanceList[3].chance)

            sunRiseTextView.text =
                LocalDateTime.parse(sunrise, timeFormatter).format(toTimeFormatter)
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

        val dayOrNight = getDayOrNight(
            weatherJson.current.last_updated_epoch,
            sunriseTodayEpoch,
            sunsetTodayEpoch
        )
        val currentConditionIconId =
            getConditionIconId(weatherJson.current.condition.code, dayOrNight)

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

    private fun getFalloutChanceHourly(weatherJson: WeatherJson): List<FalloutChanceHourly> {

        val resultList = mutableListOf<FalloutChanceHourly>()

        val todayHours = weatherJson.forecast.forecastdays[0].hour
        val startIdx = todayHours.indexOfFirst {
            it.time_epoch > weatherJson.current.last_updated_epoch
        }

        todayHours.forEachIndexed { index, hourJson ->
            if (index >= startIdx) {
                val time = LocalDateTime
                    .parse(hourJson.time, fromDateTimeFormatter)
                    .format(toTimeFormatter)
                val falloutChanceHourly = FalloutChanceHourly(
                    time = time,
                    chance = getFalloutChance(hourJson)
                )
                resultList.add(falloutChanceHourly)
            }
        }

        if (resultList.count() < 4) {
            val tomorrowHours = weatherJson.forecast.forecastdays[1].hour
            for (index in 0 until 4 - resultList.count()) {
                val hourJson = tomorrowHours[index]
                val time = LocalDateTime
                    .parse(hourJson.time, fromDateTimeFormatter)
                    .format(toTimeFormatter)
                val falloutChanceHourly = FalloutChanceHourly(
                    time = time,
                    chance = getFalloutChance(hourJson)
                )
                resultList.add(falloutChanceHourly)
            }
        }
        return resultList.toList()
    }

    private fun getFalloutChance(hourJson: HourJson): Int {
        val falloutChance = if (hourJson.chance_of_rain > 0) {
            hourJson.chance_of_rain
        } else if (hourJson.chance_of_snow > 0) {
            hourJson.chance_of_snow
        } else {
            0
        }
        return falloutChance
    }

    private fun getDayForecastList(weatherJson: WeatherJson): List<DayForecast> {

        val resultList = mutableListOf<DayForecast>()

        val daysForecastList = weatherJson.forecast.forecastdays

        daysForecastList.forEachIndexed { index, dayWeatherJson ->
            val dateForecast = if (index == 0) {
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
            val maxTemp = getString(R.string.degrees, dayWeatherJson.day.maxtemp_c.roundToInt())
            val minTemp = getString(R.string.degrees, dayWeatherJson.day.mintemp_c.roundToInt())

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