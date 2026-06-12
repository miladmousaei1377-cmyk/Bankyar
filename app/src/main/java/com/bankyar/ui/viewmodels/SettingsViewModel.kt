package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.BackupManager
import com.bankyar.data.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsManager = SettingsManager(app)
    private val backupManager = BackupManager(app)

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val isFingerprintEnabled: StateFlow<Boolean> = settingsManager.isFingerprintEnabled
        .onEach { _isLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAutoBackupEnabled: StateFlow<Boolean> = settingsManager.isAutoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setFingerprintEnabled(enabled) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setAutoBackupEnabled(enabled) }
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
            val twentyFourHours = 24L * 60 * 60 * 1000
            if (autoEnabled && System.currentTimeMillis() - lastBackup > twentyFourHours) {
                val success = backupManager.performBackup(userId)
                if (success) settingsManager.updateLastBackupTime(System.currentTimeMillis())
            }
        }
    }

    fun clearBackupMessage() { _backupMessage.value = null }
}
