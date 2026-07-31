package ir.hamedan.budgetmanagement.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // فقط وقتی پرمیشن واقعاً داده شده باشد ادامه بده
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val sender = messages[0].originatingAddress ?: ""
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        // استخراج زمان واقعی پیامک از سیستم‌عامل / اپراتور
        val smsTimestamp = messages[0].timestampMillis

        if (!SmsParser.isLikelyBankSms(fullBody)) return

        val appContext = context.applicationContext
        val asyncResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = appContext.applicationContext as BudgetApp
                val pendingRepository = app.container.pendingTransactionRepository
                val categoryRepository = app.container.categoryRepository

                val parseResult = SmsParser.parse(fullBody, smsTimestamp)

                if (!parseResult.isAmountDetected) return@launch

                val categories = categoryRepository.getAllCategories().first()
                val suggestedCategory = SmsParser.suggestCategory(fullBody, categories)

                val pending = PendingTransactionEntity(
                    rawMessage = fullBody,
                    senderAddress = sender,
                    amount = parseResult.amount,
                    isAmountDetected = parseResult.isAmountDetected,
                    type = parseResult.type,
                    isTypeDetected = parseResult.isTypeDetected,
                    suggestedTitle = parseResult.suggestedTitle,
                    suggestedCategory = suggestedCategory,
                    timestamp = parseResult.timestamp
                )

                val inserted = pendingRepository.addPending(pending)

                if (inserted) {
                    NotificationHelper.send(
                        context = appContext,
                        type = "SYSTEM",
                        titleFa = "تراکنش پیامکی جدید",
                        titleEn = "New SMS Transaction",
                        descFa = if (parseResult.isTypeDetected)
                            "یک تراکنش با مبلغ ${parseResult.amount.toLong()} شناسایی شد. برای تکمیل وارد برنامه شوید."
                        else
                            "یک پیامک بانکی دریافت شد که نیاز به بررسی شما دارد.",
                        descEn = "A transaction of ${parseResult.amount.toLong()} was detected. Open the app to complete it.",
                        tag = "SMS_PENDING_${pending.id}"
                    )
                }
            } finally {
                asyncResult.finish()
            }
        }
    }
}