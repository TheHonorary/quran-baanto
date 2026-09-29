package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = BrassSecondary,
    onSecondary = BrassOnSecondary,
    secondaryContainer = BrassSecondaryContainer,
    onSecondaryContainer = BrassOnSecondaryContainer,
    tertiary = LapisTertiary,
    onTertiary = LapisOnTertiary,
    tertiaryContainer = LapisTertiaryContainer,
    onTertiaryContainer = LapisOnTertiaryContainer,
    background = ParchmentBackground,
    onBackground = InkTextPrimary,
    surface = ParchmentSurfaceElevated,
    onSurface = InkTextPrimary,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = InkTextSecondary,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGoldPrimary,
    onPrimary = DarkGoldOnPrimary,
    primaryContainer = DarkGoldPrimaryContainer,
    onPrimaryContainer = DarkGoldOnPrimaryContainer,
    secondary = DarkSlateSecondary,
    onSecondary = DarkSlateOnSecondary,
    secondaryContainer = DarkSlateSecondaryContainer,
    onSecondaryContainer = DarkSlateOnSecondaryContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    tertiaryContainer = DarkAmberTertiaryContainer,
    onTertiaryContainer = DarkAmberOnTertiaryContainer,
    background = MidnightBackground,
    onBackground = IvoryTextPrimary,
    surface = MidnightSurface,
    onSurface = IvoryTextPrimary,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = IvoryTextSecondary,
    outline = DarkWarmOutline,
    outlineVariant = DarkWarmOutlineVariant,
    error = ErrorCrimsonDark,
    onError = Color(0xFF601410)
)

@Composable
fun QuranBaantoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
