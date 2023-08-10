package com.example.myapplication.data

import com.example.myapplication.domain.WeatherJson
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherService {

    @GET("forecast.json")
    suspend fun getWeatherByLocation(@Query("q") q: String): WeatherJson
}