package ir.hamedan.budgetmanagement.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.BuildConfig
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives SMS_RECEIVED broadcasts, filters likely bank messages,
 * parses them, and stores as PendingTransaction for user confirmation.
 *
 * Uses goAsync() so work can finish after onReceive returns.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress.orEmpty()
        val fullBody = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val smsTimestamp = messages[0].timestampMillis

        if (fullBody.isBlank() || !SmsParser.isLikelyBankSms(fullBody)) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        // SupervisorJob so one failure doesn't cancel the whole scope unexpectedly
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val app = appContext as BudgetApp
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
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to process bank SMS", e)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
