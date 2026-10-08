package com.walletplus.domain.insights

import com.walletplus.domain.CATEGORIES
import com.walletplus.domain.FOOD
import com.walletplus.domain.FUN
import com.walletplus.domain.TODAY
import com.walletplus.domain.TRANSPORT
import com.walletplus.domain.calc.FinanceCalculator
import com.walletplus.domain.expense
import com.walletplus.domain.income
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.MonthlyPlan
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.rules.FinanceRules
import com.walletplus.domain.usd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class RecommendationEngineTest {

    private val month = YearMonth.from(TODAY)
    private val rules = FinanceRules.Default
    private val engine = RecommendationEngine()

    private fun insights(
        current: List<Transaction>,
        previous: List<Transaction> = emptyList(),
        budgets: List<Budget> = emptyList(),
        goals: List<SavingsGoal> = emptyList(),
        plan: MonthlyPlan = MonthlyPlan(),
    ): List<Insight> {
        val summary = FinanceCalculator.monthlySummary(month, current)
        return engine.generate(
            InsightContext(
                summary = summary,
                previousSummary = FinanceCalculator.monthlySummary(month.minusMonths(1), previous),
                distribution = FinanceCalculator.categoryDistribution(current, CATEGORIES),
                categoryChanges = FinanceCalculator.compareCategories(current, previous, CATEGORIES, rules.insights.minimumComparableAmount),
                optionalSpending = FinanceCalculator.optionalSpending(current),
                smallExpenses = FinanceCalculator.smallFrequentExpenses(current, rules.insights),
                budgets = FinanceCalculator.budgetsProgress(month, budgets, CATEGORIES, current, rules.budget),
                goals = goals.map { FinanceCalculator.goalProgress(it, TODAY) },
                plan = plan,
                thresholds = rules.insights,
            ),
        )
    }

    @Test
    fun `regla 1 - gastos opcionales por encima del 20 por ciento`() {
        val result = insights(listOf(expense(usd(70)), expense(usd(30), FUN, necessary = false)))
        assertTrue(result.any { it is Insight.HighOptionalSpending })
        val low = insights(listOf(expense(usd(90)), expense(usd(10), FUN, necessary = false)))
        assertFalse(low.any { it is Insight.HighOptionalSpending })
        assertTrue(low.any { it is Insight.OptionalSpendingSummary })
    }

    @Test
    fun `regla 2 - categoria que aumenta mas de 20 por ciento`() {
        val previousDate = TODAY.minusMonths(1)
        val result = insights(
            current = listOf(expense(usd(130), TRANSPORT)),
            previous = listOf(expense(usd(100), TRANSPORT, date = previousDate)),
        )
        val increase = result.filterIsInstance<Insight.CategoryIncreased>().single()
        assertEquals(TRANSPORT, increase.category)
        val small = insights(
            current = listOf(expense(usd(115), TRANSPORT)),
            previous = listOf(expense(usd(100), TRANSPORT, date = previousDate)),
        )
        assertTrue(small.none { it is Insight.CategoryIncreased })
    }

    @Test
    fun `regla 3 - presupuesto superado`() {
        val result = insights(
            current = listOf(expense(usd(320), FOOD)),
            budgets = listOf(Budget(categoryId = FOOD.id, limit = usd(300))),
        )
        val exceeded = result.first()
        assertTrue(exceeded is Insight.BudgetExceeded)
        assertEquals(FOOD, (exceeded as Insight.BudgetExceeded).category)
    }

    @Test
    fun `regla 4 - ritmo semanal para la meta`() {
        val goal = SavingsGoal(
            id = 1, name = "Fondo", emoji = "🛟", target = usd(1000), saved = usd(350),
            targetDate = TODAY.plusWeeks(10), createdAt = Instant.EPOCH,
        )
        val pace = insights(listOf(income(usd(100))), goals = listOf(goal)).filterIsInstance<Insight.GoalPace>().single()
        assertEquals(usd(65), pace.perWeek)
    }

    @Test
    fun `regla 5 - porcentaje de ahorro y gasto excesivo`() {
        val saving = insights(listOf(income(usd(1000)), expense(usd(600))))
        assertTrue(saving.any { it is Insight.SavingsRate })
        val overspent = insights(listOf(income(usd(500)), expense(usd(600))))
        assertTrue(overspent.any { it is Insight.MonthOverspent })
        assertTrue(overspent.none { it is Insight.SavingsRate })
    }

    @Test
    fun `categoria principal, gastos pequenos y limite del plan`() {
        val txs = List(6) { expense(usd(3)) } + expense(usd(900), TRANSPORT)
        val result = insights(txs, plan = MonthlyPlan(spendingLimit = usd(800)))
        assertEquals(TRANSPORT, result.filterIsInstance<Insight.TopCategory>().single().category)
        assertTrue(result.any { it is Insight.SmallExpensesAddUp })
        assertTrue(result.any { it is Insight.PlanSpendingExceeded })
    }

    @Test
    fun `sin datos no hay recomendaciones y se ordenan por prioridad`() {
        assertTrue(insights(emptyList()).isEmpty())
        val result = insights(
            current = listOf(income(usd(1000)), expense(usd(320), FOOD)),
            budgets = listOf(Budget(categoryId = FOOD.id, limit = usd(300))),
        )
        assertEquals(result.sortedByDescending { it.priority }, result)
    }
}
