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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.ThousandSeparatorVisualTransformation
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.AccountsViewModel
import com.bankyar.ui.viewmodels.BudgetViewModel
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    userId: Int,
    editId: Int = -1,
    viewModel: TransactionViewModel,
    accountsViewModel: AccountsViewModel,
    budgetViewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) {
        accountsViewModel.setUser(userId)
        budgetViewModel.setUser(userId)
    }

    var existing by remember { mutableStateOf<Transaction?>(null) }
    LaunchedEffect(editId) { if (editId > 0) existing = viewModel.getById(editId) }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(TransactionCategory.OTHER) }
    var description by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showJalaliPicker by remember { mutableStateOf(false) }
    var showBudgetWarning by remember { mutableStateOf<String?>(null) }

    val accounts by accountsViewModel.accounts.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(accounts) {
        if (editId <= 0 && accountName.isEmpty()) {
            accountName = accounts.firstOrNull { it.isDefault }?.title
                ?: accounts.firstOrNull()?.title
                ?: ""
        }
    }

    LaunchedEffect(existing) {
        existing?.let {
            title = it.title; amountText = it.amount.toLong().toString()
            type = it.type; category = it.category
            description = it.description; accountName = it.accountName
            selectedDateMillis = it.date
        }
    }

    if (showJalaliPicker) {
        JalaliDatePickerDialog(
            currentMillis = selectedDateMillis,
            onConfirm = { millis -> selectedDateMillis = millis; showJalaliPicker = false },
            onDismiss = { showJalaliPicker = false }
        )
    }

    val isEdit = editId > 0
    val typeFg = when (type) {
        TransactionType.INCOME -> Color(0xFF2E7D32)
        TransactionType.EXPENSE -> Color(0xFFC62828)
        TransactionType.TRANSFER -> Color(0xFF1565C0)
    }

    // Extract actual save logic into a local lambda
    fun doSave() {
        val amount = amountText.toDoubleOrNull() ?: return
        error = null
        val tr = Transaction(
            id = if (isEdit) editId else 0,
            userId = userId, title = title, amount = amount,
            type = type, category = category,
            description = description, accountName = accountName,
            date = selectedDateMillis
        )
        if (isEdit) viewModel.updateTransaction(tr) else viewModel.addTransaction(tr)
        onBack()
    }

    if (showBudgetWarning != null) {
        AlertDialog(
            onDismissRequest = { showBudgetWarning = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100)) },
            title = { Text("هشدار سقف بودجه", fontWeight = FontWeight.Bold) },
            text = { Text(showBudgetWarning!!, lineHeight = 22.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showBudgetWarning = null
                        doSave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ادامه و ثبت", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ showBudgetWarning = null }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "ویرایش تراکنش" else "تراکنش جدید", fontWeight = FontWeight.Bold) },
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
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (accounts.isEmpty() && !isEdit) {
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null,
                            tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("حسابی ثبت نشده است. لطفاً ابتدا از بخش مدیریت حساب‌ها یک حساب اضافه کنید.",
                            color = Color(0xFFE65100), fontSize = 13.sp)
                    }
                }
            }
            // Type
            FormCard("نوع تراکنش") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.values().forEach { t ->
                        val (label, fg, bg) = when (t) {
                            TransactionType.INCOME -> Triple("درآمد", Color(0xFF2E7D32), Color(0xFFE8F5E9))
                            TransactionType.EXPENSE -> Triple("هزینه", Color(0xFFC62828), Color(0xFFFFEBEE))
                            TransactionType.TRANSFER -> Triple("انتقال", Color(0xFF1565C0), Color(0xFFE3F2FD))
                        }
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (type == t) fg else bg)
                                .clickable {
                                    type = t
                                    if (t == TransactionType.TRANSFER) category = TransactionCategory.TRANSFER
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label,
                                color = if (type == t) Color.White else fg,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Amount
            FormCard("مبلغ") {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ (تومان)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = typeFg) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    suffix = { Text("تومان", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            // Title
            FormCard("عنوان") {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("عنوان تراکنش") },
                    leadingIcon = { Icon(Icons.Default.Title, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )
            }

            // Category
            FormCard("دسته‌بندی") {
                val cats = when (type) {
                    TransactionType.INCOME -> listOf(TransactionCategory.SALARY, TransactionCategory.INVESTMENT, TransactionCategory.OTHER)
                    TransactionType.TRANSFER -> listOf(TransactionCategory.TRANSFER)
                    else -> TransactionCategory.values().filter { it != TransactionCategory.SALARY && it != TransactionCategory.TRANSFER }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { cat ->
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (category == cat)
                                            MaterialTheme.colorScheme.primary.copy(0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp,
                                            if (category == cat) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(10.dp))
                                        .clickable { category = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(cat.icon, fontSize = 18.sp)
                                        Text(cat.label, fontSize = 10.sp,
                                            color = if (category == cat) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Account picker
            FormCard("حساب بانکی") {
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("انتخاب حساب") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(acc.title, fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        if (acc.bankName.isNotBlank())
                                            Text(acc.bankName, fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = { accountName = acc.title; accountDropdownExpanded = false }
                            )
                        }
                    }
                }
            }

            // Date picker
            FormCard("تاریخ تراکنش") {
                OutlinedTextField(
                    value = JalaliCalendar.toJalaliShort(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاریخ") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showJalaliPicker = true }) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )
            }

            // Description
            FormCard("توضیحات") {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    leadingIcon = { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    colors = fieldColors()
                )
            }

            error?.let { err ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                }
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    when {
                        title.isBlank() -> error = "عنوان را وارد کنید"
                        amount == null || amount <= 0 -> error = "مبلغ معتبر وارد کنید"
                        else -> {
                            error = null
                            if (type != TransactionType.INCOME) {
                                // Check budget before saving
                                scope.launch {
                                    val currentMonth = budgetViewModel.currentYearMonth()
                                    val budgetList = budgetViewModel.getBudgetsForMonthDirect(currentMonth)
                                    val matchingBudget = budgetList.find { b ->
                                        b.categoryName == category.name &&
                                        (b.accountName == null || b.accountName == accountName)
                                    }
                                    if (matchingBudget != null) {
                                        val spent = budgetViewModel.getSpentForCategoryDirect(
                                            category, currentMonth, matchingBudget.accountName
                                        )
                                        val accLabel = if (matchingBudget.accountName != null) " (${matchingBudget.accountName})" else ""
                                        when {
                                            spent >= matchingBudget.maxAmount -> {
                                                showBudgetWarning = "سقف بودجه دسته‌بندی «${category.label}»$accLabel تکمیل شده است.\n\nهزینه شده: ${formatAmount(spent)} تومان\nسقف: ${formatAmount(matchingBudget.maxAmount)} تومان\n\nآیا با وجود این، تراکنش را ثبت می‌کنید؟"
                                                return@launch
                                            }
                                            spent + amount > matchingBudget.maxAmount -> {
                                                showBudgetWarning = "این تراکنش سقف بودجه دسته‌بندی «${category.label}»$accLabel را رد می‌کند.\n\nهزینه شده: ${formatAmount(spent)} تومان\nاین تراکنش: ${formatAmount(amount)} تومان\nسقف: ${formatAmount(matchingBudget.maxAmount)} تومان\n\nآیا ادامه می‌دهید؟"
                                                return@launch
                                            }
                                        }
                                    }
                                    doSave()
                                }
                            } else {
                                doSave()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = typeFg)
            ) {
                Icon(if (isEdit) Icons.Default.Save else Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (isEdit) "ذخیره تغییرات" else "ثبت تراکنش",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JalaliDatePickerDialog(
    currentMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val jalali = JalaliCalendar.toJalaliShort(currentMillis)
    val parts = jalali.split("/")
    var dayText by remember { mutableStateOf(parts.getOrNull(0) ?: "1") }
    var selectedMonth by remember { mutableStateOf((parts.getOrNull(1)?.toIntOrNull() ?: 1)) }
    var yearText by remember { mutableStateOf(parts.getOrNull(2) ?: "1403") }
    var monthExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ شمسی", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("سال") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = it }) {
                    OutlinedTextField(
                        value = JalaliCalendar.monthNames[selectedMonth - 1],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ماه") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors()
                    )
                    ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                        JalaliCalendar.monthNames.forEachIndexed { idx, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedMonth = idx + 1; monthExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("روز") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                if (errorMsg.isNotBlank())
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val jy = yearText.toIntOrNull() ?: 0
                val jm = selectedMonth
                val jd = dayText.toIntOrNull() ?: 0
                when {
                    jy < 1300 || jy > 1500 -> errorMsg = "سال نامعتبر است"
                    jd < 1 || jd > 31 -> errorMsg = "روز نامعتبر است"
                    else -> {
                        errorMsg = ""
                        onConfirm(JalaliCalendar.jalaliToMillis(jy, jm, jd))
                    }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("تأیید", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
