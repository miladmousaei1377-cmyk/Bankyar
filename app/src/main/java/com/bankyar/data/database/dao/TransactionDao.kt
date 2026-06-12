package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllByUser(userId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun getByDateRange(userId: Int, from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%') ORDER BY date DESC")
    fun search(userId: Int, q: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type")
    fun sumByType(userId: Int, type: TransactionType): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecent(userId: Int, limit: Int = 5): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllByUserSync(userId: Int): List<Transaction>
}
