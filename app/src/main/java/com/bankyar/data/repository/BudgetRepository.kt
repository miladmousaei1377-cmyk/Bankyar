package com.bankyar.data.repository

import com.bankyar.data.database.dao.BudgetDao
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.TransactionCategory

class BudgetRepository(private val dao: BudgetDao) {
    fun getByMonth(userId: Int, month: Int, year: Int) = dao.getByMonth(userId, month, year)
    suspend fun insert(budget: Budget) = dao.insert(budget)
    suspend fun update(budget: Budget) = dao.update(budget)
    suspend fun delete(budget: Budget) = dao.delete(budget)
    suspend fun getById(id: Int) = dao.getById(id)
    suspend fun getBudgetByCategory(userId: Int, category: TransactionCategory, month: Int, year: Int) =
        dao.getBudgetByCategory(userId, category, month, year)
}
