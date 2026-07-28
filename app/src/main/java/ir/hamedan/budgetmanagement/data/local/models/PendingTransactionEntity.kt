package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// وضعیت هر تراکنش پیامکی در انتظار بررسی کاربر
object PendingStatus {
    const val PENDING = "PENDING"       // در انتظار بررسی کاربر
    const val CONFIRMED = "CONFIRMED"   // کاربر تایید کرد و تراکنش نهایی ثبت شد
    const val IGNORED = "IGNORED"       // کاربر نادیده گرفت / رد کرد
}

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val rawMessage: String = "",       // متن کامل پیامک دریافتی (برای مرجع/بررسی کاربر)
    val senderAddress: String = "",    // شماره یا نام فرستنده پیامک (معمولا بانک)

    val amount: Double = 0.0,          // مبلغ تشخیص داده شده توسط پارسر
    val isAmountDetected: Boolean = false,

    val type: String = "EXPENSE",      // حدس سیستم: EXPENSE یا INCOME
    val isTypeDetected: Boolean = false, // آیا سیستم با اطمینان نوع را تشخیص داد یا فقط حدس زده

    val suggestedTitle: String = "",       // عنوان پیشنهادی
    val suggestedCategory: String = "",    // دسته‌بندی پیشنهادی در صورت تطبیق با دسته‌های موجود کاربر

    val timestamp: Long = System.currentTimeMillis(),
    val status: String = PendingStatus.PENDING
)