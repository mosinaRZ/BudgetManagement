package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.ui.screens.notification.NotificationViewModel
import ir.hamedan.budgetmanagement.utils.AppNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: NotificationRepository
    private lateinit var context: Context
    private lateinit var viewModel: NotificationViewModel

    private val notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val unreadFlow = MutableStateFlow(0)

    private val sample = NotificationEntity(
        id = "n1",
        type = "SUCCESS",
        titleFa = "تست",
        titleEn = "Test",
        descFa = "توضیح",
        descEn = "Desc",
        isRead = false,
        tag = "TAG_1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { repository.getAllNotifications() } returns notificationsFlow
        every { repository.getUnreadCount() } returns unreadFlow

        mockkObject(AppNotificationManager)
        every {
            AppNotificationManager.sendPushIfAllowed(any(), any(), any(), any(), any())
        } just Runs

        viewModel = NotificationViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun addNotification_callsRepositoryAndPush() = runTest(testDispatcher) {
        coEvery { repository.addNotification(any()) } just Runs

        viewModel.addNotification(sample)
        advanceUntilIdle()

        coVerify { repository.addNotification(sample) }
        verify {
            AppNotificationManager.sendPushIfAllowed(
                context = context,
                titleFa = "تست",
                titleEn = "Test",
                bodyFa = "توضیح",
                bodyEn = "Desc"
            )
        }
    }

    @Test
    fun markAsRead_delegatesToRepository() = runTest(testDispatcher) {
        coEvery { repository.markAsRead(any()) } just Runs

        viewModel.markAsRead("n1")
        advanceUntilIdle()

        coVerify { repository.markAsRead("n1") }
    }

    @Test
    fun markAllAsRead_delegatesToRepository() = runTest(testDispatcher) {
        coEvery { repository.markAllAsRead() } just Runs

        viewModel.markAllAsRead()
        advanceUntilIdle()

        coVerify { repository.markAllAsRead() }
    }
}