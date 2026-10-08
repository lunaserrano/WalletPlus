package com.walletplus.domain.insights

import com.walletplus.domain.calc.BudgetProgress
import com.walletplus.domain.calc.BudgetStatus
import com.walletplus.domain.calc.CategoryChange
import com.walletplus.domain.calc.CategoryShare
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.calc.OptionalSpending
import com.walletplus.domain.calc.PeriodSummary
import com.walletplus.domain.calc.SmallExpensesSummary
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.rules.InsightThresholds
import com.walletplus.domain.time.PeriodType

/**
 * Todo lo que las reglas necesitan para evaluar un periodo. [summary] es el periodo analizado
 * (semana, quincena o mes); [budgetSummary] es el ciclo de pago contra el que se miden plan y presupuestos.
 */
data class InsightContext(
    val summary: PeriodSummary,
    val previousSummary: PeriodSummary,
    val distribution: List<CategoryShare>,
    val categoryChanges: List<CategoryChange>,
    val optionalSpending: OptionalSpending,
    val smallExpenses: SmallExpensesSummary?,
    val budgets: List<BudgetProgress>,
    val goals: List<GoalProgress>,
    val plan: SpendingPlan,
    val thresholds: InsightThresholds,
    val budgetSummary: PeriodSummary = summary,
) {
    val period: PeriodType get() = summary.period.type
    val payPeriod: PeriodType get() = budgetSummary.period.type
}

fun interface InsightRule {
    fun evaluate(context: InsightContext): List<Insight>
}

/**
 * Motor de recomendaciones basado en reglas locales (sin IA externa).
 * Las reglas son independientes y se pueden añadir o quitar sin tocar el resto.
 */
class RecommendationEngine(
    private val rules: List<InsightRule> = DefaultRules.all,
) {
    fun generate(context: InsightContext): List<Insight> =
        rules.flatMap { it.evaluate(context) }.sortedByDescending { it.priority }
}

object DefaultRules {

    /** Regla 1: gastos opcionales por encima del umbral. */
    val optionalShare = InsightRule { ctx ->
        val share = ctx.optionalSpending.share ?: return@InsightRule emptyList()
        when {
            share > ctx.thresholds.optionalShareAlert ->
                listOf(Insight.HighOptionalSpending(ctx.optionalSpending.total, share))
            ctx.optionalSpending.total.isPositive ->
                listOf(Insight.OptionalSpendingSummary(ctx.optionalSpending.total, share, ctx.period))
            else -> emptyList()
        }
    }

    /** Regla 2: categorías que aumentaron respecto al periodo anterior. */
    val categoryIncrease = InsightRule { ctx ->
        if (!ctx.previousSummary.hasData) return@InsightRule emptyList()
        ctx.categoryChanges
            .filter { change -> change.change?.let { it > ctx.thresholds.categoryIncreaseAlert } == true }
            .take(2)
            .map { Insight.CategoryIncreased(it.category, it.previous, it.current, it.change!!, ctx.period) }
    }

    /** Regla 3: presupuestos excedidos o cerca del límite. */
    val budgets = InsightRule { ctx ->
        ctx.budgets.mapNotNull { progress ->
            when (progress.status) {
                BudgetStatus.EXCEEDED -> Insight.BudgetExceeded(progress.category, progress.budget.limit, progress.spent)
                BudgetStatus.HIGH -> progress.percent?.let {
                    Insight.BudgetNearLimit(progress.category, it, progress.remaining, ctx.payPeriod)
                }
                else -> null
            }
        }
    }

    /** Regla 4: ritmo de ahorro para la meta activa más cercana (por quincena si cobra quincenalmente). */
    val goalPace = InsightRule { ctx ->
        val perFortnight = ctx.payPeriod == PeriodType.BIWEEKLY
        ctx.goals
            .filter { !it.isCompleted && !it.goal.isArchived && it.perWeek != null }
            .minByOrNull { it.daysLeft ?: Long.MAX_VALUE }
            ?.let { goal ->
                val amount = if (perFortnight) goal.perFortnight!! else goal.perWeek!!
                val period = if (perFortnight) PeriodType.BIWEEKLY else PeriodType.WEEKLY
                listOf(Insight.GoalPace(goal.goal.name, amount, goal.remaining, period))
            }
            ?: emptyList()
    }

    /** Regla 5: porcentaje de ahorro del periodo (o alerta si se gastó más de lo ingresado). */
    val savingsRate = InsightRule { ctx ->
        val summary = ctx.summary
        val rate = summary.savingsRate
        when {
            summary.income.isPositive && summary.expense > summary.income ->
                listOf(Insight.PeriodOverspent(summary.income, summary.expense, ctx.period))
            rate != null && rate.isPositive -> listOf(Insight.SavingsRate(rate, summary.savings, ctx.period))
            else -> emptyList()
        }
    }

    val topCategory = InsightRule { ctx ->
        ctx.distribution.firstOrNull()
            ?.let { listOf(Insight.TopCategory(it.category, it.amount, it.share, ctx.period)) }
            ?: emptyList()
    }

    val smallExpenses = InsightRule { ctx ->
        ctx.smallExpenses?.let { listOf(Insight.SmallExpensesAddUp(it.count, it.total, it.limit, ctx.period)) } ?: emptyList()
    }

    val planLimit = InsightRule { ctx ->
        val limit = ctx.plan.spendingLimit
        val spent = ctx.budgetSummary.expense
        if (limit.isPositive && spent > limit) {
            listOf(Insight.PlanSpendingExceeded(limit, spent, ctx.payPeriod))
        } else {
            emptyList()
        }
    }

    val all: List<InsightRule> = listOf(
        optionalShare, categoryIncrease, budgets, goalPace, savingsRate, topCategory, smallExpenses, planLimit,
    )
}
