package com.example.freskoapp.presentation.addproduct

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.presentation.common.labelRes
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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

    LaunchedEffect(state.saved) { if (state.saved) onSaved(state.merged) }
    LaunchedEffect(state.notAvailable) { if (state.notAvailable) onUnavailable() }

    val cameraUnavailable by viewModel.cameraUnavailableNotice.collectAsStateWithLifecycle()
    val cameraUnavailableMessage = stringResource(R.string.camera_unavailable)
    LaunchedEffect(cameraUnavailable) {
        if (!cameraUnavailable) return@LaunchedEffect
        snackbar.showSnackbar(cameraUnavailableMessage)
        // Single use: cleared once shown so rotation or coming back does not show it again.
        viewModel.onCameraUnavailableShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.edit_product_title else R.string.add_product_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!state.isEditing) BarcodeScanSection(state.lookup, state.scannedBarcode, onScan = onScan, onRetry = viewModel::retry)
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.expirationDate?.format(dateFormat) ?: stringResource(R.string.pick_date))
            }
            OutlinedTextField(
                value = state.quantity,
                onValueChange = viewModel::onQuantityChange,
                label = { Text(stringResource(R.string.field_quantity)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.field_location))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Location.entries.forEachIndexed { index, location ->
                    SegmentedButton(
                        selected = state.location == location,
                        onClick = { viewModel.onLocationChange(location) },
                        shape = SegmentedButtonDefaults.itemShape(index, Location.entries.size),
                    ) { Text(stringResource(location.labelRes())) }
                }
            }
            Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
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

private enum class CameraPermissionNotice { DENIED, PERMANENTLY_DENIED }

// "Escanear código" plus the lookup status; nothing here blocks the rest of the form.
@Composable
private fun BarcodeScanSection(lookup: LookupState, barcode: String?, onScan: () -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var notice by rememberSaveable { mutableStateOf<CameraPermissionNotice?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notice = when {
            granted -> null
            // Checked only after a denial: before the first request it is also false, which would be ambiguous.
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true -> CameraPermissionNotice.DENIED
            else -> CameraPermissionNotice.PERMANENTLY_DENIED
        }
        if (granted) onScan()
    }

    if (hasCamera) {
        OutlinedButton(
            onClick = {
                notice = null
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) onScan()
                else launcher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.scan_barcode)) }
    }
    when (notice) {
        CameraPermissionNotice.DENIED -> Text(stringResource(R.string.camera_permission_denied))
        CameraPermissionNotice.PERMANENTLY_DENIED -> {
            Text(stringResource(R.string.camera_permission_permanently_denied))
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }) { Text(stringResource(R.string.open_app_settings)) }
        }
        null -> {}
    }

    if (barcode != null) Text(stringResource(R.string.scanned_barcode, barcode))
    when (lookup) {
        LookupState.Idle -> {}
        LookupState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.lookup_loading))
        }
        is LookupState.Found -> Text(stringResource(R.string.lookup_found, lookup.name))
        is LookupState.Failure -> {
            Text(
                stringResource(
                    when (lookup.kind) {
                        LookupFailure.NOT_FOUND -> R.string.lookup_not_found
                        LookupFailure.TIMEOUT -> R.string.lookup_timeout
                        LookupFailure.SERVER_ERROR -> R.string.lookup_server_error
                        LookupFailure.OFFLINE -> R.string.lookup_offline
                    }
                )
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
