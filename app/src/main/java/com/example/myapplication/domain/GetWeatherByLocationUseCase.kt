package com.example.myapplication.domain

import kotlinx.coroutines.flow.Flow

class GetWeatherByLocationUseCase(val repository: WeatherRepository) {

    suspend operator fun invoke(location: String): Flow<WeatherJson> {
        return repository.getWeatherByLocation(location)
    }
}