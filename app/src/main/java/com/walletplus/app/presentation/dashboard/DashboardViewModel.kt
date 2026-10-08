package com.walletplus.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.usecase.FinancialSnapshot
import com.walletplus.domain.usecase.ObserveFinancialSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalTime

sealed interface DashboardState {
    data object Loading : DashboardState
    data class Ready(val snapshot: FinancialSnapshot, val greeting: String) : DashboardState
}

class DashboardViewModel(observeSnapshot: ObserveFinancialSnapshot, private val clock: Clock) : ViewModel() {

    /** Siempre el ciclo de pago en curso (mes o quincena). */
    val state: StateFlow<DashboardState> = observeSnapshot()
        .map { snapshot ->
            if (snapshot == null) DashboardState.Loading
            else DashboardState.Ready(snapshot, greeting(snapshot.settings.displayName))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState.Loading)

    private fun greeting(name: String?): String {
        val hour = LocalTime.now(clock).hour
        val base = when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        return if (name.isNullOrBlank()) base else "$base, $name"
    }
}
