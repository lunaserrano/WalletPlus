package com.walletplus.domain.usecase

import com.walletplus.domain.model.Account
import com.walletplus.domain.model.SpendingPlan
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.DataStoreMaintenance
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.time.PeriodType
import com.walletplus.domain.validation.Outcome
import com.walletplus.domain.validation.SettingsError
import java.time.Clock
import java.time.LocalDate

private val CURRENCY_CODE = Regex("^[A-Z]{3}$")

class CompleteOnboardingUseCase(
    private val accounts: AccountRepository,
    private val settings: SettingsRepository,
    private val maintenance: DataStoreMaintenance,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        initialBalance: Money?,
        startDate: LocalDate,
        currencyCode: String,
        displayName: String?,
    ): Outcome<Unit, SettingsError> {
        val errors = mutableSetOf<SettingsError>()
        if (initialBalance != null && initialBalance.isNegative) errors += SettingsError.BALANCE_NEGATIVE
        if (startDate.isAfter(LocalDate.now(clock))) errors += SettingsError.DATE_IN_FUTURE
        if (!CURRENCY_CODE.matches(currencyCode)) errors += SettingsError.CURRENCY_INVALID
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)

        maintenance.atomically {
            val existing = accounts.getPrimary()
            accounts.save(
                existing?.copy(
                    initialBalance = initialBalance ?: Money.ZERO,
                    startDate = startDate,
                    currencyCode = currencyCode,
                ) ?: Account(
                    name = PRIMARY_ACCOUNT_NAME,
                    initialBalance = initialBalance ?: Money.ZERO,
                    startDate = startDate,
                    currencyCode = currencyCode,
                ),
            )
            settings.update {
                it.copy(
                    currencyCode = currencyCode,
                    displayName = displayName?.trim()?.takeIf { name -> name.isNotEmpty() },
                    onboardingCompleted = true,
                )
            }
        }
        return Outcome.Success(Unit)
    }

    companion object {
        const val PRIMARY_ACCOUNT_NAME = "Billetera principal"
    }
}

class UpdateAccountUseCase(
    private val accounts: AccountRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(initialBalance: Money?, startDate: LocalDate): Outcome<Unit, SettingsError> {
        val errors = mutableSetOf<SettingsError>()
        if (initialBalance == null || initialBalance.isNegative) errors += SettingsError.BALANCE_NEGATIVE
        if (startDate.isAfter(LocalDate.now(clock))) errors += SettingsError.DATE_IN_FUTURE
        val account = accounts.getPrimary()
        if (errors.isNotEmpty() || account == null) return Outcome.Invalid(errors)
        accounts.save(account.copy(initialBalance = initialBalance!!, startDate = startDate))
        return Outcome.Success(Unit)
    }
}

class UpdatePlanUseCase(
    private val settings: SettingsRepository,
    private val budgets: BudgetRepository,
    private val maintenance: DataStoreMaintenance,
) {
    /**
     * Guarda el plan general y el ciclo de pago. Los montos de [plan] ya vienen expresados en [payPeriod];
     * si el ciclo cambia, los presupuestos por categoría se convierten (mensual ÷ 2 = quincenal).
     */
    suspend operator fun invoke(plan: SpendingPlan, payPeriod: PeriodType): Outcome<Unit, SettingsError> {
        require(payPeriod.isPayPeriod) { "El ciclo de pago debe ser mensual o quincenal" }
        val values = listOf(plan.expectedIncome, plan.spendingLimit, plan.savingsTarget)
        if (values.any { it.isNegative }) return Outcome.Invalid(setOf(SettingsError.VALUE_NEGATIVE))
        maintenance.atomically {
            val previous = settings.get().payPeriod
            if (previous != payPeriod) {
                budgets.getAll().forEach { budget ->
                    budgets.save(budget.copy(limit = convertBetweenPayPeriods(budget.limit, previous, payPeriod)))
                }
            }
            settings.update { it.copy(plan = plan, payPeriod = payPeriod) }
        }
        return Outcome.Success(Unit)
    }
}

/** Convierte un monto entre ciclos de pago: un mes equivale a dos quincenas (al dividir se redondea hacia arriba). */
fun convertBetweenPayPeriods(amount: Money, from: PeriodType, to: PeriodType): Money = when {
    from == to -> amount
    from == PeriodType.MONTHLY && to == PeriodType.BIWEEKLY -> amount.divideRoundingUp(2)
    from == PeriodType.BIWEEKLY && to == PeriodType.MONTHLY -> amount * 2
    else -> throw IllegalArgumentException("Conversión no soportada: $from → $to")
}

class UpdatePreferencesUseCase(
    private val settings: SettingsRepository,
    private val accounts: AccountRepository,
) {
    suspend fun currency(code: String): Outcome<Unit, SettingsError> {
        if (!CURRENCY_CODE.matches(code)) return Outcome.Invalid(setOf(SettingsError.CURRENCY_INVALID))
        settings.update { it.copy(currencyCode = code) }
        accounts.getPrimary()?.let { accounts.save(it.copy(currencyCode = code)) }
        return Outcome.Success(Unit)
    }

    suspend fun tourCompleted(done: Boolean) {
        settings.update { it.copy(tourCompleted = done) }
    }

    suspend fun displayName(name: String?) {
        settings.update { it.copy(displayName = name?.trim()?.takeIf { value -> value.isNotEmpty() }) }
    }
}

/** Borra todos los datos del usuario y vuelve al estado inicial (onboarding). */
class ResetAllDataUseCase(
    private val maintenance: DataStoreMaintenance,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke() {
        maintenance.atomically {
            maintenance.clearUserData()
            settings.update { current -> UserSettings(lockEnabled = current.lockEnabled) }
        }
    }
}
