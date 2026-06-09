package com.bankyar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryName: String, // TransactionCategory.name
    val maxAmount: Double,
    val yearMonth: String    // "mm/yyyy" e.g. "01/1403"
)
