package com.example.freskoapp.presentation.common

import androidx.annotation.StringRes
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus

@StringRes
fun ExpirationStatus.labelRes(): Int = when (this) {
    ExpirationStatus.EXPIRED -> R.string.status_expired
    ExpirationStatus.EXPIRING_SOON -> R.string.status_expiring_soon
    ExpirationStatus.FRESH -> R.string.status_fresh
}
