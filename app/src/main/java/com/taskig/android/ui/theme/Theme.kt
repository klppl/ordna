package com.taskig.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

data class SemanticColors(
    val overdueRed: Color = OverdueRed,
    val completedGreen: Color = CompletedGreen,
    val completedGray: Color = CompletedGray,
)

val LocalSemanticColors = staticCompositionLocalOf { SemanticColors() }

@Composable
fun TaskigTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val themeColors = appThemeColors(appTheme)

    val colorScheme = if (themeColors != null) {
        // Custom theme — always dark
        themeColors.colorScheme
    } else {
        // System theme
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val semanticColors = if (themeColors != null) {
        SemanticColors(
            overdueRed = themeColors.overdueRed,
            completedGreen = themeColors.completedGreen,
            completedGray = themeColors.completedGray,
        )
    } else {
        SemanticColors()
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TaskigTypography,
            content = content,
        )
    }
}
