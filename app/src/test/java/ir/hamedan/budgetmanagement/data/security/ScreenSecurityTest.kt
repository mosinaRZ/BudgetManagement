package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenSecurityTest {
    @Test fun `screen protection is disabled when preference has never been set`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("budget_screen_security", Context.MODE_PRIVATE).edit().clear().commit()
        assertFalse(ScreenSecurity.isEnabled(context))
    }

    @Test fun `user choice persists for both enabled and disabled states`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ScreenSecurity.setEnabled(context, true)
        assertTrue(ScreenSecurity.isEnabled(context))
        ScreenSecurity.setEnabled(context, false)
        assertFalse(ScreenSecurity.isEnabled(context))
    }
}