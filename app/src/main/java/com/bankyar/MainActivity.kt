package com.bankyar

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.bankyar.navigation.BankYarNavGraph
import com.bankyar.ui.theme.BankYarTheme
import com.bankyar.ui.viewmodels.ThemeViewModel
import com.bankyar.util.BackupWorker
import com.bankyar.util.NotificationHelper
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        scheduleDefaultAutoBackup()
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
}
