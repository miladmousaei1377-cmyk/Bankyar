package com.bankyar.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = PreferencesManager(applicationContext)
            val userId = prefs.loggedInUserId.first()
            if (userId <= 0) return Result.success()

            val db = AppDatabase.getInstance(applicationContext)
            val user = db.userDao().getUserById(userId).first() ?: return Result.success()
            val transactions = db.transactionDao().getAllByUser(userId).first()
            val accounts = db.bankAccountDao().getAllByUser(userId).first()

            val json = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("user", JSONObject().apply {
                    put("id", user.id)
                    put("name", user.name)
                    put("phone", user.phone)
                    put("pin", user.pin)
                    put("createdAt", user.createdAt)
                })
                put("transactions", JSONArray().apply {
                    transactions.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("userId", t.userId)
                            put("title", t.title)
                            put("amount", t.amount)
                            put("type", t.type.name)
                            put("category", t.category.name)
                            put("description", t.description)
                            put("date", t.date)
                            put("accountName", t.accountName)
                        })
                    }
                })
                put("accounts", JSONArray().apply {
                    accounts.forEach { a ->
                        put(JSONObject().apply {
                            put("id", a.id)
                            put("userId", a.userId)
                            put("title", a.title)
                            put("bankName", a.bankName)
                            put("accountNumber", a.accountNumber)
                            put("cardNumber", a.cardNumber)
                            put("isDefault", a.isDefault)
                            put("initialBalance", a.initialBalance)
                        })
                    }
                })
            }

            val backupFile = File(applicationContext.getExternalFilesDir(null), BACKUP_FILE_NAME)
            backupFile.writeText(json.toString(2))
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val BACKUP_FILE_NAME = "bankyar_backup.json"
        const val WORK_NAME = "bankyar_auto_backup"
    }
}
