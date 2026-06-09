package com.bankyar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar

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
    val filterCategory by viewModel.filterCategory.collectAsState()
    val filterMonth by viewModel.filterYearMonth.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var showFilterPanel by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    val filtered = if (filterType == null) transactions else transactions.filter { it.type == filterType }

    // Collect unique months from transactions
    val availableMonths = remember(transactions) {
        transactions.map { t ->
            val j = JalaliCalendar.toJalaliShort(t.date)
            j.substring(j.indexOf('/') + 1)
        }.distinct().sortedDescending()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("همه تراکنش‌ها", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton({ showFilterPanel = !showFilterPanel }) {
                        Icon(
                            if (filterCategory != null || filterMonth != null) Icons.Default.FilterAlt else Icons.Default.FilterList,
                            null, tint = if (filterCategory != null || filterMonth != null) Color.Yellow else Color.White
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

            // Type filter chips
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(null to "همه", TransactionType.INCOME to "درآمد",
                    TransactionType.EXPENSE to "هزینه", TransactionType.TRANSFER to "انتقال")) { (type, label) ->
                    val isSelected = filterType == type
                    val (bg, fg) = when {
                        !isSelected -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        type == TransactionType.INCOME -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        type == TransactionType.EXPENSE -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                        type == TransactionType.TRANSFER -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                    }
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(bg)
                        .clickable { filterType = type }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(label, color = fg, fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            // Advanced filter panel
            if (showFilterPanel) {
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category filter
                    Text("دسته‌بندی", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = filterCategory == null,
                                onClick = { viewModel.setFilterCategory(null) },
                                label = { Text("همه", fontSize = 12.sp) })
                        }
                        items(TransactionCategory.entries.toTypedArray()) { cat ->
                            FilterChip(selected = filterCategory == cat,
                                onClick = { viewModel.setFilterCategory(if (filterCategory == cat) null else cat) },
                                label = { Text("${cat.icon} ${cat.label}", fontSize = 12.sp) })
                        }
                    }

                    // Month filter
                    if (availableMonths.isNotEmpty()) {
                        Text("ماه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(selected = filterMonth == null,
                                    onClick = { viewModel.setFilterYearMonth(null) },
                                    label = { Text("همه ماه‌ها", fontSize = 12.sp) })
                            }
                            items(availableMonths) { month ->
                                FilterChip(selected = filterMonth == month,
                                    onClick = { viewModel.setFilterYearMonth(if (filterMonth == month) null else month) },
                                    label = { Text(month, fontSize = 12.sp) })
                            }
                        }
                    }

                    if (filterCategory != null || filterMonth != null) {
                        TextButton(onClick = {
                            viewModel.setFilterCategory(null)
                            viewModel.setFilterYearMonth(null)
                        }) { Text("پاک کردن فیلترها", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
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
