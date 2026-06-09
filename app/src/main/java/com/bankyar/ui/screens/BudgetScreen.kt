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
import com.bankyar.data.database.entities.Budget
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    userId: Int,
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val currentMonth = remember { viewModel.currentYearMonth() }
    val budgets by viewModel.getBudgetsForMonth(currentMonth).collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var editBudget by remember { mutableStateOf<Budget?>(null) }
    var deleteTarget by remember { mutableStateOf<Budget?>(null) }

    if (showDialog) {
        BudgetDialog(
            budget = editBudget,
            userId = userId,
            yearMonth = currentMonth,
            onDismiss = { showDialog = false; editBudget = null },
            onSave = { catName, maxAmt ->
                viewModel.saveBudget(userId, catName, maxAmt, currentMonth)
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
        if (budgets.isEmpty()) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
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
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets, key = { it.id }) { budget ->
                    BudgetCard(
                        budget = budget,
                        userId = userId,
                        viewModel = viewModel,
                        yearMonth = currentMonth,
                        onDelete = { deleteTarget = budget }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    budget: Budget,
    userId: Int,
    viewModel: BudgetViewModel,
    yearMonth: String,
    onDelete: () -> Unit
) {
    val category = TransactionCategory.entries.find { it.name == budget.categoryName }
    val spent by viewModel.getSpentForCategory(
        category ?: TransactionCategory.OTHER, yearMonth
    ).collectAsState(initial = 0.0)

    val progress = if (budget.maxAmount > 0) (spent / budget.maxAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val progressColor = when {
        progress >= 1f -> Color(0xFFC62828)
        progress >= 0.8f -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }
    val isOverBudget = spent > budget.maxAmount

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
                        Text("سقف: ${formatAmount(budget.maxAmount)} تومان",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("هزینه شده: ${formatAmount(spent)} ت",
                    color = progressColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("${(progress * 100).toInt()}%",
                    color = progressColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    budget: Budget?,
    userId: Int,
    yearMonth: String,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
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
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (budget == null) "تعریف بودجه جدید" else "ویرایش بودجه",
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                // Max amount
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it.filter { c -> c.isDigit() } },
                    label = { Text("سقف بودجه (تومان)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        else -> onSave(selectedCategory.name, amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
