package com.example.freskoapp.presentation.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.freskoapp.R

// True only when the app can actually show a notification: system switch on and, from API 33, permission granted.
fun Context.canPostNotifications(): Boolean =
    NotificationManagerCompat.from(this).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED)

class NotificationPermissionState(
    val granted: Boolean,
    val opensSettings: Boolean, // true when request() goes to system settings instead of the dialog
    val request: () -> Unit,
)

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.canPostNotifications()) }
    // After one denial the system dialog may not show again, so the next tap sends the user to settings.
    var denied by rememberSaveable { mutableStateOf(false) }

    // The user can change the permission from system settings; re-check when coming back.
    LifecycleResumeEffect(Unit) {
        granted = context.canPostNotifications()
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = context.canPostNotifications()
        if (!isGranted) denied = true
    }

    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !denied
    return NotificationPermissionState(granted, opensSettings = !canAsk) {
        if (canAsk) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
}

// Explains why the permission is needed before asking. `onDismiss == null` hides "Ahora no" (Settings reuses it).
@Composable
fun NotificationPermissionCard(
    state: NotificationPermissionState,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notification_permission_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.notification_permission_rationale))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = state.request) {
                    Text(
                        stringResource(
                            if (state.opensSettings) R.string.notification_permission_open_settings
                            else R.string.notification_permission_enable
                        )
                    )
                }
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.notification_permission_dismiss))
                    }
                }
            }
        }
    }
}
