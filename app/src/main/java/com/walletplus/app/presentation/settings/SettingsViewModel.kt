package com.walletplus.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.app.core.security.AppLockManager
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.MonthlyPlan
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.usecase.LoadDemoDataUseCase
import com.walletplus.domain.usecase.ResetAllDataUseCase
import com.walletplus.domain.usecase.UpdateAccountUseCase
import com.walletplus.domain.usecase.UpdateMonthlyPlanUseCase
import com.walletplus.domain.usecase.UpdatePreferencesUseCase
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.SettingsError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class SettingsState(val settings: UserSettings, val account: Account?, val today: LocalDate)

class SettingsViewModel(
    settings: SettingsRepository,
    accounts: AccountRepository,
    private val preferences: UpdatePreferencesUseCase,
    private val updateAccount: UpdateAccountUseCase,
    private val updatePlan: UpdateMonthlyPlanUseCase,
    private val loadDemo: LoadDemoDataUseCase,
    private val reset: ResetAllDataUseCase,
    private val appLock: AppLockManager,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<SettingsState?> = combine(settings.observe(), accounts.observePrimary()) { s, a ->
        SettingsState(s, a, LocalDate.now(clock))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun setName(name: String) = preferences.displayName(name)
    suspend fun setCurrency(code: String) = preferences.currency(code)
    suspend fun saveAccount(balance: Money?, start: LocalDate): Outcome<Unit, SettingsError> = updateAccount(balance, start)
    suspend fun savePlan(plan: MonthlyPlan): Outcome<Unit, SettingsError> = updatePlan(plan)
    suspend fun setPin(pin: String) = appLock.setPin(pin)
    suspend fun disableLock() = appLock.disable()
    suspend fun loadDemoData() = loadDemo()
    suspend fun resetAll() = reset()
}
