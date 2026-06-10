package com.bankyar.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.BudgetItem
import com.bankyar.ui.viewmodels.BudgetViewModel
import com.bankyar.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    userId: Int,
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val budgetItems by viewModel.budgetItems.collectAsState()
    val month by viewModel.month.collectAsState()
    val year by viewModel.year.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<BudgetItem?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    if (showAddDialog || editItem != null) {
        BudgetDialog(
            userId = userId,
            month = month,
            year = year,
            existing = editItem,
            usedCategories = budgetItems.map { it.budget.category }.toSet()
                .let { if (editItem != null) it - editItem!!.budget.category else it },
            onSave = { category, amount ->
                viewModel.saveBudget(userId, category, amount, editItem?.budget?.id ?: 0)
                showAddDialog = false; editItem = null
            },
            onDismiss = { showAddDialog = false; editItem = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("مدیریت بودجه", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (budgetItems.size < TransactionCategory.entries.size) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { MonthNavigator(month, year, viewModel::previousMonth, viewModel::nextMonth) }

            if (budgetItems.isEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            Modifier.padding(40.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AccountBalance, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("هیچ بودجه‌ای تعریف نشده",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("برای این ماه بودجه اضافه کنید",
                                color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                item { BudgetSummaryCard(budgetItems) }
                items(budgetItems, key = { it.budget.id }) { item ->
                    BudgetItemCard(
                        item = item,
                        onEdit = { editItem = item },
                        onDelete = { viewModel.deleteBudget(item.budget) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthNavigator(month: Int, year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val jalaliMonths = arrayOf(
        "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
        "مهر","آبان","آذر","دی","بهمن","اسفند"
    )
    val (jy, jm, _) = JalaliCalendar.gregorianToJalali(year, month, 1)
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onNext) { Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.primary) }
            Text(
                "${jalaliMonths[jm - 1]} $jy",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onPrev) { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun BudgetSummaryCard(items: List<BudgetItem>) {
    val totalLimit = items.sumOf { it.budget.limitAmount }
    val totalSpent = items.sumOf { it.spent }
    val progress = if (totalLimit > 0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    val overBudget = totalSpent > totalLimit

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("خلاصه بودجه ماه", color = Color.White.copy(0.8f), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (overBudget) Color(0xFFEF5350) else Color(0xFF81C784),
                trackColor = Color.White.copy(0.3f)
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("هزینه شده", color = Color.White.copy(0.7f), fontSize = 11.sp)
                    Text("${formatAmount(totalSpent)} ت",
                        color = if (overBudget) Color(0xFFEF9A9A) else Color(0xFF81C784),
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("بودجه کل", color = Color.White.copy(0.7f), fontSize = 11.sp)
                    Text("${formatAmount(totalLimit)} ت",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (overBudget) {
                Spacer(Modifier.height(8.dp))
                Text("⚠️ از بودجه ماه فراتر رفته‌اید",
                    color = Color(0xFFEF9A9A), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BudgetItemCard(item: BudgetItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val progress = if (item.budget.limitAmount > 0)
        (item.spent / item.budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
    val overBudget = item.spent > item.budget.limitAmount
    val progressColor = when {
        overBudget -> Color(0xFFEF5350)
        progress > 0.7f -> Color(0xFFFFB74D)
        else -> Color(0xFF66BB6A)
    }

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.budget.category.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(item.budget.category.label, fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${formatAmount(item.spent)} / ${formatAmount(item.budget.limitAmount)} ت",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (overBudget) {
                Spacer(Modifier.height(4.dp))
                Text("${formatAmount(item.spent - item.budget.limitAmount)} ت اضافه هزینه",
                    color = Color(0xFFEF5350), fontSize = 11.sp)
            } else {
                Spacer(Modifier.height(4.dp))
                Text("${formatAmount(item.budget.limitAmount - item.spent)} ت باقی‌مانده",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    userId: Int,
    month: Int,
    year: Int,
    existing: BudgetItem?,
    usedCategories: Set<TransactionCategory>,
    onSave: (TransactionCategory, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(existing?.budget?.category ?: TransactionCategory.entries.first { it !in usedCategories })
    }
    var amountText by remember {
        mutableStateOf(existing?.budget?.limitAmount?.let { formatAmount(it) } ?: "")
    }
    var expandedDropdown by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "افزودن بودجه" else "ویرایش بودجه",
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { if (existing == null) expandedDropdown = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory.icon} ${selectedCategory.label}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("دسته‌بندی") },
                        trailingIcon = { if (existing == null) ExposedDropdownMenuDefaults.TrailingIcon(expandedDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = existing == null
                    )
                    if (existing == null) {
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            TransactionCategory.entries.filter { it !in usedCategories }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.icon} ${cat.label}") },
                                    onClick = { selectedCategory = cat; expandedDropdown = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() }; error = null },
                    label = { Text("سقف بودجه (تومان)") },
                    leadingIcon = { Text("💰", modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) { error = "مبلغ معتبر وارد کنید"; return@Button }
                    onSave(selectedCategory, amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("انصراف") }
        }
    )
}
