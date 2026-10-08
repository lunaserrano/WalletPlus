package com.walletplus.domain.calc

import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import com.walletplus.domain.money.sumOfMoney
import com.walletplus.domain.rules.BudgetThresholds
import com.walletplus.domain.rules.InsightThresholds
import com.walletplus.domain.time.Period
import com.walletplus.domain.time.remainingPeriods
import java.time.LocalDate

/** Cálculos financieros puros: sin estado, sin Android y fáciles de probar. */
object FinanceCalculator {

    fun balance(initial: Money, income: Money, expense: Money): Money = initial + income - expense

    fun totalIncome(transactions: List<Transaction>): Money =
        transactions.filter { it.isIncome }.sumOfMoney { it.amount }

    fun totalExpense(transactions: List<Transaction>): Money =
        transactions.filter { it.isExpense }.sumOfMoney { it.amount }

    fun periodSummary(period: Period, transactions: List<Transaction>): PeriodSummary {
        val inPeriod = transactions.filter { it.date in period }
        return PeriodSummary(
            period = period,
            income = totalIncome(inPeriod),
            expense = totalExpense(inPeriod),
            transactionCount = inPeriod.size,
        )
    }

    fun savingsRate(income: Money, expense: Money): Percent? = Percent.of(income - expense, income)

    /** Suma de gastos por categoría. */
    fun expensesByCategory(transactions: List<Transaction>): Map<Long, Money> =
        transactions.asSequence()
            .filter { it.isExpense }
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOfMoney { it.amount } }

    /** Distribución de gastos por categoría, de mayor a menor. */
    fun categoryDistribution(
        transactions: List<Transaction>,
        categories: List<Category>,
        type: TransactionType = TransactionType.EXPENSE,
    ): List<CategoryShare> {
        val byId = categories.associateBy { it.id }
        val relevant = transactions.filter { it.type == type }
        val total = relevant.sumOfMoney { it.amount }
        return relevant.groupBy { it.categoryId }
            .mapNotNull { (categoryId, items) ->
                val category = byId[categoryId] ?: return@mapNotNull null
                val amount = items.sumOfMoney { it.amount }
                CategoryShare(category, amount, Percent.of(amount, total) ?: Percent.ZERO, items.size)
            }
            .sortedWith(compareByDescending<CategoryShare> { it.amount }.thenBy { it.category.name })
    }

    fun topExpenseCategory(transactions: List<Transaction>, categories: List<Category>): CategoryShare? =
        categoryDistribution(transactions, categories).firstOrNull()

    fun budgetStatus(percent: Percent?, spent: Money, thresholds: BudgetThresholds): BudgetStatus {
        if (percent == null) return if (spent.isPositive) BudgetStatus.EXCEEDED else BudgetStatus.NORMAL
        return when {
            percent > thresholds.exceeded -> BudgetStatus.EXCEEDED
            percent >= thresholds.high -> BudgetStatus.HIGH
            percent >= thresholds.caution -> BudgetStatus.CAUTION
            else -> BudgetStatus.NORMAL
        }
    }

    fun budgetProgress(
        budget: Budget,
        category: Category,
        spent: Money,
        thresholds: BudgetThresholds,
    ): BudgetProgress {
        val percent = Percent.of(spent, budget.limit)
        return BudgetProgress(
            budget = budget,
            category = category,
            spent = spent,
            remaining = budget.limit - spent,
            percent = percent,
            status = budgetStatus(percent, spent, thresholds),
        )
    }

    /** Progreso de todos los presupuestos aplicables a [period] (el ciclo de pago), ordenados por urgencia. */
    fun budgetsProgress(
        period: Period,
        budgets: List<Budget>,
        categories: List<Category>,
        transactions: List<Transaction>,
        thresholds: BudgetThresholds,
    ): List<BudgetProgress> {
        val byId = categories.associateBy { it.id }
        val spentByCategory = expensesByCategory(transactions.filter { it.date in period })
        return budgets
            .filter { it.yearMonth == null || it.yearMonth == period.yearMonth }
            .mapNotNull { budget ->
                val category = byId[budget.categoryId] ?: return@mapNotNull null
                budgetProgress(budget, category, spentByCategory[budget.categoryId] ?: Money.ZERO, thresholds)
            }
            .sortedWith(
                compareByDescending<BudgetProgress> { it.percent?.basisPoints ?: Long.MAX_VALUE }
                    .thenBy { it.category.name },
            )
    }

    fun goalProgress(goal: SavingsGoal, today: LocalDate): GoalProgress {
        val remaining = (goal.target - goal.saved).coerceAtLeast(Money.ZERO)
        val percent = Percent.of(goal.saved, goal.target) ?: Percent.ZERO
        val completed = goal.target.isPositive && goal.saved >= goal.target
        val periods = goal.targetDate?.let { remainingPeriods(today, it) }
        val overdue = !completed && goal.targetDate != null && periods == null
        return GoalProgress(
            goal = goal,
            remaining = remaining,
            percent = percent,
            isCompleted = completed,
            isOverdue = overdue,
            daysLeft = periods?.days,
            perWeek = periods?.takeIf { !completed }?.let { remaining.divideRoundingUp(it.weeks) },
            perFortnight = periods?.takeIf { !completed }?.let { remaining.divideRoundingUp(it.fortnights) },
            perMonth = periods?.takeIf { !completed }?.let { remaining.divideRoundingUp(it.months) },
        )
    }

    fun compareCategories(
        current: List<Transaction>,
        previous: List<Transaction>,
        categories: List<Category>,
        minimumComparable: Money,
    ): List<CategoryChange> {
        val byId = categories.associateBy { it.id }
        val currentMap = expensesByCategory(current)
        val previousMap = expensesByCategory(previous)
        return (currentMap.keys + previousMap.keys).mapNotNull { id ->
            val category = byId[id] ?: return@mapNotNull null
            val prev = previousMap[id] ?: Money.ZERO
            val curr = currentMap[id] ?: Money.ZERO
            val change = if (prev >= minimumComparable) Percent.change(prev, curr) else null
            CategoryChange(category, prev, curr, change)
        }.sortedByDescending { it.change?.basisPoints ?: Long.MIN_VALUE }
    }

    fun optionalSpending(transactions: List<Transaction>): OptionalSpending {
        val expenses = transactions.filter { it.isExpense }
        val optional = expenses.filter { it.isOptionalExpense }
        val total = optional.sumOfMoney { it.amount }
        return OptionalSpending(
            total = total,
            share = Percent.of(total, expenses.sumOfMoney { it.amount }),
            count = optional.size,
        )
    }

    /** Detecta gastos pequeños frecuentes; null si no son relevantes según los umbrales. */
    fun smallFrequentExpenses(
        transactions: List<Transaction>,
        thresholds: InsightThresholds,
    ): SmallExpensesSummary? {
        val small = transactions.filter { it.isExpense && it.amount < thresholds.smallExpenseLimit }
        val total = small.sumOfMoney { it.amount }
        if (small.size < thresholds.smallExpenseMinCount || total < thresholds.smallExpenseMinTotal) return null
        return SmallExpensesSummary(small.size, total, thresholds.smallExpenseLimit)
    }

    fun planComparison(plan: SpendingPlan, summary: PeriodSummary): PlanComparison = PlanComparison(
        expectedIncome = plan.expectedIncome,
        actualIncome = summary.income,
        spendingLimit = plan.spendingLimit,
        actualExpense = summary.expense,
        savingsTarget = plan.savingsTarget,
        actualSavings = summary.savings,
    )

    fun trend(periods: List<Period>, transactions: List<Transaction>): List<TrendPoint> =
        periods.map { period ->
            val summary = periodSummary(period, transactions)
            TrendPoint(period, summary.income, summary.expense)
        }
}
