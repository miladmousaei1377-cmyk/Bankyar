package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.data.repository.TransactionRepository
import com.bankyar.util.JalaliCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

private data class TxFilter(
    val userId: Int, val query: String,
    val category: TransactionCategory?, val yearMonth: String?,
    val accountName: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(AppDatabase.getInstance(app).transactionDao())

    private val _userId = MutableStateFlow(-1)
    private val _searchQuery = MutableStateFlow("")
    private val _filterCategory = MutableStateFlow<TransactionCategory?>(null)
    private val _filterYearMonth = MutableStateFlow<String?>(null)
    private val _filterAccount = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _dateRange = MutableStateFlow<DateRange?>(null)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val filterCategory: StateFlow<TransactionCategory?> = _filterCategory.asStateFlow()
    val filterYearMonth: StateFlow<String?> = _filterYearMonth.asStateFlow()
    val filterAccount: StateFlow<String?> = _filterAccount.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()
    val dateRange: StateFlow<DateRange?> = _dateRange.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(
        combine(_userId, _searchQuery, _filterCategory) { uid, q, cat -> Triple(uid, q, cat) },
        combine(_filterYearMonth, _filterAccount) { month, acc -> Pair(month, acc) }
    ) { (uid, q, cat), (month, acc) -> TxFilter(uid, q, cat, month, acc) }
        .flatMapLatest { f ->
            if (f.userId < 0) flowOf(emptyList())
            else {
                val base = if (f.query.isBlank()) repo.getAll(f.userId) else repo.search(f.userId, f.query)
                base.map { list ->
                    list.filter { t ->
                        (f.category == null || t.category == f.category) &&
                        (f.accountName == null || t.accountName == f.accountName) &&
                        (f.yearMonth == null || run {
                            val jalali = JalaliCalendar.toJalaliShort(t.date)
                            val txMonth = jalali.substring(jalali.indexOf('/') + 1)
                            txMonth == f.yearMonth
                        })
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(emptyList()) else repo.getRecent(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<DashboardStats> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(DashboardStats())
        else combine(
            repo.sumByType(uid, TransactionType.INCOME),
            repo.sumByType(uid, TransactionType.EXPENSE),
            repo.sumByType(uid, TransactionType.TRANSFER)
        ) { inc, exp, tr ->
            val income = inc ?: 0.0; val expense = exp ?: 0.0; val transfer = tr ?: 0.0
            DashboardStats(income - expense - transfer, income, expense)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun setUser(userId: Int) { _userId.value = userId }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilterCategory(cat: TransactionCategory?) { _filterCategory.value = cat }
    fun setFilterYearMonth(month: String?) { _filterYearMonth.value = month }
    fun setFilterAccount(name: String?) { _filterAccount.value = name }

    fun addTransaction(t: Transaction) = viewModelScope.launch {
        repo.insert(t); _message.value = "تراکنش با موفقیت ثبت شد"
    }
    fun updateTransaction(t: Transaction) = viewModelScope.launch {
        repo.update(t); _message.value = "تراکنش با موفقیت ویرایش شد"
    }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch {
        repo.delete(t); _message.value = "تراکنش حذف شد"
    }
    suspend fun getById(id: Int) = repo.getById(id)
    fun deleteAllTransactions() = viewModelScope.launch {
        repo.deleteAll(_userId.value); _message.value = "همه تراکنش‌ها حذف شدند"
    }
    fun clearMessage() { _message.value = null }
}
