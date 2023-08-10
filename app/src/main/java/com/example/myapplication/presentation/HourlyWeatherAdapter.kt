package com.example.myapplication.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.HourlyForecastCardViewBinding
import com.example.myapplication.domain.HourJson

class HourlyWeatherAdapter : ListAdapter<HourForecast, HourlyWeatherAdapter.HourlyWeatherViewHolder>(HourlyWeatherDiffCallback()) {

    class HourlyWeatherViewHolder(
        val binding: HourlyForecastCardViewBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyWeatherViewHolder {
        val binding = HourlyForecastCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyWeatherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyWeatherViewHolder, position: Int) {
        val hourForecast = getItem(position)
        with(holder.binding){
            timeHourTextView.text = hourForecast.time
            conditionHourImageView.setImageResource(hourForecast.conditionIcon)
            degreesHourTextView.text= hourForecast.temp
        }
    }
}