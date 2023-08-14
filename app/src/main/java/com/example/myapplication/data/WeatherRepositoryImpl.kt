package com.example.myapplication.data

import android.app.Application
import com.example.myapplication.domain.FavouritePlace
import com.example.myapplication.domain.WeatherJson
import com.example.myapplication.domain.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class WeatherRepositoryImpl(application: Application) : WeatherRepository {

    private val favouritePlacesDAO =
        FavouritePlacesDataBase.geInstance(application).getFavouritePlacesDAO()

    override suspend fun getWeatherByLocation(location: String): Flow<WeatherJson> {
        return flow {
            val r = RetrofitInstance.getService().getWeatherByLocation(location)
            emit(r)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateFavouritePlace(place: FavouritePlace) {
        favouritePlacesDAO.updateFavouritePlace(place)
    }

    override suspend fun addFavouritePlace(place: FavouritePlace): Long {
        return favouritePlacesDAO.addFavouritePlace(place)
    }

    override suspend fun deleteFavouritePlace(place: FavouritePlace) {
        favouritePlacesDAO.deleteFavouritePlace(place)
    }

    override suspend fun checkFavouritePlaceByName(lat: String, lon: String): Boolean {
        return favouritePlacesDAO.checkFavouritePlaceByName(lat, lon)
    }

    override fun getAllFavouritePlaces(): Flow<List<FavouritePlace>> {
        return favouritePlacesDAO.getAllFavouritePlaces()
    }

    override suspend fun getFavouritePlaceById(id: Long): FavouritePlace {
        return favouritePlacesDAO.getFavouritePlaceById(id)
    }
}