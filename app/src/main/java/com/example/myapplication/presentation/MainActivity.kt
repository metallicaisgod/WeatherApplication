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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.domain.FavouritePlace
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

    private val conditionsCodes by lazy { viewModel.conditionsCodes }


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
    private val favouritesAdapter by lazy {
        FavouritesAdapter()
    }

    private lateinit var currentWeatherJson: WeatherJson

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
                binding.locationImageView.visibility = View.VISIBLE
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
                    setStartScreenState()
                }

                is ApiState.Failure -> {
                    it.e.printStackTrace()
                    Log.d("MainActivity", it.e.toString())
                    binding.progressBar.visibility = View.GONE
                    binding.motionLayout.visibility = View.VISIBLE
                }

                is ApiState.Success -> {
                    //  binding.swipeRefresh.isRefreshing = false
                    currentWeatherJson = it.data as WeatherJson
                    // Log.d("Success", myObj.toString())
                    embedWeatherJson()
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

        viewModel.placeStatusLivedata.observe(this) {
            var srcResId = R.drawable.favorite_white
            var srcAltResId = R.drawable.favorite_black
            when (it) {
                is PlaceStatus.Favourite -> {
                    srcResId = R.drawable.favorite_fill_white
                    srcAltResId = R.drawable.favorite_fill_black
                }

                is PlaceStatus.NotFavourite -> {
                    srcResId = R.drawable.favorite_white
                    srcAltResId = R.drawable.favorite_black
                }
            }
            binding.favoriteImageView.setImageResource(srcResId)
            binding.favoriteImageView.setAltImageResource(srcAltResId)
        }

        viewModel.favouritePlacesLivedata.observe(this) {
            favouritesAdapter.submitList(it)
        }
    }

    private fun initUi() {
        binding.hourlyForecastRecyclerView.adapter = hourlyWeatherAdapter
        binding.forecastDayRecyclerView.adapter = daysForecastAdapter
        binding.favouritesRecyclerView.adapter = favouritesAdapter
        binding.forecastDayRecyclerView.visibility = View.GONE
        binding.favouritesRecyclerView.visibility = View.GONE
        binding.daysImageView.setOnClickListener {
            binding.daysImageView.setImageResource(R.color.primary80)
            binding.todayImageView.setImageResource(R.color.white)
            binding.favouritesImageView.setImageResource(R.color.white)
            binding.scrollView.visibility = View.GONE
            binding.favouritesRecyclerView.visibility = View.GONE
            binding.forecastDayRecyclerView.visibility = View.VISIBLE
        }
        binding.todayImageView.setOnClickListener {
            setStartScreenState()
        }
        binding.favouritesImageView.setOnClickListener {
            binding.daysImageView.setImageResource(R.color.white)
            binding.todayImageView.setImageResource(R.color.white)
            binding.favouritesImageView.setImageResource(R.color.primary80)
            binding.forecastDayRecyclerView.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
            binding.favouritesRecyclerView.visibility = View.VISIBLE
        }

        favouritesAdapter.onFavouritePlaceClickListener = {
            viewModel.getWeatherByLocation("${it.latitude},${it.longitude}")
            binding.locationImageView.visibility = View.GONE
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
                binding.locationImageView.visibility = View.GONE
                searchMode = false
            }
        }

        binding.favoriteImageView.setOnClickListener {
            val placeName = getString(
                R.string.city_country,
                currentWeatherJson.location.name,
                currentWeatherJson.location.country
            )
            val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
            decimalFormatSymbols.decimalSeparator = '.'
            val latString =
                DecimalFormat("#.##", decimalFormatSymbols).format(currentWeatherJson.location.lat)
            val lonString =
                DecimalFormat("#.##", decimalFormatSymbols).format(currentWeatherJson.location.lon)
            val temp = getString(R.string.degrees, currentWeatherJson.current.temp_c.roundToInt())
            val place = FavouritePlace(
                place_name = placeName,
                latitude = latString,
                longitude = lonString,
                current_temp = temp
            )
            viewModel.addFavouritePlace(place)
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val place = favouritesAdapter.currentList[viewHolder.adapterPosition]
                viewModel.deleteFavouritePlace(place)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.place_deleted, place.place_name),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        ).attachToRecyclerView(binding.favouritesRecyclerView)
    }

    private fun setStartScreenState() {
        binding.daysImageView.setImageResource(R.color.white)
        binding.todayImageView.setImageResource(R.color.primary80)
        binding.favouritesImageView.setImageResource(R.color.white)
        binding.forecastDayRecyclerView.visibility = View.GONE
        binding.favouritesRecyclerView.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE
    }

    private fun embedWeatherJson() {

        val zoneId = ZoneId.of(currentWeatherJson.location.tz_id)

        with(binding) {
            cityNameWithCountry.text = getString(
                R.string.city_country,
                currentWeatherJson.location.name,
                currentWeatherJson.location.country
            )
            degreesTextView.text =
                getString(R.string.degrees, currentWeatherJson.current.temp_c.roundToInt())
            feelsLikeTextView.text =
                getString(R.string.feels_like, currentWeatherJson.current.feelslike_c.roundToInt())


            val today = currentWeatherJson.forecast.forecastdays[0]
            val sunrise =
                today.date + today.astro.sunrise
            val sunriseEpoch =
                LocalDateTime.parse(sunrise, timeFormatter).atZone(zoneId).toEpochSecond()
            val sunset =
                today.date + today.astro.sunset
            val sunsetEpoch =
                LocalDateTime.parse(sunset, timeFormatter).atZone(zoneId).toEpochSecond()

            val dayOrNight =
                getDayOrNight(
                    currentWeatherJson.current.last_updated_epoch,
                    sunriseEpoch,
                    sunsetEpoch
                )

            val condition = conditionsCodes.first {
                it.code == currentWeatherJson.current.condition.code
            }
            val languageJson = condition.languages.first {
                it.lang_iso == languageCode
            }
            conditionTextView.text =
                if (dayOrNight == "day") languageJson.day_text else languageJson.night_text

            conditionImageView.setImageResource(
                viewModel.getConditionIconId(currentWeatherJson.current.condition.code, dayOrNight)
            )

            dateTimeTextView.text = LocalDateTime
                .parse(currentWeatherJson.current.last_updated, fromDateTimeFormatter)
                .format(toDateTimeFormatter)

            val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
            decimalFormatSymbols.decimalSeparator = '.'
            val windSpeedMS = DecimalFormat(
                "#.#",
                decimalFormatSymbols
            ).format(currentWeatherJson.current.wind_kph / 3.6)
            windSpeedTextView.text = getString(R.string.wind_speed_ms, windSpeedMS)

            val dailyChanceOfRain =
                currentWeatherJson.forecast.forecastdays[0].day.daily_chance_of_rain
            val dailyChanceOfSnow =
                currentWeatherJson.forecast.forecastdays[0].day.daily_chance_of_snow
            val falloutChance = if (dailyChanceOfRain > 0) {
                dailyChanceOfRain
            } else if (dailyChanceOfSnow > 0) {
                dailyChanceOfSnow
            } else {
                0
            }
            falloutTextView.text = getString(
                R.string.chance_of_rain,
                falloutChance
            )

            val pressureMMHg = (currentWeatherJson.current.pressure_mb * 0.75006).roundToInt()
            pressureTextView.text = getString(R.string.pressure_mmhg, pressureMMHg)

            uvTextView.text = currentWeatherJson.current.uv.toString()

            hourlyWeatherAdapter.submitList(getHourlyForecast(currentWeatherJson))

            val falloutChanceList = getFalloutChanceHourly()
            falloutTime1TextView.text = falloutChanceList[0].time
            falloutChance1ProgressBar.progress = falloutChanceList[0].chance
            falloutChance1TextView.text =
                getString(R.string.chance_of_rain, falloutChanceList[0].chance)
            falloutTime2TextView.text = falloutChanceList[1].time
            falloutChance2ProgressBar.progress = falloutChanceList[1].chance
            falloutChance2TextView.text =
                getString(R.string.chance_of_rain, falloutChanceList[1].chance)
            falloutTime3TextView.text = falloutChanceList[2].time
            falloutChance3ProgressBar.progress = falloutChanceList[2].chance
            falloutChance3TextView.text =
                getString(R.string.chance_of_rain, falloutChanceList[2].chance)
            falloutTime4TextView.text = falloutChanceList[3].time
            falloutChance4ProgressBar.progress = falloutChanceList[3].chance
            falloutChance4TextView.text =
                getString(R.string.chance_of_rain, falloutChanceList[3].chance)

            sunRiseTextView.text =
                LocalDateTime.parse(sunrise, timeFormatter).format(toTimeFormatter)
            sunSetTextView.text = LocalDateTime.parse(sunset, timeFormatter).format(toTimeFormatter)

            daysForecastAdapter.submitList(getDayForecastList())
        }
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
            viewModel.getConditionIconId(weatherJson.current.condition.code, dayOrNight)

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
                val conditionIconId =
                    viewModel.getConditionIconId(hourJson.condition.code, dayOrNightHour)
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
                val conditionIconId =
                    viewModel.getConditionIconId(hourJson.condition.code, dayOrNightHour)
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

    private fun getFalloutChanceHourly(): List<FalloutChanceHourly> {

        val resultList = mutableListOf<FalloutChanceHourly>()

        val todayHours = currentWeatherJson.forecast.forecastdays[0].hour
        val startIdx = todayHours.indexOfFirst {
            it.time_epoch > currentWeatherJson.current.last_updated_epoch
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
            val tomorrowHours = currentWeatherJson.forecast.forecastdays[1].hour
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

    private fun getDayForecastList(): List<DayForecast> {

        val resultList = mutableListOf<DayForecast>()

        val daysForecastList = currentWeatherJson.forecast.forecastdays

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
            val conditionIconId = viewModel.getConditionIconId(condition.code, "day")
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
}