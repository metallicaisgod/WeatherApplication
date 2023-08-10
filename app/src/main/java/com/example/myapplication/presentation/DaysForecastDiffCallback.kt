package com.example.myapplication.presentation

import androidx.recyclerview.widget.DiffUtil
import com.example.myapplication.domain.HourJson

class DaysForecastDiffCallback: DiffUtil.ItemCallback<DayForecast>() {

    override fun areItemsTheSame(oldItem: DayForecast, newItem: DayForecast): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: DayForecast, newItem: DayForecast): Boolean {
        return oldItem == newItem
    }
}