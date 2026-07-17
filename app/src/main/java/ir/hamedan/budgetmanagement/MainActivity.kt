package ir.hamedan.budgetmanagement

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.hamedan.budgetmanagement.data.SharedPreferences.getIsLog
import ir.hamedan.budgetmanagement.data.ThemePreferences
import ir.hamedan.budgetmanagement.data.ThemePreferences.getThemeMode
import ir.hamedan.budgetmanagement.data.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.item.BottomNavItem
import ir.hamedan.budgetmanagement.item.CapsuleBottomNavigation
import ir.hamedan.budgetmanagement.ui.theme.BudgetManagementTheme
import ir.hamedan.budgetmanagement.ui.theme.view.HomeScreen
import ir.hamedan.budgetmanagement.ui.theme.view.SplashScreen
import ir.hamedan.budgetmanagement.ui.theme.view.TransactionsScreen
import ir.hamedan.budgetmanagement.ui.theme.view.settings.SettingsScreen
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

        // دریافت مسیر (Route) فعلی برای کنترل باتم‌بار کپسولی
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        NavHost(
            navController = navController,
            startDestination = "Splash",
            modifier = modifier
        ) {
            // ۱. صفحه اسپلش اسکرین (بدون نمایش باتم‌بار)
            composable("Splash") {
                SplashScreen(
                    onAnimationFinished = {
                        // هدایت به صفحه اصلی و پاک کردن کامل اسپلش از پشته ناوبری
                        navController.navigate("MainStructure") {
                            popUpTo("Splash") { inclusive = true }
                        }
                    }
                )
            }

            // ۲. ساختار اصلی برنامه پس از ورود (شامل صفحات ناوبری زیر داک کپسولی)
            composable("MainStructure") {
                val appNavController = rememberNavController()
                val appBackStackEntry by appNavController.currentBackStackEntryAsState()
                val appCurrentRoute = appBackStackEntry?.destination?.route

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ۱. محتوای صفحات برنامه (کل صفحه)
                    NavHost(
                        navController = appNavController,
                        startDestination = BottomNavItem.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(BottomNavItem.Home.route) {
                            HomeScreen(
                                onThemeToggle = onThemeToggle,
                                onSeeAllTransactionsClick = {
                                    // 🚀 ناوبری هوشمند به صفحه تراکنش‌ها به همراه حفظ پشته ناوبری
                                    appNavController.navigate(BottomNavItem.Transactions.route) {
                                        popUpTo(appNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        // 🚀 جایگزین کردن صفحه پیش‌فرض با صفحه تراکنش‌های جدید و لوکس
                        composable(BottomNavItem.Transactions.route) {
                            TransactionsScreen(
                                onBackClick = {
                                    // رفتن به صفحه خانه هنگام زدن دکمه بازگشت در تاپ‌بار
                                    navController.navigate(BottomNavItem.Home.route) {
                                        popUpTo(BottomNavItem.Home.route) { inclusive = false }
                                    }
                                },
                                onFilterClick = {
                                    // اینجا بعداً می‌توانی دیالوگ فیلتر یا باتم‌شیت دسته‌بندی‌ها را باز کنی
                                }
                            )
                        }
                        composable(BottomNavItem.Analytics.route) {
                            PlaceholderScreen(title = "آنالیز", titleEn = "Analytics")
                        }
                        composable(BottomNavItem.Settings.route) {
                            SettingsScreen(
                            )
                        }
                    }

                    // ۲. مجموعه داک پایینی (اصلاح شده برای جلوگیری از قاطی شدن با ناوبار سیستم)
                    val context = LocalContext.current
                    val isPersian = LocaleHelper.getLanguage(context) == "fa"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            // 🚀 کلید حل مشکل: پدینگ خودکار ناوبار سیستم را اینجا اضافه می‌کنیم
                            .navigationBarsPadding()
                            // حالا پدینگ‌های دلخواه خودمان را از اطراف و کف اعمال می‌کنیم
                            .padding(bottom = 5.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {

                        val bottomBarItem = @Composable {
                            Box(modifier = Modifier.weight(1f)) {
                                CapsuleBottomNavigation(
                                    currentRoute = appCurrentRoute,
                                    onItemSelected = { selectedItem ->
                                        appNavController.navigate(selectedItem.route) {
                                            popUpTo(appNavController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }

                        val fabItem = @Composable {
                            FloatingActionButton(
                                onClick = { /* اکشن دکمه پلاس */ },
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                // 📐 هماهنگ کردن دقیق ارتفاع FAB با ارتفاع جدید کپسول عمودی (حدوداً ۶۴ دی‌پی‌آی)
                                modifier = Modifier.size(56.dp),
                                elevation = FloatingActionButtonDefaults.elevation(0.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                    contentDescription = if (isPersian) "افزودن" else "Add",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        if (isPersian) {
                            fabItem()
                            bottomBarItem()
                        } else {
                            bottomBarItem()
                            fabItem()
                        }
                    }
                }
            }
        }
    }
}

// کامپوننت موقت برای صفحات در حال توسعه تا پروژه ارور ندهد
@Composable
fun PlaceholderScreen(title: String, titleEn: String) {
    val context = LocalContext.current
    val isPersian = LocaleHelper.getLanguage(context) == "fa"
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = if (isPersian) title else titleEn,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}