package com.bankyar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.ui.components.ThousandSeparatorVisualTransformation
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    userId: Int,
    viewModel: AccountsViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val accounts by viewModel.accounts.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<BankAccount?>(null) }
    var deleteTarget by remember { mutableStateOf<BankAccount?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    if (showDialog) {
        AccountDialog(
            account = editAccount,
            userId = userId,
            onDismiss = { showDialog = false; editAccount = null },
            onSave = { acc ->
                if (editAccount == null) viewModel.addAccount(acc)
                else viewModel.updateAccount(acc)
                showDialog = false; editAccount = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف حساب") },
            text = { Text("آیا از حذف «${target.title}» اطمینان دارید؟") },
            confirmButton = {
                TextButton({ viewModel.deleteAccount(target); deleteTarget = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("انصراف") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("مدیریت حساب‌ها", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editAccount = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalance, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("هیچ حسابی ثبت نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("روی + بزنید تا حساب اضافه کنید",
                        color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts, key = { it.id }) { acc ->
                    AccountCard(acc,
                        userId = userId,
                        viewModel = viewModel,
                        onEdit = { editAccount = acc; showDialog = true },
                        onDelete = { deleteTarget = acc }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    acc: BankAccount,
    userId: Int,
    viewModel: AccountsViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val txNetBalance by viewModel.getNetBalance(userId, acc.title).collectAsState(initial = 0.0)
    val netBalance = acc.initialBalance + txNetBalance

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(acc.title, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                    if (acc.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("پیش‌فرض", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp) }
                    }
                }
                if (acc.bankName.isNotBlank())
                    Text(acc.bankName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (acc.cardNumber.isNotBlank())
                    Text(acc.cardNumber, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                if (acc.accountNumber.isNotBlank())
                    Text("شماره حساب: ${acc.accountNumber}",
                        color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                // Net balance row
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (netBalance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("موجودی فعلی: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        "${formatAmount(netBalance)} تومان",
                        color = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                    )
                }
            }
            IconButton(onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AccountDialog(
    account: BankAccount?,
    userId: Int,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    var title by remember { mutableStateOf(account?.title ?: "") }
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var cardNumber by remember { mutableStateOf(account?.cardNumber ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var initialBalanceText by remember { mutableStateOf(if ((account?.initialBalance ?: 0.0) > 0.0) account!!.initialBalance.toLong().toString() else "") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "افزودن حساب" else "ویرایش حساب", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountField("نام حساب (اجباری)", title, { title = it }, Icons.Default.Label)
                AccountField("نام بانک", bankName, { bankName = it }, Icons.Default.AccountBalance)
                AccountField("شماره کارت", cardNumber, { cardNumber = it },
                    Icons.Default.CreditCard, KeyboardType.Number)
                AccountField("شماره حساب", accountNumber, { accountNumber = it },
                    Icons.Default.Numbers, KeyboardType.Number)
                AccountField("موجودی اولیه (تومان)", initialBalanceText,
                    { initialBalanceText = it.filter { c -> c.isDigit() } },
                    Icons.Default.AccountBalanceWallet, KeyboardType.Number,
                    ThousandSeparatorVisualTransformation())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text("حساب پیش‌فرض", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
                if (error.isNotBlank())
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) { error = "نام حساب اجباری است"; return@Button }
                onSave(BankAccount(
                    id = account?.id ?: 0,
                    userId = userId,
                    title = title,
                    bankName = bankName,
                    cardNumber = cardNumber,
                    accountNumber = accountNumber,
                    isDefault = isDefault,
                    initialBalance = initialBalanceText.toDoubleOrNull() ?: 0.0
                ))
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("ذخیره", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun AccountField(
    label: String, value: String, onChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        )
    )
}
