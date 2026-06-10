package com.bankyar.ui.screens

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.R
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.entities.BankAccount
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.AccountsViewModel
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userId: Int,
    userName: String,
    isDarkMode: Boolean,
    viewModel: TransactionViewModel,
    accountsViewModel: AccountsViewModel,
    onToggleDarkMode: () -> Unit,
    onAddTransaction: () -> Unit,
    onViewAll: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    onProfile: () -> Unit,
    onAccounts: () -> Unit,
    onReports: () -> Unit,
    onAbout: () -> Unit,
    onSettings: () -> Unit,
    onBudget: () -> Unit = {},
    onDebts: () -> Unit = {},
    onRecurring: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(userId) {
        viewModel.setUser(userId)
        accountsViewModel.setUser(userId)
    }

    val recent by viewModel.recentTransactions.collectAsState()
    val message by viewModel.message.collectAsState()
    val accounts by accountsViewModel.accounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val prefs = remember { PreferencesManager(context) }
    val hasSeenWelcome by prefs.hasSeenWelcome.collectAsState(initial = true)
    var showWelcomeDialog by remember { mutableStateOf(false) }
    LaunchedEffect(hasSeenWelcome) {
        if (!hasSeenWelcome) showWelcomeDialog = true
    }
    if (showWelcomeDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("خوش آمدید به بانک‌یار", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WelcomeTip(Icons.Default.Settings, "تنظیمات اثر انگشت",
                        "برای فعال‌سازی ورود با اثر انگشت به صفحه تنظیمات بروید.")
                    WelcomeTip(Icons.Default.Backup, "پشتیبان‌گیری خودکار",
                        "پشتیبان‌گیری خودکار هر ۳۰ دقیقه در تنظیمات قابل فعال‌سازی است.")
                    WelcomeTip(Icons.Default.AccountBalance, "مدیریت حساب‌ها",
                        "از منوی کشویی گزینه «حساب‌های بانکی» را انتخاب کنید تا حساب‌های خود را مدیریت کنید.")
                    WelcomeTip(Icons.Default.NotificationsActive, "یادآور ثبت تراکنش",
                        "از بخش تنظیمات می‌توانید یادآور روزانه فعال کنید تا هر روز در ساعت دلخواه نوتیفیکیشن دریافت کنید.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showWelcomeDialog = false
                    scope.launch { prefs.markWelcomeSeen() }
                }) {
                    Text("متوجه شدم")
                }
            }
        )
    }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("حذف همه تراکنش‌ها") },
            text = { Text("آیا از حذف تمام تراکنش‌ها اطمینان دارید؟ این عملیات قابل بازگشت نیست.") },
            confirmButton = {
                TextButton({
                    viewModel.deleteAllTransactions()
                    showDeleteAllDialog = false
                    selectionMode = false
                    selectedIds = emptySet()
                }) {
                    Text("حذف همه", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ showDeleteAllDialog = false }) { Text("انصراف") } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp)
                ) {
                    Column {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = "بانک‌یار",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("بانک‌یار", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                DrawerItem(Icons.Default.Person, "پروفایل") {
                    scope.launch { drawerState.close(); onProfile() }
                }
                DrawerItem(Icons.Default.AccountBalance, "مدیریت حساب‌ها") {
                    scope.launch { drawerState.close(); onAccounts() }
                }
                DrawerItem(Icons.Default.BarChart, "گزارشات") {
                    scope.launch { drawerState.close(); onReports() }
                }
                DrawerItem(Icons.Default.Savings, "بودجه‌بندی") {
                    scope.launch { drawerState.close(); onBudget() }
                }
                DrawerItem(Icons.Default.AccountBalanceWallet, "بدهی و طلب") {
                    scope.launch { drawerState.close(); onDebts() }
                }
                DrawerItem(Icons.Default.Repeat, "تراکنش‌های تکرارشونده") {
                    scope.launch { drawerState.close(); onRecurring() }
                }
                DrawerItem(Icons.Default.Settings, "تنظیمات") {
                    scope.launch { drawerState.close(); onSettings() }
                }
                DrawerItem(Icons.Default.Info, "درباره ما") {
                    scope.launch { drawerState.close(); onAbout() }
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(0.3f))

                DrawerItem(Icons.Default.ExitToApp, "خروج از نرم‌افزار", tint = MaterialTheme.colorScheme.error) {
                    scope.launch { drawerState.close(); (context as? Activity)?.finish() }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("تراکنش جدید", fontWeight = FontWeight.SemiBold) }
                )
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton({ scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("خوش آمدید", color = Color.White.copy(0.8f), fontSize = 12.sp)
                            Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onToggleDarkMode) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                null, tint = Color.White, modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Account cards pager
                item {
                    val sortedAccounts = remember(accounts) { accounts.sortedByDescending { it.isDefault } }

                    if (sortedAccounts.isEmpty()) {
                        Card(
                            Modifier.fillMaxWidth().padding(16.dp).clickable { onAccounts() },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier.size(56.dp).clip(CircleShape)
                                        .background(Color.White.copy(0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Text("حسابی ثبت نشده", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("برای اضافه کردن حساب کلیک کنید", color = Color.White.copy(0.75f), fontSize = 13.sp)
                            }
                        }
                    } else {
                        val pagerState = rememberPagerState(pageCount = { sortedAccounts.size })
                        Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                pageSpacing = 10.dp
                            ) { page ->
                                AccountBalanceCard(
                                    acc = sortedAccounts[page],
                                    userId = userId,
                                    accountsViewModel = accountsViewModel
                                )
                            }
                            if (sortedAccounts.size > 1) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(sortedAccounts.size) { i ->
                                        Box(
                                            Modifier.padding(horizontal = 3.dp)
                                                .size(
                                                    width = if (pagerState.currentPage == i) 18.dp else 8.dp,
                                                    height = 8.dp
                                                )
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (pagerState.currentPage == i)
                                                        MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.primary.copy(0.3f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("تراکنش‌های اخیر", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 4.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onViewAll, modifier = Modifier.weight(1f)) {
                                Text("مشاهده همه", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    selectionMode = !selectionMode
                                    if (!selectionMode) selectedIds = emptySet()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (selectionMode) "لغو انتخاب" else "انتخاب تکی",
                                    color = if (selectionMode) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (selectionMode && selectedIds.isNotEmpty()) {
                                        selectedIds.forEach { id ->
                                            recent.find { it.id == id }?.let { viewModel.deleteTransaction(it) }
                                        }
                                        selectedIds = emptySet()
                                        selectionMode = false
                                    } else {
                                        showDeleteAllDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (selectionMode && selectedIds.isNotEmpty()) "حذف انتخاب‌ها" else "حذف همه",
                                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (recent.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("هیچ تراکنشی ثبت نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(recent) { t ->
                        if (selectionMode) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(t.id),
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + t.id else selectedIds - t.id
                                    }
                                )
                                Box(Modifier.weight(1f)) {
                                    TransactionItem(t, onClick = {
                                        selectedIds = if (selectedIds.contains(t.id))
                                            selectedIds - t.id else selectedIds + t.id
                                    })
                                }
                            }
                        } else {
                            TransactionItem(t, onClick = { onTransactionClick(t.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceCard(acc: BankAccount, userId: Int, accountsViewModel: AccountsViewModel) {
    val netBalance by accountsViewModel.getNetBalance(userId, acc.title).collectAsState(initial = 0.0)
    val currentBalance = acc.initialBalance + netBalance
    val isPositive = currentBalance >= 0

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, null,
                        tint = Color.White.copy(0.9f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(acc.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (acc.isDefault) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text("پیش‌فرض", color = Color.White, fontSize = 10.sp) }
                }
            }

            if (acc.bankName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(acc.bankName, color = Color.White.copy(0.7f), fontSize = 12.sp)
            }

            if (acc.cardNumber.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(maskCardNumber(acc.cardNumber), color = Color.White.copy(0.8f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))
            Text("موجودی فعلی", color = Color.White.copy(0.75f), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatAmount(currentBalance)} تومان",
                color = if (isPositive) Color.White else Color(0xFFFFCDD2),
                fontWeight = FontWeight.Bold, fontSize = 22.sp
            )
        }
    }
}

private fun maskCardNumber(cardNumber: String): String {
    val digits = cardNumber.filter { it.isDigit() }
    return when {
        digits.length == 16 -> "${digits.take(4)}-****-****-${digits.takeLast(4)}"
        digits.length >= 4 -> "****-${digits.takeLast(4)}"
        else -> cardNumber
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector, label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, null, tint = tint) },
        label = { Text(label, color = tint, fontWeight = FontWeight.Medium) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        )
    )
}

@Composable
fun TransactionItem(t: Transaction, onClick: () -> Unit) {
    val (bg, fg) = when (t.type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.surface to Color(0xFF2E7D32)
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.surface to Color(0xFFC62828)
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.surface to Color(0xFF1565C0)
    }
    val amountPrefix = when (t.type) { TransactionType.INCOME -> "+"; TransactionType.EXPENSE -> "-"; else -> "" }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(when (t.type) {
                        TransactionType.INCOME -> Color(0xFFE8F5E9)
                        TransactionType.EXPENSE -> Color(0xFFFFEBEE)
                        TransactionType.TRANSFER -> Color(0xFFE3F2FD)
                    }),
                contentAlignment = Alignment.Center
            ) { Text(t.category.icon, fontSize = 20.sp) }

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(t.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (t.description.isNotBlank())
                    Text(t.description, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$amountPrefix${formatAmount(t.amount)} ت",
                    color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(JalaliCalendar.toJalaliShort(t.date),
                    color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WelcomeTip(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
