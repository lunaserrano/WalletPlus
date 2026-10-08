package com.walletplus.domain.usecase

import com.walletplus.domain.model.Category
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.TransactionRepository
import com.walletplus.domain.validation.CategoryError
import com.walletplus.domain.validation.Outcome

class SaveCategoryUseCase(private val categories: CategoryRepository) {

    suspend operator fun invoke(
        id: Long = 0,
        name: String,
        emoji: String,
        colorKey: String,
        type: TransactionType,
    ): Outcome<Long, CategoryError> {
        val cleanName = name.trim()
        val errors = mutableSetOf<CategoryError>()
        if (cleanName.isEmpty()) errors += CategoryError.NAME_REQUIRED
        if (cleanName.length > MAX_NAME_LENGTH) errors += CategoryError.NAME_TOO_LONG
        if (emoji.isBlank()) errors += CategoryError.EMOJI_REQUIRED

        val all = categories.getAll()
        val existing = if (id != 0L) all.firstOrNull { it.id == id } else null
        if (id != 0L && existing == null) errors += CategoryError.NOT_FOUND
        val duplicated = all.any {
            it.id != id && it.type == type && it.name.equals(cleanName, ignoreCase = true)
        }
        if (duplicated) errors += CategoryError.NAME_DUPLICATED
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)

        val category = existing?.copy(name = cleanName, emoji = emoji.trim(), colorKey = colorKey)
            ?: Category(
                name = cleanName,
                emoji = emoji.trim(),
                colorKey = colorKey,
                type = type,
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: 0) + 1,
            )
        return Outcome.Success(categories.save(category))
    }

    companion object {
        const val MAX_NAME_LENGTH = 32
    }
}

class SetCategoryActiveUseCase(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: Long, active: Boolean) = categories.setActive(id, active)
}

class RemoveCategoryUseCase(
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
) {
    enum class Result { DELETED, DEACTIVATED, NOT_FOUND }

    /**
     * Las categorías con movimientos nunca se borran físicamente: se desactivan para
     * conservar el histórico.
     */
    suspend operator fun invoke(id: Long): Result {
        categories.getById(id) ?: return Result.NOT_FOUND
        return if (transactions.countByCategory(id) > 0) {
            categories.setActive(id, false)
            Result.DEACTIVATED
        } else {
            categories.delete(id)
            Result.DELETED
        }
    }
}
