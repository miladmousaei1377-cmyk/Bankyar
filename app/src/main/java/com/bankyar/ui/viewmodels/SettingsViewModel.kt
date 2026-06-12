package com.bankyar.ui.viewmodels

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.BackupManager
import com.bankyar.data.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsManager = SettingsManager(app)
    private val backupManager = BackupManager(app)

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // onEach ensures _isLoaded is set AFTER the real DataStore value is propagated
    val isFingerprintEnabled: StateFlow<Boolean> = settingsManager.isFingerprintEnabled
        .onEach { _isLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAutoBackupEnabled: StateFlow<Boolean> = settingsManager.isAutoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isReminderEnabled: StateFlow<Boolean> = settingsManager.isReminderEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun backupFolderPath(app: Application): String {
        val dir = File(app.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "BankYar_Backups")
        return dir.absolutePath
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setFingerprintEnabled(enabled) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setAutoBackupEnabled(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setReminderEnabled(enabled) }
    }

    fun performManualBackup(userId: Int) {
        viewModelScope.launch {
            val success = backupManager.performBackup(userId)
            if (success) {
                settingsManager.updateLastBackupTime(System.currentTimeMillis())
                _backupMessage.value = "پشتیبان‌گیری با موفقیت انجام شد"
            } else {
                _backupMessage.value = "خطا در انجام پشتیبان‌گیری"
            }
        }
    }

    fun checkAndAutoBackup(userId: Int) {
        viewModelScope.launch {
            val autoEnabled = settingsManager.isAutoBackupEnabled.first()
            val lastBackup = settingsManager.lastBackupTime.first()
            val thirtyMinMs = 30L * 60 * 1000
            if (autoEnabled && System.currentTimeMillis() - lastBackup > thirtyMinMs) {
                val success = backupManager.performBackup(userId)
                if (success) settingsManager.updateLastBackupTime(System.currentTimeMillis())
            }
        }
    }

    fun clearBackupMessage() { _backupMessage.value = null }
}
