package com.bankyar.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.ProfileViewModel
import com.bankyar.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int,
    viewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val user by viewModel.getUser(userId).collectAsState(initial = null)
    val message by viewModel.message.collectAsState()
    val backupMessage by settingsViewModel.backupMessage.collectAsState()
    val isFingerprintEnabled by settingsViewModel.isFingerprintEnabled.collectAsState()
    val isAutoBackupEnabled by settingsViewModel.isAutoBackupEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val hasBiometricHardware = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .let { it == BiometricManager.BIOMETRIC_SUCCESS || it == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED }
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let { name = it.name; phone = it.phone }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
            editMode = false
            showPinDialog = false
        }
    }
    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearBackupMessage()
        }
    }

    if (showPinDialog) {
        user?.let { u ->
            ChangePinDialog(
                onConfirm = { cur, new_, conf -> viewModel.changePin(u, cur, new_, conf) },
                onDismiss = { showPinDialog = false }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("پروفایل", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (!editMode) {
                        IconButton({ editMode = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.size(90.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (user?.name?.firstOrNull() ?: "؟").toString(),
                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(
                user?.name ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                user?.phone ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            // Account info card
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "اطلاعات حساب", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام و نام خانوادگی") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                        enabled = editMode,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("شماره موبایل") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                        enabled = editMode,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )

                    if (editMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    user?.let { name = it.name; phone = it.phone }
                                    editMode = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("انصراف") }

                            Button(
                                onClick = { user?.let { viewModel.updateProfile(it, name, phone) } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("ذخیره", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            // Security settings card
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "امنیت", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    // Change PIN
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "تغییر رمز عبور",
                        subtitle = "رمز PIN ورود به برنامه",
                        onClick = { showPinDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(0.2f)
                    )

                    // Fingerprint toggle
                    if (hasBiometricHardware) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Fingerprint, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "ورود با اثر انگشت",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        if (isFingerprintEnabled) "فعال است" else "غیرفعال است",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isFingerprintEnabled,
                                onCheckedChange = { settingsViewModel.setFingerprintEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Backup settings card
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "پشتیبان‌گیری", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    // Auto-backup toggle
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Backup, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "پشتیبان‌گیری خودکار",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "روزانه یک بار بصورت خودکار",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { settingsViewModel.setAutoBackupEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(0.2f)
                    )

                    // Manual backup button
                    SettingsRow(
                        icon = Icons.Default.CloudUpload,
                        title = "پشتیبان‌گیری فوری",
                        subtitle = "ذخیره اطلاعات در حافظه دستگاه",
                        onClick = { settingsViewModel.performManualBackup(userId) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Icon(
            Icons.Default.ChevronLeft, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ChangePinDialog(
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر رمز عبور", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPin, onValueChange = { currentPin = it.filter(Char::isDigit) },
                    label = { Text("رمز فعلی") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton({ showCurrent = !showCurrent }) {
                            Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = newPin, onValueChange = { newPin = it.filter(Char::isDigit) },
                    label = { Text("رمز جدید") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton({ showNew = !showNew }) {
                            Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin, onValueChange = { confirmPin = it.filter(Char::isDigit) },
                    label = { Text("تکرار رمز جدید") },
                    leadingIcon = { Icon(Icons.Default.LockReset, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton({ showConfirm = !showConfirm }) {
                            Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPin, newPin, confirmPin) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("تغییر رمز", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
