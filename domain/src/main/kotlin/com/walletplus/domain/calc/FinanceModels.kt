package com.walletplus.domain.calc

import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import java.time.YearMonth

data class MonthlySummary(
    val month: YearMonth,
    val income: Money,
    val expense: Money,
    val transactionCount: Int,
) {
    /** Capacidad de ahorro del mes: ingresos − gastos (puede ser negativa). */
    val savings: Money get() = income - expense

    /** Porcentaje ahorrado sobre los ingresos; null si no hubo ingresos. */
    val savingsRate: Percent? get() = Percent.of(savings, income)

    val hasData: Boolean get() = transactionCount > 0
}

data class CategoryShare(
    val category: Category,
    val amount: Money,
    val share: Percent,
    val count: Int,
)

enum class BudgetStatus { NORMAL, CAUTION, HIGH, EXCEEDED }

data class BudgetProgress(
    val budget: Budget,
    val category: Category,
    val spent: Money,
    /** Puede ser negativo cuando se excede el presupuesto. */
    val remaining: Money,
    /** Null cuando el límite es cero. */
    val percent: Percent?,
    val status: BudgetStatus,
)

data class GoalProgress(
    val goal: SavingsGoal,
    val remaining: Money,
    val percent: Percent,
    val isCompleted: Boolean,
    /** La fecha objetivo ya pasó (o es hoy) y la meta no está completa. */
    val isOverdue: Boolean,
    val daysLeft: Long?,
    val perWeek: Money?,
    val perMonth: Money?,
)

data class CategoryChange(
    val category: Category,
    val previous: Money,
    val current: Money,
    /** Null si el mes anterior no tiene base suficiente para comparar. */
    val change: Percent?,
)

data class SmallExpensesSummary(
    val count: Int,
    val total: Money,
    val limit: Money,
)

data class OptionalSpending(
    val total: Money,
    val share: Percent?,
    val count: Int,
)

/** Plan mensual (presupuesto general) frente a lo real. */
data class PlanComparison(
    val expectedIncome: Money,
    val actualIncome: Money,
    val spendingLimit: Money,
    val actualExpense: Money,
    val savingsTarget: Money,
    val actualSavings: Money,
) {
    val spendingProgress: Percent? get() = Percent.of(actualExpense, spendingLimit)
    val savingsProgress: Percent? get() = Percent.of(actualSavings.coerceAtLeast(Money.ZERO), savingsTarget)
    val incomeProgress: Percent? get() = Percent.of(actualIncome, expectedIncome)
}

data class MonthTrendPoint(
    val month: YearMonth,
    val income: Money,
    val expense: Money,
)
