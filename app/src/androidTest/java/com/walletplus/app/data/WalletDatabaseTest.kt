package com.walletplus.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.walletplus.app.data.local.database.WalletDatabase
import com.walletplus.app.data.repositories.RoomAccountRepository
import com.walletplus.app.data.repositories.RoomCategoryRepository
import com.walletplus.app.data.repositories.RoomTransactionRepository
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.DefaultCategories
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionFilter
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.money.Money
import com.walletplus.domain.usecase.RemoveCategoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WalletDatabaseTest {

    private lateinit var db: WalletDatabase
    private lateinit var accounts: RoomAccountRepository
    private lateinit var categories: RoomCategoryRepository
    private lateinit var transactions: RoomTransactionRepository
    private val today = LocalDate.of(2026, 10, 15)

    @Before
    fun setUp() {
        db = WalletDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
        accounts = RoomAccountRepository(db.accountDao())
        categories = RoomCategoryRepository(db.categoryDao())
        transactions = RoomTransactionRepository(db.transactionDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun accountId() = accounts.save(
        Account(name = "Billetera", initialBalance = Money.ofUnits(500), startDate = today, currencyCode = "USD"),
    )

    private suspend fun categoryId(key: String) = categories.getAll().first { it.systemKey == key }.id

    private fun tx(account: Long, category: Long, type: TransactionType, cents: Long, description: String = "", date: LocalDate = today) =
        Transaction(
            accountId = account,
            type = type,
            amount = Money(cents),
            categoryId = category,
            description = description,
            date = date,
            paymentMethod = if (type == TransactionType.INCOME) PaymentMethod.TRANSFER else PaymentMethod.CASH,
            isNecessary = if (type == TransactionType.EXPENSE) true else null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

    @Test
    fun seedCreatesDefaultCategories() = runTest {
        assertEquals(DefaultCategories.all.size, categories.getAll().size)
    }

    @Test
    fun totalsAreSummedInCents() = runTest {
        val account = accountId()
        transactions.save(tx(account, categoryId("salary"), TransactionType.INCOME, 100_000))
        transactions.save(tx(account, categoryId("food"), TransactionType.EXPENSE, 30_000))
        val totals = transactions.observeTotals(account).first()
        assertEquals(Money(100_000), totals.income)
        assertEquals(Money(30_000), totals.expense)
    }

    @Test
    fun filtersByTypeCategoryDateAndText() = runTest {
        val account = accountId()
        val food = categoryId("food")
        transactions.save(tx(account, food, TransactionType.EXPENSE, 1_000, "Supermercado"))
        transactions.save(tx(account, categoryId("transport"), TransactionType.EXPENSE, 250, "Bus", today.minusMonths(1)))
        transactions.save(tx(account, categoryId("salary"), TransactionType.INCOME, 50_000, "Salario"))

        assertEquals(2, transactions.observeFiltered(TransactionFilter(types = setOf(TransactionType.EXPENSE))).first().size)
        assertEquals(1, transactions.observeFiltered(TransactionFilter(categoryIds = setOf(food))).first().size)
        assertEquals(1, transactions.observeFiltered(TransactionFilter(query = "super")).first().size)
        assertEquals(1, transactions.observeFiltered(TransactionFilter(query = "transporte")).first().size)
        assertEquals(2, transactions.observeFiltered(TransactionFilter(from = today.withDayOfMonth(1), to = today)).first().size)
    }

    @Test
    fun categoryWithTransactionsIsDeactivatedNotDeleted() = runTest {
        val account = accountId()
        val food = categoryId("food")
        transactions.save(tx(account, food, TransactionType.EXPENSE, 1_000))
        val result = RemoveCategoryUseCase(categories, transactions)(food)
        assertEquals(RemoveCategoryUseCase.Result.DEACTIVATED, result)
        assertFalse(categories.getById(food)!!.isActive)
        assertEquals(1, transactions.observeFiltered(TransactionFilter()).first().size)
    }
}
