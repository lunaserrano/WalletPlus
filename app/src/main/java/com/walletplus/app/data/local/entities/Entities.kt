package com.walletplus.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletplus.domain.model.PaymentMethod
import com.walletplus.domain.model.RecurrenceFrequency
import com.walletplus.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "initial_balance_cents") val initialBalanceCents: Long,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["system_key"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    @ColumnInfo(name = "color_key") val colorKey: String,
    val type: TransactionType,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "system_key") val systemKey: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // RESTRICT: una categoría con movimientos nunca se borra físicamente.
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("account_id"), Index("category_id"), Index("date"), Index("recurring_id")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    val type: TransactionType,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val description: String,
    val date: LocalDate,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @ColumnInfo(name = "is_necessary") val isNecessary: Boolean?,
    val notes: String?,
    @ColumnInfo(name = "recurring_id") val recurringId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "limit_cents") val limitCents: Long,
    /** Null = aplica a todos los meses. */
    @ColumnInfo(name = "year_month") val yearMonth: YearMonth?,
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    @ColumnInfo(name = "target_cents") val targetCents: Long,
    @ColumnInfo(name = "saved_cents") val savedCents: Long,
    @ColumnInfo(name = "target_date") val targetDate: LocalDate?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)

/** Fila única (id = 1) con las preferencias del usuario. */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "expected_income_cents") val expectedIncomeCents: Long,
    @ColumnInfo(name = "spending_limit_cents") val spendingLimitCents: Long,
    @ColumnInfo(name = "savings_target_cents") val savingsTargetCents: Long,
    @ColumnInfo(name = "onboarding_completed") val onboardingCompleted: Boolean,
    @ColumnInfo(name = "demo_mode") val demoMode: Boolean,
    @ColumnInfo(name = "lock_enabled") val lockEnabled: Boolean,
    @ColumnInfo(name = "tour_completed", defaultValue = "0") val tourCompleted: Boolean = false,
    /** Hash PBKDF2 del PIN (nunca el PIN en texto plano). */
    @ColumnInfo(name = "pin_hash") val pinHash: String?,
    @ColumnInfo(name = "pin_salt") val pinSalt: String?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** Preparada para la automatización de movimientos recurrentes en una fase posterior. */
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("account_id"), Index("category_id")],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    val type: TransactionType,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val description: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod,
    @ColumnInfo(name = "is_necessary") val isNecessary: Boolean?,
    val frequency: RecurrenceFrequency,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "end_date") val endDate: LocalDate?,
    @ColumnInfo(name = "next_occurrence") val nextOccurrence: LocalDate,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
)
