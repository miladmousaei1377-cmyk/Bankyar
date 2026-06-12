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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

    // -2 = loading (DataStore not yet read), -1 = not logged in, >0 = logged in user ID
    val loggedInUserId: StateFlow<Int> = prefs.loggedInUserId
        .stateIn(viewModelScope, SharingStarted.Eagerly, -2)

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
            val user = repo.login(phone, pin)
            if (user == null) {
                _state.value = AuthState(error = "شماره یا رمز عبور اشتباه است")
            } else {
                prefs.saveUserId(user.id)
                _state.value = AuthState(success = true)
            }
        }
    }

    fun logout() {
        viewModelScope.launch { prefs.clearUserId() }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun verifyPin(userId: Int, pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repo.verifyPin(userId, pin)
            withContext(Dispatchers.Main) {
                onResult(user != null)
            }
        }
    }
}
