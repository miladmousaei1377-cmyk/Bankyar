package com.bankyar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val accountName: String,
    val description: String = "",
    val periodDays: Int,       // 1=daily, 7=weekly, 30=monthly
    val nextDate: Long
)
