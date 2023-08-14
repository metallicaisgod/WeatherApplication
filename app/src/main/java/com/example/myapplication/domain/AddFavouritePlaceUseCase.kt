package com.example.myapplication.domain

class AddFavouritePlaceUseCase(private val repository: WeatherRepository) {

    suspend operator fun invoke(place: FavouritePlace): Long {
        return repository.addFavouritePlace(place)
    }
}