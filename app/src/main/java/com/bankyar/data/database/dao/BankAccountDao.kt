package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.BankAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: BankAccount): Long

    @Update
    suspend fun update(account: BankAccount)

    @Delete
    suspend fun delete(account: BankAccount)

    @Query("SELECT * FROM bank_accounts WHERE userId = :userId ORDER BY isDefault DESC, title ASC")
    fun getAllByUser(userId: Int): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): BankAccount?

    @Query("UPDATE bank_accounts SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefault(userId: Int)

    @Query("SELECT * FROM bank_accounts WHERE userId = :userId AND cardNumber LIKE :pattern LIMIT 1")
    suspend fun findByCardLastFour(userId: Int, pattern: String): BankAccount?
}
