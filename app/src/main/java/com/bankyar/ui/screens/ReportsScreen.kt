package com.bankyar.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    userId: Int,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val transactions by viewModel.transactions.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFormatDialog by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("csv") }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(buildCsv(transactions).toByteArray(Charsets.UTF_8))
            }
        }
    }

    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(buildTxt(transactions, stats.totalBalance, stats.totalIncome, stats.totalExpense).toByteArray(Charsets.UTF_8))
            }
        }
    }

    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("انتخاب فرمت خروجی", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("csv" to "CSV (اکسل)", "txt" to "TXT (متنی)").forEach { (fmt, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt })
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showFormatDialog = false
                    val fileName = "bankyar_report_${System.currentTimeMillis()}"
                    if (selectedFormat == "csv") csvLauncher.launch("$fileName.csv")
                    else txtLauncher.launch("$fileName.txt")
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("دانلود", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ showFormatDialog = false }) { Text("انصراف") } }
        )
    }

    val categoryExpenses = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(6)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("گزارشات", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton({ showFormatDialog = true }) {
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
            // Summary card
            item {
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("خلاصه مالی", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("${formatAmount(stats.totalBalance)} تومان",
                            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MiniStat("درآمد کل", stats.totalIncome, Color(0xFF81C784))
                            MiniStat("هزینه کل", stats.totalExpense, Color(0xFFEF9A9A))
                            MiniStat("تعداد", transactions.size.toDouble(), Color.White, isCount = true)
                        }
                    }
                }
            }

            // Donut chart for income vs expense
            if (stats.totalIncome > 0 || stats.totalExpense > 0) {
                item {
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("نسبت درآمد و هزینه", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IncomeExpenseDonut(
                                    income = stats.totalIncome,
                                    expense = stats.totalExpense
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DonutLegendItem("درآمد", stats.totalIncome, Color(0xFF66BB6A))
                                    DonutLegendItem("هزینه", stats.totalExpense, Color(0xFFEF5350))
                                    val balance = stats.totalIncome - stats.totalExpense
                                    DonutLegendItem(
                                        if (balance >= 0) "مازاد" else "کسری",
                                        kotlin.math.abs(balance),
                                        if (balance >= 0) Color(0xFF42A5F5) else Color(0xFFFF7043)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category breakdown
            if (categoryExpenses.isNotEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("هزینه‌ها بر اساس دسته‌بندی",
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(12.dp))
                            val maxAmount = categoryExpenses.maxOf { it.second }
                            categoryExpenses.forEach { (cat, amount) ->
                                CategoryBar(cat, amount, maxAmount, stats.totalExpense)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("همه تراکنش‌ها (${transactions.size})",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    TextButton({ showFormatDialog = true }) {
                        Icon(Icons.Default.FileDownload, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("دریافت فایل", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("هیچ تراکنشی یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(transactions, key = { it.id }) { t ->
                    TransactionItem(t, onClick = {})
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseDonut(income: Double, expense: Double) {
    val total = income + expense
    if (total == 0.0) return

    val incomeAngle = (income / total * 300f).toFloat()
    val expenseAngle = (expense / total * 300f).toFloat()
    val incomeColor = Color(0xFF66BB6A)
    val expenseColor = Color(0xFFEF5350)
    val gapAngle = 60f
    val startAngle = -150f + gapAngle / 2

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(140.dp)) {
            val strokeWidth = size.minDimension * 0.18f
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = incomeColor,
                startAngle = startAngle,
                sweepAngle = incomeAngle,
                useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = expenseColor,
                startAngle = startAngle + incomeAngle + 5f,
                sweepAngle = expenseAngle,
                useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val pct = (income / total * 100).toInt()
            Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text("درآمد", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DonutLegendItem(label: String, amount: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${formatAmount(amount)} ت", fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CategoryBar(
    category: TransactionCategory,
    amount: Double,
    maxAmount: Double,
    totalExpense: Double
) {
    val progress = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
    val pct = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.icon, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(category.label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$pct%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text("${formatAmount(amount)} ت", fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp, color = Color(0xFFEF5350))
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Color(0xFFEF5350),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun MiniStat(label: String, value: Double, color: Color, isCount: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(0.7f), fontSize = 11.sp)
        Text(
            if (isCount) value.toInt().toString() else "${formatAmount(value)} ت",
            color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp
        )
    }
}

private fun buildCsv(list: List<Transaction>): String {
    val sb = StringBuilder()
    sb.appendLine("تاریخ,عنوان,مبلغ,نوع,دسته‌بندی,حساب,توضیحات")
    list.forEach { t ->
        val type = when (t.type) { TransactionType.INCOME -> "درآمد"; TransactionType.EXPENSE -> "هزینه"; TransactionType.TRANSFER -> "انتقال" }
        val date = JalaliCalendar.toJalaliShort(t.date)
        sb.appendLine("$date,\"${t.title}\",${t.amount},$type,${t.category.label},\"${t.accountName}\",\"${t.description}\"")
    }
    return sb.toString()
}

private fun buildTxt(list: List<Transaction>, balance: Double, income: Double, expense: Double): String {
    val sb = StringBuilder()
    sb.appendLine("═══════════════════════════════════════════")
    sb.appendLine("          بانک‌یار - گزارش تراکنش‌ها")
    sb.appendLine("═══════════════════════════════════════════")
    sb.appendLine()
    sb.appendLine("موجودی کل:  ${formatAmount(balance)} تومان")
    sb.appendLine("درآمد کل:   ${formatAmount(income)} تومان")
    sb.appendLine("هزینه کل:   ${formatAmount(expense)} تومان")
    sb.appendLine("تعداد تراکنش: ${list.size}")
    sb.appendLine()
    sb.appendLine("───────────────────────────────────────────")
    list.forEach { t ->
        val type = when (t.type) { TransactionType.INCOME -> "درآمد ↑"; TransactionType.EXPENSE -> "هزینه ↓"; TransactionType.TRANSFER -> "انتقال ↔" }
        val prefix = when (t.type) { TransactionType.INCOME -> "+"; TransactionType.EXPENSE -> "-"; else -> "" }
        sb.appendLine("📅 ${JalaliCalendar.toJalaliString(t.date)}")
        sb.appendLine("📌 ${t.title}  |  $type")
        sb.appendLine("💰 $prefix${formatAmount(t.amount)} تومان")
        sb.appendLine("📂 ${t.category.label}  |  🏦 ${t.accountName}")
        if (t.description.isNotBlank()) sb.appendLine("📝 ${t.description}")
        sb.appendLine("───────────────────────────────────────────")
    }
    return sb.toString()
}
