package com.nexvary.securitysuite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NexvaryPalette {
    val Midnight = Color(0xFF05090D)
    val DeepNavy = Color(0xFF0C1319)
    val Gunmetal = Color(0xFF2E3945)
    val Silver = Color(0xFF9EABB6)
    val Platinum = Color(0xFFE0E6EA)
    val ElectricBlue = Color(0xFF27B7FF)
    val ElectricBlueDark = Color(0xFF0A6DA3)
    val Gold = Color(0xFFE2B84B)
    val NeonGreen = Color(0xFF6DFFB1)
    val Amber = Color(0xFFFFB347)
    val Danger = Color(0xFFFF5D73)
    val Purple = Color(0xFF9B7BFF)
    val Panel = Color(0xFF111A22)
    val PanelRaised = Color(0xFF18232D)
}

private val NexvaryColors = darkColorScheme(
    primary = NexvaryPalette.ElectricBlue,
    onPrimary = Color(0xFF001018),
    secondary = NexvaryPalette.Silver,
    onSecondary = NexvaryPalette.Midnight,
    tertiary = NexvaryPalette.Gold,
    onTertiary = NexvaryPalette.Midnight,
    background = NexvaryPalette.Midnight,
    onBackground = NexvaryPalette.Platinum,
    surface = NexvaryPalette.Panel,
    onSurface = NexvaryPalette.Platinum,
    surfaceVariant = NexvaryPalette.PanelRaised,
    onSurfaceVariant = NexvaryPalette.Silver,
    error = NexvaryPalette.Danger,
    onError = Color.Black,
    outline = NexvaryPalette.Gunmetal,
)

@Composable
fun NexvaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexvaryColors,
        content = content,
    )
}
