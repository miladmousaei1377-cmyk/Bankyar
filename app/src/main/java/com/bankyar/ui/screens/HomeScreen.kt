package com.bankyar.ui.screens

import android.app.Activity
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.R
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.theme.*
import com.bankyar.ui.viewmodels.TransactionViewModel
import com.bankyar.util.JalaliCalendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: Int,
    userName: String,
    isDarkMode: Boolean,
    viewModel: TransactionViewModel,
    onToggleDarkMode: () -> Unit,
    onAddTransaction: () -> Unit,
    onViewAll: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    onProfile: () -> Unit,
    onAccounts: () -> Unit,
    onReports: () -> Unit,
    onAbout: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val stats by viewModel.stats.collectAsState()
    val recent by viewModel.recentTransactions.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDeleteAllDialog by remember { mutableStateOf(false) }
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
                // Header
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
                    scope.launch { drawerState.close() }; onProfile()
                }
                DrawerItem(Icons.Default.AccountBalance, "مدیریت حساب‌ها") {
                    scope.launch { drawerState.close() }; onAccounts()
                }
                DrawerItem(Icons.Default.BarChart, "گزارشات") {
                    scope.launch { drawerState.close() }; onReports()
                }
                DrawerItem(Icons.Default.Settings, "تنظیمات") {
                    scope.launch { drawerState.close() }; onSettings()
                }
                DrawerItem(Icons.Default.Info, "درباره ما") {
                    scope.launch { drawerState.close() }; onAbout()
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(0.3f))

                DrawerItem(Icons.Default.ExitToApp, "خروج از نرم‌افزار", tint = MaterialTheme.colorScheme.error) {
                    scope.launch { drawerState.close() }
                    (context as? Activity)?.finish()
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
                    // TopBar
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right side: menu icon
                        IconButton({ scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("خوش آمدید", color = Color.White.copy(0.8f), fontSize = 12.sp)
                            Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        // Left side: dark mode toggle
                        IconButton(onToggleDarkMode) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                null, tint = Color.White, modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                item {
                    // Balance card
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("موجودی کل", color = Color.White.copy(0.8f), fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("${formatAmount(stats.totalBalance)} تومان",
                                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BalanceChip("درآمد", stats.totalIncome, Color(0xFF81C784))
                                BalanceChip("هزینه", stats.totalExpense, Color(0xFFEF9A9A))
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
private fun BalanceChip(label: String, amount: Double, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.15f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(0.8f), fontSize = 11.sp)
            Text("${formatAmount(amount)} ت", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
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
