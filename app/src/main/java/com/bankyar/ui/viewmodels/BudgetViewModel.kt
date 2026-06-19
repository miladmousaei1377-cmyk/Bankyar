package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.util.JalaliCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val _userId = MutableStateFlow(-1)

    fun setUser(userId: Int) { _userId.value = userId }

    fun currentYearMonth(): String {
        val jalali = JalaliCalendar.toJalaliShort(System.currentTimeMillis())
        return jalali.substring(jalali.indexOf('/') + 1)
    }

    fun getBudgetsForMonth(yearMonth: String): Flow<List<Budget>> =
        _userId.flatMapLatest { uid ->
            if (uid < 0) flowOf(emptyList()) else db.budgetDao().getByUserAndMonth(uid, yearMonth)
        }

    fun getSpentForCategoryAndAccount(category: TransactionCategory, yearMonth: String, accountName: String?): Flow<Double> =
        _userId.flatMapLatest { uid ->
            if (uid < 0) flowOf(0.0)
            else db.transactionDao().getAllByUser(uid).map { txs ->
                txs.filter { t ->
                    t.category == category && t.type == TransactionType.EXPENSE &&
                    (accountName == null || t.accountName == accountName) &&
                    run {
                        val jalali = JalaliCalendar.toJalaliShort(t.date)
                        val txMonth = jalali.substring(jalali.indexOf('/') + 1)
                        txMonth == yearMonth
                    }
                }.sumOf { it.amount }
            }
        }

    // Direct suspend methods — used in AddTransactionScreen for reliable one-shot reads
    suspend fun getBudgetsForMonthDirect(yearMonth: String): List<Budget> {
        val uid = _userId.value
        if (uid < 0) return emptyList()
        return db.budgetDao().getByUserAndMonthSync(uid, yearMonth)
    }

    suspend fun getSpentForCategoryDirect(
        category: TransactionCategory,
        yearMonth: String,
        accountName: String?
    ): Double {
        val uid = _userId.value
        if (uid < 0) return 0.0
        return db.transactionDao().getAllByUserSync(uid).filter { t ->
            t.category == category && t.type == TransactionType.EXPENSE &&
            (accountName == null || t.accountName == accountName) &&
            run {
                val jalali = JalaliCalendar.toJalaliShort(t.date)
                val txMonth = jalali.substring(jalali.indexOf('/') + 1)
                txMonth == yearMonth
            }
        }.sumOf { it.amount }
    }

    fun saveBudget(userId: Int, categoryName: String, maxAmount: Double, yearMonth: String, accountName: String? = null) =
        viewModelScope.launch {
            db.budgetDao().insert(Budget(
                userId = userId,
                categoryName = categoryName,
                maxAmount = maxAmount,
                yearMonth = yearMonth,
                accountName = accountName
            ))
        }

    fun updateBudget(budget: Budget) = viewModelScope.launch { db.budgetDao().update(budget) }

    fun deleteBudget(budget: Budget) = viewModelScope.launch { db.budgetDao().delete(budget) }
}
