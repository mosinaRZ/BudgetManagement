package ir.hamedan.budgetmanagement

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity // اضافه شدن فرگمنت اکتیویتی برای بیومتریک
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.hamedan.budgetmanagement.data.ThemePreferences
import ir.hamedan.budgetmanagement.data.ThemePreferences.getThemeMode
import ir.hamedan.budgetmanagement.data.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.item.BottomNavItem
import ir.hamedan.budgetmanagement.item.CapsuleBottomNavigation
import ir.hamedan.budgetmanagement.ui.theme.BudgetManagementTheme
import ir.hamedan.budgetmanagement.ui.theme.view.AddScreen
import ir.hamedan.budgetmanagement.ui.theme.view.AnalyticsScreen
import ir.hamedan.budgetmanagement.ui.theme.view.HomeScreen
import ir.hamedan.budgetmanagement.ui.theme.view.LoginScreen
import ir.hamedan.budgetmanagement.ui.theme.view.SplashScreen
import ir.hamedan.budgetmanagement.ui.theme.view.TransactionsScreen
import ir.hamedan.budgetmanagement.ui.theme.view.SettingsScreen
import ir.hamedan.budgetmanagement.utils.LocaleHelper

@Suppress("DEPRECATION")
// 🚀 تغییر مهم: ارث‌بری از FragmentActivity برای جلوگیری از کرش اثر انگشت
class MainActivity : FragmentActivity() {

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

        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "Splash", // ابتدا اسپلش اسکرین بالا می‌آید
            modifier = modifier
        ) {
            // ۱. صفحه اسپلش اسکرین
            composable("Splash") {
                SplashScreen(
                    onAnimationFinished = {
                        // هدایت به صفحه لاگین و پاک کردن اسپلش از پشته ناوبری
                        navController.navigate("Login") {
                            popUpTo("Splash") { inclusive = true }
                        }
                    }
                )
            }

            // 🚀 ۲. صفحه لاگین هوشمند (اثر انگشت + فرم متنی)
            composable("Login") {
                LoginScreen(
                    onLoginSuccess = {
                        // هدایت به ساختار اصلی برنامه و پاک کردن صفحه لاگین برای عدم بازگشت مجدد
                        navController.navigate("MainStructure") {
                            popUpTo("Login") { inclusive = true }
                        }
                    }
                )
            }

            // ۳. صفحه افزودن تراکنش جدید
            composable("AddScreen?highlightId={highlightId}") { backStackEntry ->
                val highlightId = backStackEntry.arguments?.getString("highlightId")
                AddScreen(
                    highlightId = highlightId, // پاس دادن آرگومان به کامپوننت
                    onBackClick = { navController.navigate("MainStructure") }
                )
            }

            // ۴. ساختار اصلی برنامه پس از لاگین موفق
            composable("MainStructure") {
                val appNavController = rememberNavController()
                val appBackStackEntry by appNavController.currentBackStackEntryAsState()
                val appCurrentRoute = appBackStackEntry?.destination?.route

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // محتوای داخلی صفحات برنامه
                    NavHost(
                        navController = appNavController,
                        startDestination = BottomNavItem.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(BottomNavItem.Home.route) {
                            HomeScreen(
                                onThemeToggle = onThemeToggle,
                                onSeeAllTransactionsClick = {
                                    appNavController.navigate(BottomNavItem.Transactions.route) {
                                        popUpTo(appNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onAddScreenClickDue = {
                                    navController.navigate("AddScreen?highlightId=due")
                                },
                                onAddScreenClickLimit = {
                                    navController.navigate("AddScreen?highlightId=limit")
                                },
                                onAddScreenClickPiggy = {
                                    navController.navigate("AddScreen?highlightId=piggy")
                                }
                            )
                        }
                        composable(BottomNavItem.Transactions.route) {
                            TransactionsScreen()
                        }
                        composable(BottomNavItem.Analytics.route) {
                            AnalyticsScreen()
                        }
                        composable(BottomNavItem.Settings.route) {
                            SettingsScreen(
                                onThemeToggle = onThemeToggle,
                                onAddScreenClick = {
                                    // ارسال شناسه دکمه هدف (در اینجا category) به عنوان آرگومان
                                    navController.navigate("AddScreen?highlightId=category")
                                },
                                onLoginClick = {
                                    navController.navigate("Login") {
                                        popUpTo("MainStructure") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    // مجموعه داک باتم‌بار و دکمه شناور پلاس (FAB)
                    val context = LocalContext.current
                    val isPersian = LocaleHelper.getLanguage(context) == "fa"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
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
                                onClick = {
                                    navController.navigate("AddScreen")
                                },
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
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