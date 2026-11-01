package com.example.freskoapp.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.PilotSummary
import com.example.freskoapp.domain.usecase.MAX_WARNING_DAYS
import com.example.freskoapp.domain.usecase.MIN_WARNING_DAYS
import com.example.freskoapp.presentation.common.FreskoIcon
import com.example.freskoapp.presentation.common.GroupCard
import com.example.freskoapp.presentation.common.SectionLabel
import com.example.freskoapp.presentation.common.Stepper
import com.example.freskoapp.presentation.common.WarningBanner
import com.example.freskoapp.presentation.common.rememberNotificationPermissionState
import com.example.freskoapp.presentation.theme.statusColors
import com.example.freskoapp.work.ExpirationCheckScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter

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
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // D4: without notifications the whole point of the warning days is lost, so say it up top.
            if (!permission.granted) {
                WarningBanner(
                    icon = R.drawable.ic_bell,
                    text = stringResource(R.string.settings_notifications_off_banner),
                    actionLabel = stringResource(R.string.settings_notifications_activate),
                    onAction = permission.request,
                )
            }
            SectionLabel(stringResource(R.string.settings_section_alerts))
            GroupCard {
                SettingRow(
                    title = stringResource(R.string.settings_warning_days_row),
                    caption = stringResource(R.string.settings_warning_days_caption),
                ) {
                    Stepper(
                        value = warningDays,
                        onDecrease = { viewModel.onWarningDaysSelected(warningDays - 1) },
                        onIncrease = { viewModel.onWarningDaysSelected(warningDays + 1) },
                        canDecrease = warningDays > MIN_WARNING_DAYS,
                        canIncrease = warningDays < MAX_WARNING_DAYS,
                    )
                }
                Divider()
                SettingRow(
                    title = stringResource(R.string.settings_notifications_title),
                    caption = stringResource(
                        if (permission.granted) R.string.settings_notifications_on else R.string.settings_notifications_off
                    ),
                ) {
                    if (permission.granted) StatusIcon(R.drawable.ic_check_circle, MaterialTheme.statusColors.success)
                    else StatusIcon(R.drawable.ic_alert_circle, MaterialTheme.statusColors.warning)
                }
                Divider()
                // Read-only: the daily check runs at a fixed time (ExpirationCheckScheduler).
                SettingRow(
                    title = stringResource(R.string.settings_alert_time),
                    caption = stringResource(R.string.settings_alert_time_caption),
                ) {
                    Text(
                        ExpirationCheckScheduler.NOTIFICATION_TIME.format(DateTimeFormatter.ofPattern("H:mm")),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            SectionLabel(stringResource(R.string.settings_section_privacy))
            GroupCard {
                SettingRow(
                    title = stringResource(R.string.settings_your_data),
                    caption = stringResource(R.string.settings_your_data_body),
                )
            }
            SectionLabel(stringResource(R.string.settings_section_pilot))
            PilotCard(summary, onExport = {
                scope.launch { context.sharePilotCsv(viewModel.pilotCsv()) }
            })
            // Required by the ODbL license of the product data used to suggest names.
            Text(
                stringResource(R.string.settings_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, caption: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun StatusIcon(@DrawableRes icon: Int, tint: Color) = FreskoIcon(icon, tint = tint)

// Not in the Figma design: the pilot summary and export, kept in the same card style as the rest.
@Composable
private fun PilotCard(summary: PilotSummary, onExport: () -> Unit) {
    GroupCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val style = MaterialTheme.typography.bodyMedium
            Text(stringResource(R.string.pilot_added, summary.addedByScan, summary.addedManually), style = style)
            Text(
                stringResource(
                    R.string.pilot_median_add_time,
                    summary.medianAddTimeMs?.let { stringResource(R.string.pilot_seconds, (it + 500) / 1000) }
                        ?: stringResource(R.string.pilot_no_data),
                ),
                style = style,
            )
            Text(stringResource(R.string.pilot_units_consumed, summary.unitsConsumed), style = style)
            Text(stringResource(R.string.pilot_units_discarded, summary.unitsDiscarded), style = style)
            Text(stringResource(R.string.pilot_units_consumed_expiring, summary.unitsConsumedExpiringSoon), style = style)
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.pilot_export))
            }
        }
    }
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
