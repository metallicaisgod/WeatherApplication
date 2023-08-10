package com.example.myapplication.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.DailyForecastCardViewBinding

class DaysForecastAdapter :
    ListAdapter<DayForecast, DaysForecastAdapter.DaysForecastViewHolder>(DaysForecastDiffCallback()) {

    class DaysForecastViewHolder(
        val binding: DailyForecastCardViewBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DaysForecastViewHolder {
        val binding = DailyForecastCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DaysForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DaysForecastViewHolder, position: Int) {
        val dayForecast = getItem(position)

        with(holder.binding){
            dayForecastTextView.text = dayForecast.day
            conditionForecastTextView.text = dayForecast.conditionText
            conditionForecastImageView.setImageResource(dayForecast.conditionIconId)
            tempMaxForecastTextView.text = dayForecast.maxTemp
            tempMinForecastTextView.text = dayForecast.minTemp
        }
    }
}