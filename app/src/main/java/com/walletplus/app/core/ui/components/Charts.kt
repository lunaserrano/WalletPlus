package com.walletplus.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import com.walletplus.app.core.ui.theme.Motion

data class ChartSlice(val value: Float, val color: Color)

/**
 * Donut con separaciones entre segmentos y animación de barrido.
 * La identidad nunca depende solo del color: siempre se acompaña de leyenda.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    thickness: Dp = 18.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    description: String = "",
    center: @Composable BoxScope.() -> Unit = {},
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(Motion.LONG + 200, easing = FastOutSlowInEasing))
    }
    Box(modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas
            // Separación de ~2px convertida a grados sobre la circunferencia.
            val gapDegrees = if (slices.size > 1) (2.dp.toPx() + stroke) / (Math.PI.toFloat() * diameter) * 360f else 0f
            var start = -90f
            val visible = 360f * sweep.value
            slices.forEach { slice ->
                val full = slice.value / total * 360f
                val drawn = (full - gapDegrees).coerceAtLeast(0.5f)
                val clipped = (visible - (start + 90f)).coerceIn(0f, drawn)
                if (clipped > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = start + gapDegrees / 2,
                        sweepAngle = clipped,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                start += full
            }
        }
        center()
    }
}

data class TrendBar(val label: String, val income: Long, val expense: Long)

/**
 * Barras agrupadas ingresos/gastos por mes. Un solo eje, marcas finas con extremos
 * redondeados, rejilla discreta y selección por toque (la barra seleccionada se resalta).
 */
@Composable
fun TrendChart(
    bars: List<TrendBar>,
    incomeColor: Color,
    expenseColor: Color,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    description: String = "",
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val strongText = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    val grow = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(Motion.LONG, easing = FastOutSlowInEasing))
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(bars.size) {
                detectTapGestures { offset ->
                    if (bars.isEmpty()) return@detectTapGestures
                    val index = (offset.x / (size.width / bars.size)).toInt().coerceIn(0, bars.lastIndex)
                    onSelect(index)
                }
            },
    ) {
        if (bars.isEmpty()) return@Canvas
        val labelHeight = 22.dp.toPx()
        val chartHeight = size.height - labelHeight
        val maxValue = bars.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1L).toFloat()
        val slot = size.width / bars.size
        val barWidth = (slot * 0.24f).coerceAtMost(14.dp.toPx())
        val gap = 2.dp.toPx()
        val radius = CornerRadius(4.dp.toPx())

        // Rejilla recesiva: tres líneas horizontales.
        for (i in 0..2) {
            val y = chartHeight * i / 3f
            drawLine(grid.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        drawLine(grid, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.dp.toPx())

        bars.forEachIndexed { index, bar ->
            val centerX = slot * index + slot / 2
            if (index == selectedIndex) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(slot * index + slot * 0.12f, 0f),
                    size = Size(slot * 0.76f, chartHeight),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                )
            }
            val incomeH = bar.income / maxValue * chartHeight * 0.92f * grow.value
            val expenseH = bar.expense / maxValue * chartHeight * 0.92f * grow.value
            drawTopRoundedBar(incomeColor, centerX - gap / 2 - barWidth, chartHeight, barWidth, incomeH, radius.x)
            drawTopRoundedBar(expenseColor, centerX + gap / 2, chartHeight, barWidth, expenseH, radius.x)
            drawLabel(measurer, bar.label, centerX, chartHeight + 6.dp.toPx(), labelStyle.copy(color = if (index == selectedIndex) strongText else mutedText))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopRoundedBar(
    color: Color,
    left: Float,
    baseline: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    if (height <= 0f) return
    val r = radius.coerceAtMost(height).coerceAtMost(width / 2)
    val top = baseline - height
    val path = Path().apply {
        moveTo(left, baseline)
        lineTo(left, top + r)
        quadraticTo(left, top, left + r, top)
        lineTo(left + width - r, top)
        quadraticTo(left + width, top, left + width, top + r)
        lineTo(left + width, baseline)
        close()
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    centerX: Float,
    top: Float,
    style: androidx.compose.ui.text.TextStyle,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top))
}

/** Barra horizontal apilada con separación de 2dp entre segmentos. */
@Composable
fun StackedBar(
    segments: List<ChartSlice>,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    Row(
        modifier.fillMaxWidth().height(height).clip(CircleShape).background(if (total <= 0f) trackColor else Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total > 0f) {
            segments.filter { it.value > 0f }.forEach { segment ->
                Box(
                    Modifier
                        .weight(segment.value / total)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(segment.color),
                )
            }
        } else {
            Box(Modifier.width(0.dp))
        }
    }
}
