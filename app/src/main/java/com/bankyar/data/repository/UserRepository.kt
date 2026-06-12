package com.bankyar.data.repository

import com.bankyar.data.database.dao.UserDao
import com.bankyar.data.database.entities.User

class UserRepository(private val dao: UserDao) {
    suspend fun register(user: User): Long = dao.insert(user)
    suspend fun login(phone: String, pin: String): User? = dao.login(phone, pin)
    suspend fun findByPhone(phone: String): User? = dao.findByPhone(phone)
    fun getUserById(id: Int) = dao.getUserById(id)
    suspend fun update(user: User) = dao.update(user)
    suspend fun verifyPin(userId: Int, pin: String): User? = dao.verifyPin(userId, pin)
}
