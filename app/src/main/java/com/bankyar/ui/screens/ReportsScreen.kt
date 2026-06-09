package com.bankyar.ui.screens

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.AccountsViewModel
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    userId: Int,
    viewModel: TransactionViewModel,
    accountsViewModel: AccountsViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) {
        viewModel.setUser(userId)
        accountsViewModel.setUser(userId)
    }

    val transactions by viewModel.transactions.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val accounts by accountsViewModel.accounts.collectAsState()
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

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                val pdf = buildPdf(transactions, stats.totalBalance, stats.totalIncome, stats.totalExpense)
                pdf.writeTo(stream)
                pdf.close()
            }
        }
    }

    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("انتخاب فرمت خروجی", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("csv" to "CSV (اکسل)", "txt" to "TXT (متنی)", "pdf" to "PDF").forEach { (fmt, label) ->
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
                    when (selectedFormat) {
                        "csv" -> csvLauncher.launch("$fileName.csv")
                        "txt" -> txtLauncher.launch("$fileName.txt")
                        "pdf" -> pdfLauncher.launch("$fileName.pdf")
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("دانلود", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ showFormatDialog = false }) { Text("انصراف") } }
        )
    }

    val monthlyData = remember(transactions) { buildMonthlyData(transactions) }

    // Per-account stats computed from in-memory transactions
    data class AccountStat(val account: BankAccount, val income: Double, val expense: Double) {
        val currentBalance: Double get() = account.initialBalance + income - expense
    }
    val accountStats = remember(transactions, accounts) {
        accounts.map { acc ->
            val txsForAcc = transactions.filter { it.accountName == acc.title }
            val income = txsForAcc.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = txsForAcc.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            AccountStat(acc, income, expense)
        }
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

            // Bar chart section
            if (monthlyData.isNotEmpty()) {
                item {
                    Text("نمودار ماهانه", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                }
                item {
                    MonthlyBarChart(monthlyData.takeLast(6).reversed())
                }
            }

            // Per-account section
            if (accountStats.isNotEmpty()) {
                item {
                    Text("خلاصه به‌تفکیک حساب", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                }
                items(accountStats) { stat ->
                    AccountReportCard(stat.account, stat.income, stat.expense, stat.currentBalance)
                }
            }

            // Monthly profit/loss section
            item {
                Text("سود و زیان ماهانه", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp))
            }

            if (monthlyData.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("داده‌ای برای نمایش وجود ندارد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(monthlyData) { month ->
                    MonthlyProfitCard(month)
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween,
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

data class MonthlyData(
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val profit: Double
)

private fun buildMonthlyData(transactions: List<Transaction>): List<MonthlyData> {
    val grouped = transactions.groupBy { t ->
        // Format: day/mm/yyyy → extract mm/yyyy as the group key
        val jalali = JalaliCalendar.toJalaliShort(t.date)
        jalali.substring(jalali.indexOf('/') + 1)
    }
    return grouped.entries
        .sortedByDescending { it.key }
        .map { (month, txList) ->
            val income = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            MonthlyData(month, income, expense, income - expense)
        }
}

@Composable
private fun MonthlyProfitCard(data: MonthlyData) {
    val isProfit = data.profit >= 0
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(data.monthLabel, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("درآمد: ${formatAmount(data.income)} ت",
                        color = Color(0xFF2E7D32), fontSize = 11.sp)
                    Text("هزینه: ${formatAmount(data.expense)} ت",
                        color = Color(0xFFC62828), fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isProfit) "+" else ""}${formatAmount(data.profit)} ت",
                    color = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Text(if (isProfit) "سود" else "زیان",
                    color = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontSize = 11.sp)
            }
        }
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

@Composable
private fun MonthlyBarChart(months: List<MonthlyData>) {
    val maxValue = months.maxOfOrNull { maxOf(it.income, it.expense) }.takeIf { it != null && it > 0 } ?: 1.0
    val incomeColor = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFC62828)
    val barWidth = 18.dp

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Legend
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(incomeColor))
                    Spacer(Modifier.width(4.dp))
                    Text("درآمد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(expenseColor))
                    Spacer(Modifier.width(4.dp))
                    Text("هزینه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))

            val chartHeight = 120.dp
            Row(
                Modifier.fillMaxWidth().height(chartHeight + 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEach { m ->
                    val incomeRatio = (m.income / maxValue).coerceIn(0.0, 1.0).toFloat()
                    val expenseRatio = (m.expense / maxValue).coerceIn(0.0, 1.0).toFloat()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.height(chartHeight + 24.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(chartHeight)
                        ) {
                            // Income bar
                            Box(
                                Modifier.width(barWidth)
                                    .fillMaxHeight(incomeRatio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(incomeColor)
                            )
                            // Expense bar
                            Box(
                                Modifier.width(barWidth)
                                    .fillMaxHeight(expenseRatio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(expenseColor)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        // Month label - show only mm/yy
                        val shortLabel = m.monthLabel.take(5)
                        Text(shortLabel, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountReportCard(account: BankAccount, income: Double, expense: Double, currentBalance: Double) {
    val isPositive = currentBalance >= 0
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(account.title, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                if (account.isDefault) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("پیش‌فرض", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp) }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("موجودی اولیه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("${formatAmount(account.initialBalance)} تومان",
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
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
        val type = when (t.type) { TransactionType.INCOME -> "درآمد"; TransactionType.EXPENSE -> "هزینه"; TransactionType.TRANSFER -> "انتقال" }
        val prefix = when (t.type) { TransactionType.INCOME -> "+"; TransactionType.EXPENSE -> "-"; else -> "" }
        sb.appendLine("${JalaliCalendar.toJalaliString(t.date)}")
        sb.appendLine("${t.title}  |  $type")
        sb.appendLine("$prefix${formatAmount(t.amount)} تومان")
        sb.appendLine("${t.category.label}  |  ${t.accountName}")
        if (t.description.isNotBlank()) sb.appendLine("${t.description}")
        sb.appendLine("───────────────────────────────────────────")
    }
    return sb.toString()
}

private fun buildPdf(list: List<Transaction>, balance: Double, income: Double, expense: Double): PdfDocument {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
    val lineHeight = 20f

    val titlePaint = Paint().apply {
        color = AndroidColor.rgb(26, 115, 232)
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val headerPaint = Paint().apply {
        color = AndroidColor.rgb(26, 115, 232)
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 11f
        isAntiAlias = true
    }
    val greenPaint = Paint().apply { color = AndroidColor.rgb(46, 125, 50); textSize = 11f; isAntiAlias = true }
    val redPaint = Paint().apply { color = AndroidColor.rgb(198, 40, 40); textSize = 11f; isAntiAlias = true }
    val dividerPaint = Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 1f }

    var pageNum = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
    var page = document.startPage(pageInfo)
    var canvas: Canvas = page.canvas
    var y = margin + 20f

    canvas.drawText("بانک‌یار - گزارش تراکنش‌ها", pageWidth / 2f, y, titlePaint)
    y += lineHeight * 1.5f
    canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
    y += lineHeight

    canvas.drawText("موجودی: ${formatAmount(balance)} تومان", margin, y, headerPaint)
    y += lineHeight
    canvas.drawText("درآمد کل: ${formatAmount(income)} تومان", margin, y, greenPaint)
    y += lineHeight
    canvas.drawText("هزینه کل: ${formatAmount(expense)} تومان", margin, y, redPaint)
    y += lineHeight
    canvas.drawText("تعداد تراکنش: ${list.size}", margin, y, bodyPaint)
    y += lineHeight * 1.5f
    canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
    y += lineHeight

    for (t in list) {
        if (y > pageHeight - margin * 2) {
            document.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = margin + 20f
        }
        val type = when (t.type) { TransactionType.INCOME -> "درآمد"; TransactionType.EXPENSE -> "هزینه"; TransactionType.TRANSFER -> "انتقال" }
        val prefix = when (t.type) { TransactionType.INCOME -> "+"; TransactionType.EXPENSE -> "-"; else -> "" }
        val amountPaint = when (t.type) { TransactionType.INCOME -> greenPaint; TransactionType.EXPENSE -> redPaint; else -> bodyPaint }

        canvas.drawText("${JalaliCalendar.toJalaliShort(t.date)}  |  ${t.title}  |  $type", margin, y, bodyPaint)
        y += lineHeight
        canvas.drawText("$prefix${formatAmount(t.amount)} تومان  |  ${t.category.label}  |  ${t.accountName}", margin, y, amountPaint)
        y += lineHeight
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += lineHeight * 0.5f
    }

    document.finishPage(page)
    return document
}
