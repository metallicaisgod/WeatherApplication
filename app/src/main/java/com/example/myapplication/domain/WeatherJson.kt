package com.example.myapplication.domain

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WeatherJson(
    @SerializedName("location")
    @Expose
    val location: LocationJson,
    @SerializedName("current")
    @Expose
    val current: CurrentJson,
    @SerializedName("forecast")
    @Expose
    val forecast: ForecastJson
)

data class LocationJson(
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("country")
    @Expose
    val country: String,
    @SerializedName("lat")
    @Expose
    val lat: Float,
    @SerializedName("lon")
    @Expose
    val lon: Float,
    @SerializedName("tz_id")
    @Expose
    val tz_id: String
)

data class CurrentJson(
    @SerializedName("last_updated_epoch")
    @Expose
    val last_updated_epoch: Long,
    @SerializedName("last_updated")
    @Expose
    val last_updated: String,
    @SerializedName("temp_c")
    @Expose
    val temp_c: Float,
    @SerializedName("condition")
    @Expose
    val condition: ConditionJson,
    @SerializedName("wind_kph")
    @Expose
    val wind_kph: Float,
    @SerializedName("pressure_mb")
    @Expose
    val pressure_mb: Float,
    @SerializedName("feelslike_c")
    @Expose
    val feelslike_c: Float,
    @SerializedName("uv")
    @Expose
    val uv: Float
)

data class ConditionJson(
    @SerializedName("text")
    @Expose
    val text: String,
    @SerializedName("icon")
    @Expose
    val icon: String,
    @SerializedName("code")
    @Expose
    val code: Int
)

data class ForecastJson(
    @SerializedName("forecastday")
    @Expose
    val forecastdays: List<DayWeatherJson>
)

data class DayWeatherJson(
    @SerializedName("date")
    @Expose
    val date: String,
    @SerializedName("date_epoch")
    @Expose
    val date_epoch: Long,
    @SerializedName("day")
    @Expose
    val day: DayJson,
    @SerializedName("astro")
    @Expose
    val astro: AstroJson,
    @SerializedName("hour")
    @Expose
    val hour: List<HourJson>
)

data class DayJson(
    @SerializedName("maxtemp_c")
    @Expose
    val maxtemp_c: Float,
    @SerializedName("mintemp_c")
    @Expose
    val mintemp_c: Float,
    @SerializedName("avgtemp_c")
    @Expose
    val avgtemp_c: Float,
    @SerializedName("maxwind_kph")
    @Expose
    val maxwind_kph: Float,
    @SerializedName("daily_chance_of_rain")
    @Expose
    val daily_chance_of_rain: Int,
    @SerializedName("daily_chance_of_snow")
    @Expose
    val daily_chance_of_snow: Int,
    @SerializedName("condition")
    @Expose
    val condition: ConditionJson,
    @SerializedName("uv")
    @Expose
    val uv: Float
)

data class AstroJson(
    @SerializedName("sunrise")
    @Expose
    val sunrise: String,
    @SerializedName("sunset")
    @Expose
    val sunset: String
)

data class HourJson(
    @SerializedName("time_epoch")
    @Expose
    val time_epoch: Long,
    @SerializedName("time")
    @Expose
    val time: String,
    @SerializedName("temp_c")
    @Expose
    val temp_c: Float,
    @SerializedName("condition")
    @Expose
    val condition: ConditionJson,
    @SerializedName("chance_of_rain")
    @Expose
    val chance_of_rain: Int,
    @SerializedName("chance_of_snow")
    @Expose
    val chance_of_snow: Int
)
