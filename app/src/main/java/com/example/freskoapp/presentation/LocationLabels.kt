package com.example.freskoapp.presentation

import androidx.annotation.StringRes
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.Location

@StringRes
fun Location.labelRes(): Int = when (this) {
    Location.HELADERA -> R.string.location_heladera
    Location.FREEZER -> R.string.location_freezer
    Location.ALACENA -> R.string.location_alacena
}
