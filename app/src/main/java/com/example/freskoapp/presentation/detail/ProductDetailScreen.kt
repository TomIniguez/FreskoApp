package com.example.freskoapp.presentation.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.presentation.common.FreskoIcon
import com.example.freskoapp.presentation.common.GroupCard
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.common.Stepper
import com.example.freskoapp.presentation.common.containerColor
import com.example.freskoapp.presentation.common.expiryText
import com.example.freskoapp.presentation.common.iconRes
import com.example.freskoapp.presentation.common.labelRes
import com.example.freskoapp.presentation.common.onContainerColor
import com.example.freskoapp.presentation.common.shortDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// "jueves 1 de octubre de 2026"; capitalized when shown.
private val longDateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    onRemoved: (ProductRemoval) -> Unit,
    viewModel: ProductDetailViewModel = viewModel(factory = ProductDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.removal) { state.removal?.let(onRemoved) }

    val snackbar = remember { SnackbarHostState() }
    val unavailable by viewModel.unavailableNotice.collectAsStateWithLifecycle()
    val unavailableMessage = stringResource(R.string.product_unavailable)
    LaunchedEffect(unavailable) {
        if (!unavailable) return@LaunchedEffect
        snackbar.showSnackbar(unavailableMessage)
        // Single use: cleared once shown so rotation or coming back does not show it again.
        viewModel.onUnavailableShown()
    }

    val item = state.item
    val active = item?.takeIf { !state.notAvailable && !state.isLoading }
    var showUnits by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    // Closed products show "not available" instead; editing is only for active ones.
                    if (active != null && !state.isProcessing) {
                        IconButton(onClick = { onEdit(active.product.id) }) {
                            Icon(painterResource(R.drawable.ic_pencil), contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (active != null) {
                Actions(
                    enabled = !state.isProcessing,
                    // One unit: nothing to choose, close it straight away.
                    onConsumed = { if (active.product.quantity == 1) viewModel.markConsumed(1) else showUnits = true },
                    onDiscarded = viewModel::markDiscarded,
                )
            }
        },
    ) { innerPadding ->
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
            item != null -> DetailContent(item, Modifier.padding(innerPadding))
        }
    }

    if (showUnits && active != null) {
        UnitsDialog(
            quantity = active.product.quantity,
            onConfirm = { showUnits = false; viewModel.markConsumed(it) },
            onDismiss = { showUnits = false },
        )
    }
}

@Composable
private fun DetailContent(item: InventoryItem, modifier: Modifier) {
    val p = item.product
    val status = item.expirationStatus
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Color + icon + text, so the status never relies on color alone.
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = status.containerColor()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FreskoIcon(status.iconRes(), tint = status.onContainerColor(), size = 24.dp)
                    Text(
                        expiryText(status, p.expirationDate),
                        style = MaterialTheme.typography.titleLarge,
                        color = status.onContainerColor(),
                    )
                }
                Text(
                    p.expirationDate.format(longDateFormat).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = status.onContainerColor(),
                )
            }
        }
        Text(p.name, style = MaterialTheme.typography.headlineSmall)
        GroupCard {
            DataRow(p.location.iconRes(), stringResource(R.string.detail_row_location), stringResource(p.location.labelRes()))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DataRow(R.drawable.ic_box, stringResource(R.string.detail_row_quantity), pluralStringResource(R.plurals.units_count, p.quantity, p.quantity))
            // Tells apart lots with the same expiration date; 0 = unknown (created before v3).
            if (p.createdAt > 0) {
                val createdOn = Instant.ofEpochMilli(p.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DataRow(R.drawable.ic_calendar, stringResource(R.string.detail_row_added), createdOn.shortDate(withYear = true))
            }
        }
    }
}

@Composable
private fun DataRow(@DrawableRes icon: Int, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        FreskoIcon(icon, size = 20.dp)
        Text(
            label,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

// Within thumb reach at the bottom, as in the design.
@Composable
private fun Actions(enabled: Boolean, onConsumed: () -> Unit, onDiscarded: () -> Unit) {
    Column(
        Modifier.navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onConsumed, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_consumed), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = onDiscarded, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_trash), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_discarded), modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            stringResource(R.string.detail_undo_hint),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Picks how many units were consumed: starts at 1, "Todas" jumps to the whole quantity.
@Composable
private fun UnitsDialog(quantity: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var units by rememberSaveable { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.units_dialog_title)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(
                    value = units,
                    onDecrease = { units-- },
                    onIncrease = { units++ },
                    canDecrease = units > 1,
                    canIncrease = units < quantity,
                    valueText = stringResource(R.string.units_of, units, quantity),
                )
                TextButton(onClick = { units = quantity }) { Text(stringResource(R.string.units_all)) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(units) }) { Text(stringResource(R.string.accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
