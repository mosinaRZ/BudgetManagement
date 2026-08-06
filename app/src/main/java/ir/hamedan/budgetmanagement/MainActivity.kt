package ir.hamedan.budgetmanagement

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity // اضافه شدن فرگمنت اکتیویتی برای بیومتریک
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.OnboardingPreferences
import ir.hamedan.budgetmanagement.data.preferences.PermissionReminderPreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.getThemeMode
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.ui.components.CapsuleBottomNavigation
import ir.hamedan.budgetmanagement.ui.components.OnboardingDialog
import ir.hamedan.budgetmanagement.ui.components.OnboardingPermission
import ir.hamedan.budgetmanagement.ui.components.PermissionReminderBanner
import ir.hamedan.budgetmanagement.ui.components.onboardingPermissions
import ir.hamedan.budgetmanagement.ui.navigation.AppRoute
import ir.hamedan.budgetmanagement.ui.navigation.MainTabRoute
import ir.hamedan.budgetmanagement.ui.screens.add.AddScreen
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsScreen
import ir.hamedan.budgetmanagement.ui.screens.home.HomeScreen
import ir.hamedan.budgetmanagement.ui.screens.auth.LoginScreen
import ir.hamedan.budgetmanagement.ui.screens.budgetLimit.BudgetLimitScreen
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesScreen
import ir.hamedan.budgetmanagement.ui.screens.debtCredit.DebtCreditScreen
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsScreen
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

    // هر بار کاربر به یک درخواست مجوز پاسخ می‌دهد، این تریگر را بالا می‌بریم
    // تا دیالوگ آنبوردینگ وضعیت «فعال/دادن» را دوباره محاسبه کند.
    private var permissionRefreshTrigger by mutableIntStateOf(0)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionRefreshTrigger++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // مقداردهی اولیه واحد پول
        CurrencySharedPreferences.init(applicationContext)

        AppNotificationManager.createChannel(applicationContext)

        // 🚀 درخواست خودکار مجوزها از اینجا حذف شد.
        // حالا دیالوگ آنبوردینگ (اولین ورود کاربر) با توضیح هر مجوز، خودش این درخواست را می‌زند.

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

            // آیا دیالوگ اولین ورود (خوش‌آمدگویی + مجوزها) باید نشان داده شود؟
            var showOnboarding by remember { mutableStateOf(!OnboardingPreferences.isCompleted(context)) }

            // اگر کاربر از تنظیمات گوشی مجوزی را تغییر داد و به اپ برگشت، وضعیت را رفرش کن
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionRefreshTrigger++
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

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

                if (showOnboarding) {
                    val isPersian = LocaleHelper.getLanguage(context) == "fa"
                    OnboardingDialog(
                        isPersian = isPersian,
                        isPermissionGranted = { permission ->
                            permissionRefreshTrigger // فقط برای وابسته‌کردن ری‌کامپوز به این state
                            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                        },
                        onRequestPermissions = { perms ->
                            requestPermissionsLauncher.launch(perms.toTypedArray())
                        },
                        onFinish = {
                            OnboardingPreferences.setCompleted(context)
                            showOnboarding = false
                        }
                    )
                }
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
                    onGoalsClick = {
                        navController.navigate(AppRoute.Goals)
                    },
                    onLimitsClick = {
                        navController.navigate(AppRoute.Limits)
                    },
                    onDebtClick = {
                        navController.navigate(AppRoute.Debt)
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
                    onBackClick = { navController.popBackStack() },
                    onCategoriesClick = {
                        navController.navigate(AppRoute.Categories)
                    }
                )
            }

            composable<AppRoute.Goals> {
                SavingGoalsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AppRoute.Debt> {
                DebtCreditScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ۴. ساختار اصلی برنامه پس از لاگین موفق
            composable<AppRoute.MainStructure> {
                val appNavController = rememberNavController()
                val appBackStackEntry by appNavController.currentBackStackEntryAsState()
                val appCurrentRoute = appBackStackEntry?.destination?.route

                val context = LocalContext.current
                val isPersian = LocaleHelper.getLanguage(context) == "fa"

                // ===== یادآوری ملایمِ مجوزهای داده‌نشده (بنر، نه دیالوگ مسدودکننده) =====
                var reminderPermission by remember { mutableStateOf<OnboardingPermission?>(null) }

                LaunchedEffect(permissionRefreshTrigger) {
                    reminderPermission = if (OnboardingPreferences.isCompleted(context)) {
                        onboardingPermissions(Build.VERSION.SDK_INT).firstOrNull { perm ->
                            val granted = perm.permissions.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            !granted && PermissionReminderPreferences.shouldRemindNow(context, perm.key)
                        }
                    } else null
                }

                // همین که بنر واقعاً روی صفحه اومد، فاصلهٔ عادی (۴ روزه) رو براش ثبت کن
                LaunchedEffect(reminderPermission) {
                    reminderPermission?.let {
                        PermissionReminderPreferences.markShownNow(context, it.key)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {

                    NavHost(
                        navController = appNavController,
                        startDestination = MainTabRoute.Home,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<MainTabRoute.Home> {
                            HomeScreen(
                                onThemeToggle = onThemeToggle,
                                onSeeAllTransactionsClick = {
                                    appNavController.navigate(MainTabRoute.Transactions) {
                                        popUpTo(appNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onAddScreenClickDebt = {
                                    navController.navigate(AppRoute.AddScreen(highlightId = "debtcredit"))
                                },
                                onAddScreenClickLimit = {
                                    navController.navigate(AppRoute.AddScreen(highlightId = "limit"))
                                },
                                onAddScreenClickPiggy = {
                                    navController.navigate(AppRoute.AddScreen(highlightId = "piggy"))
                                },
                                onCategoriesClick = {
                                    navController.navigate(AppRoute.Categories)
                                }
                            )
                        }

                        composable<MainTabRoute.Transactions> {
                            TransactionsScreen(
                                onAddTransactionClick = {
                                    navController.navigate(AppRoute.AddScreen())
                                }
                            )
                        }

                        composable<MainTabRoute.Analytics> {
                            AnalyticsScreen(
                                onAddScreenClick = {
                                    navController.navigate(AppRoute.AddScreen())
                                }
                            )
                        }

                        composable<MainTabRoute.Settings> {
                            SettingsScreen(
                                onThemeToggle = onThemeToggle,
                                onAddScreenClick = {
                                    navController.navigate(AppRoute.AddScreen(highlightId = "category"))
                                },
                                onLoginClick = {
                                    navController.navigate(AppRoute.Login) {
                                        popUpTo(AppRoute.MainStructure) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    // ===== بنر یادآوری مجوز (بالای صفحه، شناور روی محتوا) =====
                    reminderPermission?.let { perm ->
                        PermissionReminderBanner(
                            visible = true,
                            isPersian = isPersian,
                            permission = perm,
                            onAllow = {
                                requestPermissionsLauncher.launch(perm.permissions.toTypedArray())
                                reminderPermission = null
                            },
                            onLater = {
                                PermissionReminderPreferences.snooze(context, perm.key)
                                reminderPermission = null
                            },
                            onNeverAskAgain = {
                                PermissionReminderPreferences.dismissForever(context, perm.key)
                                reminderPermission = null
                            }
                        )
                    }

                    // ===== Bottom Bar + FAB =====
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
                                        // فعلاً هنوز از BottomNavItem قدیمی استفاده می‌کنیم
                                        // در مرحله بعد آن را هم Type-safe می‌کنیم
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
                                    navController.navigate(AppRoute.AddScreen())
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