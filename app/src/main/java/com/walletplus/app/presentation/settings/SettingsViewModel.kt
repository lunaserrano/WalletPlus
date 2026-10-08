package com.walletplus.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.app.core.security.AppLockManager
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.time.PeriodType
import com.walletplus.domain.usecase.LoadDemoDataUseCase
import com.walletplus.domain.usecase.ResetAllDataUseCase
import com.walletplus.domain.usecase.UpdateAccountUseCase
import com.walletplus.domain.usecase.UpdatePlanUseCase
import com.walletplus.domain.usecase.UpdatePreferencesUseCase
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.SettingsError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class SettingsState(val settings: UserSettings, val account: Account?, val hasBudgets: Boolean, val today: LocalDate)

class SettingsViewModel(
    settings: SettingsRepository,
    accounts: AccountRepository,
    budgets: BudgetRepository,
    private val preferences: UpdatePreferencesUseCase,
    private val updateAccount: UpdateAccountUseCase,
    private val updatePlan: UpdatePlanUseCase,
    private val loadDemo: LoadDemoDataUseCase,
    private val reset: ResetAllDataUseCase,
    private val appLock: AppLockManager,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<SettingsState?> = combine(settings.observe(), accounts.observePrimary(), budgets.observeAll()) { s, a, b ->
        SettingsState(s, a, b.isNotEmpty(), LocalDate.now(clock))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun setName(name: String) = preferences.displayName(name)
    suspend fun replayTour() = preferences.tourCompleted(false)
    suspend fun setCurrency(code: String) = preferences.currency(code)
    suspend fun saveAccount(balance: Money?, start: LocalDate): Outcome<Unit, SettingsError> = updateAccount(balance, start)
    suspend fun savePlan(plan: SpendingPlan, payPeriod: PeriodType): Outcome<Unit, SettingsError> = updatePlan(plan, payPeriod)
    suspend fun setPin(pin: String) = appLock.setPin(pin)
    suspend fun disableLock() = appLock.disable()
    suspend fun loadDemoData() = loadDemo()
    suspend fun resetAll() = reset()
}
