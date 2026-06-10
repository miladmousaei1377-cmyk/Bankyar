package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year ORDER BY category ASC")
    fun getByMonth(userId: Int, month: Int, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND category = :category AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetByCategory(userId: Int, category: TransactionCategory, month: Int, year: Int): Budget?

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Budget?
}
