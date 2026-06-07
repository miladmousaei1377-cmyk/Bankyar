package com.bankyar.data.database.dao

import androidx.room.*
import com.bankyar.data.database.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE phone = :phone AND pin = :pin LIMIT 1")
    suspend fun login(phone: String, pin: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): User?

    @Update
    suspend fun update(user: User)
}
