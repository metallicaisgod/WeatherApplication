package com.example.myapplication.presentation

import androidx.recyclerview.widget.DiffUtil
import com.example.myapplication.domain.FavouritePlace

class FavouritesDiffCallback: DiffUtil.ItemCallback<FavouritePlace>() {

    override fun areItemsTheSame(oldItem: FavouritePlace, newItem: FavouritePlace): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: FavouritePlace, newItem: FavouritePlace): Boolean {
        return oldItem == newItem
    }
}