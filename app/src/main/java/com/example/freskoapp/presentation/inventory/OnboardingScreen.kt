package com.example.freskoapp.presentation.inventory

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.freskoapp.R
import com.example.freskoapp.presentation.common.FreskoIcon
import com.example.freskoapp.presentation.common.IconLabelButton

// D1: shown once, before Android's notification permission dialog, so the user knows why it's asked.
@Composable
fun OnboardingScreen(onActivate: () -> Unit, onLater: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { FreskoIcon(R.drawable.ic_bell, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 48.dp) }
                Text(
                    stringResource(R.string.onboarding_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.onboarding_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Bullet(R.drawable.ic_scan_barcode, R.string.onboarding_point_add)
                    Bullet(R.drawable.ic_bell, R.string.onboarding_point_alert)
                    Bullet(R.drawable.ic_check_circle, R.string.onboarding_point_consume)
                }
            }
            IconLabelButton(R.drawable.ic_bell, stringResource(R.string.onboarding_activate), onActivate, Modifier.fillMaxWidth())
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_later))
            }
        }
    }
}

@Composable
private fun Bullet(@DrawableRes icon: Int, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FreskoIcon(icon, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}
