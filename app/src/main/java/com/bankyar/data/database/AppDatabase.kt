package com.bankyar.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bankyar.data.database.dao.*
import com.bankyar.data.database.entities.*

@Database(
    entities = [User::class, Transaction::class, BankAccount::class,
                Budget::class, Debt::class, RecurringTransaction::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS bank_accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL, title TEXT NOT NULL,
                    bankName TEXT NOT NULL DEFAULT '', accountNumber TEXT NOT NULL DEFAULT '',
                    cardNumber TEXT NOT NULL DEFAULT '', isDefault INTEGER NOT NULL DEFAULT 0)""")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN initialBalance REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL, categoryName TEXT NOT NULL,
                    maxAmount REAL NOT NULL, yearMonth TEXT NOT NULL)""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS debts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL, personName TEXT NOT NULL,
                    amount REAL NOT NULL, isIOwe INTEGER NOT NULL,
                    description TEXT NOT NULL DEFAULT '', date INTEGER NOT NULL,
                    isPaid INTEGER NOT NULL DEFAULT 0)""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS recurring_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL, title TEXT NOT NULL,
                    amount REAL NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL,
                    accountName TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
                    periodDays INTEGER NOT NULL, nextDate INTEGER NOT NULL)""")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "bankyar.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
    }
}
