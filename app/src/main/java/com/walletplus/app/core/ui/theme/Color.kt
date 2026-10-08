package com.walletplus.app.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta base: tinta profunda + esmeralda, con acentos semánticos.
private val Ink950 = Color(0xFF0B1118)
private val Ink900 = Color(0xFF111A24)
private val Ink800 = Color(0xFF1A2531)
private val Ink700 = Color(0xFF26323F)
private val Ink600 = Color(0xFF3A4757)
private val Ink500 = Color(0xFF5B6878)
private val Ink400 = Color(0xFF8592A1)
private val Ink300 = Color(0xFFB4BEC9)
private val Ink200 = Color(0xFFD9DFE6)
private val Ink100 = Color(0xFFECEFF3)
private val Ink50 = Color(0xFFF5F7F9)

private val Emerald700 = Color(0xFF087A5B)
private val Emerald600 = Color(0xFF0B9470)
private val Emerald300 = Color(0xFF5FD9AE)
private val Emerald100 = Color(0xFFD3F5E8)
private val Emerald900 = Color(0xFF053D2E)

private val Coral600 = Color(0xFFD6404A)
private val Coral300 = Color(0xFFFF8A8F)
private val Coral100 = Color(0xFFFFE1E2)
private val Coral900 = Color(0xFF5A1419)

private val Violet600 = Color(0xFF5D5FEF)
private val Violet300 = Color(0xFFA5A6FF)
private val Violet100 = Color(0xFFE5E5FF)
private val Violet900 = Color(0xFF26277A)

private val Amber500 = Color(0xFFE59A0B)
private val Amber300 = Color(0xFFFFC857)
private val Orange500 = Color(0xFFEA6A1A)
private val Orange300 = Color(0xFFFF9B5C)

val LightColors = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Ink800,
    onSecondary = Color.White,
    secondaryContainer = Ink100,
    onSecondaryContainer = Ink900,
    tertiary = Violet600,
    onTertiary = Color.White,
    tertiaryContainer = Violet100,
    onTertiaryContainer = Violet900,
    error = Coral600,
    onError = Color.White,
    errorContainer = Coral100,
    onErrorContainer = Coral900,
    background = Ink50,
    onBackground = Ink950,
    surface = Ink50,
    onSurface = Ink950,
    surfaceVariant = Ink100,
    onSurfaceVariant = Ink500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Ink100,
    surfaceContainerHighest = Ink200,
    surfaceBright = Color.White,
    surfaceDim = Ink100,
    outline = Ink300,
    outlineVariant = Ink200,
    inverseSurface = Ink900,
    inverseOnSurface = Ink50,
    inversePrimary = Emerald300,
    scrim = Color.Black,
)

val DarkColors = darkColorScheme(
    primary = Emerald300,
    onPrimary = Emerald900,
    primaryContainer = Color(0xFF0B4B39),
    onPrimaryContainer = Emerald100,
    secondary = Ink200,
    onSecondary = Ink900,
    secondaryContainer = Ink700,
    onSecondaryContainer = Ink100,
    tertiary = Violet300,
    onTertiary = Violet900,
    tertiaryContainer = Color(0xFF34358F),
    onTertiaryContainer = Violet100,
    error = Coral300,
    onError = Coral900,
    errorContainer = Color(0xFF7A1D24),
    onErrorContainer = Coral100,
    background = Ink950,
    onBackground = Ink50,
    surface = Ink950,
    onSurface = Ink50,
    surfaceVariant = Ink800,
    onSurfaceVariant = Ink400,
    surfaceContainerLowest = Color(0xFF070B10),
    surfaceContainerLow = Ink900,
    surfaceContainer = Ink900,
    surfaceContainerHigh = Ink800,
    surfaceContainerHighest = Ink700,
    surfaceBright = Ink800,
    surfaceDim = Ink950,
    outline = Ink600,
    outlineVariant = Ink700,
    inverseSurface = Ink50,
    inverseOnSurface = Ink900,
    inversePrimary = Emerald700,
    scrim = Color.Black,
)

/** Colores semánticos de la app que Material 3 no cubre. */
@Immutable
data class WalletColors(
    val income: Color,
    val expense: Color,
    val incomeContainer: Color,
    val expenseContainer: Color,
    val normal: Color,
    val caution: Color,
    val high: Color,
    val exceeded: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val track: Color,
)

val LightWalletColors = WalletColors(
    income = Emerald600,
    expense = Coral600,
    incomeContainer = Emerald100,
    expenseContainer = Coral100,
    normal = Emerald600,
    caution = Amber500,
    high = Orange500,
    exceeded = Coral600,
    heroStart = Color(0xFF0F1B26),
    heroEnd = Color(0xFF0B3B2E),
    onHero = Color.White,
    onHeroMuted = Color(0xB3FFFFFF),
    track = Ink100,
)

val DarkWalletColors = WalletColors(
    income = Emerald300,
    expense = Coral300,
    incomeContainer = Color(0xFF0B3A2D),
    expenseContainer = Color(0xFF3E1519),
    normal = Emerald300,
    caution = Amber300,
    high = Orange300,
    exceeded = Coral300,
    heroStart = Color(0xFF14212D),
    heroEnd = Color(0xFF0D4636),
    onHero = Color.White,
    onHeroMuted = Color(0xB3FFFFFF),
    track = Ink800,
)

val LocalWalletColors = staticCompositionLocalOf { LightWalletColors }

/**
 * Paleta categórica para categorías y gráficos (claro, oscuro). Ocho tonos validados para
 * daltonismo (ΔE CVD ≥ 9 entre vecinos) más un gris neutro para "Otros". Como varios tonos
 * quedan por debajo de 3:1 sobre el fondo claro, los gráficos siempre llevan leyenda con texto.
 */
object CategoryPalette {
    private val colors: Map<String, Pair<Color, Color>> = linkedMapOf(
        "blue" to (Color(0xFF2A78D6) to Color(0xFF3987E5)),
        "orange" to (Color(0xFFEB6834) to Color(0xFFD95926)),
        "aqua" to (Color(0xFF1BAF7A) to Color(0xFF199E70)),
        "yellow" to (Color(0xFFEDA100) to Color(0xFFC98500)),
        "magenta" to (Color(0xFFE87BA4) to Color(0xFFD55181)),
        "green" to (Color(0xFF008300) to Color(0xFF008300)),
        "violet" to (Color(0xFF4A3AA7) to Color(0xFF9085E9)),
        "red" to (Color(0xFFE34948) to Color(0xFFE66767)),
        "slate" to (Color(0xFF8592A1) to Color(0xFF6B7888)),
    )

    val keys: List<String> = colors.keys.toList()

    fun color(key: String, dark: Boolean): Color {
        val pair = colors[key] ?: colors.getValue("slate")
        return if (dark) pair.second else pair.first
    }
}
