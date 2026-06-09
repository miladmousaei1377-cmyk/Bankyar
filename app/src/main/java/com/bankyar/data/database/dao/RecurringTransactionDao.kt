package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rt: RecurringTransaction)

    @Update
    suspend fun update(rt: RecurringTransaction)

    @Delete
    suspend fun delete(rt: RecurringTransaction)

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextDate ASC")
    fun getAllByUser(userId: Int): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND nextDate <= :now")
    suspend fun getDue(userId: Int, now: Long): List<RecurringTransaction>
}
