package com.bankyar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("bankyar_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        private val LOGGED_IN_USER_ID = intPreferencesKey("logged_in_user_id")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val REMINDER_INITIALIZED = booleanPreferencesKey("reminder_initialized")
        private val LAST_AUTO_BACKUP_TIME = longPreferencesKey("last_auto_backup_time")
        private val BACKUP_SUGGESTION_SHOWN = booleanPreferencesKey("backup_suggestion_shown")
        private val HAS_SEEN_PERMISSION_SCREEN = booleanPreferencesKey("has_seen_permission_screen")
        private val SMS_AUTO_REGISTER_ENABLED = booleanPreferencesKey("sms_auto_register_enabled")
    }

    val loggedInUserId: Flow<Int> = context.dataStore.data.map { it[LOGGED_IN_USER_ID] ?: -1 }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_WELCOME] ?: false }
    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_ENABLED] ?: false }
    val reminderHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR] ?: 21 }
    val reminderMinute: Flow<Int> = context.dataStore.data.map { it[REMINDER_MINUTE] ?: 0 }
    val reminderInitialized: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_INITIALIZED] ?: false }
    val lastAutoBackupTime: Flow<Long> = context.dataStore.data.map { it[LAST_AUTO_BACKUP_TIME] ?: 0L }
    val backupSuggestionShown: Flow<Boolean> = context.dataStore.data.map { it[BACKUP_SUGGESTION_SHOWN] ?: false }
    val hasSeenPermissionScreen: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_PERMISSION_SCREEN] ?: false }
    val smsAutoRegisterEnabled: Flow<Boolean> = context.dataStore.data.map { it[SMS_AUTO_REGISTER_ENABLED] ?: false }

    suspend fun saveUserId(id: Int) { context.dataStore.edit { it[LOGGED_IN_USER_ID] = id } }
    suspend fun clearUserId() { context.dataStore.edit { it.remove(LOGGED_IN_USER_ID) } }
    suspend fun setBiometricEnabled(enabled: Boolean) { context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled } }
    suspend fun markWelcomeSeen() { context.dataStore.edit { it[HAS_SEEN_WELCOME] = true } }
    suspend fun setReminderEnabled(enabled: Boolean) { context.dataStore.edit { it[REMINDER_ENABLED] = enabled } }
    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { it[REMINDER_HOUR] = hour; it[REMINDER_MINUTE] = minute }
    }
    suspend fun markReminderInitialized() { context.dataStore.edit { it[REMINDER_INITIALIZED] = true } }
    suspend fun setLastAutoBackupTime(time: Long) { context.dataStore.edit { it[LAST_AUTO_BACKUP_TIME] = time } }
    suspend fun markBackupSuggestionShown() { context.dataStore.edit { it[BACKUP_SUGGESTION_SHOWN] = true } }
    suspend fun markPermissionScreenSeen() { context.dataStore.edit { it[HAS_SEEN_PERMISSION_SCREEN] = true } }
    suspend fun setSmsAutoRegisterEnabled(enabled: Boolean) { context.dataStore.edit { it[SMS_AUTO_REGISTER_ENABLED] = enabled } }
}
