package com.walletplus.app.core.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletplus.app.R

private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.inter(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = copy(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val WalletTypography = Typography(
    displayLarge = base.displayLarge.inter(FontWeight.SemiBold, 52, 58, -2.0),
    displayMedium = base.displayMedium.inter(FontWeight.SemiBold, 42, 48, -1.5),
    displaySmall = base.displaySmall.inter(FontWeight.SemiBold, 34, 40, -1.0),
    headlineLarge = base.headlineLarge.inter(FontWeight.SemiBold, 30, 36, -0.8),
    headlineMedium = base.headlineMedium.inter(FontWeight.SemiBold, 26, 32, -0.6),
    headlineSmall = base.headlineSmall.inter(FontWeight.SemiBold, 22, 28, -0.4),
    titleLarge = base.titleLarge.inter(FontWeight.SemiBold, 20, 26, -0.3),
    titleMedium = base.titleMedium.inter(FontWeight.SemiBold, 16, 22, -0.1),
    titleSmall = base.titleSmall.inter(FontWeight.SemiBold, 14, 20),
    bodyLarge = base.bodyLarge.inter(FontWeight.Normal, 16, 24),
    bodyMedium = base.bodyMedium.inter(FontWeight.Normal, 14, 20),
    bodySmall = base.bodySmall.inter(FontWeight.Normal, 12, 16),
    labelLarge = base.labelLarge.inter(FontWeight.SemiBold, 14, 20),
    labelMedium = base.labelMedium.inter(FontWeight.Medium, 12, 16, 0.1),
    labelSmall = base.labelSmall.inter(FontWeight.Medium, 11, 16, 0.2),
)

/** Cifras tabulares: los montos no "bailan" al cambiar. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

val WalletShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Tokens de movimiento: todas las animaciones comparten la misma física. */
object Motion {
    const val SHORT = 180
    const val MEDIUM = 320
    const val LONG = 650

    fun <T> gentle() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
    fun <T> emphasized(duration: Int = MEDIUM) = tween<T>(durationMillis = duration, easing = FastOutSlowInEasing)
    val offset = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    /** Margen lateral de las pantallas. */
    val screen = 20.dp
}

@Composable
fun WalletPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWalletColors provides if (darkTheme) DarkWalletColors else LightWalletColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = WalletTypography,
            shapes = WalletShapes,
            content = content,
        )
    }
}

object WalletTheme {
    val colors: WalletColors
        @Composable @ReadOnlyComposable
        get() = LocalWalletColors.current
}
