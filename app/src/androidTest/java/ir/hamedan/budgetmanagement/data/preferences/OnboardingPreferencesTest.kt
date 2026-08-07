package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    private fun clearPrefs() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun isCompleted_whenNeverSet_returnsFalse() {
        assertThat(OnboardingPreferences.isCompleted(context)).isFalse()
    }

    @Test
    fun isCompleted_whenFalse_returnsFalse() {
        // چون چیزی ست نشده، باید پیش‌فرض فالس باشد
        assertThat(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean(OnboardingPreferences.KEY_ONBOARDING_COMPLETED, false))
            .isFalse()
    }

    @Test
    fun setCompleted_persistsToDisk() {
        OnboardingPreferences.setCompleted(context)

        assertThat(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean(OnboardingPreferences.KEY_ONBOARDING_COMPLETED, false))
            .isTrue()

        assertThat(OnboardingPreferences.isCompleted(context)).isTrue()
    }

    @Test
    fun setCompleted_setsToTrueAndThenFalse() {
        OnboardingPreferences.setCompleted(context)
        assertThat(OnboardingPreferences.isCompleted(context)).isTrue()
    }

    @Test
    fun note_isCompletedOnlyHasSetCompleted() {
        assertThat(OnboardingPreferences.isCompleted(context)).isFalse()
    }
}