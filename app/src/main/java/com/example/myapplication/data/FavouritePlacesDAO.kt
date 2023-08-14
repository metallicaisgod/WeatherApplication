package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.domain.FavouritePlace
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouritePlacesDAO {

    @Update
    suspend fun updateFavouritePlace(place: FavouritePlace)

    @Insert
    suspend fun addFavouritePlace(place: FavouritePlace): Long

    @Delete
    suspend fun deleteFavouritePlace(place: FavouritePlace)

    @Query("select exists (select * from favourite_places where latitude ==:lat and longitude ==:lon )")
    suspend fun checkFavouritePlaceByName(lat: String, lon: String): Boolean

    @Query("select * from favourite_places")
    fun getAllFavouritePlaces(): Flow<List<FavouritePlace>>

    @Query("select * from favourite_places where id ==:placeId")
    suspend fun getFavouritePlaceById(placeId: Long): FavouritePlace
}