package com.hoffi.compose.complib

import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

var debugMode by mutableStateOf(true)

val debugBorderPalette: List<Color> = listOf(
    Color(0xFF1F77B4),
    Color(0xFFFF7F0E),
    Color(0xFF2CA02C),
    Color(0xFFD62728),
    Color(0xFF9467BD),
    Color(0xFF8C564B),
    Color(0xFFE377C2),
    Color(0xFF7F7F7F),
    Color(0xFFBCBD22),
    Color(0xFF17BECF),
    Color(0xFFAE76A3),
    Color(0xFF00AEEF),
    Color(0xFF00B894),
    Color(0xFFF39C12),
    Color(0xFFE74C3C),
    Color(0xFF2ECC71),
)

private var debugBorderIndex = 0

fun Modifier.debugBorder(): Modifier {
    if (!debugMode) return this

    val color = debugBorderPalette[debugBorderIndex]
    debugBorderIndex = (debugBorderIndex + 1) % debugBorderPalette.size

    return this.border(width = 2.dp, color = color)
}
