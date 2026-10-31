package com.example.freskoapp.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Light values are the Figma variables (FreskoApp · Sistema de diseño). Figma has no dark mode: the dark values are
// derived tones of the same hues (M3 style) and must be replaced if design publishes its own.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8ECD6),
    onPrimaryContainer = Color(0xFF00210F),
    inversePrimary = Color(0xFF8FD5A6),
    // Not in Figma. Mapped to the brand so no stock M3 purple leaks in (FilterChip selected and the
    // NavigationBar indicator use secondaryContainer, which Figma draws as PrimaryContainer).
    secondary = Color(0xFF1B6B4A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8ECD6),
    onSecondaryContainer = Color(0xFF00210F),
    tertiary = Color(0xFF1B6B4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8ECD6),
    onTertiaryContainer = Color(0xFF00210F),
    background = Color(0xFFFBFDF8), // keep in sync with @color/window_background_light
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFE1E8E0),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF707970),
    outlineVariant = Color(0xFFC0C9BF), // derived: dividers, not in Figma
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2E312E),
    inverseOnSurface = Color(0xFFF0F1EC),
    // Derived neutral greens: the M3 defaults are purple-tinted (cards, menus, date picker, top bar on scroll).
    surfaceTint = Color(0xFF1B6B4A),
    surfaceBright = Color(0xFFFBFDF8),
    surfaceDim = Color(0xFFD8DBD5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F7F2),
    surfaceContainer = Color(0xFFEFF2EC),
    surfaceContainerHigh = Color(0xFFE9ECE6),
    surfaceContainerHighest = Color(0xFFE1E8E0),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD5A6),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005235),
    onPrimaryContainer = Color(0xFFC8ECD6),
    inversePrimary = Color(0xFF1B6B4A),
    secondary = Color(0xFF8FD5A6),
    onSecondary = Color(0xFF003920),
    secondaryContainer = Color(0xFF005235),
    onSecondaryContainer = Color(0xFFC8ECD6),
    tertiary = Color(0xFF8FD5A6),
    onTertiary = Color(0xFF003920),
    tertiaryContainer = Color(0xFF005235),
    onTertiaryContainer = Color(0xFFC8ECD6),
    background = Color(0xFF111411), // keep in sync with @color/window_background_dark
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9BF),
    outline = Color(0xFF8A938A),
    outlineVariant = Color(0xFF414942),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFE1E3DE),
    inverseOnSurface = Color(0xFF2E312E),
    surfaceTint = Color(0xFF8FD5A6),
    surfaceBright = Color(0xFF373A36),
    surfaceDim = Color(0xFF111411),
    surfaceContainerLowest = Color(0xFF0C0F0C),
    surfaceContainerLow = Color(0xFF191C19),
    surfaceContainer = Color(0xFF1D201D),
    surfaceContainerHigh = Color(0xFF272B27),
    surfaceContainerHighest = Color(0xFF323532),
)

// Expiration states have no Material slot (only error does): Warning = "Por vencer", Success = "Vigente".
@Immutable
data class StatusColors(
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

internal val LightStatusColors = StatusColors(
    warning = Color(0xFF8A5100),
    warningContainer = Color(0xFFFFE8C2),
    onWarningContainer = Color(0xFF3A2300),
    success = Color(0xFF1B6B4A),
    successContainer = Color(0xFFC8ECD6),
    onSuccessContainer = Color(0xFF00210F),
)

internal val DarkStatusColors = StatusColors(
    warning = Color(0xFFFFB868),
    warningContainer = Color(0xFF693C00),
    onWarningContainer = Color(0xFFFFDDB3),
    success = Color(0xFF8FD5A6),
    successContainer = Color(0xFF005235),
    onSuccessContainer = Color(0xFFC8ECD6),
)
