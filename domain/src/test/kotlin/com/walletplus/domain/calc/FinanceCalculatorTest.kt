package com.walletplus.domain.calc

import com.walletplus.domain.CATEGORIES
import com.walletplus.domain.FOOD
import com.walletplus.domain.FUN
import com.walletplus.domain.TODAY
import com.walletplus.domain.TRANSPORT
import com.walletplus.domain.expense
import com.walletplus.domain.income
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.MonthlyPlan
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.Percent
import com.walletplus.domain.rules.BudgetThresholds
import com.walletplus.domain.rules.InsightThresholds
import com.walletplus.domain.usd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class FinanceCalculatorTest {

    private val month = YearMonth.from(TODAY)

    @Test
    fun `saldo = inicial + ingresos - gastos`() {
        assertEquals(usd(1200), FinanceCalculator.balance(usd(500), usd(1000), usd(300)))
    }

    @Test
    fun `totales de ingresos y gastos`() {
        val txs = listOf(income(usd(1000)), income(usd(200)), expense(usd(300)), expense(usd(57, 50)))
        assertEquals(usd(1200), FinanceCalculator.totalIncome(txs))
        assertEquals(usd(357, 50), FinanceCalculator.totalExpense(txs))
    }

    @Test
    fun `resumen mensual ignora otros meses y calcula ahorro`() {
        val txs = listOf(
            income(usd(1200)),
            expense(usd(457, 50)),
            expense(usd(999), date = TODAY.minusMonths(1)),
        )
        val summary = FinanceCalculator.monthlySummary(month, txs)
        assertEquals(usd(1200), summary.income)
        assertEquals(usd(457, 50), summary.expense)
        assertEquals(usd(742, 50), summary.savings)
        assertEquals(Percent(6188), summary.savingsRate)
    }

    @Test
    fun `porcentaje de ahorro sin ingresos es null y no NaN`() {
        val summary = FinanceCalculator.monthlySummary(month, listOf(expense(usd(10))))
        assertNull(summary.savingsRate)
        assertNull(FinanceCalculator.savingsRate(Money.ZERO, Money.ZERO))
    }

    @Test
    fun `distribucion y categoria con mayor gasto`() {
        val txs = listOf(
            expense(usd(60), FOOD), expense(usd(20), FOOD),
            expense(usd(15), TRANSPORT), expense(usd(5), FUN),
            income(usd(1000)),
        )
        val distribution = FinanceCalculator.categoryDistribution(txs, CATEGORIES)
        assertEquals(listOf(FOOD, TRANSPORT, FUN), distribution.map { it.category })
        assertEquals(Percent.ofWhole(80), distribution[0].share)
        assertEquals(FOOD, FinanceCalculator.topExpenseCategory(txs, CATEGORIES)?.category)
        assertNull(FinanceCalculator.topExpenseCategory(emptyList(), CATEGORIES))
    }

    @Test
    fun `estados de presupuesto en cada umbral`() {
        val t = BudgetThresholds()
        fun status(spentUnits: Long) = FinanceCalculator.budgetProgress(
            Budget(categoryId = FOOD.id, limit = usd(100)), FOOD, usd(spentUnits), t,
        ).status
        assertEquals(BudgetStatus.NORMAL, status(69))
        assertEquals(BudgetStatus.CAUTION, status(70))
        assertEquals(BudgetStatus.CAUTION, status(89))
        assertEquals(BudgetStatus.HIGH, status(90))
        assertEquals(BudgetStatus.HIGH, status(100))
        assertEquals(BudgetStatus.EXCEEDED, status(101))
    }

    @Test
    fun `presupuesto con limite cero no divide por cero`() {
        val t = BudgetThresholds()
        val zero = Budget(categoryId = FOOD.id, limit = Money.ZERO)
        assertEquals(BudgetStatus.NORMAL, FinanceCalculator.budgetProgress(zero, FOOD, Money.ZERO, t).status)
        val exceeded = FinanceCalculator.budgetProgress(zero, FOOD, usd(1), t)
        assertEquals(BudgetStatus.EXCEEDED, exceeded.status)
        assertNull(exceeded.percent)
    }

    @Test
    fun `progreso de presupuesto del ejemplo`() {
        val progress = FinanceCalculator.budgetsProgress(
            month,
            listOf(Budget(categoryId = FOOD.id, limit = usd(300))),
            CATEGORIES,
            listOf(expense(usd(215), FOOD), expense(usd(50), FOOD, date = TODAY.minusMonths(1))),
            BudgetThresholds(),
        ).single()
        assertEquals(usd(215), progress.spent)
        assertEquals(usd(85), progress.remaining)
        assertEquals(Percent(7167), progress.percent)
        assertEquals(BudgetStatus.CAUTION, progress.status)
    }

    @Test
    fun `meta con fecha calcula restante y ahorro recomendado`() {
        val goal = goal(target = usd(1000), saved = usd(350), date = TODAY.plusWeeks(10))
        val progress = FinanceCalculator.goalProgress(goal, TODAY)
        assertEquals(usd(650), progress.remaining)
        assertEquals(Percent.ofWhole(35), progress.percent)
        assertEquals(usd(65), progress.perWeek)
        assertEquals(usd(216, 67), progress.perMonth) // 650 / 3 meses, redondeado hacia arriba
        assertFalse(progress.isOverdue)
    }

    @Test
    fun `meta con fecha pasada u hoy no divide por cero`() {
        val today = FinanceCalculator.goalProgress(goal(usd(100), usd(10), TODAY), TODAY)
        assertTrue(today.isOverdue)
        assertNull(today.perWeek)
        val past = FinanceCalculator.goalProgress(goal(usd(100), usd(10), TODAY.minusDays(3)), TODAY)
        assertTrue(past.isOverdue)
        assertNull(past.perMonth)
    }

    @Test
    fun `meta completada y sin fecha`() {
        val done = FinanceCalculator.goalProgress(goal(usd(100), usd(120), TODAY.plusDays(30)), TODAY)
        assertTrue(done.isCompleted)
        assertEquals(Money.ZERO, done.remaining)
        assertNull(done.perWeek)
        val noDate = FinanceCalculator.goalProgress(goal(usd(100), usd(20), null), TODAY)
        assertNull(noDate.perWeek)
        assertFalse(noDate.isOverdue)
    }

    @Test
    fun `comparacion mensual incluso con cambio de anio`() {
        val january = YearMonth.of(2027, 1)
        val current = listOf(expense(usd(118), TRANSPORT, date = january.atDay(10)))
        val previous = listOf(expense(usd(100), TRANSPORT, date = january.minusMonths(1).atDay(10)))
        val change = FinanceCalculator.compareCategories(current, previous, CATEGORIES, usd(10)).single()
        assertEquals(Percent.ofWhole(18), change.change)
        assertEquals(usd(100), change.previous)
    }

    @Test
    fun `comparacion sin datos previos no inventa porcentajes`() {
        val changes = FinanceCalculator.compareCategories(listOf(expense(usd(50), FUN)), emptyList(), CATEGORIES, usd(10))
        assertNull(changes.single().change)
    }

    @Test
    fun `gastos opcionales`() {
        val txs = listOf(expense(usd(80), necessary = true), expense(usd(20), FUN, necessary = false), income(usd(500)))
        val optional = FinanceCalculator.optionalSpending(txs)
        assertEquals(usd(20), optional.total)
        assertEquals(Percent.ofWhole(20), optional.share)
        assertEquals(1, optional.count)
    }

    @Test
    fun `gastos pequenos frecuentes`() {
        val t = InsightThresholds()
        val small = List(6) { expense(usd(3)) } + expense(usd(40))
        val summary = FinanceCalculator.smallFrequentExpenses(small, t)!!
        assertEquals(6, summary.count)
        assertEquals(usd(18), summary.total)
        assertNull(FinanceCalculator.smallFrequentExpenses(List(3) { expense(usd(4)) }, t))
    }

    @Test
    fun `plan mensual frente a lo real`() {
        val summary = FinanceCalculator.monthlySummary(month, listOf(income(usd(1000)), expense(usd(600))))
        val plan = FinanceCalculator.planComparison(MonthlyPlan(usd(1200), usd(800), usd(400)), summary)
        assertEquals(Percent.ofWhole(75), plan.spendingProgress)
        assertEquals(Percent.ofWhole(100), plan.savingsProgress)
        assertNull(FinanceCalculator.planComparison(MonthlyPlan(), summary).spendingProgress)
    }

    private fun goal(target: Money, saved: Money, date: java.time.LocalDate?) = SavingsGoal(
        id = 1, name = "Meta", emoji = "🎯", target = target, saved = saved, targetDate = date, createdAt = Instant.EPOCH,
    )
}
