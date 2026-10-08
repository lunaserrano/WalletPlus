package com.walletplus.domain.insights

import com.walletplus.domain.calc.BudgetProgress
import com.walletplus.domain.calc.BudgetStatus
import com.walletplus.domain.calc.CategoryChange
import com.walletplus.domain.calc.CategoryShare
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.calc.MonthlySummary
import com.walletplus.domain.calc.OptionalSpending
import com.walletplus.domain.calc.SmallExpensesSummary
import com.walletplus.domain.model.MonthlyPlan
import com.walletplus.domain.rules.InsightThresholds

/** Todo lo que las reglas necesitan para evaluar el mes en curso. */
data class InsightContext(
    val summary: MonthlySummary,
    val previousSummary: MonthlySummary,
    val distribution: List<CategoryShare>,
    val categoryChanges: List<CategoryChange>,
    val optionalSpending: OptionalSpending,
    val smallExpenses: SmallExpensesSummary?,
    val budgets: List<BudgetProgress>,
    val goals: List<GoalProgress>,
    val plan: MonthlyPlan,
    val thresholds: InsightThresholds,
)

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
                listOf(Insight.OptionalSpendingSummary(ctx.optionalSpending.total, share))
            else -> emptyList()
        }
    }

    /** Regla 2: categorías que aumentaron respecto al mes anterior. */
    val categoryIncrease = InsightRule { ctx ->
        if (!ctx.previousSummary.hasData) return@InsightRule emptyList()
        ctx.categoryChanges
            .filter { change -> change.change?.let { it > ctx.thresholds.categoryIncreaseAlert } == true }
            .take(2)
            .map { Insight.CategoryIncreased(it.category, it.previous, it.current, it.change!!) }
    }

    /** Regla 3: presupuestos excedidos o cerca del límite. */
    val budgets = InsightRule { ctx ->
        ctx.budgets.mapNotNull { progress ->
            when (progress.status) {
                BudgetStatus.EXCEEDED -> Insight.BudgetExceeded(progress.category, progress.budget.limit, progress.spent)
                BudgetStatus.HIGH -> progress.percent?.let {
                    Insight.BudgetNearLimit(progress.category, it, progress.remaining)
                }
                else -> null
            }
        }
    }

    /** Regla 4: ritmo de ahorro para la meta activa más cercana. */
    val goalPace = InsightRule { ctx ->
        ctx.goals
            .filter { !it.isCompleted && !it.goal.isArchived && it.perWeek != null }
            .minByOrNull { it.daysLeft ?: Long.MAX_VALUE }
            ?.let { listOf(Insight.GoalPace(it.goal.name, it.perWeek!!, it.remaining)) }
            ?: emptyList()
    }

    /** Regla 5: porcentaje de ahorro del mes (o alerta si se gastó más de lo ingresado). */
    val savingsRate = InsightRule { ctx ->
        val summary = ctx.summary
        val rate = summary.savingsRate
        when {
            summary.income.isPositive && summary.expense > summary.income ->
                listOf(Insight.MonthOverspent(summary.income, summary.expense))
            rate != null && rate.isPositive -> listOf(Insight.SavingsRate(rate, summary.savings))
            else -> emptyList()
        }
    }

    val topCategory = InsightRule { ctx ->
        ctx.distribution.firstOrNull()
            ?.let { listOf(Insight.TopCategory(it.category, it.amount, it.share)) }
            ?: emptyList()
    }

    val smallExpenses = InsightRule { ctx ->
        ctx.smallExpenses?.let { listOf(Insight.SmallExpensesAddUp(it.count, it.total, it.limit)) } ?: emptyList()
    }

    val planLimit = InsightRule { ctx ->
        val limit = ctx.plan.spendingLimit
        if (limit.isPositive && ctx.summary.expense > limit) {
            listOf(Insight.PlanSpendingExceeded(limit, ctx.summary.expense))
        } else {
            emptyList()
        }
    }

    val all: List<InsightRule> = listOf(
        optionalShare, categoryIncrease, budgets, goalPace, savingsRate, topCategory, smallExpenses, planLimit,
    )
}
