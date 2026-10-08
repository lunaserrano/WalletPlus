package com.walletplus.app.core.di

import android.content.Context
import com.walletplus.app.core.security.AppLockManager
import com.walletplus.app.data.local.database.WalletDatabase
import com.walletplus.app.data.repositories.RoomAccountRepository
import com.walletplus.app.data.repositories.RoomBudgetRepository
import com.walletplus.app.data.repositories.RoomCategoryRepository
import com.walletplus.app.data.repositories.RoomDataStoreMaintenance
import com.walletplus.app.data.repositories.RoomGoalRepository
import com.walletplus.app.data.repositories.RoomSettingsRepository
import com.walletplus.app.data.repositories.RoomTransactionRepository
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.DataStoreMaintenance
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.usecase.CompleteOnboardingUseCase
import com.walletplus.domain.usecase.ContributeToGoalUseCase
import com.walletplus.domain.usecase.DeleteBudgetUseCase
import com.walletplus.domain.usecase.DeleteGoalUseCase
import com.walletplus.domain.usecase.DeleteTransactionUseCase
import com.walletplus.domain.usecase.LoadDemoDataUseCase
import com.walletplus.domain.usecase.ObserveFinancialSnapshot
import com.walletplus.domain.usecase.ObserveMonthlyTrend
import com.walletplus.domain.usecase.RemoveCategoryUseCase
import com.walletplus.domain.usecase.ResetAllDataUseCase
import com.walletplus.domain.usecase.RestoreTransactionUseCase
import com.walletplus.domain.usecase.SaveBudgetUseCase
import com.walletplus.domain.usecase.SaveCategoryUseCase
import com.walletplus.domain.usecase.SaveGoalUseCase
import com.walletplus.domain.usecase.SaveTransactionUseCase
import com.walletplus.domain.usecase.SetCategoryActiveUseCase
import com.walletplus.domain.usecase.UpdateAccountUseCase
import com.walletplus.domain.usecase.UpdateMonthlyPlanUseCase
import com.walletplus.domain.usecase.UpdatePreferencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Inyección de dependencias manual: suficiente para el tamaño de la app y sin librerías extra.
 */
class AppContainer(context: Context) {

    val clock: Clock = Clock.systemDefaultZone()

    /** Ámbito de aplicación para acciones que sobreviven a una pantalla (p. ej. "Deshacer"). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val database = WalletDatabase.build(context)

    val accounts: AccountRepository = RoomAccountRepository(database.accountDao())
    val categories: CategoryRepository = RoomCategoryRepository(database.categoryDao())
    val transactions: TransactionRepository = RoomTransactionRepository(database.transactionDao())
    val budgets: BudgetRepository = RoomBudgetRepository(database.budgetDao())
    val goals: GoalRepository = RoomGoalRepository(database.goalDao())
    val settings: SettingsRepository = RoomSettingsRepository(database.settingsDao())
    private val maintenance: DataStoreMaintenance = RoomDataStoreMaintenance(database)

    val appLock = AppLockManager(database.settingsDao())

    val observeSnapshot = ObserveFinancialSnapshot(accounts, transactions, categories, budgets, goals, settings, clock)
    val observeTrend = ObserveMonthlyTrend(transactions)

    val saveTransaction = SaveTransactionUseCase(transactions, categories, accounts, clock)
    val deleteTransaction = DeleteTransactionUseCase(transactions)
    val restoreTransaction = RestoreTransactionUseCase(transactions)

    val saveCategory = SaveCategoryUseCase(categories)
    val setCategoryActive = SetCategoryActiveUseCase(categories)
    val removeCategory = RemoveCategoryUseCase(categories, transactions)

    val saveBudget = SaveBudgetUseCase(budgets, categories)
    val deleteBudget = DeleteBudgetUseCase(budgets)

    val saveGoal = SaveGoalUseCase(goals, clock)
    val contributeToGoal = ContributeToGoalUseCase(goals)
    val deleteGoal = DeleteGoalUseCase(goals)

    val completeOnboarding = CompleteOnboardingUseCase(accounts, settings, maintenance, clock)
    val updateAccount = UpdateAccountUseCase(accounts, clock)
    val updatePlan = UpdateMonthlyPlanUseCase(settings)
    val updatePreferences = UpdatePreferencesUseCase(settings, accounts)
    val loadDemoData = LoadDemoDataUseCase(maintenance, accounts, categories, transactions, budgets, goals, settings, clock)
    val resetAllData = ResetAllDataUseCase(maintenance, settings)
}
