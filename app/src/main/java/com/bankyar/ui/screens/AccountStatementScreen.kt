package com.bankyar.ui.screens

import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.AccountsViewModel
import com.bankyar.ui.viewmodels.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatementScreen(
    userId: Int,
    accountName: String,
    viewModel: TransactionViewModel,
    accountsViewModel: AccountsViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) {
        viewModel.setUser(userId)
        accountsViewModel.setUser(userId)
    }

    val transactions by viewModel.transactions.collectAsState()
    val accounts by accountsViewModel.accounts.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("all") }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("csv") }

    val account = accounts.find { it.title == accountName }
    val accountTx = remember(transactions, accountName) {
        transactions.filter { it.accountName == accountName }.sortedByDescending { it.date }
    }
    val income = remember(accountTx) { accountTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
    val expense = remember(accountTx) { accountTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
    val transfer = remember(accountTx) { accountTx.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount } }
    val initialBalance = account?.initialBalance ?: 0.0
    val currentBalance = initialBalance + income - expense - transfer
    val isPositive = currentBalance >= 0

    val displayedTx = remember(accountTx, selectedFilter) {
        when (selectedFilter) {
            "income" -> accountTx.filter { it.type == TransactionType.INCOME }
            "expense" -> accountTx.filter { it.type == TransactionType.EXPENSE }
            "transfer" -> accountTx.filter { it.type == TransactionType.TRANSFER }
            else -> accountTx
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(buildCsv(displayedTx).toByteArray(Charsets.UTF_8))
            }
        }
    }
    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            val inc = displayedTx.filter { t -> t.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = displayedTx.filter { t -> t.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val tr = displayedTx.filter { t -> t.type == TransactionType.TRANSFER }.sumOf { it.amount }
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(buildTxt(displayedTx, inc - exp - tr, inc, exp).toByteArray(Charsets.UTF_8))
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            val inc = displayedTx.filter { t -> t.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = displayedTx.filter { t -> t.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val tr = displayedTx.filter { t -> t.type == TransactionType.TRANSFER }.sumOf { it.amount }
            context.contentResolver.openOutputStream(it)?.use { stream ->
                val pdf: PdfDocument = buildPdf(displayedTx, inc - exp - tr, inc, exp)
                pdf.writeTo(stream); pdf.close()
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("دریافت گزارش حساب", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("فرمت فایل", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    listOf("csv" to "CSV (اکسل)", "txt" to "TXT (متنی)", "pdf" to "PDF").forEach { (fmt, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = selectedExportFormat == fmt,
                                onClick = { selectedExportFormat = fmt })
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showExportDialog = false
                    val fn = "bankyar_${accountName}_${System.currentTimeMillis()}"
                    when (selectedExportFormat) {
                        "csv" -> csvLauncher.launch("$fn.csv")
                        "txt" -> txtLauncher.launch("$fn.txt")
                        "pdf" -> pdfLauncher.launch("$fn.pdf")
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("دانلود", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ showExportDialog = false }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountName, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton({ showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, null, tint = Color.White)
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
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Account summary card
            item {
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(accountName, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            account?.bankName?.takeIf { it.isNotBlank() }?.let { bank ->
                                Spacer(Modifier.width(8.dp))
                                Text(bank, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("موجودی اولیه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("${formatAmount(initialBalance)} تومان",
                                    color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("درآمد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("+${formatAmount(income)} تومان",
                                    color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("هزینه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("-${formatAmount(expense)} تومان",
                                    color = Color(0xFFC62828), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("موجودی فعلی:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                "${formatAmount(currentBalance)} تومان",
                                color = if (isPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Filter chips
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("all" to "همه", "income" to "درآمد", "expense" to "هزینه", "transfer" to "انتقال")
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                }
            }

            item {
                Text(
                    "تراکنش‌های این حساب (${displayedTx.size})",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (displayedTx.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("تراکنشی برای نمایش وجود ندارد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(displayedTx, key = { it.id }) { t ->
                    TransactionItem(t, onClick = {})
                }
            }
        }
    }
}
