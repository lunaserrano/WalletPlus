package com.walletplus.app.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.app.core.security.AppLockManager
import com.walletplus.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface RootState {
    data object Loading : RootState
    data object Onboarding : RootState
    data object Locked : RootState
    data class Ready(val currencyCode: String) : RootState
}

class RootViewModel(
    settings: SettingsRepository,
    private val appLock: AppLockManager,
) : ViewModel() {

    val state: StateFlow<RootState> = combine(settings.observe(), appLock.isUnlocked) { current, unlocked ->
        when {
            !current.onboardingCompleted -> RootState.Onboarding
            current.lockEnabled && !unlocked -> RootState.Locked
            else -> RootState.Ready(current.currencyCode)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootState.Loading)
}
