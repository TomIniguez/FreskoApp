package com.example.freskoapp.presentation.settings

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
    val permission = rememberNotificationPermissionState()

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
        }
    }
}
