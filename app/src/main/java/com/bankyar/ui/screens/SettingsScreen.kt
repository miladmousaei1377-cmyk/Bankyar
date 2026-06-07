package com.bankyar.ui.screens

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
import androidx.fragment.app.FragmentActivity
import androidx.work.*
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.util.BackupWorker
import com.bankyar.util.BiometricHelper
import com.bankyar.util.JalaliCalendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: Int,
    biometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var autoBackupEnabled by remember { mutableStateOf(false) }
    var lastAutoBackupTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val backupFile = BackupWorker.getAutoBackupFile(context)
        if (backupFile.exists()) {
            lastAutoBackupTime = JalaliCalendar.toJalaliString(backupFile.lastModified())
        }
        val workInfo = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupWorker.WORK_NAME).get()
        autoBackupEnabled = workInfo.isNotEmpty() && workInfo.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }

    val manualBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = BackupWorker.buildBackupJson(context, userId)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                snackbarHostState.showSnackbar("بکاپ با موفقیت ذخیره شد")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("خطا در بکاپ: ${e.message}")
            }
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
            // Manual backup card
            SettingsCard(title = "پشتیبان‌گیری دستی") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { manualBackupLauncher.launch("bankyar_backup.json") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Backup, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("پشتیبان‌گیری اکنون", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "شما مکان ذخیره‌سازی را انتخاب می‌کنید",
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

            // Biometric card
            val biometricAvailable = remember { BiometricHelper.canAuthenticate(context) }
            val activity = context as? FragmentActivity
            var biometricPending by remember { mutableStateOf(false) }

            SettingsCard(title = "امنیت") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Fingerprint, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "ورود با اثر انگشت",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (!biometricAvailable) "دستگاه پشتیبانی نمی‌کند"
                                    else if (biometricEnabled) "فعال است"
                                    else "غیرفعال است",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            enabled = biometricAvailable && !biometricPending,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && activity != null) {
                                    biometricPending = true
                                    BiometricHelper.showPrompt(
                                        activity = activity,
                                        title = "تأیید اثر انگشت",
                                        subtitle = "برای فعال‌سازی اثر انگشت خود را اسکن کنید",
                                        negativeText = "انصراف",
                                        onSuccess = {
                                            onBiometricToggle(true)
                                            biometricPending = false
                                        },
                                        onError = { biometricPending = false }
                                    )
                                } else {
                                    onBiometricToggle(false)
                                }
                            }
                        )
                    }
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
                            Text("پوشه bankyar در حافظه داخلی",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                autoBackupEnabled = enabled
                                if (enabled) {
                                    scheduleAutoBackup(context)
                                    scope.launch { snackbarHostState.showSnackbar("پشتیبان‌گیری خودکار فعال شد") }
                                } else {
                                    cancelAutoBackup(context)
                                    scope.launch { snackbarHostState.showSnackbar("پشتیبان‌گیری خودکار غیرفعال شد") }
                                }
                            }
                        )
                    }
                    if (lastAutoBackupTime.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("آخرین بکاپ خودکار: $lastAutoBackupTime",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                    }
                    Text(
                        "مسیر: Android/data/com.bankyar/files/bankyar/auto_backup.json",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
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

private suspend fun restoreFromJson(context: android.content.Context, userId: Int, content: String) {
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

private fun scheduleAutoBackup(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.MINUTES)
        .setConstraints(Constraints.NONE)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        BackupWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private fun cancelAutoBackup(context: android.content.Context) {
    WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.WORK_NAME)
}
