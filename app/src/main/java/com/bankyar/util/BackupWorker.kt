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
            val json = buildBackupJson(applicationContext, userId)
            val file = getAutoBackupFile(applicationContext)
            file.writeText(json)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val AUTO_BACKUP_FILE_NAME = "auto_backup.json"
        const val BACKUP_DIR = "bankyar"
        const val WORK_NAME = "bankyar_auto_backup"

        fun getAutoBackupFile(context: Context): File {
            val dir = File(context.getExternalFilesDir(null), BACKUP_DIR)
            dir.mkdirs()
            return File(dir, AUTO_BACKUP_FILE_NAME)
        }

        suspend fun buildBackupJson(context: Context, userId: Int): String {
            val db = AppDatabase.getInstance(context)
            val user = db.userDao().getUserById(userId).first() ?: return "{}"
            val transactions = db.transactionDao().getAllByUser(userId).first()
            val accounts = db.bankAccountDao().getAllByUser(userId).first()

            return JSONObject().apply {
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
            }.toString(2)
        }
    }
}
