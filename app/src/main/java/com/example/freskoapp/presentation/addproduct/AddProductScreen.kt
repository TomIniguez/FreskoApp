package com.example.freskoapp.presentation.addproduct

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.presentation.common.FieldLabel
import com.example.freskoapp.presentation.common.FieldMessage
import com.example.freskoapp.presentation.common.FreskoIcon
import com.example.freskoapp.presentation.common.IconLabelButton
import com.example.freskoapp.presentation.common.StateMessage
import com.example.freskoapp.presentation.common.Stepper
import com.example.freskoapp.presentation.common.WarningBanner
import com.example.freskoapp.presentation.common.iconRes
import com.example.freskoapp.presentation.common.labelRes
import com.example.freskoapp.presentation.theme.statusColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private enum class CameraPermissionNotice { DENIED, PERMANENTLY_DENIED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    onSaved: (merged: Boolean) -> Unit,
    onUnavailable: () -> Unit,
    onScan: () -> Unit,
    viewModel: AddProductViewModel = viewModel(factory = AddProductViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var cameraNotice by rememberSaveable { mutableStateOf<CameraPermissionNotice?>(null) }
    val startScan = rememberScanStarter(onScan, onDenied = { cameraNotice = it })

    LaunchedEffect(state.saved) { if (state.saved) onSaved(state.merged) }
    LaunchedEffect(state.notAvailable) { if (state.notAvailable) onUnavailable() }
    // "Escanear" picked in the add sheet: go straight to the camera (once).
    LaunchedEffect(Unit) { if (viewModel.takeStartScan() && hasCamera) startScan() }

    val cameraUnavailable by viewModel.cameraUnavailableNotice.collectAsStateWithLifecycle()
    val cameraUnavailableMessage = stringResource(R.string.camera_unavailable)
    LaunchedEffect(cameraUnavailable) {
        if (!cameraUnavailable) return@LaunchedEffect
        snackbar.showSnackbar(cameraUnavailableMessage)
        // Single use: cleared once shown so rotation or coming back does not show it again.
        viewModel.onCameraUnavailableShown()
    }

    val notice = cameraNotice
    if (notice != null) {
        CameraOffScreen(notice, onManual = { cameraNotice = null })
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.edit_product_title else R.string.add_product_title)) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            // Always tappable: with missing fields, a tap shows what is missing instead of doing nothing.
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
            ) { Text(stringResource(if (state.isEditing) R.string.save else R.string.save_product)) }
        },
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!state.isEditing) {
                ScanHeader(state.lookup, state.scannedBarcode, hasCamera, onScan = startScan, onRetry = viewModel::retry)
            }
            NameField(state, viewModel::onNameChange)
            DateField(state, onClick = { showDatePicker = true }, onShortcut = viewModel::onDateSelected)
            FieldLabel(stringResource(R.string.field_quantity))
            val quantity = state.quantity.toIntOrNull() ?: 1
            Stepper(
                value = quantity,
                onDecrease = { viewModel.onQuantityStep(-1) },
                onIncrease = { viewModel.onQuantityStep(1) },
                canDecrease = quantity > 1,
                canIncrease = quantity < AddProductViewModel.MAX_QUANTITY,
            )
            FieldLabel(stringResource(R.string.field_location))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Location.entries.forEach { location ->
                    val selected = state.location == location
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onLocationChange(location) },
                        label = { Text(stringResource(location.labelRes())) },
                        leadingIcon = {
                            FreskoIcon(if (selected) R.drawable.ic_check else location.iconRes(), size = 18.dp)
                        },
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.expirationDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // selectedDateMillis is UTC midnight, so convert in UTC to avoid an off-by-one day
                    pickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.back_cd))
    }
}

// Before a scan: the "Escanear código de barras" button. After: the lookup status and the scanned code.
@Composable
private fun ScanHeader(lookup: LookupState, barcode: String?, hasCamera: Boolean, onScan: () -> Unit, onRetry: () -> Unit) {
    if (barcode == null) {
        if (hasCamera) {
            OutlinedButton(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(painterResource(R.drawable.ic_scan_barcode), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.scan_barcode), modifier = Modifier.padding(start = 8.dp))
            }
        }
        return
    }
    when (lookup) {
        LookupState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.lookup_loading), style = MaterialTheme.typography.bodyMedium)
        }
        is LookupState.Failure -> WarningBanner(
            icon = if (lookup.kind == LookupFailure.OFFLINE) R.drawable.ic_wifi_off else R.drawable.ic_search,
            text = stringResource(
                when (lookup.kind) {
                    LookupFailure.NOT_FOUND -> R.string.lookup_not_found
                    LookupFailure.TIMEOUT -> R.string.lookup_timeout
                    LookupFailure.SERVER_ERROR -> R.string.lookup_server_error
                    LookupFailure.OFFLINE -> R.string.lookup_offline
                }
            ),
            actionLabel = stringResource(R.string.retry),
            onAction = onRetry,
        )
        LookupState.Idle, is LookupState.Found -> {}
    }
    // Tapping the code scans again (wrong product, or a better code).
    AssistChip(
        onClick = { if (hasCamera) onScan() },
        label = { Text(stringResource(R.string.scanned_barcode, barcode)) },
        leadingIcon = { FreskoIcon(R.drawable.ic_scan_barcode, size = 16.dp) },
    )
}

@Composable
private fun NameField(state: AddProductUiState, onChange: (String) -> Unit) {
    FieldLabel(stringResource(R.string.field_name_required), isError = state.nameError)
    OutlinedTextField(
        value = state.name,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.field_name_placeholder)) },
        singleLine = true,
        isError = state.nameError,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    val found = state.lookup as? LookupState.Found
    when {
        state.nameError -> FieldMessage(R.drawable.ic_alert_circle, stringResource(R.string.error_name_required), MaterialTheme.colorScheme.error)
        found != null && state.name == found.name ->
            FieldMessage(R.drawable.ic_check_circle, stringResource(R.string.hint_autocompleted), MaterialTheme.statusColors.success)
    }
}

// Read-only field that opens the date picker, plus quick shortcuts from today.
@Composable
private fun DateField(state: AddProductUiState, onClick: () -> Unit, onShortcut: (LocalDate) -> Unit) {
    FieldLabel(stringResource(R.string.field_date_required), isError = state.dateError)
    Box {
        OutlinedTextField(
            value = state.expirationDate?.format(dateFormat).orEmpty(),
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(stringResource(R.string.field_date_placeholder)) },
            trailingIcon = { FreskoIcon(R.drawable.ic_calendar, size = 20.dp) },
            isError = state.dateError,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        // The text field can't open a dialog on tap by itself; this layer does, and reads as a button.
        Box(
            Modifier.matchParentSize().clickable(
                onClickLabel = stringResource(R.string.field_date_cd),
                role = Role.Button,
                onClick = onClick,
            )
        )
    }
    when {
        state.dateError -> FieldMessage(R.drawable.ic_alert_circle, stringResource(R.string.error_date_required), MaterialTheme.colorScheme.error)
        state.lookup is LookupState.Found && state.expirationDate == null ->
            FieldMessage(null, stringResource(R.string.hint_date_missing), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val today = LocalDate.now()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            R.string.date_plus_3_days to today.plusDays(3),
            R.string.date_plus_1_week to today.plusWeeks(1),
            R.string.date_plus_1_month to today.plusMonths(1),
            R.string.date_plus_3_months to today.plusMonths(3),
        ).forEach { (label, date) ->
            FilterChip(
                selected = state.expirationDate == date,
                onClick = { onShortcut(date) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

// B3: shown instead of the form when the camera permission was refused; manual entry is one tap away.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraOffScreen(notice: CameraPermissionNotice, onManual: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                navigationIcon = { BackButton(onManual) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            StateMessage(
                icon = R.drawable.ic_camera_off,
                circleColor = MaterialTheme.statusColors.warningContainer,
                iconTint = MaterialTheme.statusColors.onWarningContainer,
                title = stringResource(R.string.camera_off_title),
                body = stringResource(R.string.camera_off_body),
            ) {
                IconLabelButton(R.drawable.ic_pencil, stringResource(R.string.scanner_manual), onManual)
                // After "don't ask again" the dialog never returns: system settings is the only way back.
                if (notice == CameraPermissionNotice.PERMANENTLY_DENIED) {
                    TextButton(onClick = { context.openAppSettings() }) { Text(stringResource(R.string.camera_off_settings)) }
                }
            }
        }
    }
}

private fun Context.openAppSettings() = startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
)

// Checks the camera permission and then opens the scanner; a refusal is reported through onDenied.
@Composable
private fun rememberScanStarter(onScan: () -> Unit, onDenied: (CameraPermissionNotice) -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnScan by rememberUpdatedState(onScan)
    val currentOnDenied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> currentOnScan()
            // Checked only after a denial: before the first request it is also false, which would be ambiguous.
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true ->
                currentOnDenied(CameraPermissionNotice.DENIED)
            else -> currentOnDenied(CameraPermissionNotice.PERMANENTLY_DENIED)
        }
    }
    return remember(launcher) {
        {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                currentOnScan()
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
