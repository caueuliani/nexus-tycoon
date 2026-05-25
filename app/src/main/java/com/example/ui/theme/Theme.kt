package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlueSkinScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = Color(0xFF007BFF),
    tertiary = SuccessGreen,
    background = CosmicDark,
    surface = CosmicCard,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val GoldSkinScheme = darkColorScheme(
    primary = SolarGold,
    secondary = Color(0xFFFF8B00),
    tertiary = NeonCyan,
    background = Color(0xFF14120B),
    surface = Color(0xFF221E12),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val PurpleSkinScheme = darkColorScheme(
    primary = NeonMagenta,
    secondary = Color(0xFF9E00FF),
    tertiary = SolarGold,
    background = Color(0xFF10091E),
    surface = Color(0xFF1D1135),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun MyApplicationTheme(
    selectedSkinId: Int = 0,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedSkinId) {
        1 -> GoldSkinScheme
        2 -> PurpleSkinScheme
        else -> BlueSkinScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
