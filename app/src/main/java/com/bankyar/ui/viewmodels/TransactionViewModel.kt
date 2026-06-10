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

data class DateRange(val from: Long, val to: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(AppDatabase.getInstance(app).transactionDao())

    private val _userId = MutableStateFlow(-1)
    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)
    private val _dateRange = MutableStateFlow<DateRange?>(null)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()
    val dateRange: StateFlow<DateRange?> = _dateRange.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(
        _userId, _searchQuery, _dateRange
    ) { uid, q, dr -> Triple(uid, q, dr) }
        .flatMapLatest { (uid, q, dr) ->
            if (uid < 0) flowOf(emptyList())
            else when {
                dr != null && q.isBlank() -> repo.getByDateRange(uid, dr.from, dr.to)
                dr != null -> repo.getByDateRange(uid, dr.from, dr.to)
                    .map { list -> list.filter { it.title.contains(q, true) || it.description.contains(q, true) } }
                q.isBlank() -> repo.getAll(uid)
                else -> repo.search(uid, q)
            }
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
    fun setDateRange(range: DateRange?) { _dateRange.value = range }

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
    fun clearMessage() { _message.value = null }
}
