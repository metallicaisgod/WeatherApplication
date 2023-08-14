package com.example.myapplication.domain

class DeleteFavouritePlaceUseCase (private val repository: WeatherRepository) {

    suspend operator fun invoke(place: FavouritePlace) {
        repository.deleteFavouritePlace(place)
    }
}