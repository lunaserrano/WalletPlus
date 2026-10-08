package com.walletplus.domain.usecase

import com.walletplus.domain.CATEGORIES
import com.walletplus.domain.FIXED_CLOCK
import com.walletplus.domain.FOOD
import com.walletplus.domain.SALARY
import com.walletplus.domain.TODAY
import com.walletplus.domain.insights.Insight
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.DefaultCategories
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.time.Period
import com.walletplus.domain.time.PeriodType
import com.walletplus.domain.usd
import com.walletplus.domain.validation.BudgetError
import com.walletplus.domain.validation.GoalError
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.TransactionError
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class UseCasesTest {

    private val accounts = FakeAccounts(
        Account(id = 1, name = "Billetera", initialBalance = usd(500), startDate = TODAY.minusMonths(1), currencyCode = "USD"),
    )
    private val categories = FakeCategories(CATEGORIES)
    private val transactions = FakeTransactions()
    private val budgets = FakeBudgets()
    private val goals = FakeGoals()
    private val settings = FakeSettings()

    private val save = SaveTransactionUseCase(transactions, categories, accounts, FIXED_CLOCK)
    private val snapshot = ObserveFinancialSnapshot(accounts, transactions, categories, budgets, goals, settings, FIXED_CLOCK)

    private fun draft(type: TransactionType, amount: Money?, categoryId: Long?) =
        TransactionDraft(type = type, amount = amount, categoryId = categoryId, date = TODAY)

    @Test
    fun `saldo 500 + ingreso 1000 - gasto 300 = 1200`() = runTest {
        save(draft(TransactionType.INCOME, usd(1000), SALARY.id))
        save(draft(TransactionType.EXPENSE, usd(300), FOOD.id))
        val result = snapshot(Period.month(YearMonth.from(TODAY))).filterNotNull().first()
        assertEquals(usd(1200), result.balance)
        assertEquals(usd(700), result.summary.savings)
        assertTrue(result.insights.any { it is Insight.SavingsRate })
    }

    @Test
    fun `validaciones al guardar un movimiento`() = runTest {
        val noAmount = save(draft(TransactionType.EXPENSE, null, FOOD.id)) as Outcome.Invalid
        assertEquals(setOf(TransactionError.AMOUNT_REQUIRED), noAmount.errors)

        val zero = save(draft(TransactionType.EXPENSE, Money.ZERO, null)) as Outcome.Invalid
        assertEquals(setOf(TransactionError.AMOUNT_NOT_POSITIVE, TransactionError.CATEGORY_REQUIRED), zero.errors)

        val wrongType = save(draft(TransactionType.EXPENSE, usd(5), SALARY.id)) as Outcome.Invalid
        assertEquals(setOf(TransactionError.CATEGORY_INVALID), wrongType.errors)

        val future = save(draft(TransactionType.EXPENSE, usd(5), FOOD.id).copy(date = TODAY.plusDays(1))) as Outcome.Invalid
        assertEquals(setOf(TransactionError.DATE_IN_FUTURE), future.errors)
    }

    @Test
    fun `categoria desactivada no sirve para nuevos movimientos pero se conserva en historicos`() = runTest {
        val id = (save(draft(TransactionType.EXPENSE, usd(5), FOOD.id)) as Outcome.Success).value
        val removed = RemoveCategoryUseCase(categories, transactions)(FOOD.id)
        assertEquals(RemoveCategoryUseCase.Result.DEACTIVATED, removed)
        assertEquals(false, categories.getById(FOOD.id)?.isActive)

        val newOne = save(draft(TransactionType.EXPENSE, usd(5), FOOD.id)) as Outcome.Invalid
        assertEquals(setOf(TransactionError.CATEGORY_INACTIVE), newOne.errors)

        val edit = save(draft(TransactionType.EXPENSE, usd(7), FOOD.id).copy(id = id))
        assertTrue(edit is Outcome.Success)
    }

    @Test
    fun `categoria sin movimientos se elimina`() = runTest {
        val result = RemoveCategoryUseCase(categories, transactions)(FOOD.id)
        assertEquals(RemoveCategoryUseCase.Result.DELETED, result)
    }

    @Test
    fun `eliminar y deshacer un movimiento`() = runTest {
        val id = (save(draft(TransactionType.EXPENSE, usd(5), FOOD.id)) as Outcome.Success).value
        val deleted = DeleteTransactionUseCase(transactions)(id)!!
        assertTrue(transactions.state.value.isEmpty())
        RestoreTransactionUseCase(transactions)(deleted)
        assertEquals(id, transactions.state.value.single().id)
    }

    @Test
    fun `presupuesto mayor o igual a cero y uno por categoria`() = runTest {
        val saveBudget = SaveBudgetUseCase(budgets, categories)
        val negative = saveBudget(FOOD.id, Money(-1)) as Outcome.Invalid
        assertEquals(setOf(BudgetError.LIMIT_NEGATIVE), negative.errors)
        val income = saveBudget(SALARY.id, usd(10)) as Outcome.Invalid
        assertEquals(setOf(BudgetError.CATEGORY_INVALID), income.errors)
        saveBudget(FOOD.id, usd(300))
        saveBudget(FOOD.id, usd(250))
        assertEquals(usd(250), budgets.state.value.single().limit)
    }

    @Test
    fun `metas validan objetivo y fecha y permiten aportar`() = runTest {
        val saveGoal = SaveGoalUseCase(goals, FIXED_CLOCK)
        val invalid = saveGoal(name = " ", emoji = "🎯", target = Money.ZERO, saved = null, targetDate = TODAY) as Outcome.Invalid
        assertEquals(setOf(GoalError.NAME_REQUIRED, GoalError.TARGET_NOT_POSITIVE, GoalError.DATE_IN_PAST), invalid.errors)

        val id = (saveGoal(name = "Fondo", emoji = "🛟", target = usd(1000), saved = usd(350), targetDate = TODAY.plusMonths(3)) as Outcome.Success).value
        val contribute = ContributeToGoalUseCase(goals)
        contribute(id, usd(50), deposit = true)
        assertEquals(usd(400), goals.getById(id)!!.saved)
        val tooMuch = contribute(id, usd(500), deposit = false) as Outcome.Invalid
        assertEquals(setOf(GoalError.INSUFFICIENT_SAVED), tooMuch.errors)
    }

    @Test
    fun `datos demo generan movimientos solo hasta hoy y activan el modo demo`() = runTest {
        val demoCategories = FakeCategories(DefaultCategories.asCategories().mapIndexed { i, c -> c.copy(id = i + 1L) })
        val maintenance = FakeMaintenance(transactions, budgets, goals, accounts)
        LoadDemoDataUseCase(maintenance, accounts, demoCategories, transactions, budgets, goals, settings, FIXED_CLOCK)()
        assertTrue(transactions.state.value.isNotEmpty())
        assertTrue(transactions.state.value.all { !it.date.isAfter(TODAY) })
        assertTrue(settings.state.value.demoMode)
        assertEquals(2, goals.state.value.size)

        ResetAllDataUseCase(maintenance, settings)()
        assertTrue(transactions.state.value.isEmpty())
        assertEquals(false, settings.state.value.onboardingCompleted)
    }

    @Test
    fun `ciclo quincenal convierte presupuestos y mide contra la quincena en curso`() = runTest {
        budgets.save(Budget(categoryId = FOOD.id, limit = usd(300)))
        save(draft(TransactionType.EXPENSE, usd(80), FOOD.id).copy(date = TODAY.withDayOfMonth(3)))
        save(draft(TransactionType.EXPENSE, usd(200), FOOD.id).copy(date = TODAY.minusMonths(1).withDayOfMonth(20)))
        val maintenance = FakeMaintenance(transactions, budgets, goals, accounts)
        val updatePlan = UpdatePlanUseCase(settings, budgets, maintenance)

        updatePlan(SpendingPlan(spendingLimit = usd(400)), PeriodType.BIWEEKLY)
        assertEquals(PeriodType.BIWEEKLY, settings.state.value.payPeriod)
        assertEquals(usd(150), budgets.state.value.single().limit)

        val result = snapshot().filterNotNull().first()
        assertEquals(Period.containing(PeriodType.BIWEEKLY, TODAY), result.period)
        assertEquals(result.period, result.budgetPeriod)
        assertEquals(usd(80), result.summary.expense)
        assertEquals(usd(200), result.previousSummary.expense) // 16–30 sep
        assertEquals(usd(80), result.budgets.single().spent)
        assertEquals(usd(400), result.plan.spendingLimit)

        updatePlan(SpendingPlan(spendingLimit = usd(800)), PeriodType.MONTHLY)
        assertEquals(usd(300), budgets.state.value.single().limit)
    }

    @Test
    fun `analisis semanal mide presupuestos contra el ciclo de pago`() = runTest {
        save(draft(TransactionType.EXPENSE, usd(50), FOOD.id).copy(date = TODAY.withDayOfMonth(2)))
        save(draft(TransactionType.EXPENSE, usd(20), FOOD.id))
        val week = Period.containing(PeriodType.WEEKLY, TODAY)
        val result = snapshot(week).filterNotNull().first()
        assertEquals(week, result.period)
        assertEquals(usd(20), result.summary.expense)
        assertEquals(Period.month(YearMonth.from(TODAY)), result.budgetPeriod)
        assertEquals(usd(70), result.budgetSummary.expense)
    }
}
