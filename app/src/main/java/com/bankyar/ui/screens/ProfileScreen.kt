package com.bankyar.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int,
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.getUser(userId).collectAsState(initial = null)
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showCurrentPin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let { name = it.name; phone = it.phone }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("خروج از حساب") },
            text = { Text("آیا مطمئن هستید که می‌خواهید از حساب خارج شوید؟") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("خروج", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ showLogoutDialog = false }) { Text("انصراف") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("پروفایل", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
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

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("اطلاعات حساب", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام و نام خانوادگی") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("شماره موبایل") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )

                    Button(
                        onClick = { user?.let { viewModel.updateProfile(it, name, phone) } },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("ذخیره اطلاعات", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("تغییر رمز عبور", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it },
                        label = { Text("رمز عبور فعلی") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton({ showCurrentPin = !showCurrentPin }) {
                                Icon(if (showCurrentPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        visualTransformation = if (showCurrentPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("رمز عبور جدید") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton({ showNewPin = !showNewPin }) {
                                Icon(if (showNewPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text("تکرار رمز عبور جدید") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton({ showConfirmPin = !showConfirmPin }) {
                                Icon(if (showConfirmPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )

                    Button(
                        onClick = {
                            user?.let {
                                viewModel.changePassword(it, currentPin, newPin, confirmPin)
                                currentPin = ""; newPin = ""; confirmPin = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Key, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("تغییر رمز عبور", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("خروج از حساب کاربری", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
