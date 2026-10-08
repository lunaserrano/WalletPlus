package com.walletplus.app.presentation.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.calc.TrendPoint
import com.walletplus.domain.time.Period
import com.walletplus.domain.time.PeriodType
import com.walletplus.domain.usecase.FinancialSnapshot
import com.walletplus.domain.usecase.ObserveFinancialSnapshot
import com.walletplus.domain.usecase.ObserveTrend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class AnalysisState(
    /** Null solo mientras carga la primera vez. */
    val period: Period?,
    val today: LocalDate,
    val snapshot: FinancialSnapshot?,
    val trend: List<TrendPoint>,
) {
    val canGoForward: Boolean get() = period != null && !period.next().start.isAfter(today)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel(
    observeSnapshot: ObserveFinancialSnapshot,
    observeTrend: ObserveTrend,
    private val clock: Clock,
) : ViewModel() {

    /** Null = el ciclo de pago en curso, hasta que el usuario navegue o cambie de vista. */
    private val selected = MutableStateFlow<Period?>(null)
    private val snapshot = selected.flatMapLatest { observeSnapshot(it) }
    private val trend = snapshot
        .map { it?.period?.type }
        .distinctUntilChanged()
        .flatMapLatest { type -> if (type == null) flowOf(emptyList()) else observeTrend(type, TREND_PERIODS) }

    val state: StateFlow<AnalysisState> = combine(selected, snapshot, trend) { period, snap, points ->
        val shown = snap?.takeIf { period == null || it.period == period }
        AnalysisState(
            period = shown?.period ?: period,
            today = LocalDate.now(clock),
            snapshot = shown,
            trend = points.takeIf { shown != null && points.firstOrNull()?.period?.type == shown.period.type }.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisState(null, LocalDate.now(clock), null, emptyList()))

    fun previousPeriod() {
        state.value.period?.let { selected.value = it.previous() }
    }

    fun nextPeriod() {
        val current = state.value
        if (current.canGoForward) selected.value = current.period!!.next()
    }

    fun selectPeriod(value: Period) {
        if (!value.start.isAfter(LocalDate.now(clock))) selected.value = value
    }

    /** Cambia entre vista semanal, quincenal y mensual conservando la fecha que se estaba viendo. */
    fun selectType(type: PeriodType) {
        val today = LocalDate.now(clock)
        val anchor = state.value.period?.let { minOf(it.end, today) } ?: today
        selected.value = Period.containing(type, anchor)
    }

    companion object {
        const val TREND_PERIODS = 6
    }
}
