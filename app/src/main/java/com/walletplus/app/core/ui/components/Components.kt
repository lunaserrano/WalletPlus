package com.walletplus.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.ui.theme.CategoryPalette
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.core.ui.theme.tabular
import com.walletplus.domain.calc.BudgetStatus
import com.walletplus.domain.money.Money
import kotlin.math.roundToLong

/** Tarjeta base de la app: superficie limpia con esquinas generosas. */
@Composable
fun WalletCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val base = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(onClick = onClick, modifier = base, shape = shape, color = color, tonalElevation = 0.dp) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = base, shape = shape, color = color, tonalElevation = 0.dp) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = Spacing.sm)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun categoryColor(colorKey: String): Color = CategoryPalette.color(colorKey, isSystemInDarkTheme())

@Composable
fun CategoryAvatar(
    emoji: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    muted: Boolean = false,
) {
    val color = categoryColor(colorKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color.copy(alpha = if (muted) 0.08f else 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            fontSize = (size.value * 0.46f).sp,
            modifier = Modifier.alpha(if (muted) 0.45f else 1f),
        )
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Texto de dinero con cifras tabulares. */
@Composable
fun MoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    signed: Boolean = false,
    showCents: Boolean = true,
    fontWeight: FontWeight? = null,
) {
    val formatter = LocalMoneyFormatter.current
    Text(
        text = formatter.format(amount, signed = signed, showCents = showCents),
        modifier = modifier,
        style = style.tabular(),
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Monto que cuenta hasta su valor al aparecer o cambiar: da vida sin distraer. */
@Composable
fun AnimatedMoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = Color.Unspecified,
    signed: Boolean = false,
) {
    val formatter = LocalMoneyFormatter.current
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(amount) {
        animatable.animateTo(amount.cents.toFloat(), tween(Motion.LONG, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    val shown = if (animatable.isRunning) Money(animatable.value.roundToLong()) else amount
    Text(
        text = formatter.format(shown, signed = signed),
        modifier = modifier.semantics { contentDescription = formatter.format(amount, signed = signed) },
        style = style.tabular(),
        color = color,
        maxLines = 1,
    )
}

@Composable
fun statusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.NORMAL -> WalletTheme.colors.normal
    BudgetStatus.CAUTION -> WalletTheme.colors.caution
    BudgetStatus.HIGH -> WalletTheme.colors.high
    BudgetStatus.EXCEEDED -> WalletTheme.colors.exceeded
}

fun statusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.NORMAL -> "Normal"
    BudgetStatus.CAUTION -> "Precaución"
    BudgetStatus.HIGH -> "Alto"
    BudgetStatus.EXCEEDED -> "Excedido"
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) Color.White else color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Barra de progreso redondeada y animada. [progress] puede superar 1 (se recorta visualmente). */
@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = WalletTheme.colors.track,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(Motion.LONG, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 40.sp)
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.xl))
            Button(onClick = onAction, shape = CircleShape, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                Text(actionLabel)
            }
        }
    }
}

/** Aviso permanente cuando se están viendo datos de demostración. */
@Composable
fun DemoBanner(onManage: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onManage,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text("Datos de demostración", style = MaterialTheme.typography.labelLarge)
                Text(
                    "No son tus datos reales. Toca para gestionarlos.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Título grande de pantalla con acción opcional a la derecha. */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        trailing?.invoke()
    }
}

@Composable
fun LabeledValue(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun ClickableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)) { content() }
}

@Composable
fun HorizontalSpacer(width: Dp) = Spacer(Modifier.width(width))
