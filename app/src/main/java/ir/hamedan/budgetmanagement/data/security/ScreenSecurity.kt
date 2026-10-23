package ir.hamedan.budgetmanagement.data.security

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Screen-content protection for a financial app.
 *
 * When enabled (the default, and recommended) the app window is marked FLAG_SECURE, so the
 * system blanks it in the Recents overview and rejects screenshots, screen recording and
 * casting / non-secure virtual displays. On Android 13+ the Recents thumbnail is additionally
 * disabled explicitly. Compose dialogs, bottom sheets and menus use `SecureFlagPolicy.Inherit`
 * and therefore follow the activity window automatically.
 *
 * The user can relax it in Settings → App Security (e.g. to capture a screenshot for support).
 * The preference is not a secret, so a plain private SharedPreferences file is enough; it is
 * excluded from backup and device transfer like every other file of this app.
 */
object ScreenSecurity {
    private const val PREFS_NAME = "budget_screen_security"
    private const val KEY_PROTECT_SCREEN = "protect_screen"

    private val state = MutableStateFlow(true)

    /** Current protection state; collected by [bind] so Settings changes apply instantly. */
    val enabled: StateFlow<Boolean> = state.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Secure by default: a missing value means protected. */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_PROTECT_SCREEN, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PROTECT_SCREEN, value).apply()
        state.value = value
    }

    /**
     * Applies the stored choice before the first frame is drawn (call right after
     * `super.onCreate`, before `setContent`) and keeps the window in sync afterwards.
     */
    fun bind(activity: ComponentActivity) {
        state.value = isEnabled(activity)
        apply(activity, state.value)
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.CREATED) {
                state.collect { apply(activity, it) }
            }
        }
    }

    private fun apply(activity: Activity, protect: Boolean) {
        if (protect) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(!protect)
        }
    }
}