package com.bankyar.data.sms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bankyar.R
import com.bankyar.data.PreferencesManager
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.data.repository.BankAccountRepository
import com.bankyar.ui.components.formatAmount
import com.bankyar.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }

        val parsed = SmsParser.parse(sender, body) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesManager(context)
                val smsEnabled = prefs.smsAutoRegisterEnabled.first()
                if (!smsEnabled) return@launch

                val userId = prefs.loggedInUserId.first()
                if (userId <= 0) return@launch

                val db = AppDatabase.getInstance(context)
                val accountRepo = BankAccountRepository(db.bankAccountDao())

                val matchedAccount = parsed.cardLastDigits?.let {
                    accountRepo.findByCardLastFour(userId, it)
                }
                val accountName = matchedAccount?.title ?: ""

                showTransactionNotification(context, parsed, accountName, userId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showTransactionNotification(
        context: Context,
        parsed: ParsedSmsTransaction,
        accountName: String,
        userId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notifId = abs(parsed.timestamp.toInt())
        val typeLabel = if (parsed.type == TransactionType.INCOME) "واریز" else "برداشت"
        val amountText = "${formatAmount(parsed.amount)} تومان"
        val accountLabel = if (accountName.isNotBlank()) " — $accountName" else ""

        // Confirm action
        val confirmIntent = Intent(context, ConfirmTransactionReceiver::class.java).apply {
            action = ConfirmTransactionReceiver.ACTION_CONFIRM
            putExtra(ConfirmTransactionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(ConfirmTransactionReceiver.EXTRA_USER_ID, userId)
            putExtra(ConfirmTransactionReceiver.EXTRA_TITLE, "${parsed.bankName} — $typeLabel")
            putExtra(ConfirmTransactionReceiver.EXTRA_AMOUNT, parsed.amount)
            putExtra(ConfirmTransactionReceiver.EXTRA_TYPE, parsed.type.name)
            putExtra(ConfirmTransactionReceiver.EXTRA_ACCOUNT_NAME, accountName)
            putExtra(ConfirmTransactionReceiver.EXTRA_DATE, parsed.timestamp)
        }
        val confirmPi = PendingIntent.getBroadcast(
            context, notifId,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action (no-op — just cancels notification via auto-cancel)
        val dismissPi = PendingIntent.getBroadcast(
            context, notifId + 1,
            Intent(context, ConfirmTransactionReceiver::class.java).apply {
                action = ConfirmTransactionReceiver.ACTION_DISMISS
                putExtra(ConfirmTransactionReceiver.EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SMS_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("تراکنش بانکی شناسایی شد")
            .setContentText("$typeLabel $amountText$accountLabel")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${parsed.bankName}: $typeLabel $amountText$accountLabel\n\nآیا این تراکنش را ثبت کنم؟")
            )
            .setAutoCancel(true)
            .addAction(R.drawable.app_logo, "تأیید و ثبت", confirmPi)
            .addAction(R.drawable.app_logo, "نادیده‌گرفتن", dismissPi)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
