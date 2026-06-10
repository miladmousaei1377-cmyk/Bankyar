package com.bankyar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.viewmodels.DateRange
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Int) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    val filtered = if (filterType == null) transactions else transactions.filter { it.type == filterType }

    if (showDateDialog) {
        DateRangeDialog(
            current = dateRange,
            onApply = { viewModel.setDateRange(it); showDateDialog = false },
            onDismiss = { showDateDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("همه تراکنش‌ها", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton({ showDateDialog = true }) {
                        Icon(
                            Icons.Default.DateRange, null,
                            tint = if (dateRange != null) Color(0xFFFFE082) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("جستجو در تراکنش‌ها...", color = MaterialTheme.colorScheme.outline) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton({ viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true
            )

            if (dateRange != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "از ${JalaliCalendar.toJalaliShort(dateRange!!.from)} تا ${JalaliCalendar.toJalaliShort(dateRange!!.to)}",
                            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = { viewModel.setDateRange(null) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "همه", TransactionType.INCOME to "درآمد",
                    TransactionType.EXPENSE to "هزینه", TransactionType.TRANSFER to "انتقال"
                ).forEach { (type, label) ->
                    val isSelected = filterType == type
                    val (bg, fg) = when {
                        !isSelected -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        type == TransactionType.INCOME -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        type == TransactionType.EXPENSE -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                        type == TransactionType.TRANSFER -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(bg)
                            .clickable { filterType = type }.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = fg, fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("${filtered.size} تراکنش", color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(4.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("نتیجه‌ای یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(filtered, key = { it.id }) { t ->
                        TransactionItem(t, onClick = { onTransactionClick(t.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    current: DateRange?,
    onApply: (DateRange?) -> Unit,
    onDismiss: () -> Unit
) {
    val todayMs = System.currentTimeMillis()
    val fromState = rememberDatePickerState(
        initialSelectedDateMillis = current?.from ?: run {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    )
    val toState = rememberDatePickerState(initialSelectedDateMillis = current?.to ?: todayMs)
    var step by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (step == 0) "انتخاب تاریخ شروع" else "انتخاب تاریخ پایان",
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                if (step == 0) {
                    DatePicker(state = fromState, showModeToggle = false)
                } else {
                    DatePicker(state = toState, showModeToggle = false)
                    error?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 0) {
                        step = 1
                    } else {
                        val from = fromState.selectedDateMillis
                        val to = (toState.selectedDateMillis ?: todayMs) + 86_399_999L
                        if (from == null) { error = "تاریخ شروع را انتخاب کنید"; return@Button }
                        if (to < from) { error = "تاریخ پایان باید بعد از شروع باشد"; return@Button }
                        onApply(DateRange(from, to))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (step == 0) "بعدی" else "اعمال", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onApply(null) }) {
                        Text("حذف فیلتر", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { if (step == 1) step = 0 else onDismiss() }) {
                    Text(if (step == 1) "قبلی" else "انصراف")
                }
            }
        }
    )
}
