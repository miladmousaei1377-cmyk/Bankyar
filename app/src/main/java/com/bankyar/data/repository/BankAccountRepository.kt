package com.bankyar.data.repository

import com.bankyar.data.database.dao.BankAccountDao
import com.bankyar.data.database.entities.BankAccount

class BankAccountRepository(private val dao: BankAccountDao) {
    suspend fun insert(a: BankAccount) = dao.insert(a)
    suspend fun update(a: BankAccount) = dao.update(a)
    suspend fun delete(a: BankAccount) = dao.delete(a)
    fun getAllByUser(userId: Int) = dao.getAllByUser(userId)
    suspend fun getById(id: Int) = dao.getById(id)
    suspend fun clearDefault(userId: Int) = dao.clearDefault(userId)
    suspend fun findByCardLastFour(userId: Int, lastFour: String): BankAccount? =
        dao.findByCardLastFour(userId, "%$lastFour")
}
