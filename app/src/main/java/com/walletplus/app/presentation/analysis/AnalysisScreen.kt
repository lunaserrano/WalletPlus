package com.walletplus.app.presentation.analysis

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.format.PercentFormat
import com.walletplus.app.core.ui.components.CategoryAvatar
import com.walletplus.app.core.ui.components.ChartSlice
import com.walletplus.app.core.ui.components.Dot
import com.walletplus.app.core.ui.components.EmptyState
import com.walletplus.app.core.ui.components.MoneyText
import com.walletplus.app.core.ui.components.ProgressBar
import com.walletplus.app.core.ui.components.ScreenTitle
import com.walletplus.app.core.ui.components.SectionHeader
import com.walletplus.app.core.ui.components.StackedBar
import com.walletplus.app.core.ui.components.TrendBar
import com.walletplus.app.core.ui.components.TrendChart
import com.walletplus.app.core.ui.components.WalletCard
import com.walletplus.app.core.ui.theme.CategoryPalette
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.core.ui.theme.tabular
import com.walletplus.app.presentation.common.EDUCATIONAL_DISCLAIMER
import com.walletplus.app.presentation.common.InsightCard
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import com.walletplus.domain.usecase.FinancialSnapshot

@Composable
fun AnalysisScreen(contentPadding: PaddingValues, onAddTransaction: () -> Unit) {
    val viewModel = walletViewModel { AnalysisViewModel(it.observeSnapshot, it.observeTrend, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item {
            ScreenTitle("Análisis") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mes anterior")
                    }
                    AnimatedContent(
                        targetState = state.month,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally { if (forward) it / 2 else -it / 2 } + fadeIn(Motion.emphasized()))
                                .togetherWith(slideOutHorizontally { if (forward) -it / 2 else it / 2 } + fadeOut(Motion.emphasized(Motion.SHORT)))
                        },
                        label = "month",
                    ) { month ->
                        Text(
                            DateFormats.monthShort(month) + if (month.year != state.currentMonth.year) " ${month.year % 100}" else "",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = Spacing.sm),
                        )
                    }
                    FilledTonalIconButton(onClick = viewModel::nextMonth, enabled = state.canGoForward) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Mes siguiente")
                    }
                }
            }
        }

        if (state.trend.any { it.income.isPositive || it.expense.isPositive }) {
            item(key = "trend") {
                TrendCard(state) { index -> viewModel.selectMonth(state.trend[index].month) }
            }
        }

        if (snapshot == null) return@LazyColumn

        if (!snapshot.summary.hasData) {
            item {
                WalletCard {
                    EmptyState(
                        emoji = "📊",
                        title = "Sin datos en ${DateFormats.month(state.month, snapshot.today)}",
                        message = "Cuando registres movimientos este mes, aquí verás en qué gastas y dónde podrías ahorrar.",
                        actionLabel = "Registrar movimiento",
                        onAction = onAddTransaction,
                    )
                }
            }
            return@LazyColumn
        }

        item(key = "summary") { SummaryCard(snapshot) }

        if (snapshot.distribution.isNotEmpty()) {
            item(key = "where-header") { SectionHeader("¿En qué estás gastando?") }
            item(key = "where") { DistributionList(snapshot) }
        }

        if (snapshot.summary.expense.isPositive) {
            item(key = "optional") { NecessaryVsOptional(snapshot) }
        }

        val changes = snapshot.categoryChanges.filter { it.change != null && it.current != it.previous }
        if (changes.isNotEmpty() && snapshot.previousSummary.hasData) {
            item(key = "changes-header") { SectionHeader("Respecto al mes anterior") }
            item(key = "changes") { ChangesCard(snapshot) }
        }

        snapshot.smallExpenses?.let { small ->
            item(key = "small") {
                val formatter = LocalMoneyFormatter.current
                WalletCard {
                    Text("Gastos pequeños frecuentes", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(small.total, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "  en ${small.count} gastos menores de ${formatter.format(small.limit, showCents = false)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Cafés, snacks o transporte corto: por separado parecen poco, pero juntos suman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (snapshot.insights.isNotEmpty()) {
            item(key = "insights-header") { SectionHeader("Recomendaciones") }
            snapshot.insights.forEach { insight ->
                item(key = "insight-${insight::class.simpleName}-${insight.hashCode()}") { InsightCard(insight) }
            }
        }

        item(key = "disclaimer") {
            Row(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(EDUCATIONAL_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrendCard(state: AnalysisState, onSelect: (Int) -> Unit) {
    val formatter = LocalMoneyFormatter.current
    val selectedIndex = state.trend.indexOfFirst { it.month == state.month }
    val selected = state.trend.getOrNull(selectedIndex)
    WalletCard {
        Text("Últimos ${state.trend.size} meses", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.sm))
        // Leyenda + valores del mes seleccionado (la identidad nunca depende solo del color).
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            LegendValue("Ingresos", WalletTheme.colors.income, selected?.income?.let { formatter.format(it) } ?: "—")
            LegendValue("Gastos", WalletTheme.colors.expense, selected?.expense?.let { formatter.format(it) } ?: "—")
        }
        Spacer(Modifier.height(Spacing.lg))
        TrendChart(
            bars = state.trend.map { TrendBar(DateFormats.monthShort(it.month), it.income.cents, it.expense.cents) },
            incomeColor = WalletTheme.colors.income,
            expenseColor = WalletTheme.colors.expense,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            description = "Ingresos y gastos de los últimos meses. Toca un mes para verlo en detalle.",
        )
    }
}

@Composable
private fun LegendValue(label: String, color: Color, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, size = 8.dp)
            Spacer(Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleSmall.tabular())
    }
}

@Composable
private fun SummaryCard(snapshot: FinancialSnapshot) {
    val summary = snapshot.summary
    val previous = snapshot.previousSummary
    WalletCard {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            SummaryMetric("Ingresos", summary.income, previous.income, previous.hasData, higherIsGood = true, partial = snapshot.month == java.time.YearMonth.from(snapshot.today), modifier = Modifier.weight(1f))
            SummaryMetric("Gastos", summary.expense, previous.expense, previous.hasData, higherIsGood = false, partial = snapshot.month == java.time.YearMonth.from(snapshot.today), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ahorro del mes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(
                    summary.savings,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (summary.savings.isNegative) WalletTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Porcentaje ahorrado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    PercentFormat.format(summary.savingsRate),
                    style = MaterialTheme.typography.headlineSmall.tabular(),
                    color = if ((summary.savingsRate?.basisPoints ?: 0) > 0) WalletTheme.colors.income else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    current: Money,
    previous: Money,
    hasPrevious: Boolean,
    higherIsGood: Boolean,
    partial: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(current, style = MaterialTheme.typography.titleLarge)
        val change = if (hasPrevious) Percent.change(previous, current) else null
        // El mes en curso aún no termina: se compara contra el mes anterior completo.
        val versus = if (partial) "vs. mes anterior completo" else "vs. mes anterior"
        if (change != null && change.basisPoints == 0L) {
            Text("Igual que el mes anterior", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (change != null) {
            val up = change.basisPoints > 0
            val good = up == higherIsGood
            val color = if (good) WalletTheme.colors.income else WalletTheme.colors.expense
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(
                    "${PercentFormat.format(change).removePrefix("−")} $versus",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        } else {
            Text("Sin comparación", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DistributionList(snapshot: FinancialSnapshot) {
    val formatter = LocalMoneyFormatter.current
    val dark = isSystemInDarkTheme()
    WalletCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            snapshot.distribution.forEach { share ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(share.category.emoji, share.category.colorKey, size = 36.dp)
                    Spacer(Modifier.size(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                share.category.name,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(formatter.format(share.amount), style = MaterialTheme.typography.labelLarge.tabular())
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ProgressBar(share.share.toFraction(), CategoryPalette.color(share.category.colorKey, dark), height = 6.dp)
                            }
                            Text(
                                PercentFormat.format(share.share),
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NecessaryVsOptional(snapshot: FinancialSnapshot) {
    val formatter = LocalMoneyFormatter.current
    val optional = snapshot.optionalSpending
    val necessary = snapshot.summary.expense - optional.total
    val necessaryColor = MaterialTheme.colorScheme.secondary
    val optionalColor = WalletTheme.colors.caution
    WalletCard {
        Text("Necesario vs. opcional", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.md))
        StackedBar(
            listOf(
                ChartSlice(necessary.cents.toFloat(), necessaryColor),
                ChartSlice(optional.total.cents.toFloat(), optionalColor),
            ),
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            LegendValue(
                "Necesario · ${PercentFormat.format(Percent.of(necessary, snapshot.summary.expense))}",
                necessaryColor,
                formatter.format(necessary),
            )
            LegendValue(
                "Opcional · ${PercentFormat.format(optional.share)}",
                optionalColor,
                formatter.format(optional.total),
            )
        }
        if (optional.total.isPositive) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                "Este mes has gastado ${formatter.format(optional.total)} en ${optional.count} gastos que marcaste como opcionales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChangesCard(snapshot: FinancialSnapshot) {
    val formatter = LocalMoneyFormatter.current
    val changes = snapshot.categoryChanges.filter { it.change != null && it.current != it.previous }.take(5)
    WalletCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            changes.forEach { change ->
                val up = change.change!!.basisPoints > 0
                val color = if (up) WalletTheme.colors.expense else WalletTheme.colors.income
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(change.category.emoji, change.category.colorKey, size = 36.dp)
                    Spacer(Modifier.size(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(change.category.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${formatter.format(change.previous)} → ${formatter.format(change.current)}",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, contentDescription = if (up) "Aumentó" else "Disminuyó", tint = color, modifier = Modifier.size(16.dp))
                        Text(
                            PercentFormat.format(change.change).removePrefix("−"),
                            style = MaterialTheme.typography.labelLarge.tabular(),
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
