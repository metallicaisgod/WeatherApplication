package com.example.myapplication.domain

import kotlinx.coroutines.flow.Flow

interface WeatherRepository {

    suspend fun getWeatherByLocation(location: String): Flow<WeatherJson>

    suspend fun updateFavouritePlace(place: FavouritePlace)
    suspend fun addFavouritePlace(place: FavouritePlace): Long

    suspend fun deleteFavouritePlace(place: FavouritePlace)

    suspend fun checkFavouritePlaceByName(lat: String, lon: String): Boolean

    fun getAllFavouritePlaces(): Flow<List<FavouritePlace>>

    suspend fun getFavouritePlaceById(id: Long): FavouritePlace

}