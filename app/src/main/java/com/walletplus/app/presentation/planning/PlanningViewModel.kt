package com.walletplus.app.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.time.PeriodType
import com.walletplus.domain.usecase.ContributeToGoalUseCase
import com.walletplus.domain.usecase.DeleteBudgetUseCase
import com.walletplus.domain.usecase.DeleteGoalUseCase
import com.walletplus.domain.usecase.FinancialSnapshot
import com.walletplus.domain.usecase.ObserveFinancialSnapshot
import com.walletplus.domain.usecase.SaveBudgetUseCase
import com.walletplus.domain.usecase.SaveGoalUseCase
import com.walletplus.domain.usecase.UpdatePlanUseCase
import com.walletplus.domain.validation.BudgetError
import com.walletplus.domain.validation.GoalError
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.SettingsError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class PlanningViewModel(
    observeSnapshot: ObserveFinancialSnapshot,
    private val saveBudget: SaveBudgetUseCase,
    private val deleteBudget: DeleteBudgetUseCase,
    private val saveGoal: SaveGoalUseCase,
    private val contribute: ContributeToGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase,
    private val updatePlan: UpdatePlanUseCase,
) : ViewModel() {

    /** Ciclo de pago en curso: presupuestos y plan se miden por mes o por quincena. */
    val snapshot: StateFlow<FinancialSnapshot?> = observeSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Categorías de gasto activas que aún no tienen presupuesto. */
    fun availableBudgetCategories(snapshot: FinancialSnapshot, includeId: Long? = null) =
        snapshot.categories.filter { c ->
            c.type == TransactionType.EXPENSE && c.isActive &&
                (snapshot.budgets.none { it.category.id == c.id } || c.id == includeId)
        }

    suspend fun saveBudget(categoryId: Long, limit: Money?): Outcome<Long, BudgetError> = saveBudget.invoke(categoryId, limit)
    suspend fun deleteBudget(id: Long) = deleteBudget.invoke(id)

    suspend fun saveGoal(
        id: Long,
        name: String,
        emoji: String,
        target: Money?,
        saved: Money?,
        date: LocalDate?,
    ): Outcome<Long, GoalError> = saveGoal.invoke(id, name, emoji, target, saved, date)

    suspend fun contribute(goalId: Long, amount: Money?, deposit: Boolean) = contribute.invoke(goalId, amount, deposit)
    suspend fun deleteGoal(id: Long) = deleteGoal.invoke(id)

    suspend fun updatePlan(plan: SpendingPlan, payPeriod: PeriodType): Outcome<Unit, SettingsError> =
        updatePlan.invoke(plan, payPeriod)
}
