package com.bankyar.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.User
import com.bankyar.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(AppDatabase.getInstance(app).userDao())
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun getUser(id: Int): Flow<User?> = repo.getUserById(id)

    fun updateProfile(user: User, name: String, phone: String) = viewModelScope.launch {
        if (name.isBlank()) { _message.value = "نام نمی‌تواند خالی باشد"; return@launch }
        if (phone.length < 10) { _message.value = "شماره موبایل معتبر نیست"; return@launch }
        repo.update(user.copy(name = name, phone = phone))
        _message.value = "اطلاعات با موفقیت ذخیره شد"
    }

    fun changePin(user: User, currentPin: String, newPin: String, confirmPin: String) = viewModelScope.launch {
        when {
            currentPin != user.pin -> _message.value = "رمز فعلی اشتباه است"
            newPin.length < 4 -> _message.value = "رمز جدید باید حداقل ۴ رقم باشد"
            newPin != confirmPin -> _message.value = "تکرار رمز مطابقت ندارد"
            newPin == currentPin -> _message.value = "رمز جدید با رمز فعلی یکسان است"
            else -> { repo.update(user.copy(pin = newPin)); _message.value = "رمز با موفقیت تغییر یافت" }
        }
    }

    fun clearMessage() { _message.value = null }
}
