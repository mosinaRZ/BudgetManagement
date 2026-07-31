package ir.hamedan.budgetmanagement

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity // اضافه شدن فرگمنت اکتیویتی برای بیومتریک
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.getThemeMode
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.ui.components.BottomNavItem
import ir.hamedan.budgetmanagement.ui.components.CapsuleBottomNavigation
import ir.hamedan.budgetmanagement.ui.navigation.AppRoute
import ir.hamedan.budgetmanagement.ui.screens.add.AddScreen
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsScreen
import ir.hamedan.budgetmanagement.ui.screens.home.HomeScreen
import ir.hamedan.budgetmanagement.ui.screens.auth.LoginScreen
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitScreen
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesScreen
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsScreen
import ir.hamedan.budgetmanagement.ui.screens.upcomings.UpcomingPaymentsScreen
import ir.hamedan.budgetmanagement.ui.screens.splash.SplashScreen
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionsScreen
import ir.hamedan.budgetmanagement.ui.screens.settings.SettingsScreen
import ir.hamedan.budgetmanagement.ui.theme.BudgetManagementTheme
import ir.hamedan.budgetmanagement.utils.AppNotificationManager
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
// 🚀 تغییر مهم: ارث‌بری از FragmentActivity برای جلوگیری از کرش اثر انگشت
class MainActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // فعلاً فقط لاگ می‌کنیم. بعداً می‌توانیم UI مناسب نشان دهیم
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        // اینجا می‌توانی بعداً منطق بیشتری بگذاری
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // مقداردهی اولیه واحد پول
        CurrencySharedPreferences.init(applicationContext)

        AppNotificationManager.createChannel(applicationContext)

        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

// فقط پرمیشن‌هایی که هنوز داده نشده‌اند را درخواست کن
        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }

// اعلان خوش‌آمدگویی (فقط یک‌بار)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("welcome_shown", false)) {
            NotificationHelper.send(
                context = applicationContext,
                type = "SYSTEM",
                titleFa = "خوش آمدید!",
                titleEn = "Welcome!",
                descFa = "به برنامه مدیریت بودجه سیدنا خوش آمدید. امیدواریم تجربه خوبی داشته باشید.",
                descEn = "Welcome to Cidna Budget Management. We hope you have a great experience.",
                tag = "WELCOME"
            )
            prefs.edit().putBoolean("welcome_shown", true).apply()
        }

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

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                        }
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
            startDestination = AppRoute.Splash,
            modifier = modifier
        ) {
            // ۱. صفحه اسپلش اسکرین
            composable<AppRoute.Splash> {
                SplashScreen(
                    onAnimationFinished = {
                        navController.navigate(AppRoute.Login) {
                            popUpTo(AppRoute.Splash) { inclusive = true }
                        }
                    }
                )
            }

            // 🚀 ۲. صفحه لاگین هوشمند (اثر انگشت + فرم متنی)
            composable<AppRoute.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(AppRoute.MainStructure) {
                            popUpTo(AppRoute.Login) { inclusive = true }
                        }
                    }
                )
            }

            // ۳. صفحه افزودن تراکنش جدید
            composable<AppRoute.AddScreen> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.AddScreen>()

                AddScreen(
                    highlightId = route.highlightId,
                    onBackClick = {
                        navController.navigate(AppRoute.MainStructure) {
                            popUpTo(AppRoute.MainStructure) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onCategoriesClick = {
                        navController.navigate(AppRoute.Categories)
                    },
                    onDueClick = {
                        navController.navigate(AppRoute.Upcoming)
                    },
                    onGoalsClick = {
                        navController.navigate(AppRoute.Goals)
                    },
                    onLimitsClick = {
                        navController.navigate(AppRoute.Limits)
                    }
                )
            }

            composable<AppRoute.Categories> {
                CategoriesScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AppRoute.Limits> {
                BudgetLimitScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AppRoute.Upcoming> {
                UpcomingPaymentsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AppRoute.Goals> {
                SavingGoalsScreen(
                    onBackClick = { navController.popBackStack() }
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
                            TransactionsScreen(
                                onAddTransactionClick = {
                                    navController.navigate("AddScreen")
                                }
                            )
                        }
                        composable(BottomNavItem.Analytics.route) {
                            AnalyticsScreen(
                                onAddScreenClick = {
                                    navController.navigate("AddScreen")
                                }
                            )
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