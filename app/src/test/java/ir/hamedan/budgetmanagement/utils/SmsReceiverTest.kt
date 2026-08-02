package ir.hamedan.budgetmanagement.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.content.ContextCompat
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.di.AppContainer
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * SmsReceiver is a thin orchestrator:
 * permission → parse SMS PDU → SmsParser → PendingRepository → optional notification.
 *
 * Heavy lifting stays in SmsParser (already unit-tested).
 * Here we lock the gate conditions and the insert path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = BudgetApp::class)
class SmsReceiverTest {

    private lateinit var context: Context
    private lateinit var pendingRepository: PendingTransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var container: AppContainer
    private lateinit var app: BudgetApp

    private val bankBody =
        "برداشت مبلغ 150,000 ریال از کارت. مانده 1,000,000 ریال حساب"

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        app = context.applicationContext as BudgetApp

        pendingRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        container = mockk(relaxed = true)

        every { container.pendingTransactionRepository } returns pendingRepository
        every { container.categoryRepository } returns categoryRepository
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())
        coEvery { pendingRepository.addPending(any()) } returns true

        // Inject mocked container into the real Application if your BudgetApp exposes it.
        // Adjust field name if different (e.g. app.container is val set in onCreate).
        try {
            val field = BudgetApp::class.java.getDeclaredField("container")
            field.isAccessible = true
            field.set(app, container)
        } catch (_: Exception) {
            // If container is not a field, expose a test seam in BudgetApp (see note below).
        }

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.RECEIVE_SMS)
        } returns PackageManager.PERMISSION_GRANTED
    }

    @After
    fun tearDown() {
        unmockkAll()
        clearAllMocks()
    }

    private fun smsReceivedIntent(body: String, sender: String = "10000"): Intent {
        // createFromPdu is awkward under Robolectric; we mock Telephony static instead when needed.
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        // Minimal extras path: many tests mock getMessagesFromIntent
        return intent
    }

    // -------------------------------------------------------------------------
    // Gate conditions (no insert)
    // -------------------------------------------------------------------------

    @Test
    fun onReceive_wrongAction_doesNothing() {
        val receiver = SmsReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        coVerify(exactly = 0) { pendingRepository.addPending(any()) }
    }

    @Test
    fun onReceive_withoutSmsPermission_doesNothing() {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.RECEIVE_SMS)
        } returns PackageManager.PERMISSION_DENIED

        mockkStatic(Telephony.Sms.Intents::class)
        every { Telephony.Sms.Intents.getMessagesFromIntent(any()) } returns arrayOf(mockSms(bankBody))

        SmsReceiver().onReceive(context, Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))

        coVerify(exactly = 0) { pendingRepository.addPending(any()) }
    }

    @Test
    fun onReceive_nonBankBody_doesNothing() {
        mockkStatic(Telephony.Sms.Intents::class)
        every {
            Telephony.Sms.Intents.getMessagesFromIntent(any())
        } returns arrayOf(mockSms("کد تایید شما 12345 است"))

        SmsReceiver().onReceive(context, Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))

        coVerify(exactly = 0) { pendingRepository.addPending(any()) }
    }

    @Test
    fun onReceive_bankSmsWithoutDetectableAmount_doesNotInsert() {
        mockkStatic(Telephony.Sms.Intents::class)
        // bank-ish words but no usable amount
        val body = "برداشت از کارت انجام شد مانده حساب بروزرسانی گردید"
        every {
            Telephony.Sms.Intents.getMessagesFromIntent(any())
        } returns arrayOf(mockSms(body))

        SmsReceiver().onReceive(context, Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))

        // may or may not pass isLikelyBankSms; if it does, amount gate should block insert
        Thread.sleep(500) // goAsync + IO
        coVerify(exactly = 0) { pendingRepository.addPending(any()) }
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun onReceive_bankSmsWithAmount_insertsPending_andNotifies() {
        mockkStatic(Telephony.Sms.Intents::class)
        every {
            Telephony.Sms.Intents.getMessagesFromIntent(any())
        } returns arrayOf(mockSms(bankBody, ts = 1_700_000_000_000L))

        every { categoryRepository.getAllCategories() } returns flowOf(
            listOf(CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true))
        )
        coEvery { pendingRepository.addPending(any()) } returns true

        SmsReceiver().onReceive(context, Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))

        Thread.sleep(800)

        coVerify(timeout = 3_000) {
            pendingRepository.addPending(
                match {
                    it.isAmountDetected &&
                            it.amount > 0.0 &&
                            it.rawMessage.contains("برداشت") &&
                            it.timestamp == 1_700_000_000_000L
                }
            )
        }
        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = any(),
                type = "SYSTEM",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = match { it.startsWith("SMS_PENDING_") }
            )
        }
    }

    @Test
    fun onReceive_whenAddPendingReturnsFalse_doesNotNotify() {
        mockkStatic(Telephony.Sms.Intents::class)
        every {
            Telephony.Sms.Intents.getMessagesFromIntent(any())
        } returns arrayOf(mockSms(bankBody))

        coEvery { pendingRepository.addPending(any()) } returns false // duplicate window

        SmsReceiver().onReceive(context, Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
        Thread.sleep(800)

        verify(exactly = 0) {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun mockSms(body: String, sender: String = "10000", ts: Long = 1_700_000_000_000L): SmsMessage {
        val sms = mockk<SmsMessage>(relaxed = true)
        every { sms.messageBody } returns body
        every { sms.originatingAddress } returns sender
        every { sms.timestampMillis } returns ts
        return sms
    }
}