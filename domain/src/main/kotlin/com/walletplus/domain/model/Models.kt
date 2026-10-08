package com.walletplus.domain.model

import com.walletplus.domain.money.Money
import com.walletplus.domain.time.PeriodType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

enum class TransactionType { INCOME, EXPENSE }

enum class PaymentMethod { CASH, DEBIT_CARD, CREDIT_CARD, TRANSFER, OTHER }

/**
 * Cuenta o billetera. El saldo actual nunca se almacena: se calcula como
 * saldo inicial + ingresos − gastos (ver [com.walletplus.domain.calc.FinanceCalculator.balance]).
 */
data class Account(
    val id: Long = 0,
    val name: String,
    val initialBalance: Money,
    val startDate: LocalDate,
    val currencyCode: String,
    val isArchived: Boolean = false,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorKey: String,
    val type: TransactionType,
    val isActive: Boolean = true,
    /** Clave estable de las categorías predefinidas (null en las creadas por el usuario). */
    val systemKey: String? = null,
    val sortOrder: Int = 0,
)

data class Transaction(
    val id: Long = 0,
    val accountId: Long,
    val type: TransactionType,
    val amount: Money,
    val categoryId: Long,
    val description: String,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    /** Solo aplica a gastos: true = necesario, false = opcional. */
    val isNecessary: Boolean?,
    val notes: String? = null,
    /** Preparado para movimientos generados por una regla recurrente. */
    val recurringId: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
    val isIncome: Boolean get() = type == TransactionType.INCOME
    val isOptionalExpense: Boolean get() = isExpense && isNecessary == false
}

/**
 * Presupuesto por categoría para cada ciclo de pago ([UserSettings.payPeriod]).
 * [yearMonth] null significa que aplica a todos los meses.
 */
data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val limit: Money,
    val yearMonth: YearMonth? = null,
)

data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val target: Money,
    val saved: Money,
    val targetDate: LocalDate?,
    val createdAt: Instant,
    val isArchived: Boolean = false,
)

/** Presupuesto general por ciclo de pago (mes o quincena). Un valor cero significa "no definido". */
data class SpendingPlan(
    val expectedIncome: Money = Money.ZERO,
    val spendingLimit: Money = Money.ZERO,
    val savingsTarget: Money = Money.ZERO,
) {
    val isConfigured: Boolean
        get() = expectedIncome.isPositive || spendingLimit.isPositive || savingsTarget.isPositive
}

data class UserSettings(
    val currencyCode: String = DEFAULT_CURRENCY,
    val displayName: String? = null,
    val plan: SpendingPlan = SpendingPlan(),
    /** Cada cuánto cobra el usuario: define el periodo de presupuestos, plan y Dashboard. */
    val payPeriod: PeriodType = PeriodType.MONTHLY,
    val onboardingCompleted: Boolean = false,
    val demoMode: Boolean = false,
    val lockEnabled: Boolean = false,
    /** El usuario ya vio (o descartó) el recorrido guiado de primer uso. */
    val tourCompleted: Boolean = false,
) {
    companion object {
        const val DEFAULT_CURRENCY = "USD"
    }
}

enum class RecurrenceFrequency { WEEKLY, BIWEEKLY, MONTHLY, YEARLY }

/**
 * Regla de movimiento recurrente (salario, alquiler, suscripciones...).
 * Preparada en el modelo y la base de datos; la automatización llega en una fase posterior.
 */
data class RecurringRule(
    val id: Long = 0,
    val accountId: Long,
    val type: TransactionType,
    val amount: Money,
    val categoryId: Long,
    val description: String,
    val paymentMethod: PaymentMethod,
    val isNecessary: Boolean?,
    val frequency: RecurrenceFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val nextOccurrence: LocalDate,
    val isActive: Boolean = true,
)

/** Criterios de búsqueda del historial. Conjuntos vacíos significan "sin filtrar". */
data class TransactionFilter(
    val query: String = "",
    val types: Set<TransactionType> = emptySet(),
    val categoryIds: Set<Long> = emptySet(),
    val paymentMethods: Set<PaymentMethod> = emptySet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || types.isNotEmpty() || categoryIds.isNotEmpty() ||
            paymentMethods.isNotEmpty() || from != null || to != null

    val activeCount: Int
        get() = listOf(
            types.isNotEmpty(),
            categoryIds.isNotEmpty(),
            paymentMethods.isNotEmpty(),
            from != null || to != null,
        ).count { it }
}

data class MoneyTotals(val income: Money = Money.ZERO, val expense: Money = Money.ZERO)
