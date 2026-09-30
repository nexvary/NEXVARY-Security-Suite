package com.nexvary.securitysuite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexvaryColors = darkColorScheme(
    primary = Color(0xFF6A88A0),
    secondary = Color(0xFFD9D7D4),
    tertiary = Color(0xFF57E389),
    background = Color(0xFF0C1319),
    surface = Color(0xFF131D25),
    surfaceVariant = Color(0xFF2E3945),
    onPrimary = Color.Black,
    onBackground = Color(0xFFD9D7D4),
    onSurface = Color(0xFFD9D7D4),
)

@Composable
fun NexvaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NexvaryColors, content = content)
}
