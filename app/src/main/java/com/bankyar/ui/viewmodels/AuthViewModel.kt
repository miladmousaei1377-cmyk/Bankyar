package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.User
import com.bankyar.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(AppDatabase.getInstance(app).userDao())
    private val prefs = PreferencesManager(app)

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // In-memory only — resets to false on every process start (app close/reopen)
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    val loggedInUserId: StateFlow<Int> = prefs.loggedInUserId
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1)

    val biometricEnabled: StateFlow<Boolean> = prefs.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun register(name: String, phone: String, pin: String) {
        if (name.isBlank() || phone.isBlank() || pin.isBlank()) {
            _state.value = AuthState(error = "لطفاً همه فیلدها را پر کنید")
            return
        }
        if (phone.length < 10) {
            _state.value = AuthState(error = "شماره موبایل معتبر نیست")
            return
        }
        if (pin.length < 4) {
            _state.value = AuthState(error = "رمز عبور باید حداقل ۴ رقم باشد")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            val existing = repo.findByPhone(phone)
            if (existing != null) {
                _state.value = AuthState(error = "این شماره قبلاً ثبت شده است")
                return@launch
            }
            val id = repo.register(User(name = name, phone = phone, pin = pin))
            prefs.saveUserId(id.toInt())
            _isSessionActive.value = true
            _state.value = AuthState(success = true)
        }
    }

    fun login(phone: String, pin: String) {
        if (phone.isBlank() || pin.isBlank()) {
            _state.value = AuthState(error = "لطفاً شماره و رمز را وارد کنید")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            val userByPhone = repo.findByPhone(phone)
            if (userByPhone == null) {
                _state.value = AuthState(error = "این شماره موبایل ثبت نشده است، ابتدا ثبت‌نام کنید")
                return@launch
            }
            if (userByPhone.pin != pin) {
                _state.value = AuthState(error = "رمز عبور اشتباه است")
                return@launch
            }
            prefs.saveUserId(userByPhone.id)
            _isSessionActive.value = true
            _state.value = AuthState(success = true)
        }
    }

    fun activateSession() {
        _isSessionActive.value = true
    }

    fun loginWithBiometric(userId: Int) {
        viewModelScope.launch {
            prefs.saveUserId(userId)
            _isSessionActive.value = true
            _state.value = AuthState(success = true)
        }
    }

    fun logout() {
        viewModelScope.launch { prefs.clearUserId() }
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setBiometricEnabled(enabled)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
