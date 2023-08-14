package com.example.myapplication.presentation

sealed class PlaceStatus{

    object Favourite: PlaceStatus()
    object NotFavourite: PlaceStatus()
}
