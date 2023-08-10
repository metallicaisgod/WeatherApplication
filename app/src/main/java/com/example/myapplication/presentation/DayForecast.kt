package com.example.myapplication.presentation

data class DayForecast(
    val day: String,
    val conditionText: String,
    val minTemp: String,
    val maxTemp: String,
    val conditionIconId: Int
)
