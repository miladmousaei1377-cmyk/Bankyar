package com.bankyar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("bankyar_settings")

class SettingsManager(private val context: Context) {
    companion object {
        private val FINGERPRINT_ENABLED = booleanPreferencesKey("fingerprint_enabled")
        private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    }

    val isFingerprintEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[FINGERPRINT_ENABLED] ?: false }

    // Default: true → auto backup is ON out of the box
    val isAutoBackupEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[AUTO_BACKUP_ENABLED] ?: true }

    val isReminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[REMINDER_ENABLED] ?: false }

    val lastBackupTime: Flow<Long> =
        context.settingsDataStore.data.map { it[LAST_BACKUP_TIME] ?: 0L }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[FINGERPRINT_ENABLED] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun updateLastBackupTime(time: Long) {
        context.settingsDataStore.edit { it[LAST_BACKUP_TIME] = time }
    }
}
