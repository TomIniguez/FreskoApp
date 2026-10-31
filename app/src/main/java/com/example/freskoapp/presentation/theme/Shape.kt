package com.example.freskoapp.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Figma radii: chips 8, fields 12, cards 16, FAB 16, buttons 24 (pill: M3 buttons already use a full shape).
// M3 reads these slots per component: chips/snackbar -> small/extraSmall, FAB -> large, Card -> medium,
// OutlinedTextField -> extraSmall. Cards (16) and fields (12) don't match their default slot, so the shared
// card and field components must ask for shapes.large and shapes.medium explicitly.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)
