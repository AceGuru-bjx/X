package com.unknown.security.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Brand palette — deep emerald on near-black; deliberately no blue/indigo. */
object UnknownPalette {
    // Dark scheme
    val darkBackground = Color(0xFF0A100D)
    val darkSurface = Color(0xFF121A16)
    val darkPrimary = Color(0xFF2DD4A7)
    val darkOnPrimary = Color(0xFF04281D)
    val darkPrimaryContainer = Color(0xFF0E3B2E)
    val darkOnPrimaryContainer = Color(0xFF9FF2D3)
    val darkSecondary = Color(0xFFB7A4F5)
    val darkOnSecondary = Color(0xFF231A44)
    val darkTertiary = Color(0xFFF5C46B)
    val darkOnTertiary = Color(0xFF3B2A06)
    val darkError = Color(0xFFFF7A8A)
    val darkOnError = Color(0xFF45000E)
    val darkOnErrorContainer = Color(0xFFFFD9DC)
    val darkOnSurface = Color(0xFFE4F0EA)
    val darkOnSurfaceVariant = Color(0xFFB4C4BB)
    val darkOutline = Color(0xFF6F8078)

    // Light scheme
    val lightBackground = Color(0xFFF3F9F5)
    val lightSurface = Color(0xFFFFFFFF)
    val lightPrimary = Color(0xFF0C8F66)
    val lightOnPrimary = Color(0xFFFFFFFF)
    val lightPrimaryContainer = Color(0xFFB9F4DC)
    val lightOnPrimaryContainer = Color(0xFF00291B)
    val lightSecondary = Color(0xFF6A56C5)
    val lightOnSecondary = Color(0xFFFFFFFF)
    val lightTertiary = Color(0xFFB07C14)
    val lightOnTertiary = Color(0xFFFFFFFF)
    val lightError = Color(0xFFB3261E)
    val lightOnError = Color(0xFFFFFFFF)
    val lightOnSurface = Color(0xFF1A2C24)
    val lightOnSurfaceVariant = Color(0xFF41544A)
    val lightOutline = Color(0xFF718679)

    fun darkScheme() =
        darkColorScheme(
            primary = darkPrimary,
            onPrimary = darkOnPrimary,
            primaryContainer = darkPrimaryContainer,
            onPrimaryContainer = darkOnPrimaryContainer,
            secondary = darkSecondary,
            onSecondary = darkOnSecondary,
            tertiary = darkTertiary,
            onTertiary = darkOnTertiary,
            error = darkError,
            onError = darkOnError,
            onErrorContainer = darkOnErrorContainer,
            background = darkBackground,
            onBackground = darkOnSurface,
            surface = darkSurface,
            onSurface = darkOnSurface,
            surfaceVariant = darkSurface,
            onSurfaceVariant = darkOnSurfaceVariant,
            outline = darkOutline,
        )

    fun lightScheme() =
        lightColorScheme(
            primary = lightPrimary,
            onPrimary = lightOnPrimary,
            primaryContainer = lightPrimaryContainer,
            onPrimaryContainer = lightOnPrimaryContainer,
            secondary = lightSecondary,
            onSecondary = lightOnSecondary,
            tertiary = lightTertiary,
            onTertiary = lightOnTertiary,
            error = lightError,
            onError = lightOnError,
            background = lightBackground,
            onBackground = lightOnSurface,
            surface = lightSurface,
            onSurface = lightOnSurface,
            surfaceVariant = lightSurface,
            onSurfaceVariant = lightOnSurfaceVariant,
            outline = lightOutline,
        )
}

/** Root theme — wrap the whole app; glass components read colors from it. */
@Composable
fun UnknownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) UnknownPalette.darkScheme() else UnknownPalette.lightScheme(),
        content = content,
    )
}
