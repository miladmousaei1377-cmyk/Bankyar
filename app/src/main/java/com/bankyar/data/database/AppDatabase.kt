package com.bankyar.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bankyar.data.database.dao.BankAccountDao
import com.bankyar.data.database.dao.BudgetDao
import com.bankyar.data.database.dao.TransactionDao
import com.bankyar.data.database.dao.UserDao
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.User

@Database(
    entities = [User::class, Transaction::class, BankAccount::class, Budget::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bank_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        bankName TEXT NOT NULL DEFAULT '',
                        accountNumber TEXT NOT NULL DEFAULT '',
                        cardNumber TEXT NOT NULL DEFAULT '',
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        limitAmount REAL NOT NULL,
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "bankyar.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
