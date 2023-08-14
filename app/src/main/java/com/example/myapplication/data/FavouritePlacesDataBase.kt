package com.example.myapplication.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.domain.FavouritePlace

@Database(entities = [FavouritePlace::class], version = 3, exportSchema = false)
abstract class FavouritePlacesDataBase : RoomDatabase() {

    abstract fun getFavouritePlacesDAO(): FavouritePlacesDAO

    companion object {
        private var INSTANCE: FavouritePlacesDataBase? = null
        private val LOCK = Any()
        private const val DB_NAME = "favourite_places.db"

        fun geInstance(application: Application): FavouritePlacesDataBase {
            INSTANCE?.let {
                return it
            }
            synchronized(LOCK) {
                INSTANCE?.let {
                    return it
                }
                val db = Room.databaseBuilder(
                    application,
                    FavouritePlacesDataBase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = db
                return db
            }
        }
    }

}