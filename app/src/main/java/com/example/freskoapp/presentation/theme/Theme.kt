package com.example.freskoapp.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

// Warning/Success for the expiration states, next to MaterialTheme.colorScheme.error.
val MaterialTheme.statusColors: StatusColors
    @Composable @ReadOnlyComposable get() = LocalStatusColors.current

// No dynamic color (Material You): the brand and the status colors must look the same on every device,
// and their contrast is verified only for these values.
@Composable
fun FreskoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
