package com.example.myapplication.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_places", indices = [Index(value = ["place_name", "latitude","longitude"], unique = true)])
data class FavouritePlace(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = UNDEFINED_ID,
    @ColumnInfo(name = "place_name")
    var place_name: String = "",
    @ColumnInfo(name = "latitude")
    var latitude: String = "",
    @ColumnInfo(name = "longitude")
    var longitude: String = "",
    @ColumnInfo(name = "current_temp")
    var current_temp: String? = null,
    @ColumnInfo(name = "condition_text")
    var conditionText: String? = null,
    @ColumnInfo(name = "condition_icon_id")
    var conditionIconId: Int? = null
) {
    companion object {
        const val UNDEFINED_ID = 0L
    }
}
