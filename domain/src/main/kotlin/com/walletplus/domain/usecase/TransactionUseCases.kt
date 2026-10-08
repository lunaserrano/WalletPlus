package com.walletplus.domain.usecase

import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.TransactionError
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Datos introducidos en el formulario, aún sin validar. */
data class TransactionDraft(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Money?,
    val categoryId: Long?,
    val description: String = "",
    val date: LocalDate,
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT_CARD,
    val isNecessary: Boolean? = true,
    val notes: String? = null,
)

class SaveTransactionUseCase(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: TransactionDraft): Outcome<Long, TransactionError> {
        val errors = mutableSetOf<TransactionError>()
        val account = accounts.getPrimary()
        if (account == null) errors += TransactionError.NO_ACCOUNT

        when {
            draft.amount == null -> errors += TransactionError.AMOUNT_REQUIRED
            !draft.amount.isPositive -> errors += TransactionError.AMOUNT_NOT_POSITIVE
        }

        val existing = if (draft.id != 0L) transactions.getById(draft.id) else null
        if (draft.categoryId == null) {
            errors += TransactionError.CATEGORY_REQUIRED
        } else {
            val category = categories.getById(draft.categoryId)
            when {
                category == null || category.type != draft.type -> errors += TransactionError.CATEGORY_INVALID
                // Una categoría desactivada solo se conserva en movimientos que ya la usaban.
                !category.isActive && existing?.categoryId != category.id ->
                    errors += TransactionError.CATEGORY_INACTIVE
            }
        }

        val today = LocalDate.now(clock)
        if (draft.date.isAfter(today)) errors += TransactionError.DATE_IN_FUTURE

        if (errors.isNotEmpty() || account == null) return Outcome.Invalid(errors)

        val now = Instant.now(clock)
        val transaction = Transaction(
            id = draft.id,
            accountId = existing?.accountId ?: account.id,
            type = draft.type,
            amount = draft.amount!!,
            categoryId = draft.categoryId!!,
            description = draft.description.trim(),
            date = draft.date,
            paymentMethod = draft.paymentMethod,
            isNecessary = if (draft.type == TransactionType.EXPENSE) draft.isNecessary ?: true else null,
            notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
            recurringId = existing?.recurringId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return Outcome.Success(transactions.save(transaction))
    }
}

class DeleteTransactionUseCase(private val transactions: TransactionRepository) {
    /** Devuelve el movimiento eliminado para poder deshacer la acción. */
    suspend operator fun invoke(id: Long): Transaction? {
        val existing = transactions.getById(id) ?: return null
        transactions.delete(id)
        return existing
    }
}

class RestoreTransactionUseCase(private val transactions: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction) {
        transactions.save(transaction)
    }
}
