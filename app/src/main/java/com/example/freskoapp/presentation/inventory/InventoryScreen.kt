package com.example.freskoapp.presentation.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.presentation.common.labelRes
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAddClick: () -> Unit,
    viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product_cd))
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ExpirationStatus.entries) { status ->
                    FilterChip(
                        selected = filter.status == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(stringResource(status.labelRes())) },
                    )
                }
                items(Location.entries) { location ->
                    FilterChip(
                        selected = filter.location == location,
                        onClick = { viewModel.onLocationSelected(location) },
                        label = { Text(stringResource(location.labelRes())) },
                    )
                }
                if (filter.isActive) {
                    item {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text(stringResource(R.string.clear_filters))
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(
                            if (filter.isActive) R.string.inventory_empty_filtered else R.string.inventory_empty
                        )
                    )
                }
            } else {
                LazyColumn {
                    items(items, key = { it.product.id }) { item ->
                        val p = item.product
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string.inventory_item_detail,
                                        p.expirationDate.format(dateFormat),
                                        stringResource(p.location.labelRes()),
                                        p.quantity,
                                    )
                                )
                            },
                            trailingContent = {
                                Text(
                                    stringResource(item.expirationStatus.labelRes()),
                                    color = item.expirationStatus.color(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpirationStatus.color() = when (this) {
    ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.error
    ExpirationStatus.EXPIRING_SOON -> MaterialTheme.colorScheme.tertiary
    ExpirationStatus.FRESH -> MaterialTheme.colorScheme.primary
}
