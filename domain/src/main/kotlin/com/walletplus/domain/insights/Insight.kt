package com.walletplus.domain.insights

import com.walletplus.domain.model.Category
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import com.walletplus.domain.time.PeriodType

enum class InsightTone { POSITIVE, NEUTRAL, ATTENTION, ALERT }

/**
 * Observación sobre los datos del usuario. El dominio solo describe el hallazgo;
 * la capa de presentación decide el texto (educativo, nunca como verdad absoluta).
 */
sealed interface Insight {
    /** Mayor = más relevante. */
    val priority: Int
    val tone: InsightTone

    data class BudgetExceeded(val category: Category, val limit: Money, val spent: Money) : Insight {
        override val priority = 100
        override val tone = InsightTone.ALERT
    }

    data class PeriodOverspent(val income: Money, val expense: Money, val period: PeriodType) : Insight {
        override val priority = 95
        override val tone = InsightTone.ALERT
    }

    data class PlanSpendingExceeded(val limit: Money, val spent: Money, val period: PeriodType) : Insight {
        override val priority = 90
        override val tone = InsightTone.ALERT
    }

    data class BudgetNearLimit(
        val category: Category,
        val percent: Percent,
        val remaining: Money,
        val period: PeriodType,
    ) : Insight {
        override val priority = 80
        override val tone = InsightTone.ATTENTION
    }

    data class CategoryIncreased(
        val category: Category,
        val previous: Money,
        val current: Money,
        val change: Percent,
        val period: PeriodType,
    ) : Insight {
        override val priority = 70
        override val tone = InsightTone.ATTENTION
    }

    data class HighOptionalSpending(val total: Money, val share: Percent) : Insight {
        override val priority = 65
        override val tone = InsightTone.ATTENTION
    }

    data class SmallExpensesAddUp(val count: Int, val total: Money, val limit: Money, val period: PeriodType) : Insight {
        override val priority = 55
        override val tone = InsightTone.NEUTRAL
    }

    /** [amount] es lo que conviene apartar en cada [period] (semana, o quincena si cobra quincenalmente). */
    data class GoalPace(val goalName: String, val amount: Money, val remaining: Money, val period: PeriodType) : Insight {
        override val priority = 50
        override val tone = InsightTone.NEUTRAL
    }

    data class TopCategory(val category: Category, val amount: Money, val share: Percent, val period: PeriodType) : Insight {
        override val priority = 40
        override val tone = InsightTone.NEUTRAL
    }

    data class OptionalSpendingSummary(val total: Money, val share: Percent?, val period: PeriodType) : Insight {
        override val priority = 30
        override val tone = InsightTone.NEUTRAL
    }

    data class SavingsRate(val rate: Percent, val savings: Money, val period: PeriodType) : Insight {
        override val priority = 20
        override val tone = InsightTone.POSITIVE
    }
}
