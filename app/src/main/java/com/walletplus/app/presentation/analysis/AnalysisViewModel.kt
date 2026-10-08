package com.walletplus.app.presentation.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.calc.MonthTrendPoint
import com.walletplus.domain.usecase.FinancialSnapshot
import com.walletplus.domain.usecase.ObserveFinancialSnapshot
import com.walletplus.domain.usecase.ObserveMonthlyTrend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.YearMonth

data class AnalysisState(
    val month: YearMonth,
    val currentMonth: YearMonth,
    val snapshot: FinancialSnapshot?,
    val trend: List<MonthTrendPoint>,
) {
    val canGoForward: Boolean get() = month < currentMonth
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel(
    observeSnapshot: ObserveFinancialSnapshot,
    observeTrend: ObserveMonthlyTrend,
    clock: Clock,
) : ViewModel() {

    private val currentMonth = YearMonth.now(clock)
    private val month = MutableStateFlow(currentMonth)

    val state: StateFlow<AnalysisState> = combine(
        month,
        month.flatMapLatest { observeSnapshot(it) },
        observeTrend(currentMonth, TREND_MONTHS),
    ) { selected, snapshot, trend ->
        AnalysisState(selected, currentMonth, snapshot?.takeIf { it.month == selected }, trend)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisState(currentMonth, currentMonth, null, emptyList()))

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        if (month.value < currentMonth) month.value = month.value.plusMonths(1)
    }

    fun selectMonth(value: YearMonth) {
        if (value <= currentMonth) month.value = value
    }

    companion object {
        const val TREND_MONTHS = 6
    }
}
