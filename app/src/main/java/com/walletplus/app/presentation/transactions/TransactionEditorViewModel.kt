package com.walletplus.app.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.usecase.DeleteTransactionUseCase
import com.walletplus.domain.usecase.SaveTransactionUseCase
import com.walletplus.domain.usecase.TransactionDraft
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.TransactionError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class EditorState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val allCategories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val originalCategoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT_CARD,
    val isNecessary: Boolean = true,
    val description: String = "",
    val notes: String = "",
    val errors: Set<TransactionError> = emptySet(),
    val saving: Boolean = false,
) {
    /** Activas del tipo actual, más la original si fue desactivada (para editar históricos). */
    val categories: List<Category>
        get() = allCategories.filter { it.type == type && (it.isActive || it.id == originalCategoryId) }

    val amount: Money? get() = AmountInput.toMoney(amountText)
    val canSave: Boolean get() = !saving && amount?.isPositive == true && categoryId != null
}

sealed interface EditorEvent {
    data class Saved(val id: Long, val isNew: Boolean, val type: TransactionType, val amount: Money) : EditorEvent
    data class Deleted(val transaction: Transaction) : EditorEvent
}

class TransactionEditorViewModel(
    private val editId: Long,
    initialType: TransactionType,
    private val transactions: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val save: SaveTransactionUseCase,
    private val delete: DeleteTransactionUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val _state = MutableStateFlow(EditorState(type = initialType, date = today, today = today))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val lastCategory = mutableMapOf<TransactionType, Long?>()

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getAll()
            val existing = if (editId != 0L) transactions.getById(editId) else null
            if (existing != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        isEdit = true,
                        type = existing.type,
                        amountText = AmountInput.fromMoney(existing.amount),
                        allCategories = categories,
                        categoryId = existing.categoryId,
                        originalCategoryId = existing.categoryId,
                        date = existing.date,
                        paymentMethod = existing.paymentMethod,
                        isNecessary = existing.isNecessary ?: true,
                        description = existing.description,
                        notes = existing.notes.orEmpty(),
                    )
                }
            } else {
                for (type in TransactionType.entries) {
                    lastCategory[type] = transactions.lastUsedCategoryId(type)
                        ?.takeIf { id -> categories.any { it.id == id && it.isActive } }
                }
                _state.update {
                    it.copy(
                        loading = false,
                        allCategories = categories,
                        categoryId = lastCategory[it.type],
                        paymentMethod = if (it.type == TransactionType.INCOME) PaymentMethod.TRANSFER else PaymentMethod.DEBIT_CARD,
                    )
                }
            }
        }
    }

    fun setType(type: TransactionType) = _state.update {
        if (it.type == type) return@update it
        it.copy(
            type = type,
            categoryId = lastCategory[type],
            paymentMethod = if (!it.isEdit) {
                if (type == TransactionType.INCOME) PaymentMethod.TRANSFER else PaymentMethod.DEBIT_CARD
            } else {
                it.paymentMethod
            },
            errors = emptySet(),
        )
    }

    fun digit(d: Char) = _state.update { it.copy(amountText = AmountInput.appendDigit(it.amountText, d), errors = it.errors - AMOUNT_ERRORS) }
    fun decimal() = _state.update { it.copy(amountText = AmountInput.appendDecimal(it.amountText)) }
    fun backspace() = _state.update { it.copy(amountText = AmountInput.backspace(it.amountText)) }
    fun clear() = _state.update { it.copy(amountText = "") }
    fun selectCategory(id: Long) = _state.update {
        it.copy(categoryId = id, errors = it.errors - setOf(TransactionError.CATEGORY_REQUIRED, TransactionError.CATEGORY_INVALID, TransactionError.CATEGORY_INACTIVE))
    }
    fun setDate(date: LocalDate) = _state.update { it.copy(date = date, errors = it.errors - TransactionError.DATE_IN_FUTURE) }
    fun setMethod(method: PaymentMethod) = _state.update { it.copy(paymentMethod = method) }
    fun setNecessary(necessary: Boolean) = _state.update { it.copy(isNecessary = necessary) }
    fun setDescription(text: String) = _state.update { it.copy(description = text.take(80)) }
    fun setNotes(text: String) = _state.update { it.copy(notes = text.take(300)) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = save(
                TransactionDraft(
                    id = editId,
                    type = current.type,
                    amount = current.amount,
                    categoryId = current.categoryId,
                    description = current.description,
                    date = current.date,
                    paymentMethod = current.paymentMethod,
                    isNecessary = current.isNecessary,
                    notes = current.notes,
                ),
            )
            when (result) {
                is Outcome.Success -> {
                    _events.send(EditorEvent.Saved(result.value, editId == 0L, current.type, current.amount!!))
                }
                is Outcome.Invalid -> _state.update { it.copy(errors = result.errors, saving = false) }
            }
        }
    }

    fun deleteCurrent() {
        if (editId == 0L) return
        viewModelScope.launch {
            delete(editId)?.let { _events.send(EditorEvent.Deleted(it)) }
        }
    }

    private companion object {
        val AMOUNT_ERRORS = setOf(TransactionError.AMOUNT_REQUIRED, TransactionError.AMOUNT_NOT_POSITIVE)
    }
}
