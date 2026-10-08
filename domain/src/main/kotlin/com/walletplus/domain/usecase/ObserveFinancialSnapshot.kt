package com.walletplus.domain.usecase

import com.walletplus.domain.calc.BudgetProgress
import com.walletplus.domain.calc.CategoryChange
import com.walletplus.domain.calc.CategoryShare
import com.walletplus.domain.calc.FinanceCalculator
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.calc.MonthTrendPoint
import com.walletplus.domain.calc.MonthlySummary
import com.walletplus.domain.calc.OptionalSpending
import com.walletplus.domain.calc.PlanComparison
import com.walletplus.domain.calc.SmallExpensesSummary
import com.walletplus.domain.insights.Insight
import com.walletplus.domain.insights.InsightContext
import com.walletplus.domain.insights.RecommendationEngine
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.MoneyTotals
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.model.Budget
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.rules.FinanceRules
import com.walletplus.domain.time.lastMonths
import com.walletplus.domain.time.previous
import com.walletplus.domain.time.toDateRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** Fotografía financiera completa de un mes: la fuente única para Dashboard, Presupuestos y Análisis. */
data class FinancialSnapshot(
    val month: YearMonth,
    val today: LocalDate,
    val settings: UserSettings,
    val account: Account,
    val balance: Money,
    val totals: MoneyTotals,
    val summary: MonthlySummary,
    val previousSummary: MonthlySummary,
    val distribution: List<CategoryShare>,
    val categoryChanges: List<CategoryChange>,
    val optionalSpending: OptionalSpending,
    val smallExpenses: SmallExpensesSummary?,
    val budgets: List<BudgetProgress>,
    val goals: List<GoalProgress>,
    val plan: PlanComparison,
    val insights: List<Insight>,
    val categories: List<Category>,
    val monthTransactions: List<Transaction>,
)

class ObserveFinancialSnapshot(
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val budgets: BudgetRepository,
    private val goals: GoalRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val rules: FinanceRules = FinanceRules.Default,
    private val engine: RecommendationEngine = RecommendationEngine(),
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(month: YearMonth): Flow<FinancialSnapshot?> =
        accounts.observePrimary().flatMapLatest { account ->
            if (account == null) return@flatMapLatest flowOf(null)
            val previousMonth = month.previous()
            combine(
                transactions.observeTotals(account.id),
                transactions.observeBetween(previousMonth.atDay(1), month.atEndOfMonth()),
                categories.observeAll(),
                budgets.observeAll(),
                goals.observeAll(),
                settings.observe(),
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                build(
                    month = month,
                    account = account,
                    totals = values[0] as MoneyTotals,
                    transactions = values[1] as List<Transaction>,
                    categories = values[2] as List<Category>,
                    budgets = values[3] as List<Budget>,
                    goals = values[4] as List<SavingsGoal>,
                    settings = values[5] as UserSettings,
                )
            }
        }

    private fun build(
        month: YearMonth,
        account: Account,
        totals: MoneyTotals,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        settings: UserSettings,
    ): FinancialSnapshot {
        val today = LocalDate.now(clock)
        val range = month.toDateRange()
        val previousRange = month.previous().toDateRange()
        val current = transactions.filter { it.date in range }
        val previous = transactions.filter { it.date in previousRange }

        val summary = FinanceCalculator.monthlySummary(month, current)
        val previousSummary = FinanceCalculator.monthlySummary(month.previous(), previous)
        val distribution = FinanceCalculator.categoryDistribution(current, categories)
        val changes = FinanceCalculator.compareCategories(
            current, previous, categories, rules.insights.minimumComparableAmount,
        )
        val optional = FinanceCalculator.optionalSpending(current)
        val small = FinanceCalculator.smallFrequentExpenses(current, rules.insights)
        val budgetProgress = FinanceCalculator.budgetsProgress(month, budgets, categories, current, rules.budget)
        val goalProgress = goals.filter { !it.isArchived }.map { FinanceCalculator.goalProgress(it, today) }

        val insights = engine.generate(
            InsightContext(
                summary = summary,
                previousSummary = previousSummary,
                distribution = distribution,
                categoryChanges = changes,
                optionalSpending = optional,
                smallExpenses = small,
                budgets = budgetProgress,
                goals = goalProgress,
                plan = settings.plan,
                thresholds = rules.insights,
            ),
        )

        return FinancialSnapshot(
            month = month,
            today = today,
            settings = settings,
            account = account,
            balance = FinanceCalculator.balance(account.initialBalance, totals.income, totals.expense),
            totals = totals,
            summary = summary,
            previousSummary = previousSummary,
            distribution = distribution,
            categoryChanges = changes,
            optionalSpending = optional,
            smallExpenses = small,
            budgets = budgetProgress,
            goals = goalProgress,
            plan = FinanceCalculator.planComparison(settings.plan, summary),
            insights = insights,
            categories = categories,
            monthTransactions = current.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt }),
        )
    }
}

/** Ingresos y gastos de los últimos meses para gráficos de tendencia. */
class ObserveMonthlyTrend(private val transactions: TransactionRepository) {
    operator fun invoke(endMonth: YearMonth, months: Int = 6): Flow<List<MonthTrendPoint>> {
        val range = endMonth.lastMonths(months)
        return transactions.observeBetween(range.first().atDay(1), endMonth.atEndOfMonth())
            .map { FinanceCalculator.monthlyTrend(range, it) }
    }
}
