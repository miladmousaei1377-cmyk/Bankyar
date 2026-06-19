package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.data.repository.BankAccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BankAccountRepository(AppDatabase.getInstance(app).bankAccountDao())
    private val txDao = AppDatabase.getInstance(app).transactionDao()
    private val _userId = MutableStateFlow(-1)
    private val _message = MutableStateFlow<String?>(null)

    val message: StateFlow<String?> = _message.asStateFlow()

    val accounts: StateFlow<List<BankAccount>> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(emptyList()) else repo.getAllByUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUser(userId: Int) { _userId.value = userId }

    fun getNetBalance(userId: Int, accountName: String): Flow<Double> = combine(
        txDao.sumByTypeAndAccount(userId, accountName, TransactionType.INCOME),
        txDao.sumByTypeAndAccount(userId, accountName, TransactionType.EXPENSE),
        txDao.sumByTypeAndAccount(userId, accountName, TransactionType.TRANSFER)
    ) { inc, exp, tr -> (inc ?: 0.0) - (exp ?: 0.0) - (tr ?: 0.0) }

    fun addAccount(account: BankAccount) = viewModelScope.launch {
        repo.insert(account); _message.value = "حساب با موفقیت اضافه شد"
    }
    fun updateAccount(account: BankAccount) = viewModelScope.launch {
        repo.update(account); _message.value = "حساب با موفقیت ویرایش شد"
    }
    fun deleteAccount(account: BankAccount) = viewModelScope.launch {
        repo.delete(account); _message.value = "حساب حذف شد"
    }
    fun clearMessage() { _message.value = null }
}
