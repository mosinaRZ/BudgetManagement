package ir.hamedan.budgetmanagement

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.hamedan.budgetmanagement.data.SharedPreferences.getIsLog
import ir.hamedan.budgetmanagement.data.ThemePreferences
import ir.hamedan.budgetmanagement.data.ThemePreferences.getThemeMode
import ir.hamedan.budgetmanagement.data.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.ui.theme.BudgetManagementTheme
import ir.hamedan.budgetmanagement.ui.theme.view.HomeScreen
import ir.hamedan.budgetmanagement.ui.theme.view.SplashScreen
import ir.hamedan.budgetmanagement.utils.LocaleHelper

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current

            // حالت تم ذخیره شده
            var themeMode by remember { mutableIntStateOf(getThemeMode(context)) }

            // وضعیت تم سیستم
            val isSystemDark = isSystemInDarkTheme()

            BudgetManagementTheme(themeMode = themeMode) {
                TheApp(
                    modifier = Modifier.fillMaxSize(),
                    onThemeToggle = {
                        // جابه‌جایی سریع و بدون دردسر تم
                        val newMode = when (themeMode) {
                            ThemePreferences.MODE_LIGHT -> ThemePreferences.MODE_DARK
                            ThemePreferences.MODE_DARK -> ThemePreferences.MODE_LIGHT
                            else -> if (isSystemDark) ThemePreferences.MODE_LIGHT else ThemePreferences.MODE_DARK
                        }

                        themeMode = newMode
                        saveThemeMode(context, newMode)
                    }
                )
            }
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun TheApp(
        modifier: Modifier = Modifier,
        onThemeToggle: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val isLogin = getIsLog(context)

        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "Splash"
        ) {
            composable("Splash") {
                SplashScreen(
                    onAnimationFinished = {
                        // رفتن به هوم و پاک کردن اسپلش از پشته (بک‌استک) تا کاربر با زدن دکمه بک دوباره به اسپلش برنگردد
                        navController.navigate("Home") {
                            popUpTo("Splash") { inclusive = true }
                        }
                    }
                )
            }

            composable("Home") {
                HomeScreen(
                    onThemeToggle = onThemeToggle
                )
            }
        }
    }
}