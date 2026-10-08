package com.walletplus.domain.usecase

import com.walletplus.domain.calc.BudgetProgress
import com.walletplus.domain.calc.CategoryChange
import com.walletplus.domain.calc.CategoryShare
import com.walletplus.domain.calc.FinanceCalculator
import com.walletplus.domain.calc.GoalProgress
import com.walletplus.domain.calc.OptionalSpending
import com.walletplus.domain.calc.PeriodSummary
import com.walletplus.domain.calc.PlanComparison
import com.walletplus.domain.calc.SmallExpensesSummary
import com.walletplus.domain.calc.TrendPoint
import com.walletplus.domain.insights.Insight
import com.walletplus.domain.insights.InsightContext
import com.walletplus.domain.insights.RecommendationEngine
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.MoneyTotals
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.rules.FinanceRules
import com.walletplus.domain.time.Period
import com.walletplus.domain.time.PeriodType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * Fotografía financiera completa de un periodo: la fuente única para Dashboard, Presupuestos y Análisis.
 *
 * [period] es el periodo que se observa (semana, quincena o mes). Plan y presupuestos siempre se
 * miden contra [budgetPeriod], el ciclo de pago del usuario que contiene ese periodo (o hoy).
 */
data class FinancialSnapshot(
    val period: Period,
    val budgetPeriod: Period,
    val today: LocalDate,
    val settings: UserSettings,
    val account: Account,
    val balance: Money,
    val totals: MoneyTotals,
    val summary: PeriodSummary,
    val previousSummary: PeriodSummary,
    val budgetSummary: PeriodSummary,
    val distribution: List<CategoryShare>,
    val categoryChanges: List<CategoryChange>,
    val optionalSpending: OptionalSpending,
    val smallExpenses: SmallExpensesSummary?,
    val budgets: List<BudgetProgress>,
    val goals: List<GoalProgress>,
    val plan: PlanComparison,
    val insights: List<Insight>,
    val categories: List<Category>,
    val periodTransactions: List<Transaction>,
) {
    /** El periodo observado aún no termina: las comparaciones son contra un periodo completo. */
    val isCurrentPeriod: Boolean get() = today in period
}

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
    /** [period] null = el ciclo de pago en curso (mes o quincena, según los ajustes). */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(period: Period? = null): Flow<FinancialSnapshot?> =
        combine(
            accounts.observePrimary(),
            settings.observe().map { it.payPeriod }.distinctUntilChanged(),
        ) { account, payPeriod -> account to payPeriod }
            .distinctUntilChanged()
            .flatMapLatest { (account, payPeriod) ->
                if (account == null) return@flatMapLatest flowOf(null)
                val today = LocalDate.now(clock)
                val view = period ?: Period.containing(payPeriod, today)
                val budgetPeriod = Period.containing(payPeriod, minOf(view.end, today))
                val previous = view.previous()
                combine(
                    transactions.observeTotals(account.id),
                    transactions.observeBetween(
                        minOf(previous.start, budgetPeriod.start),
                        maxOf(view.end, budgetPeriod.end),
                    ),
                    categories.observeAll(),
                    budgets.observeAll(),
                    goals.observeAll(),
                    settings.observe(),
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    build(
                        period = view,
                        budgetPeriod = budgetPeriod,
                        today = today,
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
        period: Period,
        budgetPeriod: Period,
        today: LocalDate,
        account: Account,
        totals: MoneyTotals,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        settings: UserSettings,
    ): FinancialSnapshot {
        val previousPeriod = period.previous()
        val current = transactions.filter { it.date in period }
        val previous = transactions.filter { it.date in previousPeriod }
        val inBudgetPeriod = transactions.filter { it.date in budgetPeriod }

        val summary = FinanceCalculator.periodSummary(period, current)
        val previousSummary = FinanceCalculator.periodSummary(previousPeriod, previous)
        val budgetSummary = FinanceCalculator.periodSummary(budgetPeriod, inBudgetPeriod)
        val distribution = FinanceCalculator.categoryDistribution(current, categories)
        val changes = FinanceCalculator.compareCategories(
            current, previous, categories, rules.insights.minimumComparableAmount,
        )
        val optional = FinanceCalculator.optionalSpending(current)
        val small = FinanceCalculator.smallFrequentExpenses(current, rules.insights)
        val budgetProgress = FinanceCalculator.budgetsProgress(budgetPeriod, budgets, categories, inBudgetPeriod, rules.budget)
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
                budgetSummary = budgetSummary,
            ),
        )

        return FinancialSnapshot(
            period = period,
            budgetPeriod = budgetPeriod,
            today = today,
            settings = settings,
            account = account,
            balance = FinanceCalculator.balance(account.initialBalance, totals.income, totals.expense),
            totals = totals,
            summary = summary,
            previousSummary = previousSummary,
            budgetSummary = budgetSummary,
            distribution = distribution,
            categoryChanges = changes,
            optionalSpending = optional,
            smallExpenses = small,
            budgets = budgetProgress,
            goals = goalProgress,
            plan = FinanceCalculator.planComparison(settings.plan, budgetSummary),
            insights = insights,
            categories = categories,
            periodTransactions = current.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt }),
        )
    }
}

/** Ingresos y gastos de los últimos periodos (semanas, quincenas o meses) para gráficos de tendencia. */
class ObserveTrend(
    private val transactions: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(type: PeriodType, count: Int = 6): Flow<List<TrendPoint>> {
        val periods = Period.containing(type, LocalDate.now(clock)).lastPeriods(count)
        return transactions.observeBetween(periods.first().start, periods.last().end)
            .map { FinanceCalculator.trend(periods, it) }
    }
}
