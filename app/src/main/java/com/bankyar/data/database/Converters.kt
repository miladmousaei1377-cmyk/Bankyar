package com.bankyar.data.database

import androidx.room.TypeConverter
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType

class Converters {
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)
    @TypeConverter fun fromTransactionCategory(v: TransactionCategory): String = v.name
    @TypeConverter fun toTransactionCategory(v: String): TransactionCategory = TransactionCategory.valueOf(v)
}
