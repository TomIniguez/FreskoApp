package com.example.freskoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.presentation.navigation.FreskoNavHost
import com.example.freskoapp.presentation.theme.FreskoAppTheme
import com.example.freskoapp.work.ExpirationNotifier
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A fresh start of the UI (launcher or alert), not a rotation or a restore after process death.
        // The background worker never creates this activity, so it never counts as an open.
        if (savedInstanceState == null) {
            val record = (application as FreskoApplication).recordAppEventUseCase()
            val fromAlert = intent.getBooleanExtra(ExpirationNotifier.EXTRA_FROM_NOTIFICATION, false)
            lifecycleScope.launch {
                record(EventType.APP_OPEN)
                if (fromAlert) record(EventType.NOTIFICATION_TAPPED)
            }
        }
        enableEdgeToEdge()
        setContent {
            FreskoAppTheme { FreskoNavHost() }
        }
    }
}
