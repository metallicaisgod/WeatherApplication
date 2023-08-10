package com.example.myapplication.presentation

import androidx.recyclerview.widget.DiffUtil
import com.example.myapplication.domain.HourJson

class HourlyWeatherDiffCallback: DiffUtil.ItemCallback<HourForecast>() {

    override fun areItemsTheSame(oldItem: HourForecast, newItem: HourForecast): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: HourForecast, newItem: HourForecast): Boolean {
        return oldItem == newItem
    }
}