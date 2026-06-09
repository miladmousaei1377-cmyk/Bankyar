package com.bankyar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
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

    val account = accounts.find { it.title == accountName }
    val accountTx = remember(transactions, accountName) {
        transactions.filter { it.accountName == accountName }
            .sortedByDescending { it.date }
    }
    val income = remember(accountTx) { accountTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
    val expense = remember(accountTx) { accountTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
    val initialBalance = account?.initialBalance ?: 0.0
    val currentBalance = initialBalance + income - expense
    val isPositive = currentBalance >= 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountName, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
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
                                    color = Color(0xFF2E7D32), fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("هزینه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("-${formatAmount(expense)} تومان",
                                    color = Color(0xFFC62828), fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium)
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

            item {
                Text("تراکنش‌های این حساب (${accountTx.size})",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp))
            }

            if (accountTx.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("هیچ تراکنشی برای این حساب یافت نشد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(accountTx, key = { it.id }) { t ->
                    TransactionItem(t, onClick = {})
                }
            }
        }
    }
}
