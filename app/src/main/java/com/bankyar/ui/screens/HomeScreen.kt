package com.bankyar.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onBudget: () -> Unit,
    onAbout: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val stats by viewModel.stats.collectAsState()
    val recent by viewModel.recentTransactions.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                // Header
                Box(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp)
                ) {
                    Column {
                        Box(
                            Modifier.size(60.dp).clip(CircleShape)
                                .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userName.firstOrNull()?.toString() ?: "؟",
                                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
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
                DrawerItem(Icons.Default.Savings, "مدیریت بودجه") {
                    scope.launch { drawerState.close() }; onBudget()
                }
                DrawerItem(Icons.Default.Info, "درباره ما") {
                    scope.launch { drawerState.close() }; onAbout()
                }
                DrawerItem(Icons.Default.Settings, "تنظیمات") {
                    scope.launch { drawerState.close() }; onSettings()
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(0.3f))

                DrawerItem(Icons.Default.Logout, "خروج", tint = MaterialTheme.colorScheme.error) {
                    scope.launch { drawerState.close() }; onLogout()
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
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("تراکنش‌های اخیر", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground)
                        TextButton(onViewAll) {
                            Text("مشاهده همه", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
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
                    items(recent) { t -> TransactionItem(t, onClick = { onTransactionClick(t.id) }) }
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
