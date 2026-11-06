package com.example.freskoapp.presentation.navigation

import android.net.Uri
import androidx.core.net.toUri
import com.example.freskoapp.domain.model.ExpirationStatus

const val INVENTORY_STATUS_ARG = "status"
const val INVENTORY_ROUTE = "inventory?status={status}"
const val INVENTORY_DEEP_LINK_PATTERN = "fresko://inventory?status={status}"

// Used by the notification to open the inventory with a status chip preselected.
fun inventoryDeepLink(status: ExpirationStatus): Uri = "fresko://inventory?status=${status.name}".toUri()
