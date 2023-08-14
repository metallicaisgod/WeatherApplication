package com.example.myapplication.domain

class CheckFavouritePlaceUseCase(private val repository: WeatherRepository) {

    suspend operator fun invoke(lat: String, lon: String): Boolean {
        return repository.checkFavouritePlaceByName(lat, lon)
    }
}