package com.example.myapplication.domain

import kotlinx.coroutines.flow.Flow

interface WeatherRepository {

    suspend fun getWeatherByLocation(location: String): Flow<WeatherJson>
}