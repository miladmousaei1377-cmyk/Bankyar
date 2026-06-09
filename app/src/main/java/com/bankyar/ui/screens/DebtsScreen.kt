package com.bankyar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.data.database.entities.Debt
import com.bankyar.ui.components.formatAmount
import com.bankyar.ui.viewmodels.DebtViewModel
import com.bankyar.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    userId: Int,
    viewModel: DebtViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(userId) { viewModel.setUser(userId) }

    val debts by viewModel.debts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }

    val iOwe = debts.filter { it.isIOwe }
    val theyOwe = debts.filter { !it.isIOwe }

    if (showDialog) {
        DebtDialog(
            userId = userId,
            onDismiss = { showDialog = false },
            onSave = { debt -> viewModel.addDebt(debt); showDialog = false }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف بدهی") },
            text = { Text("آیا از حذف بدهی «${target.personName}» اطمینان دارید؟") },
            confirmButton = {
                TextButton({ viewModel.deleteDebt(target); deleteTarget = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بدهی و طلب", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        if (debts.isEmpty()) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalanceWallet, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("هیچ بدهی‌ای ثبت نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("روی + بزنید تا بدهی یا طلب ثبت کنید",
                        color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 80.dp)
            ) {
                // Summary row
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "جمع بدهی من",
                            amount = iOwe.filter { !it.isPaid }.sumOf { it.amount },
                            color = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "جمع طلب من",
                            amount = theyOwe.filter { !it.isPaid }.sumOf { it.amount },
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // I Owe section
                if (iOwe.isNotEmpty()) {
                    item {
                        DebtSectionHeader(title = "من بدهکارم", count = iOwe.size, color = Color(0xFFC62828))
                    }
                    items(iOwe, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            onTogglePaid = { viewModel.togglePaid(debt) },
                            onDelete = { deleteTarget = debt }
                        )
                    }
                }

                // They Owe section
                if (theyOwe.isNotEmpty()) {
                    item {
                        DebtSectionHeader(
                            title = "من طلبکارم",
                            count = theyOwe.size,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(top = if (iOwe.isNotEmpty()) 12.dp else 0.dp)
                        )
                    }
                    items(theyOwe, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            onTogglePaid = { viewModel.togglePaid(debt) },
                            onDelete = { deleteTarget = debt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text("${formatAmount(amount)} ت", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun DebtSectionHeader(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) { Text("$count", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    val cardAlpha = if (debt.isPaid) 0.5f else 1f
    val sectionColor = if (debt.isIOwe) Color(0xFFC62828) else Color(0xFF2E7D32)

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
        ),
        elevation = CardDefaults.cardElevation(if (debt.isPaid) 0.dp else 2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(sectionColor.copy(if (debt.isPaid) 0.05f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (debt.isIOwe) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null,
                    tint = sectionColor.copy(if (debt.isPaid) 0.4f else 1f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    debt.personName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(if (debt.isPaid) 0.5f else 1f),
                    textDecoration = if (debt.isPaid) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    "${formatAmount(debt.amount)} تومان",
                    color = sectionColor.copy(if (debt.isPaid) 0.4f else 1f),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    textDecoration = if (debt.isPaid) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(JalaliCalendar.toJalaliShort(debt.date),
                    color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                if (debt.description.isNotBlank())
                    Text(debt.description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, maxLines = 2)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (debt.isPaid) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) { Text("تسویه شده", color = Color(0xFF2E7D32), fontSize = 10.sp) }
                } else {
                    OutlinedButton(
                        onClick = onTogglePaid,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Text("تسویه شد", color = Color(0xFF2E7D32), fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error.copy(0.7f),
                        modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DebtDialog(
    userId: Int,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIOwe by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت بدهی / طلب", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("نام شخص") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ (تومان)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Type toggle
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (isIOwe) Color(0xFFFFEBEE) else Color.Transparent)
                            .clickable { isIOwe = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("من بدهکارم", color = if (isIOwe) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isIOwe) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                    }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (!isIOwe) Color(0xFFE8F5E9) else Color.Transparent)
                            .clickable { isIOwe = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("من طلبکارم", color = if (!isIOwe) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (!isIOwe) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    leadingIcon = { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )

                if (error.isNotBlank())
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    when {
                        personName.isBlank() -> error = "نام شخص را وارد کنید"
                        amt == null || amt <= 0 -> error = "مبلغ معتبر وارد کنید"
                        else -> onSave(
                            Debt(
                                userId = userId,
                                personName = personName,
                                amount = amt,
                                isIOwe = isIOwe,
                                description = description,
                                date = System.currentTimeMillis()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}
