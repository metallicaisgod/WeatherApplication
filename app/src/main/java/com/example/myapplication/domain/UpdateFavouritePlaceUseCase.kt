package com.example.myapplication.domain

class UpdateFavouritePlaceUseCase(private val repository: WeatherRepository) {

    suspend operator fun invoke(place: FavouritePlace) {
        repository.updateFavouritePlace(place)
    }
}