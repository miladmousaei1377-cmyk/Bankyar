package com.bankyar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.ui.components.ThousandSeparatorVisualTransformation
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.AccountsViewModel
import com.bankyar.ui.viewmodels.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    userId: Int,
    viewModel: BudgetViewModel,
    accountsViewModel: AccountsViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val currentMonth = remember { viewModel.currentYearMonth() }
    val budgets by viewModel.getBudgetsForMonth(currentMonth).collectAsState(initial = emptyList())
    val accounts by accountsViewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var editBudget by remember { mutableStateOf<Budget?>(null) }
    var deleteTarget by remember { mutableStateOf<Budget?>(null) }

    if (showDialog) {
        BudgetDialog(
            budget = editBudget,
            yearMonth = currentMonth,
            accounts = accounts,
            onDismiss = { showDialog = false; editBudget = null },
            onSave = { catName, maxAmt, accName ->
                if (editBudget != null) {
                    viewModel.updateBudget(editBudget!!.copy(
                        categoryName = catName,
                        maxAmount = maxAmt,
                        accountName = accName
                    ))
                } else {
                    viewModel.saveBudget(userId, catName, maxAmt, currentMonth, accName)
                }
                showDialog = false; editBudget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف بودجه") },
            text = {
                val cat = TransactionCategory.entries.find { it.name == target.categoryName }
                Text("آیا از حذف بودجه «${cat?.label ?: target.categoryName}» اطمینان دارید؟")
            },
            confirmButton = {
                TextButton({ viewModel.deleteBudget(target); deleteTarget = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بودجه‌بندی ماه $currentMonth", fontWeight = FontWeight.Bold) },
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
                onClick = { editBudget = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)
        ) {
            if (accounts.isNotEmpty()) {
                LazyRow(
                    Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAccount == null,
                            onClick = { selectedAccount = null },
                            label = { Text("همه حساب‌ها", fontSize = 12.sp) }
                        )
                    }
                    items(accounts) { acc ->
                        FilterChip(
                            selected = selectedAccount == acc.title,
                            onClick = { selectedAccount = if (selectedAccount == acc.title) null else acc.title },
                            label = { Text(acc.title, fontSize = 12.sp) }
                        )
                    }
                }
            }

            val filteredBudgets = if (selectedAccount == null) budgets
                else budgets.filter { it.accountName == null || it.accountName == selectedAccount }

            if (filteredBudgets.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Savings, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("هیچ بودجه‌ای تعریف نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("روی + بزنید تا بودجه تعریف کنید",
                            color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBudgets, key = { it.id }) { budget ->
                        BudgetCard(
                            budget = budget,
                            viewModel = viewModel,
                            yearMonth = currentMonth,
                            onDelete = { deleteTarget = budget },
                            onEdit = { editBudget = budget; showDialog = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    budget: Budget,
    viewModel: BudgetViewModel,
    yearMonth: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val category = TransactionCategory.entries.find { it.name == budget.categoryName }
    val resolvedCategory = category ?: TransactionCategory.OTHER
    // produceState: immediately loads via suspend then stays reactive via Flow
    val spent by produceState(initialValue = 0.0, budget.id, yearMonth, budget.accountName) {
        value = viewModel.getSpentForCategoryDirect(resolvedCategory, yearMonth, budget.accountName)
        viewModel.getSpentForCategoryAndAccount(resolvedCategory, yearMonth, budget.accountName)
            .collect { value = it }
    }

    val progress = if (budget.maxAmount > 0) (spent / budget.maxAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val progressColor = when {
        progress >= 1f -> Color(0xFFC62828)
        progress >= 0.8f -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }
    val isOverBudget = spent > budget.maxAmount
    val remaining = budget.maxAmount - spent

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Text(category?.icon ?: "💰", fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(category?.label ?: budget.categoryName,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("سقف: ${formatAmount(budget.maxAmount)} تومان",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            if (budget.accountName != null) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(budget.accountName, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverBudget) {
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) { Text("از بودجه گذشت!", color = Color(0xFFC62828), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("هزینه شده",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("${formatAmount(spent)} تومان",
                        color = progressColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("درصد مصرف",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("${(progress * 100).toInt()}%",
                        color = progressColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isOverBudget) "مازاد" else "باقی‌مانده",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("${formatAmount(kotlin.math.abs(remaining))} تومان",
                        color = if (isOverBudget) Color(0xFFC62828) else Color(0xFF2E7D32),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    budget: Budget?,
    yearMonth: String,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String?) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(
            if (budget != null)
                TransactionCategory.entries.find { it.name == budget.categoryName } ?: TransactionCategory.OTHER
            else TransactionCategory.OTHER
        )
    }
    var maxAmountText by remember {
        mutableStateOf(if (budget != null) budget.maxAmount.toLong().toString() else "")
    }
    var selectedAccountName by remember {
        mutableStateOf(budget?.accountName)
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (budget == null) "تعریف بودجه جدید" else "ویرایش بودجه",
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory.icon} ${selectedCategory.label}",
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
                                onClick = { selectedCategory = cat; categoryExpanded = false }
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
                            value = selectedAccountName ?: "همه حساب‌ها",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("حساب بانکی") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("همه حساب‌ها") },
                                onClick = { selectedAccountName = null; accountExpanded = false }
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.title) },
                                    onClick = { selectedAccountName = acc.title; accountExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Max amount
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it.filter { c -> c.isDigit() } },
                    label = { Text("سقف بودجه (تومان)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (error.isNotBlank())
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = maxAmountText.toDoubleOrNull()
                    when {
                        amt == null || amt <= 0 -> error = "مبلغ معتبر وارد کنید"
                        else -> onSave(selectedCategory.name, amt, selectedAccountName)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
