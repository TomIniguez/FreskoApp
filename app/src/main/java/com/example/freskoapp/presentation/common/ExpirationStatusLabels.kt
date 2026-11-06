package com.example.freskoapp.presentation.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.presentation.theme.statusColors

@StringRes
fun ExpirationStatus.labelRes(): Int = when (this) {
    ExpirationStatus.EXPIRED -> R.string.status_expired
    ExpirationStatus.EXPIRING_SOON -> R.string.status_expiring_soon
    ExpirationStatus.FRESH -> R.string.status_fresh
}

// Plural form for the inventory filter chips ("Vencidos", "Vigentes").
@StringRes
fun ExpirationStatus.filterLabelRes(): Int = when (this) {
    ExpirationStatus.EXPIRED -> R.string.filter_expired
    ExpirationStatus.EXPIRING_SOON -> R.string.filter_expiring_soon
    ExpirationStatus.FRESH -> R.string.filter_fresh
}

@Composable
fun ExpirationStatus.color() = when (this) {
    ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.error
    ExpirationStatus.EXPIRING_SOON -> MaterialTheme.statusColors.warning
    ExpirationStatus.FRESH -> MaterialTheme.statusColors.success
}

// Background of the card's icon tile and of the detail status block.
@Composable
fun ExpirationStatus.containerColor() = when (this) {
    ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
    ExpirationStatus.EXPIRING_SOON -> MaterialTheme.statusColors.warningContainer
    ExpirationStatus.FRESH -> MaterialTheme.statusColors.successContainer
}

@Composable
fun ExpirationStatus.onContainerColor() = when (this) {
    ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.onErrorContainer
    ExpirationStatus.EXPIRING_SOON -> MaterialTheme.statusColors.onWarningContainer
    ExpirationStatus.FRESH -> MaterialTheme.statusColors.onSuccessContainer
}

// Color + icon + text, so the status never relies on color alone.
@DrawableRes
fun ExpirationStatus.iconRes(): Int = when (this) {
    ExpirationStatus.EXPIRED -> R.drawable.ic_alert_circle
    ExpirationStatus.EXPIRING_SOON -> R.drawable.ic_clock
    ExpirationStatus.FRESH -> R.drawable.ic_check_circle
}
