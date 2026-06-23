package com.bankyar.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bankyar.BuildConfig
import com.bankyar.R
import com.bankyar.data.PreferencesManager
import com.bankyar.util.BackupWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 2 })

    var autoBackupEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val workInfo = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupWorker.WORK_NAME).get()
        autoBackupEnabled = workInfo.isNotEmpty() && workInfo.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }

    fun proceed() {
        scope.launch {
            prefs.setOnboardingSlidesSeenVersion(BuildConfig.VERSION_CODE)
            prefs.markWelcomeSeen()
            onContinue()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> WelcomeSlide(onNext = {
                    scope.launch { pagerState.animateScrollToPage(1) }
                })
                1 -> SettingsSlide(
                    autoBackupEnabled = autoBackupEnabled,
                    onBackupToggle = { enabled ->
                        autoBackupEnabled = enabled
                        if (enabled) scheduleAutoBackup(context)
                        else WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.WORK_NAME)
                    },
                    onProceed = { proceed() }
                )
            }
        }

        // Page indicators
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomeSlide(onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
        }

        Text(
            "به بانک‌یار خوش آمدید",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            "دستیار مالی شخصی شما — کاملاً آفلاین، امن و در دسترس.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        // Feature highlights
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnboardingFeatureRow(
                    icon = Icons.Default.AccountBalance,
                    title = "مدیریت حساب‌ها",
                    description = "درآمد، هزینه و تراکنش‌های خود را در یک جا پیگیری کنید."
                )
                OnboardingFeatureRow(
                    icon = Icons.Default.PieChart,
                    title = "گزارش‌های مالی",
                    description = "نمودارها و آمارهای ماهانه برای تصویر روشن از وضعیت مالی."
                )
                OnboardingFeatureRow(
                    icon = Icons.Default.Lock,
                    title = "حریم خصوصی ۱۰۰٪",
                    description = "هیچ داده‌ای به سرور ارسال نمی‌شود — همه چیز روی دستگاه شماست."
                )
                OnboardingFeatureRow(
                    icon = Icons.Default.Backup,
                    title = "پشتیبان‌گیری خودکار",
                    description = "هر ساعت یک نسخه پشتیبان از داده‌ها در پوشه Download ذخیره می‌شود."
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.ArrowForward, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("بعدی", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SettingsSlide(
    autoBackupEnabled: Boolean,
    onBackupToggle: (Boolean) -> Unit,
    onProceed: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Text(
            "تنظیمات اولیه",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            "قابلیت‌های زیر را فعال کنید تا از بانک‌یار بهترین تجربه را داشته باشید.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        // Auto-backup card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text(
                        "پشتیبان‌گیری خودکار",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }

                Text(
                    "هر ساعت یک فایل پشتیبان از داده‌های شما در پوشه Download دستگاه ذخیره می‌شود.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "فعال‌سازی پشتیبان‌گیری خودکار",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            "پشتیبان هر ساعت در Download",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = onBackupToggle
                    )
                }

                if (autoBackupEnabled) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E7D32).copy(alpha = 0.1f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Text("پشتیبان‌گیری خودکار فعال است", color = Color(0xFF2E7D32), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("شروع", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
        }

        TextButton(onClick = onProceed) {
            Text(
                "بدون تنظیمات ادامه می‌دهم",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Text(
            "می‌توانید این تنظیمات را بعداً از بخش تنظیمات تغییر دهید.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))
    }
}

private fun scheduleAutoBackup(context: android.content.Context) {
    val request = PeriodicWorkRequestBuilder<BackupWorker>(60, TimeUnit.MINUTES)
        .setConstraints(Constraints.NONE)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        BackupWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, title: String, description: String) {
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
