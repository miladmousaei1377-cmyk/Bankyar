package com.bankyar.data

import android.content.Context
import android.os.Environment
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.util.JalaliCalendar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    suspend fun performBackup(userId: Int): Boolean {
        return try {
            val db = AppDatabase.getInstance(context)
            val transactions = db.transactionDao().getAllByUserSync(userId)
            if (transactions.isEmpty()) return true

            val backupDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "BankYar_Backups"
            )
            if (!backupDir.exists()) backupDir.mkdirs()

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "bankyar_backup_$dateStr.csv")
            backupFile.writeText(buildCsv(transactions), Charsets.UTF_8)

            pruneOldBackups(backupDir)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun pruneOldBackups(dir: File) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size > 5) {
            files.take(files.size - 5).forEach { it.delete() }
        }
    }

    fun getBackupList(): List<File> {
        val backupDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "BankYar_Backups"
        )
        return backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun buildCsv(list: List<Transaction>): String {
        val sb = StringBuilder()
        sb.appendLine("تاریخ,عنوان,مبلغ,نوع,دسته‌بندی,حساب,توضیحات")
        list.forEach { t ->
            val type = when (t.type) {
                TransactionType.INCOME -> "درآمد"
                TransactionType.EXPENSE -> "هزینه"
                TransactionType.TRANSFER -> "انتقال"
            }
            val date = JalaliCalendar.toJalaliShort(t.date)
            sb.appendLine("$date,\"${t.title}\",${t.amount},$type,${t.category.label},\"${t.accountName}\",\"${t.description}\"")
        }
        return sb.toString()
    }
}
