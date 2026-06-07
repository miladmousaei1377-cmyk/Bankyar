package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(AppDatabase.getInstance(app).transactionDao())

    private val _userId = MutableStateFlow(-1)
    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(_userId, _searchQuery) { uid, q ->
        uid to q
    }.flatMapLatest { (uid, q) ->
        if (uid < 0) flowOf(emptyList())
        else if (q.isBlank()) repo.getAll(uid)
        else repo.search(uid, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(emptyList()) else repo.getRecent(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<DashboardStats> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(DashboardStats())
        else combine(
            repo.sumByType(uid, TransactionType.INCOME),
            repo.sumByType(uid, TransactionType.EXPENSE)
        ) { inc, exp ->
            val income = inc ?: 0.0
            val expense = exp ?: 0.0
            DashboardStats(income - expense, income, expense)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun setUser(userId: Int) { _userId.value = userId }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun addTransaction(t: Transaction) {
        viewModelScope.launch {
            repo.insert(t)
            _message.value = "تراکنش با موفقیت ثبت شد"
        }
    }

    fun updateTransaction(t: Transaction) {
        viewModelScope.launch {
            repo.update(t)
            _message.value = "تراکنش با موفقیت ویرایش شد"
        }
    }

    fun deleteTransaction(t: Transaction) {
        viewModelScope.launch {
            repo.delete(t)
            _message.value = "تراکنش حذف شد"
        }
    }

    suspend fun getById(id: Int) = repo.getById(id)

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repo.deleteAll(_userId.value)
            _message.value = "همه تراکنش‌ها حذف شدند"
        }
    }

    fun clearMessage() { _message.value = null }
}
