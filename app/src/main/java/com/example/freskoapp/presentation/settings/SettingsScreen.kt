package com.example.freskoapp.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.freskoapp.domain.model.PilotSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.presentation.common.NotificationPermissionCard
import com.example.freskoapp.presentation.common.rememberNotificationPermissionState

private val WARNING_DAYS_OPTIONS = listOf(1, 2, 3, 5, 7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val warningDays by viewModel.warningDays.collectAsStateWithLifecycle()
    val summary by viewModel.pilotSummary.collectAsStateWithLifecycle()
    val permission = rememberNotificationPermissionState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            Text(stringResource(R.string.settings_warning_days_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.settings_warning_days_description,
                    pluralStringResource(R.plurals.days_count, warningDays, warningDays),
                )
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                WARNING_DAYS_OPTIONS.forEachIndexed { index, days ->
                    SegmentedButton(
                        selected = warningDays == days,
                        onClick = { viewModel.onWarningDaysSelected(days) },
                        shape = SegmentedButtonDefaults.itemShape(index, WARNING_DAYS_OPTIONS.size),
                    ) { Text(days.toString()) }
                }
            }
            Text(stringResource(R.string.settings_notifications_title), style = MaterialTheme.typography.titleMedium)
            if (permission.granted) {
                Text(stringResource(R.string.notifications_enabled))
            } else {
                NotificationPermissionCard(permission, onDismiss = null)
            }
            PilotSection(summary, onExport = {
                scope.launch { context.sharePilotCsv(viewModel.pilotCsv()) }
            })
            // Required by the ODbL license of the product data used to suggest names.
            Text(stringResource(R.string.settings_attribution), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PilotSection(summary: PilotSummary, onExport: () -> Unit) {
    Text(stringResource(R.string.pilot_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.pilot_added, summary.addedByScan, summary.addedManually))
    Text(
        stringResource(
            R.string.pilot_median_add_time,
            summary.medianAddTimeMs?.let { stringResource(R.string.pilot_seconds, (it + 500) / 1000) }
                ?: stringResource(R.string.pilot_no_data),
        )
    )
    Text(stringResource(R.string.pilot_units_consumed, summary.unitsConsumed))
    Text(stringResource(R.string.pilot_units_discarded, summary.unitsDiscarded))
    Text(stringResource(R.string.pilot_units_consumed_expiring, summary.unitsConsumedExpiringSoon))
    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.pilot_export)) }
}

// Writes the CSV to the app cache (overwritten on every export) and opens the share sheet through the FileProvider.
private suspend fun Context.sharePilotCsv(csv: String) {
    val file = withContext(Dispatchers.IO) {
        File(cacheDir, "pilot").apply { mkdirs() }.resolve("fresko_piloto.csv").apply { writeText(csv) }
    }
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(send, getString(R.string.pilot_export)))
}
