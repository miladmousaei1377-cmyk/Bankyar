package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.data.repository.BudgetRepository
import com.bankyar.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class BudgetItem(
    val budget: Budget,
    val spent: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val budgetRepo = BudgetRepository(AppDatabase.getInstance(app).budgetDao())
    private val txRepo = TransactionRepository(AppDatabase.getInstance(app).transactionDao())

    private val _userId = MutableStateFlow(-1)
    private val _message = MutableStateFlow<String?>(null)

    private val now = Calendar.getInstance()
    private val _month = MutableStateFlow(now.get(Calendar.MONTH) + 1)
    private val _year = MutableStateFlow(now.get(Calendar.YEAR))

    val month: StateFlow<Int> = _month.asStateFlow()
    val year: StateFlow<Int> = _year.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()

    val budgetItems: StateFlow<List<BudgetItem>> = combine(_userId, _month, _year) { uid, m, y ->
        Triple(uid, m, y)
    }.flatMapLatest { (uid, m, y) ->
        if (uid < 0) return@flatMapLatest flowOf(emptyList())
        val (from, to) = monthRange(y, m)
        combine(
            budgetRepo.getByMonth(uid, m, y),
            txRepo.getAll(uid)
        ) { budgets, allTx ->
            val expensesThisMonth = allTx.filter {
                it.type == TransactionType.EXPENSE && it.date in from..to
            }
            budgets.map { budget ->
                val spent = expensesThisMonth.filter { it.category == budget.category }.sumOf { it.amount }
                BudgetItem(budget, spent)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUser(userId: Int) { _userId.value = userId }

    fun previousMonth() {
        if (_month.value == 1) { _month.value = 12; _year.value-- }
        else _month.value--
    }

    fun nextMonth() {
        if (_month.value == 12) { _month.value = 1; _year.value++ }
        else _month.value++
    }

    fun saveBudget(userId: Int, category: TransactionCategory, amount: Double, budgetId: Int = 0) {
        if (amount <= 0) { _message.value = "مبلغ باید بیشتر از صفر باشد"; return }
        viewModelScope.launch {
            val budget = Budget(
                id = budgetId, userId = userId, category = category,
                limitAmount = amount, month = _month.value, year = _year.value
            )
            if (budgetId == 0) budgetRepo.insert(budget) else budgetRepo.update(budget)
            _message.value = "بودجه با موفقیت ذخیره شد"
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepo.delete(budget)
            _message.value = "بودجه حذف شد"
        }
    }

    fun clearMessage() { _message.value = null }

    private fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val to = cal.timeInMillis - 1
        return from to to
    }
}
