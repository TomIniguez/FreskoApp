package com.example.freskoapp.presentation.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

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
