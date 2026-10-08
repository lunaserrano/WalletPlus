package com.walletplus.app.presentation.planning

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletplus.app.core.di.walletViewModel
import com.walletplus.app.core.format.DateFormats
import com.walletplus.app.core.format.LocalMoneyFormatter
import com.walletplus.app.core.format.PercentFormat
import com.walletplus.app.core.ui.components.CategoryAvatar
import com.walletplus.app.core.ui.components.EmptyState
import com.walletplus.app.core.ui.components.MoneyText
import com.walletplus.app.core.ui.components.Pill
import com.walletplus.app.core.ui.components.ProgressBar
import com.walletplus.app.core.ui.components.ScreenTitle
import com.walletplus.app.core.ui.components.SectionHeader
import com.walletplus.app.core.ui.components.SegmentedControl
import com.walletplus.app.core.ui.components.WalletCard
import com.walletplus.app.core.ui.components.statusColor
import com.walletplus.app.core.ui.components.statusLabel
import com.walletplus.app.core.ui.theme.Motion
import com.walletplus.app.core.ui.theme.Spacing
import com.walletplus.app.core.ui.theme.WalletTheme
import com.walletplus.app.core.ui.theme.tabular
import com.walletplus.domain.calc.BudgetProgress
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.calc.PlanComparison
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import com.walletplus.domain.usecase.FinancialSnapshot

/** Qué hoja está abierta en Planificar. */
sealed interface PlanSheet {
    data object MonthlyPlan : PlanSheet
    data class Budget(val progress: BudgetProgress?) : PlanSheet
    data class Goal(val progress: GoalProgress?) : PlanSheet
    data class Contribute(val progress: GoalProgress) : PlanSheet
}

@Composable
fun PlanningScreen(contentPadding: PaddingValues, initialTab: Int) {
    val viewModel = walletViewModel { c ->
        PlanningViewModel(
            c.observeSnapshot, c.saveBudget, c.deleteBudget, c.saveGoal, c.contributeToGoal, c.deleteGoal, c.updatePlan, c.clock,
        )
    }
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    var sheet by remember { mutableStateOf<PlanSheet?>(null) }
    val current = snapshot ?: return Box(Modifier.fillMaxSize())

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
        item { ScreenTitle("Planificar", subtitle = DateFormats.month(current.month, current.today)) }
        item {
            SegmentedControl(
                options = listOf("Presupuestos", "Metas"),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }
        item {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(Motion.emphasized()) togetherWith fadeOut(Motion.emphasized(Motion.SHORT)) },
                label = "planTab",
            ) { selected ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (selected == 0) BudgetsTab(current, onOpen = { sheet = it }) else GoalsTab(current, onOpen = { sheet = it })
                }
            }
        }
    }

    when (val open = sheet) {
        PlanSheet.MonthlyPlan -> MonthlyPlanSheet(current.settings.plan, viewModel::updatePlan, onDismiss = { sheet = null })
        is PlanSheet.Budget -> BudgetSheet(
            progress = open.progress,
            categories = viewModel.availableBudgetCategories(current, open.progress?.category?.id),
            viewModel = viewModel,
            onDismiss = { sheet = null },
        )
        is PlanSheet.Goal -> GoalSheet(open.progress, current.today, viewModel, onDismiss = { sheet = null })
        is PlanSheet.Contribute -> ContributeSheet(open.progress, viewModel, onDismiss = { sheet = null })
        null -> Unit
    }
}

@Composable
private fun BudgetsTab(snapshot: FinancialSnapshot, onOpen: (PlanSheet) -> Unit) {
    MonthlyPlanCard(snapshot.plan, snapshot.settings.plan.isConfigured) { onOpen(PlanSheet.MonthlyPlan) }
    SectionHeader("Por categoría")
    if (snapshot.budgets.isEmpty()) {
        WalletCard {
            EmptyState(
                emoji = "🎯",
                title = "Sin presupuestos todavía",
                message = "Ponle un límite mensual a las categorías que más te importan y te avisaremos al acercarte.",
                actionLabel = "Crear presupuesto",
                onAction = { onOpen(PlanSheet.Budget(null)) },
            )
        }
        return
    }
    snapshot.budgets.forEach { progress ->
        BudgetCard(progress) { onOpen(PlanSheet.Budget(progress)) }
    }
    val hasAvailable = snapshot.categories.any { c ->
        c.type == com.walletplus.domain.model.TransactionType.EXPENSE && c.isActive && snapshot.budgets.none { it.category.id == c.id }
    }
    if (hasAvailable) {
        OutlinedButton(
            onClick = { onOpen(PlanSheet.Budget(null)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Agregar presupuesto")
        }
    }
}

@Composable
private fun MonthlyPlanCard(plan: PlanComparison, configured: Boolean, onEdit: () -> Unit) {
    WalletCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Presupuesto general", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (configured) "Tu plan frente a lo real este mes" else "Define tu ingreso esperado, gasto máximo y ahorro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Editar presupuesto general") }
        }
        if (!configured) return@WalletCard
        Spacer(Modifier.height(Spacing.md))
        PlanRow("Ingresos", plan.actualIncome, plan.expectedIncome, plan.incomeProgress, WalletTheme.colors.income)
        Spacer(Modifier.height(Spacing.md))
        PlanRow(
            "Gastos",
            plan.actualExpense,
            plan.spendingLimit,
            plan.spendingProgress,
            when {
                plan.spendingProgress == null -> WalletTheme.colors.normal
                plan.spendingProgress!! > Percent.HUNDRED -> WalletTheme.colors.exceeded
                plan.spendingProgress!! >= 90 -> WalletTheme.colors.high
                plan.spendingProgress!! >= 70 -> WalletTheme.colors.caution
                else -> WalletTheme.colors.normal
            },
        )
        Spacer(Modifier.height(Spacing.md))
        PlanRow("Ahorro", plan.actualSavings, plan.savingsTarget, plan.savingsProgress, MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun PlanRow(label: String, actual: Money, planned: Money, progress: Percent?, color: Color) {
    val formatter = LocalMoneyFormatter.current
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                if (planned.isPositive) "${formatter.format(actual)} / ${formatter.format(planned)}" else formatter.format(actual),
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(if (planned.isPositive) progress?.toFraction() ?: 0f else 0f, color, height = 6.dp)
    }
}

@Composable
private fun BudgetCard(progress: BudgetProgress, onClick: () -> Unit) {
    val formatter = LocalMoneyFormatter.current
    val color = statusColor(progress.status)
    WalletCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(progress.category.emoji, progress.category.colorKey, muted = !progress.category.isActive)
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(progress.category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatter.format(progress.spent)} de ${formatter.format(progress.budget.limit)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(statusLabel(progress.status), color)
        }
        Spacer(Modifier.height(Spacing.md))
        ProgressBar(progress.percent?.toFraction() ?: if (progress.spent.isPositive) 1f else 0f, color)
        Spacer(Modifier.height(Spacing.sm))
        Row {
            Text(
                if (progress.remaining.isNegative) "Excedido por ${formatter.format(progress.remaining.abs())}"
                else "Disponible ${formatter.format(progress.remaining)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (progress.remaining.isNegative) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(PercentFormat.format(progress.percent), style = MaterialTheme.typography.labelMedium.tabular())
        }
    }
}

@Composable
private fun GoalsTab(snapshot: FinancialSnapshot, onOpen: (PlanSheet) -> Unit) {
    if (snapshot.goals.isEmpty()) {
        WalletCard {
            EmptyState(
                emoji = "🌱",
                title = "Crea tu primera meta",
                message = "Un fondo de emergencia, un viaje o un curso: te diremos cuánto apartar por semana para lograrlo.",
                actionLabel = "Nueva meta",
                onAction = { onOpen(PlanSheet.Goal(null)) },
            )
        }
        return
    }
    snapshot.goals.sortedBy { it.isCompleted }.forEach { progress ->
        GoalCard(
            progress = progress,
            onEdit = { onOpen(PlanSheet.Goal(progress)) },
            onContribute = { onOpen(PlanSheet.Contribute(progress)) },
        )
    }
    OutlinedButton(
        onClick = { onOpen(PlanSheet.Goal(null)) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Nueva meta")
    }
}

@Composable
private fun GoalCard(progress: GoalProgress, onEdit: () -> Unit, onContribute: () -> Unit) {
    val formatter = LocalMoneyFormatter.current
    val goal = progress.goal
    val accent = if (progress.isCompleted) WalletTheme.colors.income else MaterialTheme.colorScheme.tertiary
    WalletCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Text(goal.emoji, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(goal.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        progress.isCompleted -> "¡Meta cumplida! 🎉"
                        progress.isOverdue -> "La fecha objetivo ya pasó"
                        goal.targetDate != null -> "${DateFormats.short(goal.targetDate!!)} · ${progress.daysLeft} días"
                        else -> "Sin fecha objetivo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.isOverdue) WalletTheme.colors.caution else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(PercentFormat.format(progress.percent), style = MaterialTheme.typography.titleMedium.tabular(), color = accent)
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.Bottom) {
            MoneyText(goal.saved, style = MaterialTheme.typography.titleLarge)
            Text(
                "  de ${formatter.format(goal.target)}",
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        ProgressBar(progress.percent.toFraction(), accent, height = 10.dp)
        if (!progress.isCompleted) {
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                GoalStat("Te falta", formatter.format(progress.remaining), Modifier.weight(1f))
                progress.perWeek?.let { GoalStat("Por semana", formatter.format(it), Modifier.weight(1f)) }
                progress.perMonth?.let { GoalStat("Por mes", formatter.format(it), Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        FilledTonalButton(onClick = onContribute, modifier = Modifier.fillMaxWidth(), shape = CircleShape) {
            Text(if (progress.isCompleted) "Ajustar monto" else "Aportar o retirar")
        }
    }
}

@Composable
private fun GoalStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge.tabular(), maxLines = 1)
    }
}
