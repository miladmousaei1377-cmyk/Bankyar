package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND yearMonth = :yearMonth")
    fun getByUserAndMonth(userId: Int, yearMonth: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getAllByUser(userId: Int): Flow<List<Budget>>
}
