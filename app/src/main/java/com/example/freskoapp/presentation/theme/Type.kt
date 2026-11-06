package com.example.freskoapp.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.freskoapp.R

// One variable font file (Inter, OFL) for every weight; minSdk 26 supports font variations.
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold), inter(FontWeight.Bold))

private fun style(weight: FontWeight, size: Int, lineHeight: Int) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = 0.sp)

// Figma text styles (Fresko/…) mapped onto the Material slots the components read. All sizes are sp, so they scale
// with the system font size. Slots Figma doesn't define keep the M3 sizes, only switched to Inter.
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Inter),
    displayMedium = base.displayMedium.copy(fontFamily = Inter),
    displaySmall = base.displaySmall.copy(fontFamily = Inter),
    headlineLarge = base.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter),
    headlineSmall = style(FontWeight.SemiBold, 24, 32), // Título grande: product name in Detail
    titleLarge = style(FontWeight.SemiBold, 20, 28), // Título de pantalla: top app bar
    titleMedium = style(FontWeight.SemiBold, 16, 22), // Título de tarjeta: product name in the list
    titleSmall = base.titleSmall.copy(fontFamily = Inter),
    bodyLarge = style(FontWeight.Normal, 16, 24), // Cuerpo: fields and general content
    bodyMedium = style(FontWeight.Normal, 14, 20), // Cuerpo chico: descriptions and notices
    bodySmall = style(FontWeight.Normal, 12, 16), // Caption: help and notes
    // Buttons read labelLarge: Figma draws them 15 sp SemiBold (from the specimens, not a named text style).
    labelLarge = style(FontWeight.SemiBold, 15, 20),
    labelMedium = style(FontWeight.Medium, 13, 18), // Etiqueta: chips, field labels, metadata
    labelSmall = style(FontWeight.Normal, 12, 16), // Caption
)
