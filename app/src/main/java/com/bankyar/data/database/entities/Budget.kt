package com.bankyar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val category: TransactionCategory,
    val limitAmount: Double,
    val month: Int,
    val year: Int
)
