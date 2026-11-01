package com.example.freskoapp.presentation.inventory

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.InventoryFilter
import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.presentation.common.FreskoIcon
import com.example.freskoapp.presentation.common.GroupCard
import com.example.freskoapp.presentation.common.IconLabelButton
import com.example.freskoapp.presentation.common.NotificationPermissionCard
import com.example.freskoapp.presentation.common.StateMessage
import com.example.freskoapp.presentation.common.WarningBanner
import com.example.freskoapp.presentation.common.color
import com.example.freskoapp.presentation.common.containerColor
import com.example.freskoapp.presentation.common.expiryText
import com.example.freskoapp.presentation.common.filterLabelRes
import com.example.freskoapp.presentation.common.iconRes
import com.example.freskoapp.presentation.common.labelRes
import com.example.freskoapp.presentation.common.onContainerColor
import com.example.freskoapp.presentation.common.rememberNotificationPermissionState
import com.example.freskoapp.presentation.common.shortDate
import com.example.freskoapp.presentation.theme.statusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAdd: (scan: Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onProductClick: (Long) -> Unit,
    viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val promptDismissed by viewModel.notificationPromptDismissed.collectAsStateWithLifecycle()
    val removal by viewModel.pendingRemoval.collectAsStateWithLifecycle()
    val permission = rememberNotificationPermissionState()
    val snackbar = remember { SnackbarHostState() }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // Resolved before the effect: stringResource can't be called inside it.
    val removalMessage = removal?.let {
        when {
            !it.closed -> pluralStringResource(R.plurals.removal_consumed_partial, it.units, it.name, it.units)
            it.status == ProductStatus.CONSUMED -> stringResource(R.string.removal_consumed, it.name)
            else -> stringResource(R.string.removal_discarded, it.name)
        }
    }
    val undoLabel = stringResource(R.string.undo)
    val lotMerged by viewModel.lotMerged.collectAsStateWithLifecycle()
    val lotMergedMessage = stringResource(R.string.lot_merged)
    LaunchedEffect(lotMerged) {
        if (!lotMerged) return@LaunchedEffect
        snackbar.showSnackbar(lotMergedMessage)
        // Single use: cleared once shown so rotation or coming back does not show it again.
        viewModel.onLotMergedShown()
    }
    LaunchedEffect(removal) {
        if (removal == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = removalMessage.orEmpty(),
            actionLabel = undoLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoRemoval()
        // Cleared only now: clearing earlier changes the key, cancelling the effect and dismissing the snackbar.
        viewModel.onRemovalShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(painterResource(R.drawable.ic_sliders), contentDescription = stringResource(R.string.settings_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product_cd))
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            if (!promptDismissed && !permission.granted) {
                NotificationPermissionCard(
                    state = permission,
                    onDismiss = viewModel::dismissNotificationPrompt,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Filters(filter, viewModel::onAllStatusesSelected, viewModel::onStatusSelected, viewModel::onLocationSelected)
            val list = (content as? InventoryContent.Loaded)?.items
            when {
                content == InventoryContent.Loading -> LoadingList()
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StateMessage(
                        icon = R.drawable.ic_alert_circle,
                        circleColor = MaterialTheme.colorScheme.errorContainer,
                        iconTint = MaterialTheme.colorScheme.onErrorContainer,
                        title = stringResource(R.string.inventory_error_title),
                        body = stringResource(R.string.inventory_error_body),
                    ) {
                        IconLabelButton(R.drawable.ic_refresh, stringResource(R.string.retry), viewModel::retry)
                    }
                }
                list.isEmpty() && filter.isActive -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StateMessage(
                        icon = R.drawable.ic_search,
                        circleColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        title = stringResource(R.string.inventory_no_results),
                        body = noResultsText(filter),
                    ) {
                        TextButton(onClick = viewModel::clearFilters) { Text(stringResource(R.string.clear_filters)) }
                    }
                }
                list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StateMessage(
                        icon = R.drawable.ic_box,
                        circleColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        title = stringResource(R.string.inventory_empty),
                        body = stringResource(R.string.inventory_empty_body),
                    ) {
                        IconLabelButton(R.drawable.ic_plus, stringResource(R.string.inventory_add_first), { showAddSheet = true })
                    }
                }
                else -> ProductList(
                    items = list,
                    filter = filter,
                    fromAlert = viewModel.openedFromAlert && filter.status == ExpirationStatus.EXPIRING_SOON,
                    onShowAll = viewModel::onAllStatusesSelected,
                    onProductClick = onProductClick,
                )
            }
        }
    }

    if (showAddSheet) {
        AddMethodSheet(
            onDismiss = { showAddSheet = false },
            onPick = { scan -> showAddSheet = false; onAdd(scan) },
        )
    }
}

// Two wrapping rows (status, then location) so every filter is visible without scrolling sideways.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filters(
    filter: InventoryFilter,
    onAll: () -> Unit,
    onStatus: (ExpirationStatus) -> Unit,
    onLocation: (Location) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter.status == null,
                onClick = onAll,
                label = { Text(stringResource(R.string.filter_all)) },
                leadingIcon = if (filter.status == null) ({ CheckIcon() }) else null,
            )
            ExpirationStatus.entries.forEach { status ->
                FilterChip(
                    selected = filter.status == status,
                    onClick = { onStatus(status) },
                    label = { Text(stringResource(status.filterLabelRes())) },
                    leadingIcon = if (filter.status == status) ({ CheckIcon() }) else null,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Location.entries.forEach { location ->
                val selected = filter.location == location
                FilterChip(
                    selected = selected,
                    onClick = { onLocation(location) },
                    label = { Text(stringResource(location.labelRes())) },
                    leadingIcon = { if (selected) CheckIcon() else FreskoIcon(location.iconRes(), size = 18.dp) },
                )
            }
        }
    }
}

@Composable
private fun CheckIcon() = FreskoIcon(R.drawable.ic_check, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 18.dp)

@Composable
private fun ProductList(
    items: List<InventoryItem>,
    filter: InventoryFilter,
    fromAlert: Boolean,
    onShowAll: () -> Unit,
    onProductClick: (Long) -> Unit,
) {
    // Bottom padding keeps the last card clear of the FAB.
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (fromAlert) {
            item {
                WarningBanner(
                    icon = R.drawable.ic_info,
                    text = stringResource(R.string.inventory_from_alert),
                    actionLabel = stringResource(R.string.inventory_show_all),
                    onAction = onShowAll,
                    container = MaterialTheme.statusColors.successContainer,
                    content = MaterialTheme.statusColors.onSuccessContainer,
                )
            }
        }
        item {
            Text(
                summaryText(items, filter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(items, key = { it.product.id }) { ProductCard(it, onClick = { onProductClick(it.product.id) }) }
    }
}

@Composable
private fun ProductCard(item: InventoryItem, onClick: () -> Unit) {
    val p = item.product
    GroupCard(Modifier.clip(MaterialTheme.shapes.large).clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(p.location.iconRes(), item.expirationStatus)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FreskoIcon(item.expirationStatus.iconRes(), tint = item.expirationStatus.color(), size = 14.dp)
                    Text(
                        expiryText(item.expirationStatus, p.expirationDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = item.expirationStatus.color(),
                    )
                }
                Text(
                    stringResource(
                        R.string.inventory_item_meta,
                        stringResource(p.location.labelRes()),
                        pluralStringResource(R.plurals.units_count, p.quantity, p.quantity),
                        p.expirationDate.shortDate(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FreskoIcon(R.drawable.ic_chevron_right, size = 20.dp)
        }
    }
}

// Location icon on a tile tinted by the expiration status.
@Composable
private fun IconTile(@DrawableRes icon: Int, status: ExpirationStatus) {
    Box(
        Modifier.size(40.dp).background(status.containerColor(), MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) { FreskoIcon(icon, tint = status.onContainerColor(), size = 20.dp) }
}

// Placeholder cards while Room delivers the first list (usually a few milliseconds).
@Composable
private fun LoadingList() {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.inventory_loading),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val bone = MaterialTheme.colorScheme.surfaceContainerHigh
        repeat(6) {
            GroupCard {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(bone, MaterialTheme.shapes.medium))
                    Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.width(140.dp).height(12.dp).background(bone, CircleShape))
                        Box(Modifier.width(96.dp).height(10.dp).background(bone, CircleShape))
                        Box(Modifier.width(180.dp).height(10.dp).background(bone, CircleShape))
                    }
                }
            }
        }
    }
}

// "7 productos · 1 vencido · 3 por vencer", or "3 productos por vencer" when a status filter is on.
@Composable
private fun summaryText(items: List<InventoryItem>, filter: InventoryFilter): String {
    val status = filter.status
    if (status != null) {
        return pluralStringResource(R.plurals.summary_filtered, items.size, items.size, stringResource(status.noResultsRes()))
    }
    val expired = items.count { it.expirationStatus == ExpirationStatus.EXPIRED }
    val soon = items.count { it.expirationStatus == ExpirationStatus.EXPIRING_SOON }
    return listOfNotNull(
        pluralStringResource(R.plurals.summary_products, items.size, items.size),
        if (expired > 0) pluralStringResource(R.plurals.summary_expired, expired, expired) else null,
        if (soon > 0) pluralStringResource(R.plurals.summary_expiring_soon, soon, soon) else null,
    ).joinToString(stringResource(R.string.summary_separator))
}

@Composable
private fun noResultsText(filter: InventoryFilter): String {
    val status = filter.status?.let { stringResource(it.noResultsRes()) }
    val location = filter.location?.let { stringResource(it.noResultsRes()) }
    return when {
        status != null && location != null -> stringResource(R.string.inventory_no_results_both, status, location)
        status != null -> stringResource(R.string.inventory_no_results_status, status)
        else -> stringResource(R.string.inventory_no_results_location, location.orEmpty())
    }
}

private fun ExpirationStatus.noResultsRes() = when (this) {
    ExpirationStatus.EXPIRED -> R.string.no_results_status_expired
    ExpirationStatus.EXPIRING_SOON -> R.string.no_results_status_expiring_soon
    ExpirationStatus.FRESH -> R.string.no_results_status_fresh
}

private fun Location.noResultsRes() = when (this) {
    Location.HELADERA -> R.string.no_results_location_heladera
    Location.FREEZER -> R.string.no_results_location_freezer
    Location.ALACENA -> R.string.no_results_location_alacena
}

// "Agregar producto": scanning is optional, manual entry always works.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMethodSheet(onDismiss: () -> Unit, onPick: (scan: Boolean) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.add_sheet_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            SheetOption(R.drawable.ic_scan_barcode, R.string.add_sheet_scan_title, R.string.add_sheet_scan_body) { onPick(true) }
            SheetOption(R.drawable.ic_pencil, R.string.add_sheet_manual_title, R.string.add_sheet_manual_body) { onPick(false) }
        }
    }
}

@Composable
private fun SheetOption(@DrawableRes icon: Int, title: Int, body: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) { FreskoIcon(icon, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 20.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FreskoIcon(R.drawable.ic_chevron_right, size = 20.dp)
    }
    Spacer(Modifier.height(4.dp))
}
