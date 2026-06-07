package com.bankyar.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.R
import com.bankyar.data.database.AppDatabase
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    // 0 = normal, 1 = forgot: enter phone, 2 = forgot: enter new pin
    var forgotStep by remember { mutableStateOf(0) }
    var forgotPhone by remember { mutableStateOf("") }
    var forgotNewPin by remember { mutableStateOf("") }
    var forgotConfirmPin by remember { mutableStateOf("") }
    var forgotNewPinVisible by remember { mutableStateOf(false) }
    var forgotConfirmPinVisible by remember { mutableStateOf(false) }
    var forgotError by remember { mutableStateOf<String?>(null) }
    var forgotLoading by remember { mutableStateOf(false) }
    var resetSuccessMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.success) { if (state.success) onAuthenticated() }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd, Color(0xFF0A2472))))
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "لوگوی بانک‌یار",
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))
            Text("بانک‌یار", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("مدیریت هوشمند حساب‌های بانکی", fontSize = 14.sp, color = Color.White.copy(0.8f))

            Spacer(Modifier.height(40.dp))

            when (forgotStep) {
                0 -> {
                    // Normal login / register card
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            // Tabs
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEEF2FB))
                            ) {
                                listOf("ورود" to true, "ثبت‌نام" to false).forEach { (label, isLoginTab) ->
                                    Box(
                                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                            .background(if (isLogin == isLoginTab) Primary else Color.Transparent)
                                            .clickable { isLogin = isLoginTab; viewModel.clearError() }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label,
                                            color = if (isLogin == isLoginTab) Color.White else Color(0xFF6B7280),
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            AnimatedVisibility(!isLogin, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                Column {
                                    AuthTextField(
                                        label = "نام و نام خانوادگی", value = name,
                                        onValueChange = { name = it }, icon = Icons.Default.Person
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                            }

                            AuthTextField(
                                label = "شماره موبایل", value = phone,
                                onValueChange = { phone = it }, icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = pin,
                                onValueChange = { if (it.length <= 8) pin = it },
                                label = { Text("رمز عبور", color = Color(0xFF6B7280)) },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                                trailingIcon = {
                                    IconButton({ pinVisible = !pinVisible }) {
                                        Icon(if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null, tint = Color(0xFF6B7280))
                                    }
                                },
                                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Color(0xFF9CA3AF),
                                    focusedTextColor = Color(0xFF1A1A2E),
                                    unfocusedTextColor = Color(0xFF1A1A2E),
                                    cursorColor = Primary,
                                )
                            )

                            resetSuccessMsg?.let { msg ->
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5E9)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(msg, color = Color(0xFF2E7D32), fontSize = 13.sp)
                                }
                            }

                            state.error?.let { err ->
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFEBEE)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(err, color = Color(0xFFC62828), fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    resetSuccessMsg = null
                                    if (isLogin) viewModel.login(phone, pin)
                                    else viewModel.register(name, phone, pin)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = !state.isLoading
                            ) {
                                if (state.isLoading)
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else
                                    Text(if (isLogin) "ورود به حساب" else "ایجاد حساب",
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            if (isLogin) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        forgotStep = 1
                                        forgotError = null
                                        forgotPhone = ""
                                        forgotNewPin = ""
                                        forgotConfirmPin = ""
                                        resetSuccessMsg = null
                                        viewModel.clearError()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("فراموشی رمز عبور؟",
                                        color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Forgot: step 1 — verify phone
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text("بازیابی رمز عبور",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                            Spacer(Modifier.height(6.dp))
                            Text("شماره موبایل ثبت‌شده را وارد کنید",
                                color = Color(0xFF6B7280), fontSize = 13.sp)
                            Spacer(Modifier.height(20.dp))

                            AuthTextField(
                                label = "شماره موبایل", value = forgotPhone,
                                onValueChange = { forgotPhone = it; forgotError = null },
                                icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone
                            )

                            forgotError?.let { err ->
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFEBEE)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(err, color = Color(0xFFC62828), fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (forgotPhone.length < 10) {
                                        forgotError = "شماره موبایل معتبر نیست"
                                        return@Button
                                    }
                                    forgotLoading = true
                                    forgotError = null
                                    scope.launch {
                                        val user = AppDatabase.getInstance(context).userDao().findByPhone(forgotPhone)
                                        forgotLoading = false
                                        if (user == null) {
                                            forgotError = "این شماره موبایل ثبت نشده است"
                                        } else {
                                            forgotStep = 2
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = !forgotLoading
                            ) {
                                if (forgotLoading)
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else
                                    Text("تأیید شماره موبایل",
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { forgotStep = 0; forgotError = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بازگشت به صفحه ورود", color = Primary, fontSize = 14.sp)
                            }
                        }
                    }
                }

                2 -> {
                    // Forgot: step 2 — set new pin
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text("رمز عبور جدید",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                            Spacer(Modifier.height(6.dp))
                            Text("رمز عبور جدید خود را وارد کنید",
                                color = Color(0xFF6B7280), fontSize = 13.sp)
                            Spacer(Modifier.height(20.dp))

                            OutlinedTextField(
                                value = forgotNewPin,
                                onValueChange = { forgotNewPin = it; forgotError = null },
                                label = { Text("رمز عبور جدید", color = Color(0xFF6B7280)) },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                                trailingIcon = {
                                    IconButton({ forgotNewPinVisible = !forgotNewPinVisible }) {
                                        Icon(if (forgotNewPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null, tint = Color(0xFF6B7280))
                                    }
                                },
                                visualTransformation = if (forgotNewPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Color(0xFF9CA3AF),
                                    focusedTextColor = Color(0xFF1A1A2E),
                                    unfocusedTextColor = Color(0xFF1A1A2E),
                                    cursorColor = Primary,
                                )
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = forgotConfirmPin,
                                onValueChange = { forgotConfirmPin = it; forgotError = null },
                                label = { Text("تکرار رمز عبور جدید", color = Color(0xFF6B7280)) },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                                trailingIcon = {
                                    IconButton({ forgotConfirmPinVisible = !forgotConfirmPinVisible }) {
                                        Icon(if (forgotConfirmPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null, tint = Color(0xFF6B7280))
                                    }
                                },
                                visualTransformation = if (forgotConfirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Color(0xFF9CA3AF),
                                    focusedTextColor = Color(0xFF1A1A2E),
                                    unfocusedTextColor = Color(0xFF1A1A2E),
                                    cursorColor = Primary,
                                )
                            )

                            forgotError?.let { err ->
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFEBEE)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(err, color = Color(0xFFC62828), fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (forgotNewPin.length < 4) {
                                        forgotError = "رمز عبور باید حداقل ۴ رقم باشد"
                                        return@Button
                                    }
                                    if (forgotNewPin != forgotConfirmPin) {
                                        forgotError = "تکرار رمز عبور مطابقت ندارد"
                                        return@Button
                                    }
                                    forgotLoading = true
                                    scope.launch {
                                        val db = AppDatabase.getInstance(context)
                                        val user = db.userDao().findByPhone(forgotPhone)
                                        if (user != null) {
                                            db.userDao().update(user.copy(pin = forgotNewPin))
                                        }
                                        forgotLoading = false
                                        forgotStep = 0
                                        forgotPhone = ""; forgotNewPin = ""; forgotConfirmPin = ""
                                        forgotError = null
                                        isLogin = true
                                        resetSuccessMsg = "رمز عبور با موفقیت تغییر یافت. وارد شوید"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = !forgotLoading
                            ) {
                                if (forgotLoading)
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else
                                    Text("ذخیره رمز عبور جدید",
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { forgotStep = 1; forgotError = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بازگشت", color = Primary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF6B7280)) },
        leadingIcon = { Icon(icon, null, tint = Primary) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color(0xFF9CA3AF),
            focusedTextColor = Color(0xFF1A1A2E),
            unfocusedTextColor = Color(0xFF1A1A2E),
            cursorColor = Primary,
        )
    )
}
