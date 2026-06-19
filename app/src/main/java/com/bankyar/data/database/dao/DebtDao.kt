package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: Debt): Long

    @Update
    suspend fun update(debt: Debt)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY isPaid ASC, date DESC")
    fun getAllByUser(userId: Int): Flow<List<Debt>>
}
