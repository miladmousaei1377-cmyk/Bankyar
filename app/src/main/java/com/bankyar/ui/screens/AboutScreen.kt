package com.bankyar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankyar.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("درباره ما", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "لوگوی بانک‌یار",
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )

            Text("بانک‌یار", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("نسخه ۱.۰.۰", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("درباره نرم‌افزار", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text(
                        "بانک‌یار یک نرم‌افزار مدیریت مالی شخصی است که به شما کمک می‌کند تراکنش‌های بانکی خود را به صورت کامل و دقیق ثبت و مدیریت کنید.\n\n" +
                        "با بانک‌یار می‌توانید:\n" +
                        "• تراکنش‌های درآمد، هزینه و انتقال را ثبت کنید\n" +
                        "• حساب‌های بانکی مختلف را مدیریت کنید\n" +
                        "• گزارش‌های مالی را دریافت کنید\n" +
                        "• موجودی کل خود را در یک نگاه مشاهده کنید",
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        fontSize = 14.sp
                    )
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اطلاعات فنی", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    AboutRow(Icons.Default.Code, "نسخه نرم‌افزار", "۱.۰.۰")
                    AboutRow(Icons.Default.Person, "توسعه‌دهنده", "میلاد موسایی")
                    Row(
                        Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:miladmousaei1377@gmail.com"))
                            context.startActivity(intent)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("ایمیل: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text(
                            "miladmousaei1377@gmail.com",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
