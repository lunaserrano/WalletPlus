package com.walletplus.app.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.walletplus.app.data.local.dao.AccountDao
import com.walletplus.app.data.local.dao.BudgetDao
import com.walletplus.app.data.local.dao.CategoryDao
import com.walletplus.app.data.local.dao.GoalDao
import com.walletplus.app.data.local.dao.MaintenanceDao
import com.walletplus.app.data.local.dao.SettingsDao
import com.walletplus.app.data.local.dao.TransactionDao
import com.walletplus.app.data.local.entities.AccountEntity
import com.walletplus.app.data.local.entities.BudgetEntity
import com.walletplus.app.data.local.entities.CategoryEntity
import com.walletplus.app.data.local.entities.RecurringTransactionEntity
import com.walletplus.app.data.local.entities.SavingsGoalEntity
import com.walletplus.app.data.local.entities.TransactionEntity
import com.walletplus.app.data.local.entities.UserSettingsEntity
import com.walletplus.domain.model.DefaultCategories
import com.walletplus.domain.model.UserSettings

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        UserSettingsEntity::class,
        RecurringTransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun settingsDao(): SettingsDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        private const val NAME = "walletplus.db"

        fun build(context: Context): WalletDatabase =
            Room.databaseBuilder(context, WalletDatabase::class.java, NAME)
                .addCallback(SeedCallback)
                .build()

        fun inMemory(context: Context): WalletDatabase =
            Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .addCallback(SeedCallback)
                .allowMainThreadQueries()
                .build()
    }

    /** Inserta las categorías predefinidas y la fila de ajustes al crear la base de datos. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            DefaultCategories.all.forEachIndexed { index, seed ->
                val values = ContentValues().apply {
                    put("name", seed.name)
                    put("emoji", seed.emoji)
                    put("color_key", seed.colorKey)
                    put("type", seed.type.name)
                    put("is_active", 1)
                    put("system_key", seed.key)
                    put("sort_order", index)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
            val settings = ContentValues().apply {
                put("id", UserSettingsEntity.SINGLETON_ID)
                put("currency_code", UserSettings.DEFAULT_CURRENCY)
                putNull("display_name")
                put("expected_income_cents", 0L)
                put("spending_limit_cents", 0L)
                put("savings_target_cents", 0L)
                put("onboarding_completed", 0)
                put("demo_mode", 0)
                put("lock_enabled", 0)
                putNull("pin_hash")
                putNull("pin_salt")
            }
            db.insert("user_settings", SQLiteDatabase.CONFLICT_IGNORE, settings)
        }
    }
}
