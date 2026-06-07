package com.bankyar.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.util.BackupWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(userId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var autoBackupEnabled by remember { mutableStateOf(false) }
    var lastBackupTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val backupFile = File(context.getExternalFilesDir(null), BackupWorker.BACKUP_FILE_NAME)
        if (backupFile.exists()) {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            lastBackupTime = sdf.format(Date(backupFile.lastModified()))
        }
        val workInfo = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupWorker.WORK_NAME).get()
        autoBackupEnabled = workInfo.isNotEmpty() && workInfo.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: return@launch
                restoreFromJson(context, userId, content)
                snackbarHostState.showSnackbar("اطلاعات با موفقیت بازیابی شد")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("خطا در بازیابی: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup card
            SettingsCard(title = "پشتیبان‌گیری") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (lastBackupTime.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("آخرین بکاپ: $lastBackupTime",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    performBackup(context, userId)
                                    val backupFile = File(context.getExternalFilesDir(null), BackupWorker.BACKUP_FILE_NAME)
                                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                    lastBackupTime = sdf.format(Date(backupFile.lastModified()))
                                    snackbarHostState.showSnackbar("بکاپ با موفقیت ذخیره شد")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("خطا در بکاپ: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Backup, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("پشتیبان‌گیری اکنون", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "فایل بکاپ در حافظه داخلی گوشی ذخیره می‌شود",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Restore card
            SettingsCard(title = "بازیابی اطلاعات") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("انتخاب فایل بکاپ", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "فایل JSON بکاپ را از حافظه گوشی انتخاب کنید",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Auto backup card
            SettingsCard(title = "پشتیبان‌گیری خودکار") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("پشتیبان‌گیری هر ۳۰ دقیقه",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("فایل قبلی جایگزین می‌شود",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                autoBackupEnabled = enabled
                                if (enabled) {
                                    scheduleAutoBackup(context)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("پشتیبان‌گیری خودکار فعال شد")
                                    }
                                } else {
                                    cancelAutoBackup(context)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("پشتیبان‌گیری خودکار غیرفعال شد")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            content()
        }
    }
}

private suspend fun performBackup(context: Context, userId: Int) {
    val db = AppDatabase.getInstance(context)
    val user = db.userDao().getUserById(userId).first() ?: return
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
        put("transactions", org.json.JSONArray().apply {
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
        put("accounts", org.json.JSONArray().apply {
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

    val backupFile = File(context.getExternalFilesDir(null), BackupWorker.BACKUP_FILE_NAME)
    backupFile.writeText(json.toString(2))
}

private suspend fun restoreFromJson(context: Context, userId: Int, content: String) {
    val root = JSONObject(content)
    val db = AppDatabase.getInstance(context)

    db.transactionDao().deleteAllByUser(userId)

    val txArray = root.getJSONArray("transactions")
    for (i in 0 until txArray.length()) {
        val obj = txArray.getJSONObject(i)
        db.transactionDao().insert(
            Transaction(
                id = 0,
                userId = userId,
                title = obj.getString("title"),
                amount = obj.getDouble("amount"),
                type = TransactionType.valueOf(obj.getString("type")),
                category = TransactionCategory.valueOf(obj.getString("category")),
                description = obj.optString("description", ""),
                date = obj.getLong("date"),
                accountName = obj.optString("accountName", "حساب اصلی")
            )
        )
    }

    val accArray = root.getJSONArray("accounts")
    for (i in 0 until accArray.length()) {
        val obj = accArray.getJSONObject(i)
        db.bankAccountDao().insert(
            BankAccount(
                id = 0,
                userId = userId,
                title = obj.getString("title"),
                bankName = obj.optString("bankName", ""),
                accountNumber = obj.optString("accountNumber", ""),
                cardNumber = obj.optString("cardNumber", ""),
                isDefault = obj.optBoolean("isDefault", false),
                initialBalance = obj.optDouble("initialBalance", 0.0)
            )
        )
    }
}

private fun scheduleAutoBackup(context: Context) {
    val request = PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.MINUTES)
        .setConstraints(Constraints.NONE)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        BackupWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private fun cancelAutoBackup(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.WORK_NAME)
}
