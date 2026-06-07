package com.bankyar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val bankName: String = "",
    val accountNumber: String = "",
    val cardNumber: String = "",
    val isDefault: Boolean = false,
    val initialBalance: Double = 0.0
)
