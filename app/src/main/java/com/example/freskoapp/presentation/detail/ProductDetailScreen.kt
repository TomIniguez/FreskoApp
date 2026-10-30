package com.example.freskoapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.common.color
import com.example.freskoapp.presentation.common.icon
import com.example.freskoapp.presentation.common.labelRes
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onRemoved: (ProductRemoval) -> Unit,
    viewModel: ProductDetailViewModel = viewModel(factory = ProductDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.removal) { state.removal?.let(onRemoved) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
            )
        },
    ) { innerPadding ->
        val item = state.item
        when {
            state.isLoading -> Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.notAvailable -> Column(
                Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.detail_not_available))
                Button(onClick = onBack) { Text(stringResource(R.string.back_cd)) }
            }
            item != null -> DetailContent(
                item = item,
                enabled = !state.isProcessing,
                onConsumed = viewModel::markConsumed,
                onDiscarded = viewModel::markDiscarded,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun DetailContent(
    item: InventoryItem,
    enabled: Boolean,
    onConsumed: () -> Unit,
    onDiscarded: () -> Unit,
    contentPadding: PaddingValues,
) {
    val p = item.product
    Column(
        Modifier.padding(contentPadding).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(p.name, style = MaterialTheme.typography.headlineSmall)
        // Color + icon + text, so the status never relies on color alone.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(item.expirationStatus.icon(), contentDescription = null, tint = item.expirationStatus.color())
            Text(
                stringResource(item.expirationStatus.labelRes()),
                color = item.expirationStatus.color(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(stringResource(R.string.detail_expires_on, p.expirationDate.format(dateFormat)))
        Text(stringResource(R.string.detail_quantity, p.quantity))
        Text(stringResource(R.string.detail_location, stringResource(p.location.labelRes())))
        Text(stringResource(R.string.detail_question), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onConsumed, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_consumed), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = onDiscarded, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_discarded), modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            stringResource(R.string.detail_undo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
