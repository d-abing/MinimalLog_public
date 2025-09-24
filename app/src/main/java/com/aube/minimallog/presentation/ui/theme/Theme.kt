package com.aube.minimallog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Tertiary,
    surface = Tertiary,
    onPrimary = OnPrimary,
    onSecondary = onSecondary,
    onTertiary = OnPrimary,
    onBackground = OnPrimary,
    onSurface = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondaryContainer = Secondary,
    tertiaryContainer = Tertiary,
    onPrimaryContainer = OnPrimaryContainer,
    onSecondaryContainer = onSecondary,
    onTertiaryContainer = OnPrimary,
)

@Composable
fun MinimalLogTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}