package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.RecurringTransaction
import com.bankyar.data.database.entities.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val _userId = MutableStateFlow(-1)

    val items: StateFlow<List<RecurringTransaction>> = _userId.flatMapLatest { uid ->
        if (uid < 0) flowOf(emptyList()) else db.recurringTransactionDao().getAllByUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUser(userId: Int) { _userId.value = userId }

    fun add(rt: RecurringTransaction) = viewModelScope.launch { db.recurringTransactionDao().insert(rt) }
    fun update(rt: RecurringTransaction) = viewModelScope.launch { db.recurringTransactionDao().update(rt) }
    fun delete(rt: RecurringTransaction) = viewModelScope.launch { db.recurringTransactionDao().delete(rt) }

    fun processDue(userId: Int) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val due = db.recurringTransactionDao().getDue(userId, now)
        due.forEach { rt ->
            db.transactionDao().insert(Transaction(
                userId = rt.userId,
                title = rt.title,
                amount = rt.amount,
                type = rt.type,
                category = rt.category,
                accountName = rt.accountName,
                description = rt.description,
                date = System.currentTimeMillis()
            ))
            val next = rt.nextDate + rt.periodDays.toLong() * 24 * 60 * 60 * 1000
            db.recurringTransactionDao().update(rt.copy(nextDate = next))
        }
    }
}
