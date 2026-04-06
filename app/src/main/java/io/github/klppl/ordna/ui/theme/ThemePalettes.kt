package io.github.klppl.ordna.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    SYSTEM,
    CATPPUCCIN,
    ROSE_PINE,
    GRUVBOX,
    TOKYO_NIGHT,
    DRACULA,
    KANAGAWA,
    OXOCARBON,
}

data class ThemeColors(
    val colorScheme: androidx.compose.material3.ColorScheme,
    val overdueRed: Color,
    val completedGreen: Color,
    val completedGray: Color,
)

fun appThemeColors(theme: AppTheme): ThemeColors? = when (theme) {
    AppTheme.SYSTEM -> null
    AppTheme.CATPPUCCIN -> catppuccinMocha
    AppTheme.ROSE_PINE -> rosePine
    AppTheme.GRUVBOX -> gruvboxDark
    AppTheme.TOKYO_NIGHT -> tokyoNight
    AppTheme.DRACULA -> dracula
    AppTheme.KANAGAWA -> kanagawa
    AppTheme.OXOCARBON -> oxocarbon
}

// -- Catppuccin Mocha --

private val catppuccinMocha = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFFCBA6F7),
        onPrimary = Color(0xFF1E1E2E),
        primaryContainer = Color(0xFF45475A),
        onPrimaryContainer = Color(0xFFCBA6F7),
        secondary = Color(0xFF89B4FA),
        onSecondary = Color(0xFF1E1E2E),
        tertiary = Color(0xFFF5C2E7),
        background = Color(0xFF1E1E2E),
        onBackground = Color(0xFFCDD6F4),
        surface = Color(0xFF1E1E2E),
        onSurface = Color(0xFFCDD6F4),
        surfaceVariant = Color(0xFF313244),
        onSurfaceVariant = Color(0xFFA6ADC8),
        outline = Color(0xFF585B70),
        error = Color(0xFFF38BA8),
        onError = Color(0xFF1E1E2E),
    ),
    overdueRed = Color(0xFFF38BA8),
    completedGreen = Color(0xFFA6E3A1),
    completedGray = Color(0xFF6C7086),
)

// -- Rosé Pine --

private val rosePine = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFFC4A7E7),
        onPrimary = Color(0xFF191724),
        primaryContainer = Color(0xFF26233A),
        onPrimaryContainer = Color(0xFFC4A7E7),
        secondary = Color(0xFF31748F),
        onSecondary = Color(0xFFE0DEF4),
        tertiary = Color(0xFFEBBCBA),
        background = Color(0xFF191724),
        onBackground = Color(0xFFE0DEF4),
        surface = Color(0xFF191724),
        onSurface = Color(0xFFE0DEF4),
        surfaceVariant = Color(0xFF26233A),
        onSurfaceVariant = Color(0xFF908CAA),
        outline = Color(0xFF524F67),
        error = Color(0xFFEB6F92),
        onError = Color(0xFF191724),
    ),
    overdueRed = Color(0xFFEB6F92),
    completedGreen = Color(0xFF9CCFD8),
    completedGray = Color(0xFF6E6A86),
)

// -- Gruvbox Dark --

private val gruvboxDark = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFFD79921),
        onPrimary = Color(0xFF282828),
        primaryContainer = Color(0xFF3C3836),
        onPrimaryContainer = Color(0xFFFABD2F),
        secondary = Color(0xFF83A598),
        onSecondary = Color(0xFF282828),
        tertiary = Color(0xFFD3869B),
        background = Color(0xFF282828),
        onBackground = Color(0xFFEBDBB2),
        surface = Color(0xFF282828),
        onSurface = Color(0xFFEBDBB2),
        surfaceVariant = Color(0xFF3C3836),
        onSurfaceVariant = Color(0xFFA89984),
        outline = Color(0xFF665C54),
        error = Color(0xFFFB4934),
        onError = Color(0xFF282828),
    ),
    overdueRed = Color(0xFFFB4934),
    completedGreen = Color(0xFFB8BB26),
    completedGray = Color(0xFF928374),
)

// -- Tokyo Night --

private val tokyoNight = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFF7AA2F7),
        onPrimary = Color(0xFF1A1B26),
        primaryContainer = Color(0xFF24283B),
        onPrimaryContainer = Color(0xFF7AA2F7),
        secondary = Color(0xFF7DCFFF),
        onSecondary = Color(0xFF1A1B26),
        tertiary = Color(0xFFBB9AF7),
        background = Color(0xFF1A1B26),
        onBackground = Color(0xFFC0CAF5),
        surface = Color(0xFF1A1B26),
        onSurface = Color(0xFFC0CAF5),
        surfaceVariant = Color(0xFF24283B),
        onSurfaceVariant = Color(0xFF565F89),
        outline = Color(0xFF414868),
        error = Color(0xFFF7768E),
        onError = Color(0xFF1A1B26),
    ),
    overdueRed = Color(0xFFF7768E),
    completedGreen = Color(0xFF9ECE6A),
    completedGray = Color(0xFF565F89),
)

// -- Dracula --

private val dracula = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFFBD93F9),
        onPrimary = Color(0xFF282A36),
        primaryContainer = Color(0xFF44475A),
        onPrimaryContainer = Color(0xFFBD93F9),
        secondary = Color(0xFF8BE9FD),
        onSecondary = Color(0xFF282A36),
        tertiary = Color(0xFFFF79C6),
        background = Color(0xFF282A36),
        onBackground = Color(0xFFF8F8F2),
        surface = Color(0xFF282A36),
        onSurface = Color(0xFFF8F8F2),
        surfaceVariant = Color(0xFF44475A),
        onSurfaceVariant = Color(0xFF6272A4),
        outline = Color(0xFF6272A4),
        error = Color(0xFFFF5555),
        onError = Color(0xFF282A36),
    ),
    overdueRed = Color(0xFFFF5555),
    completedGreen = Color(0xFF50FA7B),
    completedGray = Color(0xFF6272A4),
)

// -- Kanagawa --

private val kanagawa = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFF7E9CD8),
        onPrimary = Color(0xFF1F1F28),
        primaryContainer = Color(0xFF2A2A37),
        onPrimaryContainer = Color(0xFF7E9CD8),
        secondary = Color(0xFF7FB4CA),
        onSecondary = Color(0xFF1F1F28),
        tertiary = Color(0xFF957FB8),
        background = Color(0xFF1F1F28),
        onBackground = Color(0xFFDCD7BA),
        surface = Color(0xFF1F1F28),
        onSurface = Color(0xFFDCD7BA),
        surfaceVariant = Color(0xFF2A2A37),
        onSurfaceVariant = Color(0xFF727169),
        outline = Color(0xFF54546D),
        error = Color(0xFFE82424),
        onError = Color(0xFF1F1F28),
    ),
    overdueRed = Color(0xFFE82424),
    completedGreen = Color(0xFF98BB6C),
    completedGray = Color(0xFF727169),
)

// -- Oxocarbon --

private val oxocarbon = ThemeColors(
    colorScheme = darkColorScheme(
        primary = Color(0xFFBE95FF),
        onPrimary = Color(0xFF161616),
        primaryContainer = Color(0xFF262626),
        onPrimaryContainer = Color(0xFFBE95FF),
        secondary = Color(0xFF78A9FF),
        onSecondary = Color(0xFF161616),
        tertiary = Color(0xFFEE5396),
        background = Color(0xFF161616),
        onBackground = Color(0xFFF2F4F8),
        surface = Color(0xFF161616),
        onSurface = Color(0xFFF2F4F8),
        surfaceVariant = Color(0xFF262626),
        onSurfaceVariant = Color(0xFF525252),
        outline = Color(0xFF393939),
        error = Color(0xFFEE5396),
        onError = Color(0xFF161616),
    ),
    overdueRed = Color(0xFFEE5396),
    completedGreen = Color(0xFF42BE65),
    completedGray = Color(0xFF525252),
)
