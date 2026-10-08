package com.walletplus.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.walletplus.app.data.local.database.WalletDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), WalletDatabase::class.java)

    @Test
    fun migrate1To2KeepsSettingsAndAddsTourFlag() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO user_settings (id, currency_code, display_name, expected_income_cents, spending_limit_cents,
                    savings_target_cents, onboarding_completed, demo_mode, lock_enabled, pin_hash, pin_salt)
                VALUES (1, 'EUR', 'Ana', 120000, 80000, 40000, 1, 0, 0, NULL, NULL)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(DB, 2, true, WalletDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT currency_code, display_name, tour_completed FROM user_settings WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("EUR", cursor.getString(0))
                assertEquals("Ana", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
            }
        }
    }

    @Test
    fun migrate2To3DefaultsToMonthlyPayPeriod() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO user_settings (id, currency_code, display_name, expected_income_cents, spending_limit_cents,
                    savings_target_cents, onboarding_completed, demo_mode, lock_enabled, tour_completed, pin_hash, pin_salt)
                VALUES (1, 'MXN', 'Ana', 120000, 80000, 40000, 1, 0, 0, 1, NULL, NULL)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(DB, 3, true, WalletDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT currency_code, spending_limit_cents, pay_period FROM user_settings WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("MXN", cursor.getString(0))
                assertEquals(80000L, cursor.getLong(1))
                assertEquals("MONTHLY", cursor.getString(2))
            }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
