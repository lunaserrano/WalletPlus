package com.walletplus.domain.validation

/** Resultado de una operación que puede fallar por validación. */
sealed interface Outcome<out T, out E> {
    data class Success<T>(val value: T) : Outcome<T, Nothing>
    data class Invalid<E>(val errors: Set<E>) : Outcome<Nothing, E>
}

enum class TransactionError {
    AMOUNT_REQUIRED,
    AMOUNT_NOT_POSITIVE,
    CATEGORY_REQUIRED,
    CATEGORY_INVALID,
    CATEGORY_INACTIVE,
    DATE_IN_FUTURE,
    DATE_BEFORE_START,
    NO_ACCOUNT,
}

enum class CategoryError { NAME_REQUIRED, NAME_TOO_LONG, NAME_DUPLICATED, EMOJI_REQUIRED, NOT_FOUND }

enum class BudgetError { CATEGORY_INVALID, LIMIT_NEGATIVE, LIMIT_REQUIRED }

enum class GoalError { NAME_REQUIRED, TARGET_NOT_POSITIVE, SAVED_NEGATIVE, DATE_IN_PAST, NOT_FOUND, AMOUNT_NOT_POSITIVE, INSUFFICIENT_SAVED }

enum class SettingsError { BALANCE_NEGATIVE, DATE_IN_FUTURE, VALUE_NEGATIVE, CURRENCY_INVALID }
