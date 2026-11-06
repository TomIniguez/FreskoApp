package com.example.freskoapp.presentation.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.Location

@StringRes
fun Location.labelRes(): Int = when (this) {
    Location.HELADERA -> R.string.location_heladera
    Location.FREEZER -> R.string.location_freezer
    Location.ALACENA -> R.string.location_alacena
}

@DrawableRes
fun Location.iconRes(): Int = when (this) {
    Location.HELADERA -> R.drawable.ic_fridge
    Location.FREEZER -> R.drawable.ic_snowflake
    Location.ALACENA -> R.drawable.ic_shelves
}
