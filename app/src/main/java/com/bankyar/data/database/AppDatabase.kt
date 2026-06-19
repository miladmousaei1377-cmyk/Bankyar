package com.bankyar.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bankyar.data.database.dao.BankAccountDao
import com.bankyar.data.database.dao.BudgetDao
import com.bankyar.data.database.dao.DebtDao
import com.bankyar.data.database.dao.RecurringTransactionDao
import com.bankyar.data.database.dao.TransactionDao
import com.bankyar.data.database.dao.UserDao
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.Debt
import com.bankyar.data.database.entities.RecurringTransaction
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.User

@Database(
    entities = [User::class, Transaction::class, BankAccount::class,
                Budget::class, Debt::class, RecurringTransaction::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN cardColor TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN accountName TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "bankyar.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { INSTANCE = it }
            }
    }
}
