package com.bankyar.data.repository

import com.bankyar.data.database.dao.TransactionDao
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType

class TransactionRepository(private val dao: TransactionDao) {
    suspend fun insert(t: Transaction) = dao.insert(t)
    suspend fun update(t: Transaction) = dao.update(t)
    suspend fun delete(t: Transaction) = dao.delete(t)
    fun getAll(userId: Int) = dao.getAllByUser(userId)
    fun getRecent(userId: Int) = dao.getRecent(userId)
    fun search(userId: Int, q: String) = dao.search(userId, q)
    fun sumByType(userId: Int, type: TransactionType) = dao.sumByType(userId, type)
    suspend fun getById(id: Int) = dao.getById(id)
    suspend fun deleteAll(userId: Int) = dao.deleteAllByUser(userId)
}
