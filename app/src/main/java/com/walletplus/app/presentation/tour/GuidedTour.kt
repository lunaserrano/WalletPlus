package com.walletplus.app.presentation.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.Spacing

/** Paso del recorrido. [targetId] null = tarjeta centrada sin resaltar nada. */
data class TourStep(
    val targetId: String?,
    val title: String,
    val body: String,
    val icon: ImageVector? = null,
    val circular: Boolean = false,
    /** Radio de la ventana iluminada; conviene igualarlo al del elemento + el margen. */
    val corner: androidx.compose.ui.unit.Dp = 22.dp,
)

/**
 * Registro de posiciones de los elementos que el recorrido puede señalar.
 * Cada elemento se marca con [Modifier.tourTarget].
 */
@Stable
class TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

val LocalTourTargets = staticCompositionLocalOf { TourTargets() }

fun Modifier.tourTarget(id: String): Modifier = composed {
    val targets = LocalTourTargets.current
    onGloballyPositioned { targets.bounds[id] = it.boundsInRoot() }
}

/**
 * Recorrido guiado: oscurece la pantalla, ilumina el elemento del paso actual con una
 * transición suave y explica qué hace. Se puede omitir en cualquier momento.
 */
@Composable
fun GuidedTour(
    steps: List<TourStep>,
    targets: TourTargets,
    onFinish: () -> Unit,
) {
    var index by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val step = steps[index]
    val target = step.targetId?.let { targets.bounds[it] }

    fun go(next: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        if (next >= steps.size) onFinish() else index = next.coerceAtLeast(0)
    }

    BackHandler { if (index > 0) go(index - 1) else onFinish() }

    // Ventana iluminada: se expande desde el centro cuando no hay objetivo.
    val padding = with(density) { 6.dp.toPx() }
    val desired = target?.inflate(padding)
    var lastCenter by androidx.compose.runtime.remember { mutableStateOf(Offset.Zero) }
    if (desired != null) lastCenter = desired.center
    val spotlight by animateValueAsState(
        targetValue = desired ?: Rect(lastCenter, 0f),
        typeConverter = Rect.VectorConverter,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "spotlight",
    )
    val radius by animateDpAsState(if (step.circular) 48.dp else step.corner, label = "radius")
    val scrimAlpha by animateFloatAsState(0.74f, tween(Motion.MEDIUM), label = "scrim")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                // Bloquea los toques sobre la app mientras dura el recorrido.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            drawRect(Color(0xFF05090D).copy(alpha = scrimAlpha))
            if (spotlight.width > 1f) {
                val corner = CornerRadius(radius.toPx().coerceAtMost(spotlight.minDimension / 2))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = spotlight.topLeft,
                    size = spotlight.size,
                    cornerRadius = corner,
                    blendMode = BlendMode.Clear,
                )
                val grow = 14.dp.toPx() * ring
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.55f * (1f - ring)),
                    topLeft = spotlight.topLeft - Offset(grow, grow),
                    size = Size(spotlight.width + grow * 2, spotlight.height + grow * 2),
                    cornerRadius = CornerRadius(corner.x + grow),
                    style = Stroke(2.dp.toPx()),
                )
            }
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (fadeIn(Motion.emphasized(Motion.MEDIUM)) + scaleIn(Motion.emphasized(Motion.MEDIUM), initialScale = 0.96f))
                    .togetherWith(fadeOut(Motion.emphasized(Motion.SHORT)))
            },
            label = "tourCard",
        ) { current ->
            val currentStep = steps[current]
            TooltipLayout(anchor = currentStep.targetId?.let { targets.bounds[it]?.inflate(padding) }) {
                TourCard(
                    step = currentStep,
                    index = current,
                    total = steps.size,
                    onSkip = onFinish,
                    onBack = { go(current - 1) },
                    onNext = { go(current + 1) },
                )
            }
        }
    }
}

/** Coloca la tarjeta encima o debajo del elemento resaltado, según el espacio disponible. */
@Composable
private fun TooltipLayout(anchor: Rect?, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val top = WindowInsets.statusBars.getTop(density)
    val bottom = WindowInsets.navigationBars.getBottom(density)
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val side = Spacing.screen.roundToPx()
        val gap = 16.dp.roundToPx()
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val placeable = measurables.first().measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = (width - side * 2).coerceAtLeast(0)),
        )
        val y = when {
            anchor == null -> (height - placeable.height) / 2
            anchor.center.y > height / 2f -> anchor.top.toInt() - gap - placeable.height
            else -> anchor.bottom.toInt() + gap
        }.coerceIn(top + gap, (height - bottom - gap - placeable.height).coerceAtLeast(top + gap))
        layout(width, height) { placeable.place(side, y) }
    }
}

@Composable
private fun TourCard(
    step: TourStep,
    index: Int,
    total: Int,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val isIntro = index == 0
    val isLast = index == total - 1
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(Spacing.xl)) {
            if (step.icon != null) {
                Box(
                    Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(step.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(Spacing.md))
            }
            if (!isIntro) {
                Text(
                    "Paso $index de ${total - 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(step.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.sm))
            Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.lg))
            if (!isIntro) {
                ProgressDots(current = index - 1, total = total - 1)
                Spacer(Modifier.height(Spacing.md))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isLast) {
                    TextButton(onClick = onSkip) { Text(if (isIntro) "Ahora no" else "Omitir") }
                }
                Spacer(Modifier.weight(1f))
                if (index > 1) {
                    TextButton(onClick = onBack) { Text("Atrás") }
                    Spacer(Modifier.width(4.dp))
                }
                Button(onClick = onNext, shape = CircleShape) {
                    Text(
                        when {
                            isIntro -> "Empezar recorrido"
                            isLast -> "¡Entendido!"
                            else -> "Siguiente"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 20.dp else 6.dp, Motion.gentle(), label = "dotWidth")
            val color by animateColorAsState(
                if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(Modifier.height(6.dp).width(width).clip(CircleShape).background(color))
        }
    }
}
