package com.example.myapplication.domain

import kotlinx.coroutines.flow.Flow


class GetAllFavouritePlacesUseCase(private val repository: WeatherRepository) {

    operator fun invoke(): Flow<List<FavouritePlace>> {
        return repository.getAllFavouritePlaces()
    }
}