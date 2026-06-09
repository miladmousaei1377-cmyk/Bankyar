package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Debt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DebtViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).debtDao()
    private val _userId = MutableStateFlow(-1)

    val debts: StateFlow<List<Debt>> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(emptyList()) else dao.getAllByUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUser(userId: Int) { _userId.value = userId }

    fun addDebt(debt: Debt) = viewModelScope.launch { dao.insert(debt) }
    fun togglePaid(debt: Debt) = viewModelScope.launch { dao.update(debt.copy(isPaid = !debt.isPaid)) }
    fun deleteDebt(debt: Debt) = viewModelScope.launch { dao.delete(debt) }
}
