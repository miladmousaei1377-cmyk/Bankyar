package com.bankyar.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bankyar.data.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var smsGranted by remember {
        val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        mutableStateOf(receive && read)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.RECEIVE_SMS] == true &&
                      results[Manifest.permission.READ_SMS] == true
        smsGranted = granted
        if (granted) {
            scope.launch { prefs.setSmsAutoRegisterEnabled(true) }
        }
    }

    fun proceed() {
        scope.launch {
            prefs.markPermissionScreenSeen()
            onContinue()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Icon header
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
        }

        Text(
            "ثبت خودکار تراکنش‌های بانکی",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            "بانک‌یار می‌تواند پیامک‌های بانکی شما را بخواند و تراکنش‌ها را به‌صورت خودکار پیشنهاد دهد — شما تأیید می‌کنید.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        // Feature bullets
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PermissionFeatureRow(
                    icon = Icons.Default.Notifications,
                    title = "پیشنهاد تراکنش",
                    description = "هر بار که پیامک بانکی دریافت کنید، یک نوتیفیکیشن با خلاصه تراکنش نشان داده می‌شود."
                )
                PermissionFeatureRow(
                    icon = Icons.Default.CheckCircle,
                    title = "تأیید قبل از ثبت",
                    description = "هیچ چیزی بدون تأیید شما ثبت نمی‌شود — کنترل کامل در دست شماست."
                )
                PermissionFeatureRow(
                    icon = Icons.Default.Lock,
                    title = "حریم خصوصی",
                    description = "پیامک‌ها هرگز از دستگاه شما خارج نمی‌شوند — پردازش کاملاً آفلاین است."
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (smsGranted) {
            // Permission already granted — show confirmation
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                Text(
                    "دسترسی پیامک اعطا شد — قابلیت ثبت خودکار فعال است.",
                    color = Color(0xFF2E7D32), fontSize = 13.sp
                )
            }
        }

        Button(
            onClick = {
                if (!smsGranted) {
                    permLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    )
                } else {
                    proceed()
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                if (smsGranted) Icons.Default.ArrowForward else Icons.Default.Sms,
                null, tint = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (smsGranted) "ادامه" else "اعطای دسترسی پیامک",
                fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp
            )
        }

        TextButton(onClick = { proceed() }) {
            Text(
                "بدون دسترسی ادامه می‌دهم",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Text(
            "می‌توانید این دسترسی را بعداً از بخش پروفایل تغییر دهید.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionFeatureRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
