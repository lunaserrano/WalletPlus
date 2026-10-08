package com.walletplus.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.walletplus.app.data.local.entities.AccountEntity
import com.walletplus.app.data.local.entities.BudgetEntity
import com.walletplus.app.data.local.entities.CategoryEntity
import com.walletplus.app.data.local.entities.SavingsGoalEntity
import com.walletplus.app.data.local.entities.TransactionEntity
import com.walletplus.app.data.local.entities.UserSettingsEntity
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY id LIMIT 1")
    fun observePrimary(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY id LIMIT 1")
    suspend fun getPrimary(): AccountEntity?

    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET is_active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories WHERE system_key IS NULL")
    suspend fun deleteCustom()

    @Query("UPDATE categories SET is_active = 1")
    suspend fun activateAll()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

data class TotalsRow(
    @ColumnInfo(name = "income") val income: Long,
    @ColumnInfo(name = "expense") val expense: Long,
)

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount_cents ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_cents ELSE 0 END), 0) AS expense
        FROM transactions WHERE account_id = :accountId
        """,
    )
    fun observeTotals(accountId: Long): Flow<TotalsRow>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, created_at DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT t.* FROM transactions t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE (:query = '' OR t.description LIKE '%' || :query || '%'
                OR t.notes LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
          AND (:filterTypes = 0 OR t.type IN (:types))
          AND (:filterCategories = 0 OR t.category_id IN (:categoryIds))
          AND (:filterMethods = 0 OR t.payment_method IN (:methods))
          AND (:from IS NULL OR t.date >= :from)
          AND (:to IS NULL OR t.date <= :to)
        ORDER BY t.date DESC, t.created_at DESC
        """,
    )
    fun observeFiltered(
        query: String,
        filterTypes: Boolean,
        types: List<TransactionType>,
        filterCategories: Boolean,
        categoryIds: List<Long>,
        filterMethods: Boolean,
        methods: List<PaymentMethod>,
        from: LocalDate?,
        to: LocalDate?,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("SELECT category_id FROM transactions WHERE type = :type ORDER BY created_at DESC LIMIT 1")
    suspend fun lastUsedCategoryId(type: TransactionType): Long?
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND year_month IS NULL LIMIT 1")
    suspend fun getByCategory(categoryId: Long): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY is_archived, created_at")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Upsert
    suspend fun upsert(goal: SavingsGoalEntity): Long

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun get(): UserSettingsEntity?

    @Upsert
    suspend fun upsert(settings: UserSettingsEntity)
}

@Dao
interface MaintenanceDao {
    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteRecurring()
}
