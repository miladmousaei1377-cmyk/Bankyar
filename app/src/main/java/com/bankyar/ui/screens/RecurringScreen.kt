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
import com.bankyar.data.database.entities.RecurringTransaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.ThousandSeparatorVisualTransformation
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.AccountsViewModel
import com.bankyar.ui.viewmodels.RecurringViewModel
import com.bankyar.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    userId: Int,
    viewModel: RecurringViewModel,
    accountsViewModel: AccountsViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val items by viewModel.items.collectAsState()
    val accounts by accountsViewModel.accounts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<RecurringTransaction?>(null) }
    var deleteTarget by remember { mutableStateOf<RecurringTransaction?>(null) }

    if (showDialog) {
        RecurringDialog(
            userId = userId,
            edit = editItem,
            accounts = accounts,
            onDismiss = { showDialog = false; editItem = null },
            onSave = { rt ->
                if (editItem != null) viewModel.update(rt.copy(id = editItem!!.id))
                else viewModel.add(rt)
                showDialog = false; editItem = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف تراکنش تکرارشونده") },
            text = { Text("آیا از حذف «${target.title}» اطمینان دارید؟") },
            confirmButton = {
                TextButton({ viewModel.delete(target); deleteTarget = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تراکنش‌های تکرارشونده", fontWeight = FontWeight.Bold) },
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
                onClick = { editItem = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Repeat, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("هیچ تراکنش تکرارشونده‌ای ثبت نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("روی + بزنید تا الگو تعریف کنید",
                        color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { rt ->
                    RecurringCard(
                        rt = rt,
                        onDelete = { deleteTarget = rt },
                        onEdit = { editItem = rt; showDialog = true }
                    )
                }
            }
        }
    }
}

private fun periodLabel(periodDays: Int): String = when (periodDays) {
    1 -> "هر روز"
    7 -> "هر ۷ روز (هفتگی)"
    30 -> "هر ۳۰ روز (ماهانه)"
    else -> "هر $periodDays روز"
}

@Composable
private fun RecurringCard(rt: RecurringTransaction, onDelete: () -> Unit, onEdit: () -> Unit) {
    val (typeColor, typeBg, typeLabel) = when (rt.type) {
        TransactionType.INCOME -> Triple(Color(0xFF2E7D32), Color(0xFFE8F5E9), "درآمد")
        TransactionType.EXPENSE -> Triple(Color(0xFFC62828), Color(0xFFFFEBEE), "هزینه")
        TransactionType.TRANSFER -> Triple(Color(0xFF1565C0), Color(0xFFE3F2FD), "انتقال")
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(typeBg),
                contentAlignment = Alignment.Center
            ) { Text(rt.category.icon, fontSize = 22.sp) }

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rt.title, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(typeBg)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) { Text(typeLabel, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.height(2.dp))
                Text("${formatAmount(rt.amount)} تومان",
                    color = typeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(periodLabel(rt.periodDays),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("سررسید: ${JalaliCalendar.toJalaliShort(rt.nextDate)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
                if (rt.accountName.isNotBlank()) {
                    Text("حساب: ${rt.accountName}",
                        color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                }
                if (rt.description.isNotBlank()) {
                    Text(rt.description, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, maxLines = 1)
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringDialog(
    userId: Int,
    edit: RecurringTransaction?,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit
) {
    val defaultAccountName = edit?.accountName
        ?: accounts.firstOrNull { it.isDefault }?.title
        ?: accounts.firstOrNull()?.title
        ?: "حساب اصلی"

    var title by remember { mutableStateOf(edit?.title ?: "") }
    var amountText by remember { mutableStateOf(edit?.amount?.toLong()?.toString() ?: "") }
    var type by remember { mutableStateOf(edit?.type ?: TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(edit?.category ?: TransactionCategory.OTHER) }
    var accountName by remember { mutableStateOf(defaultAccountName) }
    var description by remember { mutableStateOf(edit?.description ?: "") }
    var periodDays by remember { mutableStateOf(edit?.periodDays ?: 30) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val periodOptions = listOf(1 to "هر روز", 7 to "هر ۷ روز (هفتگی)", 30 to "هر ۳۰ روز (ماهانه)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (edit == null) "تراکنش تکرارشونده جدید" else "ویرایش تراکنش تکرارشونده",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                Modifier.imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransactionType.entries.forEach { t ->
                        val (lbl, fg, bg) = when (t) {
                            TransactionType.INCOME -> Triple("درآمد", Color(0xFF2E7D32), Color(0xFFE8F5E9))
                            TransactionType.EXPENSE -> Triple("هزینه", Color(0xFFC62828), Color(0xFFFFEBEE))
                            TransactionType.TRANSFER -> Triple("انتقال", Color(0xFF1565C0), Color(0xFFE3F2FD))
                        }
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (type == t) fg else bg)
                                .clickable { type = t; if (t == TransactionType.TRANSFER) category = TransactionCategory.TRANSFER }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lbl, color = if (type == t) Color.White else fg,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ (تومان)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Category
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${category.icon} ${category.label}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("دسته‌بندی") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        TransactionCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.label}") },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                // Account dropdown
                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = accountName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نام حساب") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.title) },
                                    onClick = { accountName = acc.title; accountExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("نام حساب") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Period selector
                Text("دوره تکرار", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    periodOptions.forEach { (days, label) ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (periodDays == days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { periodDays = days }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label.substringBefore(" ("),
                                color = if (periodDays == days) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (periodDays == days) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )

                if (error.isNotBlank())
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    when {
                        title.isBlank() -> error = "عنوان را وارد کنید"
                        amt == null || amt <= 0 -> error = "مبلغ معتبر وارد کنید"
                        else -> onSave(
                            RecurringTransaction(
                                userId = userId,
                                title = title,
                                amount = amt,
                                type = type,
                                category = category,
                                accountName = accountName,
                                description = description,
                                periodDays = periodDays,
                                nextDate = edit?.nextDate ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
