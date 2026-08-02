package ir.hamedan.budgetmanagement.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.di.AppContainer
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * SmsReceiver is a thin orchestrator. Money logic lives in SmsParser (unit-tested).
 *
 * We only test early gates here. Do NOT mockkStatic(Telephony.Sms.Intents) —
 * MockK cannot redefine that Android framework class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmsReceiverTest {

    private lateinit var context: Context
    private lateinit var pendingRepository: PendingTransactionRepository
    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()

        pendingRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)

        val container = mockk<AppContainer>(relaxed = true)
        every { container.pendingTransactionRepository } returns pendingRepository
        every { container.categoryRepository } returns categoryRepository
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())

        // Optional: inject container into BudgetApp if your app uses it and is the test Application.
        // If this fails, early-gate tests below still work without container.
        runCatching {
            val app = context.applicationContext
            if (app is BudgetApp) {
                val field = BudgetApp::class.java.getDeclaredField("container")
                field.isAccessible = true
                field.set(app, container)
            }
        }

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } returns Unit

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

    @Test
    fun onReceive_wrongAction_doesNotTouchRepository() {
        SmsReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        coVerify(exactly = 0) { pendingRepository.addPending(any()) }
    }

    @Test
    fun onReceive_withoutSmsPermission_doesNotTouchRepository() {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.RECEIVE_SMS)
        } returns PackageManager.PERMISSION_DENIED

        SmsReceiver().onReceive(
            context,
            Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        )

        coVerify(exactly = 0) { pendingRepository.addPending(any()) }
    }
}