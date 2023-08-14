package com.example.myapplication.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FavouritesCardViewBinding
import com.example.myapplication.domain.FavouritePlace

class FavouritesAdapter :
    ListAdapter<FavouritePlace, FavouritesAdapter.FavouritesViewHolder>(FavouritesDiffCallback()) {

    var onFavouritePlaceClickListener: ((FavouritePlace) -> Unit)? = null

    class FavouritesViewHolder(
        val binding: FavouritesCardViewBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouritesViewHolder {
        val binding = FavouritesCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavouritesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouritesViewHolder, position: Int) {
        val place = getItem(position)

        with(holder.binding) {
            favouritesTextView.text = place.place_name
            conditionFavouritesTextView.text = place.conditionText
            conditionFavouritesImageView.setImageResource(
                place.conditionIconId ?: R.drawable.day_113
            )
            tempFavouritesTextView.text = place.current_temp
        }
        holder.itemView.setOnClickListener {
            onFavouritePlaceClickListener?.invoke(place)
        }
    }
}