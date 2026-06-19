package com.bankyar.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.work.*
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.Debt
import com.bankyar.data.database.entities.RecurringTransaction
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.util.BackupWorker
import com.bankyar.util.BiometricHelper
import com.bankyar.util.JalaliCalendar
import com.bankyar.util.ReminderWorker
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
    val prefs = remember { PreferencesManager(context) }

    var autoBackupEnabled by remember { mutableStateOf(false) }
    var lastAutoBackupTime by remember { mutableStateOf("") }
    val smsAutoRegisterEnabled by prefs.smsAutoRegisterEnabled.collectAsState(initial = false)

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        scope.launch {
            if (granted) {
                prefs.setSmsAutoRegisterEnabled(true)
                snackbarHostState.showSnackbar("ثبت خودکار پیامک فعال شد")
            } else {
                snackbarHostState.showSnackbar("دسترسی به پیامک داده نشد")
            }
        }
    }

    val reminderEnabled by prefs.reminderEnabled.collectAsState(initial = false)
    val reminderHour by prefs.reminderHour.collectAsState(initial = 20)
    val reminderMinute by prefs.reminderMinute.collectAsState(initial = 0)
    var showTimePicker by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                prefs.setReminderEnabled(true)
                ReminderWorker.schedule(context, reminderHour, reminderMinute)
                snackbarHostState.showSnackbar("یادآور فعال شد")
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("دسترسی به اعلان‌ها داده نشد") }
        }
    }

    val storagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            autoBackupEnabled = true
            scheduleAutoBackup(context)
            scope.launch {
                try {
                    val json = BackupWorker.buildBackupJson(context, userId)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        BackupWorker.writeBackupFile(context, json)
                    }
                    prefs.setLastAutoBackupTime(System.currentTimeMillis())
                    lastAutoBackupTime = JalaliCalendar.toJalaliString(System.currentTimeMillis())
                    snackbarHostState.showSnackbar("پشتیبان‌گیری خودکار فعال شد — نسخه در Downloads/Bankyar ذخیره شد")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("پشتیبان‌گیری فعال شد")
                }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("دسترسی به حافظه داده نشد") }
        }
    }

    fun requestEnableReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch {
                prefs.setReminderEnabled(true)
                ReminderWorker.schedule(context, reminderHour, reminderMinute)
                snackbarHostState.showSnackbar("یادآور فعال شد")
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { h, m ->
                showTimePicker = false
                scope.launch {
                    prefs.setReminderTime(h, m)
                    if (reminderEnabled) ReminderWorker.schedule(context, h, m)
                    snackbarHostState.showSnackbar("زمان یادآور به ${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')} تغییر کرد")
                }
            },
            onDismiss = { showTimePicker = false }
        )
    }

    LaunchedEffect(Unit) {
        val ts = prefs.lastAutoBackupTime.first()
        if (ts > 0L) lastAutoBackupTime = JalaliCalendar.toJalaliString(ts)
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

            // Reminder card
            SettingsCard(title = "یادآور ثبت تراکنش") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("یادآور روزانه", fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("هر روز در ساعت مشخص یادآوری ثبت تراکنش",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) requestEnableReminder()
                                else scope.launch {
                                    prefs.setReminderEnabled(false)
                                    ReminderWorker.cancel(context)
                                    snackbarHostState.showSnackbar("یادآور غیرفعال شد")
                                }
                            }
                        )
                    }
                    if (reminderEnabled) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.3f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ساعت یادآور", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text(
                                    "${reminderHour.toString().padStart(2,'0')}:${reminderMinute.toString().padStart(2,'0')}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary, fontSize = 22.sp
                                )
                            }
                            TextButton({ showTimePicker = true }) {
                                Icon(Icons.Default.Schedule, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تغییر ساعت", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }
                    }
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

            // SMS auto-register card
            SettingsCard(title = "پیامک بانکی") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sms, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("ثبت خودکار از پیامک",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (smsAutoRegisterEnabled) "فعال — تراکنش‌های بانکی شناسایی می‌شوند"
                                    else "غیرفعال",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = smsAutoRegisterEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val perms = arrayOf(
                                        Manifest.permission.RECEIVE_SMS,
                                        Manifest.permission.READ_SMS
                                    )
                                    val allGranted = perms.all {
                                        ContextCompat.checkSelfPermission(context, it) ==
                                            PackageManager.PERMISSION_GRANTED
                                    }
                                    if (allGranted) {
                                        scope.launch {
                                            prefs.setSmsAutoRegisterEnabled(true)
                                            snackbarHostState.showSnackbar("ثبت خودکار پیامک فعال شد")
                                        }
                                    } else {
                                        smsPermLauncher.launch(perms)
                                    }
                                } else {
                                    scope.launch {
                                        prefs.setSmsAutoRegisterEnabled(false)
                                        snackbarHostState.showSnackbar("ثبت خودکار پیامک غیرفعال شد")
                                    }
                                }
                            }
                        )
                    }
                    Text(
                        "پس از دریافت پیامک بانکی، تراکنش جهت تأیید نمایش داده می‌شود",
                        color = MaterialTheme.colorScheme.outline,
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
                            Text("پشتیبان‌گیری هر ۱ ساعت",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("پوشه Bankyar در Downloads",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        storagePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    } else {
                                        autoBackupEnabled = true
                                        scheduleAutoBackup(context)
                                        scope.launch {
                                            try {
                                                val json = BackupWorker.buildBackupJson(context, userId)
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    BackupWorker.writeBackupFile(context, json)
                                                }
                                                prefs.setLastAutoBackupTime(System.currentTimeMillis())
                                                lastAutoBackupTime = JalaliCalendar.toJalaliString(System.currentTimeMillis())
                                                snackbarHostState.showSnackbar("نسخه پشتیبان در Downloads/Bankyar ذخیره شد")
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("خطا در پشتیبان‌گیری: ${e.message}")
                                            }
                                        }
                                    }
                                } else {
                                    autoBackupEnabled = false
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
                        "مسیر اصلی: Downloads/Bankyar/auto_backup.json\nمسیر پشتیبان: Android/data/com.bankyar/files/Bankyar/auto_backup.json",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
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

    db.bankAccountDao().deleteAllByUser(userId)

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

    if (root.has("budgets")) {
        val budgetArray = root.getJSONArray("budgets")
        for (i in 0 until budgetArray.length()) {
            val obj = budgetArray.getJSONObject(i)
            db.budgetDao().insert(
                Budget(
                    id = 0,
                    userId = userId,
                    categoryName = obj.getString("categoryName"),
                    maxAmount = obj.getDouble("maxAmount"),
                    yearMonth = obj.getString("yearMonth"),
                    accountName = if (obj.has("accountName") && !obj.isNull("accountName")) obj.getString("accountName") else null
                )
            )
        }
    }

    if (root.has("debts")) {
        val debtArray = root.getJSONArray("debts")
        for (i in 0 until debtArray.length()) {
            val obj = debtArray.getJSONObject(i)
            db.debtDao().insert(
                Debt(
                    id = 0,
                    userId = userId,
                    personName = obj.getString("personName"),
                    amount = obj.getDouble("amount"),
                    isIOwe = obj.getBoolean("isIOwe"),
                    description = obj.optString("description", ""),
                    date = obj.getLong("date"),
                    isPaid = obj.optBoolean("isPaid", false)
                )
            )
        }
    }

    if (root.has("recurring")) {
        val recurArray = root.getJSONArray("recurring")
        for (i in 0 until recurArray.length()) {
            val obj = recurArray.getJSONObject(i)
            db.recurringTransactionDao().insert(
                RecurringTransaction(
                    id = 0,
                    userId = userId,
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    category = TransactionCategory.valueOf(obj.getString("category")),
                    accountName = obj.getString("accountName"),
                    description = obj.optString("description", ""),
                    periodDays = obj.getInt("periodDays"),
                    nextDate = obj.getLong("nextDate")
                )
            )
        }
    }
}

private fun scheduleAutoBackup(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<BackupWorker>(60, TimeUnit.MINUTES)
        .setConstraints(Constraints.NONE)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        BackupWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ساعت یادآور", fontWeight = FontWeight.Bold) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(state.hour, state.minute) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("تأیید", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}

private fun cancelAutoBackup(context: android.content.Context) {
    WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.WORK_NAME)
}
