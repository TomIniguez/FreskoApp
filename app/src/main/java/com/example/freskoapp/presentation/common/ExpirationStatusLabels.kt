package com.example.freskoapp.presentation.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.presentation.theme.statusColors

@StringRes
fun ExpirationStatus.labelRes(): Int = when (this) {
    ExpirationStatus.EXPIRED -> R.string.status_expired
    ExpirationStatus.EXPIRING_SOON -> R.string.status_expiring_soon
    ExpirationStatus.FRESH -> R.string.status_fresh
}

@Composable
fun ExpirationStatus.color() = when (this) {
    ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.error
    ExpirationStatus.EXPIRING_SOON -> MaterialTheme.statusColors.warning
    ExpirationStatus.FRESH -> MaterialTheme.statusColors.success
}

fun ExpirationStatus.icon(): ImageVector = when (this) {
    ExpirationStatus.EXPIRED -> Icons.Default.Warning
    ExpirationStatus.EXPIRING_SOON -> Icons.Default.DateRange
    ExpirationStatus.FRESH -> Icons.Default.CheckCircle
}
