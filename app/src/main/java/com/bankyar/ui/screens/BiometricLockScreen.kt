package com.bankyar.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bankyar.ui.theme.GradientEnd
import com.bankyar.ui.theme.GradientStart

@Composable
fun BiometricLockScreen(
    userId: Int,
    onUnlocked: () -> Unit,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var showPinDialog by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val currentOnUnlocked by rememberUpdatedState(onUnlocked)

    val canAuthenticate = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val executor = remember { ContextCompat.getMainExecutor(context) }

    val callback = remember {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                currentOnUnlocked()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED -> showPinDialog = true
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        biometricError = "اثر انگشت قفل شد. از رمز عبور استفاده کنید"
                        showPinDialog = true
                    }
                    else -> biometricError = errString.toString()
                }
            }
            override fun onAuthenticationFailed() {
                biometricError = "اثر انگشت شناخته نشد. دوباره امتحان کنید"
            }
        }
    }

    val biometricPrompt = remember { BiometricPrompt(activity, executor, callback) }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("بانک‌یار")
            .setSubtitle("اثر انگشت خود را قرار دهید")
            .setNegativeButtonText("استفاده از رمز عبور")
            .setConfirmationRequired(false)
            .build()
    }

    LaunchedEffect(Unit) {
        if (canAuthenticate) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            showPinDialog = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd, Color(0xFF0A2472)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f))
                    .border(2.dp, Color.White.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Fingerprint, null,
                    tint = Color.White, modifier = Modifier.size(60.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text("بانک‌یار", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "برای ورود، اثر انگشت خود را اسکن کنید",
                color = Color.White.copy(0.8f), fontSize = 14.sp, textAlign = TextAlign.Center
            )

            biometricError?.let { err ->
                Text(
                    err, color = Color(0xFFFF8A80),
                    fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            if (canAuthenticate) {
                Button(
                    onClick = {
                        biometricError = null
                        biometricPrompt.authenticate(promptInfo)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("اسکن اثر انگشت", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            TextButton(onClick = { showPinDialog = true; biometricError = null }) {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("استفاده از رمز عبور", color = Color.White.copy(0.7f))
            }
        }
    }

    if (showPinDialog) {
        PinFallbackDialog(
            error = pinError,
            isVerifying = isVerifying,
            onVerify = { enteredPin ->
                if (enteredPin.isBlank()) { pinError = "رمز عبور را وارد کنید"; return@PinFallbackDialog }
                isVerifying = true
                pinError = null
                onVerifyPin(enteredPin) { success ->
                    isVerifying = false
                    if (success) {
                        showPinDialog = false
                        onUnlocked()
                    } else {
                        pinError = "رمز عبور اشتباه است"
                    }
                }
            },
            onDismiss = { showPinDialog = false; pinError = null }
        )
    }
}

@Composable
private fun PinFallbackDialog(
    error: String?,
    isVerifying: Boolean,
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ورود با رمز عبور", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "رمز عبور حساب خود را وارد کنید",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    label = { Text("رمز عبور") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton({ pinVisible = !pinVisible }) {
                            Icon(
                                if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null
                            )
                        }
                    },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(pin) },
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("تایید", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
