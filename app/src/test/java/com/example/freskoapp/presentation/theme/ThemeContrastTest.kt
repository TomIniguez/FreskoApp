package com.example.freskoapp.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

// The pairs the design system verifies (Figma "Contraste verificado (WCAG)"), checked for both schemes:
// the light values come from Figma, the dark ones are derived, so this is what keeps them honest.
class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun pairs(c: ColorScheme, s: StatusColors) = mapOf(
        "Error/Surface" to (c.error to c.surface),
        "Warning/Surface" to (s.warning to c.surface),
        "Success/Surface" to (s.success to c.surface),
        "OnSurface/Background" to (c.onSurface to c.background),
        "OnSurfaceVariant/Background" to (c.onSurfaceVariant to c.background),
        "OnErrorContainer/ErrorContainer" to (c.onErrorContainer to c.errorContainer),
        "OnWarningContainer/WarningContainer" to (s.onWarningContainer to s.warningContainer),
        "OnSuccessContainer/SuccessContainer" to (s.onSuccessContainer to s.successContainer),
        "OnPrimaryContainer/PrimaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
        "OnPrimary/Primary" to (c.onPrimary to c.primary),
        "InverseOnSurface/InverseSurface" to (c.inverseOnSurface to c.inverseSurface),
        "InversePrimary/InverseSurface" to (c.inversePrimary to c.inverseSurface), // snackbar "Deshacer"
    )

    private fun assertReadable(name: String, c: ColorScheme, s: StatusColors) {
        val failures = pairs(c, s).mapNotNull { (pair, colors) ->
            contrast(colors.first, colors.second).takeIf { it < 4.5 }?.let { "$pair %.2f:1".format(it) }
        } + listOfNotNull(
            // Borders only need 3:1.
            contrast(c.outline, c.surface).takeIf { it < 3.0 }?.let { "Outline/Surface %.2f:1".format(it) },
        )
        assertTrue("$name below WCAG AA: $failures", failures.isEmpty())
    }

    @Test
    fun lightSchemeMeetsAA() = assertReadable("light", LightColors, LightStatusColors)

    @Test
    fun darkSchemeMeetsAA() = assertReadable("dark", DarkColors, DarkStatusColors)
}
