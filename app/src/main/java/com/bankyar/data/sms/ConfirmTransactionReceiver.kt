package com.bankyar.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.bankyar.data.database.AppDatabase
import com.bankyar.data.database.entities.Transaction
import com.bankyar.data.database.entities.TransactionCategory
import com.bankyar.data.database.entities.TransactionType
import com.bankyar.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfirmTransactionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

        // Dismiss action: just cancel the notification
        if (intent.action == ACTION_DISMISS) {
            if (notifId != -1) NotificationManagerCompat.from(context).cancel(notifId)
            return
        }

        val userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val typeName = intent.getStringExtra(EXTRA_TYPE) ?: return
        val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) ?: ""
        val date = intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis())

        if (userId <= 0 || amount <= 0.0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TransactionRepository(AppDatabase.getInstance(context).transactionDao())
                repo.insert(
                    Transaction(
                        userId = userId,
                        title = title,
                        amount = amount,
                        type = TransactionType.valueOf(typeName),
                        category = TransactionCategory.OTHER,
                        accountName = accountName,
                        date = date
                    )
                )
                if (notifId != -1) {
                    NotificationManagerCompat.from(context).cancel(notifId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.bankyar.action.CONFIRM_SMS_TRANSACTION"
        const val ACTION_DISMISS = "com.bankyar.action.DISMISS_SMS_TRANSACTION"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_TYPE = "type"
        const val EXTRA_ACCOUNT_NAME = "account_name"
        const val EXTRA_DATE = "date"
    }
}
