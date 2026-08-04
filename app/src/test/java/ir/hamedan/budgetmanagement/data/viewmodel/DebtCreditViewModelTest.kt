package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.google.common.truth.Truth.assertThat
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
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.ui.viewmodels.DebtCreditViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * نکات کلیدی این تست (نسخه‌ی اصلاح‌شده):
 *
 * ۱) DebtCreditViewModel قبلاً Dispatchers.IO را هارد-کد داشت. چون
 *    Dispatchers.setMain(testDispatcher) فقط Main را جایگزین می‌کند نه IO،
 *    کوروتین‌های viewModelScope.launch(Dispatchers.IO) روی یک ترد واقعیِ
 *    غیرقابل‌کنترل اجرا می‌شدند و advanceUntilIdle() هیچ اثری روی آن‌ها
 *    نداشت. راه‌حل: پارامتر ioDispatcher به DebtCreditViewModel اضافه شد و
 *    اینجا testDispatcher به آن تزریق می‌شود.
 *
 * ۲) BalanceWidget().updateAll(context) یک extension function واقعی از
 *    Glance است که در unit test بدون mock کردن throw می‌کند. با
 *    mockkStatic روی فایل تولیدشده‌ی Kotlin برای این extension function،
 *    آن را بی‌اثر می‌کنیم.
 *
 * ۳) insertOrUpdateDebtCredit / deleteDebtCredit در DebtCreditRepository
 *    suspend هستند، پس با coEvery / coVerify کار می‌کنیم نه every / verify.
 *
 * === باگ‌هایی که در نسخه‌ی قبلی تست باعث fail شدن بودند و اینجا رفع شدن ===
 *
 * ۴) deposit() برای آیتم‌های DEBT قبل از هر کاری موجودی حساب را با
 *    transactionRepository.getCurrentBalance() چک می‌کند. چون این تابع
 *    قبلاً stub نشده بود، relaxed mock مقدار پیش‌فرض Double یعنی 0.0
 *    برمی‌گرداند و همیشه شاخه‌ی «موجودی ناکافی» اجرا و تابع زودتر return
 *    می‌شد. الان قبل از تست‌های deposit این مقدار stub شده است.
 *
 * ۵) تست deposit_credit_updatesPaidAmount قبلاً به اشتباه از sampleDebt
 *    (type = "DEBT") استفاده می‌کرد و در واقع مسیر CREDIT را تست نمی‌کرد.
 *    الان یک sampleCredit جداگانه با type = "CREDIT" اضافه شده و
 *    repository از ابتدا هر دو آیتم را برمی‌گرداند.
 *
 * ۶) withdraw() در کد فعلی از coerceAtLeast(0.0) استفاده می‌کند، پس
 *    paidAmount هرگز منفی نمی‌شود. انتظار تست با همین رفتار واقعی
 *    (0.0) هماهنگ شده است. اگر منطق مورد نظر این باشد که withdraw
 *    بتواند مقدار را منفی هم بکند، باید coerceAtLeast(0.0) از
 *    DebtCreditViewModel.kt حذف شود و این تست به حالت قبلی (-10000.0)
 *    برگردد.
 *
 * ۷) toggleSettled() در کد فعلی عنوان نوتیفیکیشن را ثابت می‌فرستد
 *    ("تغییر وضعیت تسویه" / "Settlement Status Changed") و فقط متن
 *    توضیح (desc) بر اساس وضعیت جدید تغییر می‌کند. انتظار تست با همین
 *    رفتار واقعی هماهنگ شده. اگر بخواهید عنوان هم شرطی باشد، باید در
 *    ViewModel تغییرش بدهید.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebtCreditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var debtCreditRepository: DebtCreditRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var context: Context
    private lateinit var viewModel: DebtCreditViewModel

    private val sampleDebt = DebtCreditEntity(
        id = "debt1",
        type = "DEBT",
        personName = "مهدی",
        totalAmount = 50000.0,
        paidAmount = 0.0,
        isMonthly = false,
        monthlyAmount = 0.0,
        dueDay = 5,
        dueDateMillis = System.currentTimeMillis() + 86400000L * 7,
        note = "وام شخصی",
        isSettled = false
    )

    private val sampleCredit = DebtCreditEntity(
        id = "credit1",
        type = "CREDIT",
        personName = "سارا",
        totalAmount = 80000.0,
        paidAmount = 0.0,
        isMonthly = false,
        monthlyAmount = 0.0,
        dueDay = 10,
        dueDateMillis = System.currentTimeMillis() + 86400000L * 14,
        note = "طلب از سارا",
        isSettled = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        debtCreditRepository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { debtCreditRepository.allDebtCredits } returns flowOf(listOf(sampleDebt, sampleCredit))

        // پیش‌فرض: موجودی کافی برای هر عملیاتی که آن را چک می‌کند
        // (deposit روی آیتم DEBT، و ساخت CREDIT جدید با addToBalance=true).
        coEvery { transactionRepository.getCurrentBalance() } returns 1_000_000.0

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        // BalanceWidget().updateAll(context) یک extension function روی
        // GlanceAppWidget است. چون کلاس facade تولیدشده (GlanceAppWidgetKt)
        // مستقیماً قابل import/resolve نیست، با نام کامل رشته‌ای mock می‌کنیم.
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<BalanceWidget>().updateAll(any()) } just Runs

        viewModel = DebtCreditViewModel(
            debtCreditRepository = debtCreditRepository,
            transactionRepository = transactionRepository,
            context = context,
            ioDispatcher = testDispatcher // ← نکته‌ی کلیدی: دیسپچر تست تزریق شد
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    // ====================== observing با coroutine ======================
    @Test
    fun debtCreditList_emitsRepositoryData() = runTest(testDispatcher) {
        val result = collectLastDebtCredits()
        assertThat(result).isNotNull()
        assertThat(result).hasSize(2)
        assertThat(result!!.map { it.personName }).containsExactly("مهدی", "سارا")
    }

    private fun TestScope.collectLastDebtCredits(): List<DebtCreditEntity>? {
        val values = mutableListOf<List<DebtCreditEntity>?>()
        backgroundScope.launch(testDispatcher) {
            viewModel.debtCreditList.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull { it != null } ?: values.lastOrNull()
    }

    /**
     * چون debtCreditList با SharingStarted.WhileSubscribed(5000) ساخته شده،
     * تا وقتی هیچ subscriber فعالی روی آن نباشد، upstream اصلاً collect
     * نمی‌شود و debtCreditList.value همان initialValue = emptyList() باقی
     * می‌ماند. توابعی مثل deposit/withdraw/toggleSettled/delete برای پیدا
     * کردن آیتم به debtCreditList.value وابسته‌اند، پس قبل از فراخوانی آن‌ها
     * باید حتماً یک subscriber (در backgroundScope) روی این StateFlow باز
     * کنیم تا واقعاً از repository پر شود.
     */
    private fun TestScope.observeDebtCreditList() {
        backgroundScope.launch(testDispatcher) {
            viewModel.debtCreditList.collect { }
        }
        advanceUntilIdle()
    }

    // ====================== saveOrUpdate ======================
    @Test
    fun saveOrUpdate_createDebt_callsRepositoryAndSendsSuccessNotification() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.insertOrUpdateDebtCredit(any()) } returns Unit

        viewModel.saveOrUpdate(
            id = null,
            type = "DEBT",
            personName = "علی",
            totalAmount = 100000.0,
            isMonthly = false,
            monthlyAmount = 0.0,
            dueDay = 15,
            oneTimeDueDateMillis = System.currentTimeMillis() + 86400000L * 10,
            note = "وام خودرو",
            addToBalance = true
        )
        advanceUntilIdle()

        coVerify {
            debtCreditRepository.insertOrUpdateDebtCredit(match { it.personName == "علی" })
        }

        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = match { it.contains("ثبت بدهی/طلب جدید") },
                titleEn = match { it.contains("Record Added") || it.contains("New") },
                descFa = match { it.contains("علی") },
                descEn = match { it.contains("علی") },
                tag = match { it.startsWith("DEBT_SAVE_") }
            )
        }
    }

    @Test
    fun saveOrUpdate_editUpdatesRepository() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.insertOrUpdateDebtCredit(any()) } returns Unit
        observeDebtCreditList()

        viewModel.saveOrUpdate(
            id = "debt1",
            type = "DEBT",
            personName = "مهدی",
            totalAmount = 55000.0,
            isMonthly = false,
            monthlyAmount = 0.0,
            dueDay = 5,
            oneTimeDueDateMillis = sampleDebt.dueDateMillis,
            note = "به‌روزرسانی وام",
            addToBalance = false
        )
        advanceUntilIdle()

        coVerify {
            debtCreditRepository.insertOrUpdateDebtCredit(match { it.id == "debt1" && it.totalAmount == 55000.0 })
        }
    }

    // ====================== deposit ======================
    @Test
    fun deposit_debt_updatesPaidAmountAndSendsSuccessNotification() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.insertOrUpdateDebtCredit(any()) } returns Unit
        observeDebtCreditList()

        viewModel.deposit(id = "debt1", amount = 30000.0)
        advanceUntilIdle()

        coVerify {
            debtCreditRepository.insertOrUpdateDebtCredit(match { it.id == "debt1" && it.paidAmount == 30000.0 })
        }

        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = match { it.contains("پرداخت بدهی") },
                titleEn = match { it.contains("Debt Payment") },
                descFa = match { it.contains("مهدی") && it.contains("30000") },
                descEn = match { it.contains("مهدی") && it.contains("30000") },
                tag = match { it.startsWith("DEBT_DEPOSIT_") }
            )
        }
    }

    @Test
    fun deposit_credit_updatesPaidAmount() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.insertOrUpdateDebtCredit(any()) } returns Unit
        observeDebtCreditList()

        viewModel.deposit(id = "credit1", amount = 20000.0)
        advanceUntilIdle()

        coVerify {
            debtCreditRepository.insertOrUpdateDebtCredit(match { it.id == "credit1" && it.paidAmount == 20000.0 })
        }

        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = match { it.contains("دریافت طلب") },
                titleEn = match { it.contains("Credit Received") },
                descFa = match { it.contains("سارا") && it.contains("20000") },
                descEn = match { it.contains("سارا") && it.contains("20000") },
                tag = match { it.startsWith("DEBT_DEPOSIT_") }
            )
        }
    }

    // ====================== withdraw ======================
    @Test
    fun withdraw_clampsPaidAmountAtZeroAndSendsWarningNotification() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.insertOrUpdateDebtCredit(any()) } returns Unit
        observeDebtCreditList()

        viewModel.withdraw(id = "debt1", amount = 10000.0)
        advanceUntilIdle()

        // paidAmount اولیه 0.0 است و withdraw از coerceAtLeast(0.0) استفاده
        // می‌کند، پس نتیجه هرگز منفی نمی‌شود.
        coVerify {
            debtCreditRepository.insertOrUpdateDebtCredit(match { it.id == "debt1" && it.paidAmount == 0.0 })
        }

        verify {
            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = match { it.contains("اصلاح واریزی بدهی") },
                titleEn = match { it.contains("Payment Adjustment") },
                descFa = match { it.contains("مهدی") },
                descEn = match { it.contains("مهدی") },
                tag = match { it.startsWith("DEBT_WITHDRAW_") }
            )
        }
    }

    // ====================== delete ======================
    @Test
    fun delete_deletesAndSendsErrorNotification() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.deleteDebtCredit(any()) } returns Unit
        observeDebtCreditList()

        viewModel.delete(id = "debt1")
        advanceUntilIdle()

        coVerify {
            debtCreditRepository.deleteDebtCredit("debt1")
        }

        verify {
            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = match { it.contains("حذف بدهی/طلب") },
                titleEn = match { it.contains("Record Deleted") },
                descFa = match { it.contains("مهدی") },
                descEn = match { it.contains("مهدی") },
                tag = match { it.startsWith("DEBT_DELETE_") }
            )
        }
    }

    // ====================== toggleSettled ======================
    @Test
    fun toggleSettled_togglesStatusAndSendsNotification() = runTest(testDispatcher) {
        coEvery { debtCreditRepository.insertOrUpdateDebtCredit(any()) } returns Unit
        observeDebtCreditList()

        viewModel.toggleSettled(id = "debt1", currentStatus = false)
        advanceUntilIdle()

        coVerify {
            debtCreditRepository.insertOrUpdateDebtCredit(match { it.id == "debt1" && it.isSettled == true })
        }

        // توجه: عنوان نوتیفیکیشن در کد فعلی ثابت است و به وضعیت وابسته
        // نیست؛ فقط متن توضیح (desc) شرطی است.
        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "تغییر وضعیت تسویه",
                titleEn = "Settlement Status Changed",
                descFa = match { it.contains("مهدی") && it.contains("تسویه شد") },
                descEn = match { it.contains("مهدی") && it.contains("Settled") },
                tag = match { it.startsWith("DEBT_SETTLE_") }
            )
        }
    }

    // ====================== calculateExpirationDate ======================
    @Test
    fun calculateExpirationDate_monthlyCreatesCorrectDate() = runTest(testDispatcher) {
        // زمان مرجع مشترک برای جلوگیری از اختلاف چند میلی‌ثانیه‌ای بین
        // فراخوانی تابع تحت تست و محاسبه‌ی expected در خود تست
        val reference = Calendar.getInstance()

        val dueDate = viewModel.calculateExpirationDate(100000.0, 25000.0, 15)

        val months = (100000 / 25000).toInt()
        assertThat(dueDate).isGreaterThan(System.currentTimeMillis() - 1000)

        val expected = (reference.clone() as Calendar).apply {
            val day = get(Calendar.DAY_OF_MONTH)
            if (day > 15) add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 15.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            add(Calendar.MONTH, months - 1)
        }.timeInMillis

        assertThat(dueDate).isEqualTo(expected)
    }
}