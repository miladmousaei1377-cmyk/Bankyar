package com.bankyar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.bankyar.data.PreferencesManager
import com.bankyar.navigation.BankYarNavGraph
import com.bankyar.ui.theme.BankYarTheme
import com.bankyar.ui.viewmodels.ThemeViewModel
import com.bankyar.util.BackupWorker
import com.bankyar.util.NotificationHelper
import com.bankyar.util.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently; reminder is already scheduled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        scheduleDefaultAutoBackup()
        initDefaultReminderIfNeeded()
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BankYarTheme(darkTheme = isDarkMode) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        BankYarNavGraph()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initDefaultReminderIfNeeded() {
        val prefs = PreferencesManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            if (!prefs.reminderInitialized.first()) {
                prefs.setReminderEnabled(true)
                prefs.setReminderTime(21, 0)
                prefs.markReminderInitialized()
                ReminderWorker.schedule(this@MainActivity, 21, 0)
            }
        }
    }

    private fun scheduleDefaultAutoBackup() {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
