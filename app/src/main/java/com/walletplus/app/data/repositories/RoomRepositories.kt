package com.walletplus.app.data.repositories

import androidx.room.withTransaction
import com.walletplus.app.data.local.dao.AccountDao
import com.walletplus.app.data.local.dao.BudgetDao
import com.walletplus.app.data.local.dao.CategoryDao
import com.walletplus.app.data.local.dao.GoalDao
import com.walletplus.app.data.local.dao.SettingsDao
import com.walletplus.app.data.local.dao.TransactionDao
import com.walletplus.app.data.local.database.WalletDatabase
import com.walletplus.app.data.local.toDomain
import com.walletplus.app.data.local.toEntity
import com.walletplus.domain.model.Account
import com.walletplus.domain.model.Budget
import com.walletplus.domain.model.Category
import com.walletplus.domain.model.MoneyTotals
import com.walletplus.domain.model.SavingsGoal
import com.walletplus.domain.model.Transaction
import com.walletplus.domain.model.TransactionFilter
import com.walletplus.domain.model.TransactionType
import com.walletplus.domain.model.UserSettings
import com.walletplus.domain.money.Money
import com.walletplus.domain.repository.AccountRepository
import com.walletplus.domain.repository.BudgetRepository
import com.walletplus.domain.repository.CategoryRepository
import com.walletplus.domain.repository.DataStoreMaintenance
import com.walletplus.domain.repository.GoalRepository
import com.walletplus.domain.repository.SettingsRepository
import com.walletplus.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** @Upsert devuelve -1 al actualizar; en ese caso el id es el de la entidad. */
private fun resolveId(entityId: Long, upsertResult: Long): Long = if (entityId != 0L) entityId else upsertResult

class RoomAccountRepository(private val dao: AccountDao) : AccountRepository {
    override fun observePrimary(): Flow<Account?> = dao.observePrimary().map { it?.toDomain() }.distinctUntilChanged()
    override suspend fun getPrimary(): Account? = dao.getPrimary()?.toDomain()
    override suspend fun save(account: Account): Long = resolveId(account.id, dao.upsert(account.toEntity()))
}

class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<Category> = dao.getAll().map { it.toDomain() }
    override suspend fun getById(id: Long): Category? = dao.getById(id)?.toDomain()
    override suspend fun save(category: Category): Long = resolveId(category.id, dao.upsert(category.toEntity()))
    override suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomTransactionRepository(private val dao: TransactionDao) : TransactionRepository {
    override fun observeTotals(accountId: Long): Flow<MoneyTotals> =
        dao.observeTotals(accountId).map { MoneyTotals(Money(it.income), Money(it.expense)) }.distinctUntilChanged()

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        dao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> =
        dao.observeFiltered(
            query = filter.query.trim(),
            filterTypes = filter.types.isNotEmpty(),
            types = filter.types.toList(),
            filterCategories = filter.categoryIds.isNotEmpty(),
            categoryIds = filter.categoryIds.toList(),
            filterMethods = filter.paymentMethods.isNotEmpty(),
            methods = filter.paymentMethods.toList(),
            from = filter.from,
            to = filter.to,
        ).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Transaction? = dao.getById(id)?.toDomain()
    override suspend fun save(transaction: Transaction): Long =
        resolveId(transaction.id, dao.upsert(transaction.toEntity()))
    override suspend fun saveAll(transactions: List<Transaction>) = dao.insertAll(transactions.map { it.toEntity() })
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun countByCategory(categoryId: Long): Int = dao.countByCategory(categoryId)
    override suspend fun lastUsedCategoryId(type: TransactionType): Long? = dao.lastUsedCategoryId(type)
}

class RoomBudgetRepository(private val dao: BudgetDao) : BudgetRepository {
    override fun observeAll(): Flow<List<Budget>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getByCategory(categoryId: Long): Budget? = dao.getByCategory(categoryId)?.toDomain()
    override suspend fun save(budget: Budget): Long = resolveId(budget.id, dao.upsert(budget.toEntity()))
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomGoalRepository(private val dao: GoalDao) : GoalRepository {
    override fun observeAll(): Flow<List<SavingsGoal>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getById(id: Long): SavingsGoal? = dao.getById(id)?.toDomain()
    override suspend fun save(goal: SavingsGoal): Long = resolveId(goal.id, dao.upsert(goal.toEntity()))
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomSettingsRepository(private val dao: SettingsDao) : SettingsRepository {
    override fun observe(): Flow<UserSettings> = dao.observe().map { it.toDomain() }.distinctUntilChanged()
    override suspend fun get(): UserSettings = dao.get().toDomain()
    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        val existing = dao.get()
        dao.upsert(transform(existing.toDomain()).toEntity(existing))
    }
}

class RoomDataStoreMaintenance(private val db: WalletDatabase) : DataStoreMaintenance {
    override suspend fun <R> atomically(block: suspend () -> R): R = db.withTransaction { block() }

    override suspend fun clearUserData() {
        db.withTransaction {
            db.transactionDao().deleteAll()
            db.maintenanceDao().deleteRecurring()
            db.budgetDao().deleteAll()
            db.goalDao().deleteAll()
            db.accountDao().deleteAll()
            db.categoryDao().deleteCustom()
            db.categoryDao().activateAll()
        }
    }
}
