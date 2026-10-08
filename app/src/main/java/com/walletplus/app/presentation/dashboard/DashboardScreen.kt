package com.walletplus.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.format.PercentFormat
import com.walletplus.app.core.ui.components.AnimatedMoneyText
import com.walletplus.app.core.ui.components.ChartSlice
import com.walletplus.app.core.ui.components.DemoBanner
import com.walletplus.app.core.ui.components.DonutChart
import com.walletplus.app.core.ui.components.Dot
import com.walletplus.app.core.ui.components.EmptyState
import com.walletplus.app.core.ui.components.MoneyText
import com.walletplus.app.core.ui.components.ProgressBar
import com.walletplus.app.core.ui.components.SectionHeader
import com.walletplus.app.core.ui.components.WalletCard
import com.walletplus.app.core.ui.components.categoryColor
import com.walletplus.app.core.ui.theme.CategoryPalette
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.core.ui.theme.tabular
import com.walletplus.app.presentation.common.InsightCard
import com.walletplus.app.presentation.common.TransactionRow
import com.walletplus.app.presentation.tour.TourIds
import com.walletplus.app.presentation.tour.tourTarget
import com.walletplus.domain.calc.CategoryShare
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.calc.PlanComparison
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import com.walletplus.domain.money.sumOfMoney
import com.walletplus.domain.usecase.FinancialSnapshot
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onAddTransaction: (Long, TransactionType) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenPlan: (Int) -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel = walletViewModel { DashboardViewModel(it.observeSnapshot, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        DashboardState.Loading -> Box(Modifier.fillMaxSize())
        is DashboardState.Ready -> DashboardContent(
            snapshot = current.snapshot,
            greeting = current.greeting,
            contentPadding = contentPadding,
            onAddTransaction = onAddTransaction,
            onEditTransaction = onEditTransaction,
            onOpenTransactions = onOpenTransactions,
            onOpenPlan = onOpenPlan,
            onOpenAnalysis = onOpenAnalysis,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun DashboardContent(
    snapshot: FinancialSnapshot,
    greeting: String,
    contentPadding: PaddingValues,
    onAddTransaction: (Long, TransactionType) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenPlan: (Int) -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val categoriesById = remember(snapshot.categories) { snapshot.categories.associateBy { it.id } }
    val hasMonthData = snapshot.summary.hasData

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

        // 1. Saludo
        item(key = "greeting") {
            Row(Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        DateFormats.long(snapshot.today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = onOpenSettings, modifier = Modifier.tourTarget(TourIds.SETTINGS)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Configuración")
                }
            }
        }

        if (snapshot.settings.demoMode) {
            item(key = "demo") { DemoBanner(onManage = onOpenSettings) }
        }

        // 2-3. Saldo, ingresos y gastos
        item(key = "balance") { Reveal(0) { BalanceHero(snapshot) } }

        if (!hasMonthData && snapshot.totals.income.isZero && snapshot.totals.expense.isZero) {
            item(key = "empty") {
                WalletCard {
                    EmptyState(
                        emoji = "✍️",
                        title = "Registra tu primer movimiento",
                        message = "Anota un gasto o un ingreso y aquí verás tu resumen, presupuestos y recomendaciones.",
                        actionLabel = "Agregar gasto",
                        onAction = { onAddTransaction(0, TransactionType.EXPENSE) },
                    )
                }
            }
            return@LazyColumn
        }

        // 4. Capacidad de ahorro
        item(key = "savings") { Reveal(1) { SavingsCard(snapshot) } }

        // 5. Presupuesto general
        item(key = "plan") { Reveal(2) { PlanCard(snapshot.plan, snapshot.settings.plan.isConfigured) { onOpenPlan(0) } } }

        // 6. Distribución de gastos
        if (snapshot.distribution.isNotEmpty()) {
            item(key = "distribution") {
                Reveal(3) { DistributionCard(snapshot.distribution, snapshot.summary.expense, onOpenAnalysis) }
            }
        }

        // 7. Meta de ahorro
        snapshot.goals.filter { !it.isCompleted }.minByOrNull { it.daysLeft ?: Long.MAX_VALUE }?.let { goal ->
            item(key = "goal") { Reveal(4) { GoalHighlight(goal) { onOpenPlan(1) } } }
        }

        // 8. Recomendación principal
        snapshot.insights.firstOrNull()?.let { insight ->
            item(key = "insight-header") { SectionHeader("Para ti", action = "Ver análisis", onAction = onOpenAnalysis) }
            item(key = "insight") { InsightCard(insight, onClick = onOpenAnalysis) }
        }

        recentTransactions(snapshot, categoriesById, onEditTransaction, onOpenTransactions)
    }
}

private fun LazyListScope.recentTransactions(
    snapshot: FinancialSnapshot,
    categoriesById: Map<Long, com.walletplus.domain.model.Category>,
    onEdit: (Long) -> Unit,
    onOpenAll: () -> Unit,
) {
    val recent = snapshot.monthTransactions.take(5)
    if (recent.isEmpty()) return
    item(key = "recent-header") { SectionHeader("Movimientos recientes", action = "Ver todo", onAction = onOpenAll) }
    item(key = "recent") {
        WalletCard(contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)) {
            recent.forEach { tx ->
                TransactionRow(tx, categoriesById[tx.categoryId], snapshot.today, onClick = { onEdit(tx.id) })
            }
        }
    }
}

/** Entrada escalonada suave de las tarjetas al abrir la pantalla. */
@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(Motion.emphasized(Motion.MEDIUM + index * 60)) +
            slideInVertically(Motion.emphasized(Motion.MEDIUM + index * 60)) { it / 6 },
    ) { content() }
}

@Composable
private fun BalanceHero(snapshot: FinancialSnapshot) {
    val colors = WalletTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .tourTarget(TourIds.BALANCE)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd))),
    ) {
        // Brillo decorativo
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x3329D69A), Color.Transparent),
                    center = Offset(size.width * 0.95f, 0f),
                    radius = size.width * 0.7f,
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.95f, 0f),
            )
        }
        Column(Modifier.padding(Spacing.xxl)) {
            Text("Saldo actual", style = MaterialTheme.typography.labelLarge, color = colors.onHeroMuted)
            Spacer(Modifier.height(Spacing.xs))
            AnimatedMoneyText(
                amount = snapshot.balance,
                style = MaterialTheme.typography.displayMedium,
                color = colors.onHero,
            )
            Spacer(Modifier.height(Spacing.xl))
            Text(
                DateFormats.month(snapshot.month, snapshot.today),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onHeroMuted,
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                HeroFlow(
                    label = "Ingresos",
                    amount = snapshot.summary.income,
                    icon = Icons.Rounded.ArrowDownward,
                    accent = Color(0xFF5FD9AE),
                    modifier = Modifier.weight(1f),
                )
                HeroFlow(
                    label = "Gastos",
                    amount = -snapshot.summary.expense,
                    icon = Icons.Rounded.ArrowUpward,
                    accent = Color(0xFFFF8A8F),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroFlow(
    label: String,
    amount: Money,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = WalletTheme.colors.onHeroMuted)
            MoneyText(amount, signed = true, style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
    }
}

@Composable
private fun SavingsCard(snapshot: FinancialSnapshot) {
    val summary = snapshot.summary
    val rate = summary.savingsRate
    val positive = !summary.savings.isNegative
    val accent = if (positive) WalletTheme.colors.income else WalletTheme.colors.expense
    WalletCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Capacidad de ahorro", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                MoneyText(summary.savings, style = MaterialTheme.typography.headlineMedium, color = if (positive) MaterialTheme.colorScheme.onSurface else accent)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    when {
                        rate == null -> "Registra tus ingresos para calcular tu ahorro."
                        positive -> "Has ahorrado el ${PercentFormat.format(rate)} de tus ingresos este mes."
                        else -> "Este mes gastas más de lo que ingresas."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(Spacing.lg))
            RingProgress(
                fraction = rate?.toFraction()?.coerceIn(0f, 1f) ?: 0f,
                color = accent,
                label = PercentFormat.format(rate?.takeIf { positive }),
            )
        }
    }
}

@Composable
private fun RingProgress(fraction: Float, color: Color, label: String) {
    val track = WalletTheme.colors.track
    val animated by androidx.compose.animation.core.animateFloatAsState(
        fraction, Motion.emphasized(Motion.LONG), label = "ring",
    )
    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(76.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(track, 0f, 360f, false, style = Stroke(stroke), topLeft = Offset(stroke / 2, stroke / 2), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
            drawArc(color, -90f, 360f * animated, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(stroke / 2, stroke / 2), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
        }
        Text(label, style = MaterialTheme.typography.labelLarge.tabular(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlanCard(plan: PlanComparison, configured: Boolean, onOpen: () -> Unit) {
    WalletCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Presupuesto del mes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(Spacing.md))
        if (!configured || !plan.spendingLimit.isPositive) {
            Text(
                "Define cuánto quieres gastar y ahorrar cada mes para comparar tu plan con la realidad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text("Configurar presupuesto →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            return@WalletCard
        }
        val progress = plan.spendingProgress
        val remaining = plan.spendingLimit - plan.actualExpense
        val color = when {
            progress == null -> WalletTheme.colors.normal
            progress > Percent.HUNDRED -> WalletTheme.colors.exceeded
            progress >= 90 -> WalletTheme.colors.high
            progress >= 70 -> WalletTheme.colors.caution
            else -> WalletTheme.colors.normal
        }
        Row(verticalAlignment = Alignment.Bottom) {
            MoneyText(plan.actualExpense, style = MaterialTheme.typography.titleLarge)
            Text(
                "  de ${LocalMoneyFormatter.current.format(plan.spendingLimit)}",
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        ProgressBar(progress?.toFraction() ?: 0f, color)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            if (remaining.isNegative) "Superaste tu límite por ${LocalMoneyFormatter.current.format(remaining.abs())}"
            else "Te quedan ${LocalMoneyFormatter.current.format(remaining)} para gastar este mes",
            style = MaterialTheme.typography.bodySmall,
            color = if (remaining.isNegative) WalletTheme.colors.exceeded else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (plan.savingsTarget.isPositive) {
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ahorro objetivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    "${LocalMoneyFormatter.current.format(plan.actualSavings.coerceAtLeast(Money.ZERO))} / ${LocalMoneyFormatter.current.format(plan.savingsTarget)}",
                    style = MaterialTheme.typography.labelMedium.tabular(),
                )
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(plan.savingsProgress?.toFraction() ?: 0f, MaterialTheme.colorScheme.tertiary, height = 6.dp)
        }
    }
}

/** Top 4 categorías + "Otros", siempre con leyenda de texto (la identidad no depende solo del color). */
@Composable
private fun DistributionCard(distribution: List<CategoryShare>, total: Money, onOpen: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val top = distribution.take(4)
    val rest = distribution.drop(4)
    val restAmount = rest.sumOfMoney { it.amount }
    val slices = top.map { ChartSlice(it.amount.cents.toFloat(), CategoryPalette.color(it.category.colorKey, dark)) } +
        listOfNotNull(restAmount.takeIf { it.isPositive }?.let { ChartSlice(it.cents.toFloat(), CategoryPalette.color("slate", dark)) })
    val formatter = LocalMoneyFormatter.current
    WalletCard(onClick = onOpen) {
        Text("Distribución de gastos", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                slices = slices,
                size = 132.dp,
                thickness = 16.dp,
                description = "Distribución de gastos: " + top.joinToString { "${it.category.name} ${PercentFormat.format(it.share)}" },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatter.compact(total), style = MaterialTheme.typography.titleSmall.tabular())
                }
            }
            Spacer(Modifier.size(Spacing.xl))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                top.forEach { share ->
                    LegendRow(share.category.emoji + " " + share.category.name, categoryColor(share.category.colorKey), PercentFormat.format(share.share))
                }
                if (restAmount.isPositive) {
                    LegendRow("Otros", categoryColor("slate"), PercentFormat.format(Percent.of(restAmount, total)))
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, size = 8.dp)
        Spacer(Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.labelMedium.tabular(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GoalHighlight(progress: GoalProgress, onOpen: () -> Unit) {
    val formatter = LocalMoneyFormatter.current
    WalletCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Text(progress.goal.emoji, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(progress.goal.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatter.format(progress.goal.saved)} de ${formatter.format(progress.goal.target)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(PercentFormat.format(progress.percent), style = MaterialTheme.typography.titleMedium.tabular(), color = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(Modifier.height(Spacing.md))
        ProgressBar(progress.percent.toFraction(), MaterialTheme.colorScheme.tertiary)
        progress.perWeek?.let {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Ahorra ${formatter.format(it)} por semana para llegar a tiempo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
