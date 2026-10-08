package com.walletplus.domain.repository

import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.MoneyTotals
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionFilter
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AccountRepository {
    fun observePrimary(): Flow<Account?>
    suspend fun getPrimary(): Account?
    suspend fun save(account: Account): Long
}

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun getAll(): List<Category>
    suspend fun getById(id: Long): Category?
    suspend fun save(category: Category): Long
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun delete(id: Long)
}

interface TransactionRepository {
    fun observeTotals(accountId: Long): Flow<MoneyTotals>
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>>
    fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>>
    suspend fun getById(id: Long): Transaction?
    suspend fun save(transaction: Transaction): Long
    suspend fun saveAll(transactions: List<Transaction>)
    suspend fun delete(id: Long)
    suspend fun countByCategory(categoryId: Long): Int
    suspend fun lastUsedCategoryId(type: TransactionType): Long?
}

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    suspend fun getAll(): List<Budget>
    suspend fun getByCategory(categoryId: Long): Budget?
    suspend fun save(budget: Budget): Long
    suspend fun delete(id: Long)
}

interface GoalRepository {
    fun observeAll(): Flow<List<SavingsGoal>>
    suspend fun getById(id: Long): SavingsGoal?
    suspend fun save(goal: SavingsGoal): Long
    suspend fun delete(id: Long)
}

interface SettingsRepository {
    fun observe(): Flow<UserSettings>
    suspend fun get(): UserSettings
    suspend fun update(transform: (UserSettings) -> UserSettings)
}

/** Ejecuta varias escrituras de forma atómica y permite limpiar los datos del usuario. */
interface DataStoreMaintenance {
    suspend fun <R> atomically(block: suspend () -> R): R
    /** Borra movimientos, presupuestos, metas, cuentas y categorías personalizadas. */
    suspend fun clearUserData()
}
