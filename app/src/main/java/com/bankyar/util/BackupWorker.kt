package com.bankyar.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = PreferencesManager(applicationContext)
            val userId = prefs.loggedInUserId.first()
            if (userId <= 0) return Result.success()
            val json = buildBackupJson(applicationContext, userId)
            writeBackupFile(applicationContext, json)
            prefs.setLastAutoBackupTime(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val AUTO_BACKUP_FILE_NAME = "auto_backup.json"
        const val BACKUP_FOLDER = "Bankyar"
        const val BACKUP_DIR = "Bankyar"
        const val WORK_NAME = "bankyar_auto_backup"

        fun writeBackupFile(context: Context, json: String) {
            val written = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { writeViaMediaStore(context, json) }.isSuccess
            } else {
                runCatching { writeViaLegacyFile(json) }.isSuccess
            }
            // Always write to app-specific storage as reliable fallback
            if (!written) {
                writeViaAppFiles(context, json)
            }
            // Also keep an app-specific copy as reliable fallback regardless
            runCatching { writeViaAppFiles(context, json) }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun writeViaMediaStore(context: Context, json: String) {
            val resolver = context.contentResolver
            val relPath = "Download/$BACKUP_FOLDER/"
            resolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(relPath, AUTO_BACKUP_FILE_NAME)
            )
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, AUTO_BACKUP_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, relPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                ?: throw IOException("MediaStore insert returned null")
            try {
                resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: throw IOException("openOutputStream returned null for $uri")
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        private fun writeViaLegacyFile(json: String) {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_FOLDER
            )
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Cannot create dir: $dir")
            File(dir, AUTO_BACKUP_FILE_NAME).writeText(json)
        }

        private fun writeViaAppFiles(context: Context, json: String) {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, BACKUP_FOLDER)
            dir.mkdirs()
            File(dir, AUTO_BACKUP_FILE_NAME).writeText(json)
        }

        // Legacy method kept for SettingsScreen compatibility (Android < 29 only)
        fun getAutoBackupFile(context: Context): File? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_FOLDER
            )
            return File(dir, AUTO_BACKUP_FILE_NAME)
        }

        suspend fun buildBackupJson(context: Context, userId: Int): String {
            val db = AppDatabase.getInstance(context)
            val user = db.userDao().getUserById(userId).first() ?: return "{}"
            val transactions = db.transactionDao().getAllByUser(userId).first()
            val accounts = db.bankAccountDao().getAllByUser(userId).first()
            val budgets = db.budgetDao().getAllByUser(userId).first()
            val debts = db.debtDao().getAllByUser(userId).first()
            val recurring = db.recurringTransactionDao().getAllByUser(userId).first()

            return JSONObject().apply {
                put("version", 2)
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
                            put("title", a.title)
                            put("bankName", a.bankName)
                            put("accountNumber", a.accountNumber)
                            put("cardNumber", a.cardNumber)
                            put("isDefault", a.isDefault)
                            put("initialBalance", a.initialBalance)
                        })
                    }
                })
                put("budgets", JSONArray().apply {
                    budgets.forEach { b ->
                        put(JSONObject().apply {
                            put("categoryName", b.categoryName)
                            put("maxAmount", b.maxAmount)
                            put("yearMonth", b.yearMonth)
                            if (b.accountName != null) put("accountName", b.accountName)
                        })
                    }
                })
                put("debts", JSONArray().apply {
                    debts.forEach { d ->
                        put(JSONObject().apply {
                            put("personName", d.personName)
                            put("amount", d.amount)
                            put("isIOwe", d.isIOwe)
                            put("description", d.description)
                            put("date", d.date)
                            put("isPaid", d.isPaid)
                        })
                    }
                })
                put("recurring", JSONArray().apply {
                    recurring.forEach { r ->
                        put(JSONObject().apply {
                            put("title", r.title)
                            put("amount", r.amount)
                            put("type", r.type.name)
                            put("category", r.category.name)
                            put("accountName", r.accountName)
                            put("description", r.description)
                            put("periodDays", r.periodDays)
                            put("nextDate", r.nextDate)
                        })
                    }
                })
            }.toString(2)
        }
    }
}
