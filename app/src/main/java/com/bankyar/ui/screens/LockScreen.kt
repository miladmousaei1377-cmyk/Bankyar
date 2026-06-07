package com.bankyar.ui.screens

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
import androidx.fragment.app.FragmentActivity
import com.bankyar.R
import com.bankyar.data.database.AppDatabase
import com.bankyar.ui.theme.GradientEnd
import com.bankyar.ui.theme.GradientStart
import com.bankyar.ui.theme.Primary
import com.bankyar.util.BiometricHelper
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    userId: Int,
    userName: String,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    val biometricAvailable = remember { BiometricHelper.canAuthenticate(context) }
    val showBiometric = biometricEnabled && biometricAvailable

    var showPinForm by remember { mutableStateOf(!showBiometric) }
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun triggerBiometric() {
        if (activity == null || !showBiometric) return
        error = null
        BiometricHelper.showPrompt(
            activity = activity,
            onSuccess = onUnlocked,
            onError = { error = "شناسایی نشد" },
            onFailed = { error = "شناسایی نشد" }
        )
    }

    LaunchedEffect(Unit) { if (showBiometric) triggerBiometric() }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd, Color(0xFF0A2472))))
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "بانک‌یار",
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(22.dp))
                    .background(Color.White).padding(8.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Text("بانک‌یار", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("خوش آمدید، $userName", fontSize = 14.sp, color = Color.White.copy(0.85f))

            Spacer(Modifier.height(36.dp))

            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "تأیید هویت", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = Color(0xFF1A1A2E)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "برای ورود هویت خود را تأیید کنید",
                        color = Color(0xFF6B7280), fontSize = 13.sp
                    )
                    Spacer(Modifier.height(24.dp))

                    if (showBiometric && !showPinForm) {
                        Box(
                            Modifier.size(80.dp).clip(RoundedCornerShape(40.dp))
                                .background(Primary.copy(alpha = 0.1f))
                                .clickable { triggerBiometric() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Fingerprint, null,
                                tint = Primary, modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("اثر انگشت خود را بگذارید", color = Color(0xFF6B7280), fontSize = 13.sp)

                        error?.let { err ->
                            Spacer(Modifier.height(8.dp))
                            Text(err, color = Color(0xFFC62828), fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(20.dp))
                        TextButton(onClick = { showPinForm = true; error = null }) {
                            Text("ورود با رمز عبور", color = Primary, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it; error = null },
                            label = { Text("رمز عبور", color = Color(0xFF6B7280)) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                            trailingIcon = {
                                IconButton({ pinVisible = !pinVisible }) {
                                    Icon(
                                        if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null, tint = Color(0xFF6B7280)
                                    )
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
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            )
                        )

                        error?.let { err ->
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
                                if (pin.length < 4) { error = "رمز عبور باید حداقل ۴ رقم باشد"; return@Button }
                                loading = true
                                scope.launch {
                                    val user = AppDatabase.getInstance(context).userDao().findById(userId)
                                    loading = false
                                    if (user == null || user.pin != pin) {
                                        error = "رمز عبور اشتباه است"
                                    } else {
                                        onUnlocked()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            enabled = !loading
                        ) {
                            if (loading)
                                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            else
                                Text("ورود", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }

                        if (showBiometric) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showPinForm = false; pin = ""; error = null; triggerBiometric() }) {
                                Icon(Icons.Default.Fingerprint, null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ورود با اثر انگشت", color = Primary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onSwitchAccount) {
                Text("تغییر حساب کاربری", color = Color.White.copy(0.7f), fontSize = 13.sp)
            }
        }
    }
}
