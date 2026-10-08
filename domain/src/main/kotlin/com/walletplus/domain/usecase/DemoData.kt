package com.walletplus.domain.usecase

import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.MonthlyPlan
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.DataStoreMaintenance
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * Genera movimientos realistas y deterministas para los últimos 3 meses.
 * Pensado para probar gráficos y análisis; siempre se marca como modo demostración.
 */
class DemoDataGenerator(private val seed: Int = 42) {

    fun generate(today: LocalDate, accountId: Long, categoryIds: Map<String, Long>): List<Transaction> {
        val random = Random(seed)
        val result = mutableListOf<Transaction>()
        val months = listOf(2L, 1L, 0L).map { YearMonth.from(today).minusMonths(it) }

        fun add(
            type: TransactionType,
            key: String,
            cents: Long,
            date: LocalDate,
            description: String,
            method: PaymentMethod,
            necessary: Boolean? = null,
        ) {
            if (date.isAfter(today)) return
            val categoryId = categoryIds[key] ?: return
            val instant = date.atTime(12, 0).toInstant(ZoneOffset.UTC).plusSeconds(result.size.toLong())
            result += Transaction(
                accountId = accountId,
                type = type,
                amount = Money(cents),
                categoryId = categoryId,
                description = description,
                date = date,
                paymentMethod = method,
                isNecessary = if (type == TransactionType.EXPENSE) necessary ?: true else null,
                createdAt = instant,
                updatedAt = instant,
            )
        }

        fun expense(key: String, cents: Long, date: LocalDate, text: String, method: PaymentMethod, necessary: Boolean) =
            add(TransactionType.EXPENSE, key, cents, date, text, method, necessary)

        fun between(min: Long, max: Long): Long = random.nextLong(min, max + 1)

        months.forEachIndexed { index, month ->
            val isCurrent = index == months.lastIndex
            fun day(d: Int) = month.atDay(d.coerceAtMost(month.lengthOfMonth()))

            add(TransactionType.INCOME, "salary", 120_000, day(1), "Salario mensual", PaymentMethod.TRANSFER)
            if (index != 1) {
                add(TransactionType.INCOME, "freelance", between(15_000, 28_000), day(16), "Proyecto freelance", PaymentMethod.TRANSFER)
            }

            expense("housing", 35_000, day(3), "Alquiler", PaymentMethod.TRANSFER, true)
            expense("utilities", 3_000, day(5), "Internet hogar", PaymentMethod.DEBIT_CARD, true)
            expense("utilities", between(2_400, 4_100), day(8), "Electricidad", PaymentMethod.DEBIT_CARD, true)
            expense("subscriptions", 1_500, day(10), "Plan de teléfono", PaymentMethod.CREDIT_CARD, true)
            expense("subscriptions", 999, day(12), "Streaming", PaymentMethod.CREDIT_CARD, false)

            var groceryDay = 2
            while (groceryDay <= month.lengthOfMonth()) {
                expense("food", between(1_800, 5_500), day(groceryDay), "Supermercado", PaymentMethod.DEBIT_CARD, true)
                groceryDay += between(3, 5).toInt()
            }

            repeat(if (isCurrent) 14 else 10) {
                expense("food", between(250, 480), day(between(1, 28).toInt()), "Café", PaymentMethod.CASH, false)
            }

            // El transporte crece en el mes actual para ilustrar la comparación mensual.
            repeat(if (isCurrent) 14 else 9) {
                expense("transport", between(125, 300), day(between(1, 28).toInt()), "Bus", PaymentMethod.CASH, true)
            }
            repeat(if (isCurrent) 3 else 1) {
                expense("transport", between(800, 1_400), day(between(1, 28).toInt()), "Taxi", PaymentMethod.DEBIT_CARD, false)
            }

            repeat(2) {
                expense("entertainment", between(1_200, 3_500), day(between(5, 27).toInt()), listOf("Cine", "Concierto", "Salida con amigos")[random.nextInt(3)], PaymentMethod.DEBIT_CARD, false)
            }
            expense("shopping", between(2_500, 7_000), day(between(6, 26).toInt()), "Ropa", PaymentMethod.CREDIT_CARD, false)
            if (index == 0) expense("health", 4_500, day(14), "Consulta médica", PaymentMethod.DEBIT_CARD, true)
            if (index == 1) expense("education", 2_900, day(9), "Curso en línea", PaymentMethod.CREDIT_CARD, true)
        }
        return result.sortedBy { it.date }
    }
}

class LoadDemoDataUseCase(
    private val maintenance: DataStoreMaintenance,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val budgets: BudgetRepository,
    private val goals: GoalRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val generator: DemoDataGenerator = DemoDataGenerator(),
) {
    suspend operator fun invoke() {
        val today = LocalDate.now(clock)
        maintenance.atomically {
            maintenance.clearUserData()
            val currency = settings.get().currencyCode
            val accountId = accounts.save(
                Account(
                    name = CompleteOnboardingUseCase.PRIMARY_ACCOUNT_NAME,
                    initialBalance = Money.ofUnits(500),
                    startDate = YearMonth.from(today).minusMonths(2).atDay(1),
                    currencyCode = currency,
                ),
            )
            val ids = categories.getAll().mapNotNull { c -> c.systemKey?.let { it to c.id } }.toMap()
            transactions.saveAll(generator.generate(today, accountId, ids))

            listOf("food" to 30_000L, "transport" to 4_500L, "entertainment" to 5_000L, "shopping" to 6_000L)
                .forEach { (key, cents) ->
                    ids[key]?.let { budgets.save(Budget(categoryId = it, limit = Money(cents))) }
                }

            val now = Instant.now(clock)
            goals.save(
                SavingsGoal(
                    name = "Fondo de emergencia",
                    emoji = "🛟",
                    target = Money.ofUnits(1_000),
                    saved = Money.ofUnits(350),
                    targetDate = YearMonth.from(today).plusMonths(6).atEndOfMonth(),
                    createdAt = now,
                ),
            )
            goals.save(
                SavingsGoal(
                    name = "Vacaciones",
                    emoji = "🏖️",
                    target = Money.ofUnits(600),
                    saved = Money.ofUnits(120),
                    targetDate = null,
                    createdAt = now.plusSeconds(1),
                ),
            )

            settings.update {
                it.copy(
                    onboardingCompleted = true,
                    demoMode = true,
                    plan = MonthlyPlan(
                        expectedIncome = Money.ofUnits(1_200),
                        spendingLimit = Money.ofUnits(800),
                        savingsTarget = Money.ofUnits(400),
                    ),
                )
            }
        }
    }
}
