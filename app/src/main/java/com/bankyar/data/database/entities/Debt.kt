package com.bankyar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val personName: String,
    val amount: Double,
    val isIOwe: Boolean,      // true = I owe them, false = they owe me
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false
)
