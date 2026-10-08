package com.walletplus.app.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionFilter
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.money.sumOfMoney
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.usecase.DeleteTransactionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

enum class DatePreset(val label: String) {
    ALL("Todo"),
    THIS_MONTH("Este mes"),
    LAST_MONTH("Mes anterior"),
    LAST_3_MONTHS("Últimos 3 meses"),
    CUSTOM("Personalizado"),
}

data class DayGroup(val date: LocalDate, val items: List<Transaction>, val net: Money)

data class TransactionsState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter(),
    val datePreset: DatePreset = DatePreset.ALL,
    val groups: List<DayGroup> = emptyList(),
    val resultCount: Int = 0,
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO,
    val categories: List<Category> = emptyList(),
    val hasAnyTransaction: Boolean = false,
    val today: LocalDate = LocalDate.now(),
) {
    val categoriesById: Map<Long, Category> by lazy { categories.associateBy { it.id } }
}

private data class FilterInput(val query: String, val filter: TransactionFilter, val preset: DatePreset)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TransactionsViewModel(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    private val delete: DeleteTransactionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TransactionFilter())
    private val preset = MutableStateFlow(DatePreset.ALL)

    private val debouncedQuery = query.debounce(200).distinctUntilChanged()

    private val results = combine(debouncedQuery, filter) { q, f -> f.copy(query = q) }
        .flatMapLatest { transactions.observeFiltered(it) }

    private val hasAny = transactions.observeFiltered(TransactionFilter()).map { it.isNotEmpty() }.distinctUntilChanged()

    val state: StateFlow<TransactionsState> = combine(
        combine(query, filter, preset, ::FilterInput),
        results,
        categories.observeAll(),
        hasAny,
    ) { input, list, cats, any ->
        TransactionsState(
            loading = false,
            query = input.query,
            filter = input.filter,
            datePreset = input.preset,
            groups = list.groupBy { it.date }.map { (date, items) ->
                DayGroup(
                    date = date,
                    items = items,
                    net = items.sumOfMoney { if (it.isIncome) it.amount else -it.amount },
                )
            },
            resultCount = list.size,
            income = list.filter { it.isIncome }.sumOfMoney { it.amount },
            expense = list.filter { it.isExpense }.sumOfMoney { it.amount },
            categories = cats,
            hasAnyTransaction = any,
            today = LocalDate.now(clock),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsState())

    fun setQuery(value: String) {
        query.value = value.take(60)
    }

    fun toggleType(type: TransactionType) = filter.update {
        it.copy(types = if (it.types == setOf(type)) emptySet() else setOf(type))
    }

    fun setCategories(ids: Set<Long>) = filter.update { it.copy(categoryIds = ids) }

    fun setMethods(methods: Set<PaymentMethod>) = filter.update { it.copy(paymentMethods = methods) }

    fun setPreset(value: DatePreset, from: LocalDate? = null, to: LocalDate? = null) {
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val (start, end) = when (value) {
            DatePreset.ALL -> null to null
            DatePreset.THIS_MONTH -> month.atDay(1) to month.atEndOfMonth()
            DatePreset.LAST_MONTH -> month.minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
            DatePreset.LAST_3_MONTHS -> month.minusMonths(2).atDay(1) to month.atEndOfMonth()
            DatePreset.CUSTOM -> from to to
        }
        preset.value = value
        filter.update { it.copy(from = start, to = end) }
    }

    fun clearFilters() {
        query.value = ""
        preset.value = DatePreset.ALL
        filter.value = TransactionFilter()
    }

    suspend fun deleteTransaction(id: Long): Transaction? = delete(id)
}
