package com.walletplus.domain.usecase

import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.validation.BudgetError
import com.walletplus.domain.validation.GoalError
import com.walletplus.domain.validation.Outcome
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class SaveBudgetUseCase(
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
) {
    /** Crea o actualiza el presupuesto mensual de una categoría (uno por categoría). */
    suspend operator fun invoke(categoryId: Long, limit: Money?): Outcome<Long, BudgetError> {
        val errors = mutableSetOf<BudgetError>()
        val category = categories.getById(categoryId)
        if (category == null || category.type != TransactionType.EXPENSE) errors += BudgetError.CATEGORY_INVALID
        when {
            limit == null -> errors += BudgetError.LIMIT_REQUIRED
            limit.isNegative -> errors += BudgetError.LIMIT_NEGATIVE
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)

        val existing = budgets.getByCategory(categoryId)
        val budget = existing?.copy(limit = limit!!) ?: Budget(categoryId = categoryId, limit = limit!!)
        return Outcome.Success(budgets.save(budget))
    }
}

class DeleteBudgetUseCase(private val budgets: BudgetRepository) {
    suspend operator fun invoke(id: Long) = budgets.delete(id)
}

class SaveGoalUseCase(
    private val goals: GoalRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        id: Long = 0,
        name: String,
        emoji: String,
        target: Money?,
        saved: Money?,
        targetDate: LocalDate?,
    ): Outcome<Long, GoalError> {
        val errors = mutableSetOf<GoalError>()
        val cleanName = name.trim()
        if (cleanName.isEmpty()) errors += GoalError.NAME_REQUIRED
        if (target == null || !target.isPositive) errors += GoalError.TARGET_NOT_POSITIVE
        if (saved != null && saved.isNegative) errors += GoalError.SAVED_NEGATIVE

        val existing = if (id != 0L) goals.getById(id) else null
        if (id != 0L && existing == null) errors += GoalError.NOT_FOUND
        val today = LocalDate.now(clock)
        // Una meta nueva no puede tener fecha pasada; al editar se permite conservar la original.
        if (targetDate != null && !targetDate.isAfter(today) && targetDate != existing?.targetDate) {
            errors += GoalError.DATE_IN_PAST
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)

        val goal = existing?.copy(
            name = cleanName,
            emoji = emoji,
            target = target!!,
            saved = saved ?: existing.saved,
            targetDate = targetDate,
        ) ?: SavingsGoal(
            name = cleanName,
            emoji = emoji,
            target = target!!,
            saved = saved ?: Money.ZERO,
            targetDate = targetDate,
            createdAt = Instant.now(clock),
        )
        return Outcome.Success(goals.save(goal))
    }
}

class ContributeToGoalUseCase(private val goals: GoalRepository) {
    /** Aporta (deposit = true) o retira dinero de una meta. */
    suspend operator fun invoke(goalId: Long, amount: Money?, deposit: Boolean): Outcome<SavingsGoal, GoalError> {
        val goal = goals.getById(goalId) ?: return Outcome.Invalid(setOf(GoalError.NOT_FOUND))
        if (amount == null || !amount.isPositive) return Outcome.Invalid(setOf(GoalError.AMOUNT_NOT_POSITIVE))
        if (!deposit && amount > goal.saved) return Outcome.Invalid(setOf(GoalError.INSUFFICIENT_SAVED))
        val updated = goal.copy(saved = if (deposit) goal.saved + amount else goal.saved - amount)
        goals.save(updated)
        return Outcome.Success(updated)
    }
}

class DeleteGoalUseCase(private val goals: GoalRepository) {
    suspend operator fun invoke(id: Long) = goals.delete(id)
}
