package ir.hamedan.budgetmanagement

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity // اضافه شدن فرگمنت اکتیویتی برای بیومتریک
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.security.AppLockPreferences
import ir.hamedan.budgetmanagement.data.preferences.OnboardingPreferences
import ir.hamedan.budgetmanagement.data.preferences.PermissionReminderPreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.getThemeMode
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.ui.components.CapsuleBottomNavigation
import ir.hamedan.budgetmanagement.ui.components.InAppNotificationHint
import ir.hamedan.budgetmanagement.ui.components.OnboardingDialog
import ir.hamedan.budgetmanagement.ui.components.OnboardingPermission
import ir.hamedan.budgetmanagement.ui.components.PermissionReminderItem
import ir.hamedan.budgetmanagement.ui.components.PermissionReminderSheet
import ir.hamedan.budgetmanagement.ui.components.onboardingPermissions
import ir.hamedan.budgetmanagement.ui.navigation.AppRoute
import ir.hamedan.budgetmanagement.ui.navigation.MainTabRoute
import ir.hamedan.budgetmanagement.ui.screens.profile.ProfileScreen
import ir.hamedan.budgetmanagement.ui.screens.add.AddScreen
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsScreen
import ir.hamedan.budgetmanagement.ui.screens.home.HomeScreen
import ir.hamedan.budgetmanagement.ui.screens.auth.LoginScreen
import ir.hamedan.budgetmanagement.ui.screens.auth.RegisterScreen
import ir.hamedan.budgetmanagement.ui.screens.auth.PasswordResetScreen
import ir.hamedan.budgetmanagement.ui.screens.budgetLimit.BudgetLimitScreen
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesScreen
import ir.hamedan.budgetmanagement.ui.screens.debtCredit.DebtCreditScreen
import ir.hamedan.budgetmanagement.ui.screens.devices.DevicesScreen
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsScreen
import ir.hamedan.budgetmanagement.ui.screens.splash.SplashScreen
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionsScreen
import ir.hamedan.budgetmanagement.ui.screens.settings.NotificationCalibrationScreen
import ir.hamedan.budgetmanagement.ui.screens.settings.SettingsScreen
import ir.hamedan.budgetmanagement.ui.theme.BudgetManagementTheme
import ir.hamedan.budgetmanagement.data.notification.AppNotificationManager
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
// 🚀 تغییر مهم: ارث‌بری از FragmentActivity برای جلوگیری از کرش اثر انگشت
class MainActivity : FragmentActivity() {

    companion object {
        const val ACTION_ADD_TRANSACTION = "ir.hamedan.budgetmanagement.action.ADD_TRANSACTION"
        private const val SESSION_CHECK_INTERVAL_MS = 60_000L
    }

    override fun onStart() {
        super.onStart()
        val app = applicationContext as BudgetApp
        app.isAppInForeground = true
        AppNotificationManager.cancelAll(applicationContext)
    }

    override fun onStop() {
        val app = applicationContext as BudgetApp
        app.isAppInForeground = false
        super.onStop()
    }


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    // هر بار کاربر به یک درخواست مجوز پاسخ می‌دهد، این تریگر را بالا می‌بریم
    // تا دیالوگ آنبوردینگ وضعیت «فعال/دادن» را دوباره محاسبه کند.
    private var permissionRefreshTrigger by mutableIntStateOf(0)

    private var shortcutOpenAddTransaction by mutableStateOf(false)

    // Set when the user taps a device-security notification: open the Devices screen.
    private var openDevicesRequested by mutableStateOf(false)

    private fun handleShortcutIntent(intent: Intent?) {
        shortcutOpenAddTransaction = intent?.action == ACTION_ADD_TRANSACTION
        openDevicesRequested =
            intent?.getStringExtra(AppNotificationManager.EXTRA_OPEN_ROUTE) == AppNotificationManager.ROUTE_DEVICES
        // Consume it so a configuration change does not re-open the screen.
        intent?.removeExtra(AppNotificationManager.EXTRA_OPEN_ROUTE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionRefreshTrigger++
    }

    private fun isPermissionGranted(permission: OnboardingPermission): Boolean =
        permission.permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * کاربر قبلاً درخواست سیستمی را دیده و (دوبار) رد کرده؛ اندروید دیگر دیالوگ نشان نمی‌دهد
     * و تنها راه، تنظیمات برنامه است. پیش از اولین درخواست، این تابع false برمی‌گرداند.
     */
    private fun isPermanentlyDenied(permission: OnboardingPermission): Boolean {
        if (isPermissionGranted(permission)) return false
        if (!PermissionReminderPreferences.wasRequested(this, permission.key)) return false
        return permission.permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }
    }

    /** درخواست سیستمی؛ و اگر دیگر ممکن نیست، مستقیم صفحهٔ تنظیمات برنامه. */
    private fun requestOrOpenSettings(permission: OnboardingPermission) {
        if (isPermanentlyDenied(permission)) {
            openAppSettings()
            return
        }
        PermissionReminderPreferences.markRequested(this, permission.key)
        requestPermissionsLauncher.launch(permission.permissions.toTypedArray())
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // مقداردهی اولیه واحد پول
        CurrencySharedPreferences.init(applicationContext)

        AppNotificationManager.createChannel(applicationContext)
        handleShortcutIntent(intent)

        // While the app is visible, periodically verify the session with the server. If this
        // device was removed from the account, the check fails with 401/403, the session is
        // cleared and the navigation guard in TheApp sends the user to the login screen,
        // whichever screen they are on. The first check runs as soon as the app is shown.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val app = applicationContext as BudgetApp
                while (true) {
                    if (app.container.authRepository.isAuthenticated()) {
                        app.container.syncScheduler.enqueueNow()
                    }
                    delay(SESSION_CHECK_INTERVAL_MS)
                }
            }
        }

        // 🚀 درخواست خودکار مجوزها از اینجا حذف شد.
        // حالا دیالوگ آنبوردینگ (اولین ورود کاربر) با توضیح هر مجوز، خودش این درخواست را می‌زند.

        setContent {
            val context = LocalContext.current

            // حالت تم ذخیره شده
            var themeMode by remember { mutableIntStateOf(getThemeMode(context)) }

            // وضعیت تم سیستم
            val isSystemDark = isSystemInDarkTheme()

            // آیا دیالوگ اولین ورود (خوش‌آمدگویی + مجوزها) باید نشان داده شود؟
            var showOnboarding by remember { mutableStateOf(!OnboardingPreferences.isCompleted(context)) }
            var inAppNotification by remember {
                mutableStateOf<NotificationHelper.InAppNotification?>(null)
            }

            LaunchedEffect(Unit) {
                NotificationHelper.inAppNotifications.collect { notification ->
                    inAppNotification = notification
                }
            }

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
                Box(modifier = Modifier.fillMaxSize()) {

                    TheApp(
                        modifier = Modifier.fillMaxSize(),
                        openAddTransactionRequested = shortcutOpenAddTransaction,
                        onShortcutConsumed = { shortcutOpenAddTransaction = false },
                        openDevicesRequested = openDevicesRequested,
                        onDevicesConsumed = { openDevicesRequested = false },
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
                                val permission = onboardingPermissions(Build.VERSION.SDK_INT)
                                    .firstOrNull { it.permissions == perms }
                                if (permission != null) {
                                    requestOrOpenSettings(permission)
                                } else {
                                    requestPermissionsLauncher.launch(perms.toTypedArray())
                                }
                            },
                            isPermissionBlocked = { permission ->
                                permissionRefreshTrigger // وابستگی به state برای ری‌کامپوز
                                isPermanentlyDenied(permission)
                            },
                            onFinish = {
                                OnboardingPreferences.setCompleted(context)
                                showOnboarding = false
                            }
                        )
                    }


                    inAppNotification?.let { notification ->
                        InAppNotificationHint(
                            isPersian = LocaleHelper.getLanguage(context) == "fa",
                            titleFa = notification.titleFa,
                            titleEn = notification.titleEn,
                            bodyFa = notification.bodyFa,
                            bodyEn = notification.bodyEn,
                            visible = true,
                            onDismiss = { inAppNotification = null },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 150.dp)
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    private fun TheApp(
        modifier: Modifier = Modifier,
        openAddTransactionRequested: Boolean = false,
        onShortcutConsumed: () -> Unit = {},
        openDevicesRequested: Boolean = false,
        onDevicesConsumed: () -> Unit = {},
        onThemeToggle: () -> Unit = {}
    ) {

        val navController = rememberNavController()
        val app = LocalContext.current.applicationContext as BudgetApp
        val authScope = rememberCoroutineScope()
        val sessionAuthenticated by app.container.authSessionStore.authenticated.collectAsState()
        val currentRouteEntry by navController.currentBackStackEntryAsState()
        val applicationContext = LocalContext.current.applicationContext

        LaunchedEffect(openAddTransactionRequested, sessionAuthenticated, currentRouteEntry?.destination?.route) {
            val route = currentRouteEntry?.destination?.route.orEmpty()
            if (openAddTransactionRequested &&
                sessionAuthenticated &&
                route.isNotBlank() &&
                !route.contains("Login") &&
                !route.contains("Splash") &&
                !route.contains("AddScreen")
            ) {
                navController.navigate(AppRoute.AddScreen())
                onShortcutConsumed()
            }
        }

        // Tapped a "new device" / "device removed" alert: go straight to the device list.
        LaunchedEffect(openDevicesRequested, sessionAuthenticated, currentRouteEntry?.destination?.route) {
            val route = currentRouteEntry?.destination?.route.orEmpty()
            if (openDevicesRequested &&
                sessionAuthenticated &&
                route.isNotBlank() &&
                !route.contains("Login") &&
                !route.contains("Register") &&
                !route.contains("PasswordReset") &&
                !route.contains("Splash")
            ) {
                if (!route.contains("Devices")) navController.navigate(AppRoute.Devices)
                onDevicesConsumed()
            }
        }

        val appLockPreferences = remember(applicationContext) {
            AppLockPreferences(applicationContext)
        }
        var lifecycleResumeTrigger by remember { mutableIntStateOf(0) }

        // After two minutes in the background, require local re-authentication.
        // This never clears or changes access/refresh tokens.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, sessionAuthenticated) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        if (sessionAuthenticated) {
                            appLockPreferences.markBackgrounded(System.currentTimeMillis())
                        }
                    }
                    Lifecycle.Event.ON_START -> lifecycleResumeTrigger++
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(sessionAuthenticated, currentRouteEntry?.destination?.route, lifecycleResumeTrigger) {
            val route = currentRouteEntry?.destination?.route.orEmpty()

            // The lock must apply to EVERY signed-in screen (Profile, Devices, Categories, Add...),
            // not only the tabbed MainStructure: those screens are siblings of MainStructure in the
            // root NavHost, so checking for "MainStructure" alone let the user stay on them forever.
            val isAuthOrSplashRoute = route.contains("Login") ||
                    route.contains("Register") ||
                    route.contains("PasswordReset") ||
                    route.contains("Splash")

            if (sessionAuthenticated &&
                route.isNotBlank() &&
                !isAuthOrSplashRoute &&
                appLockPreferences.shouldLockNow()
            ) {
                appLockPreferences.markLockRequired()
                navController.navigate(AppRoute.Login(localUnlockOnly = true)) {
                    // Clear the whole back stack so Back from the lock screen can never return
                    // to the screen that was open when the app was locked.
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }

            if (!sessionAuthenticated &&
                route.isNotBlank() &&
                !route.contains("Login") &&
                !route.contains("Register") &&
                !route.contains("PasswordReset") &&
                !route.contains("Splash")
            ) {
                navController.navigate(AppRoute.Login()) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = AppRoute.Splash,
            modifier = modifier
        ) {
            // ۱. صفحه اسپلش اسکرین
            composable<AppRoute.Splash> {
                SplashScreen(
                    onAnimationFinished = {
                        // Navigate with a concrete route instance. Do not store the two
                        // branches in a variable typed as AppRoute: typed Navigation infers
                        // the generic route from the static type, which would make it try
                        // to serialize the sealed parent instead of the concrete destination.
                        if (sessionAuthenticated) {
                            // The existing server session remains valid. Opening the app only
                            // requires local re-authentication; no token refresh/login is triggered.
                            navController.navigate(AppRoute.Login(localUnlockOnly = true)) {
                                popUpTo(AppRoute.Splash) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(AppRoute.Login()) {
                                popUpTo(AppRoute.Splash) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            // 🚀 ۲. صفحه لاگین هوشمند (اثر انگشت + فرم متنی)
            composable<AppRoute.Login> { backStackEntry ->
                val loginRoute = backStackEntry.toRoute<AppRoute.Login>()

                // The server session can end while the local-unlock screen is showing (for example
                // this device was removed from the account while the app was in the background).
                // Local unlock is meaningless then: switch to the full sign-in form.
                LaunchedEffect(sessionAuthenticated, loginRoute.localUnlockOnly) {
                    if (loginRoute.localUnlockOnly && !sessionAuthenticated) {
                        navController.navigate(AppRoute.Login()) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                LoginScreen(
                    localUnlockOnly = loginRoute.localUnlockOnly,
                    onLoginSuccess = {
                        appLockPreferences.clearLock()
                        navController.navigate(AppRoute.MainStructure) {
                            popUpTo(AppRoute.Login()) { inclusive = true }
                        }
                    },
                    onRegister = {
                        navController.navigate(AppRoute.Register)
                    },
                    onForgotPassword = {
                        navController.navigate(AppRoute.PasswordReset)
                    }
                )
            }

            composable<AppRoute.PasswordReset> {
                PasswordResetScreen(
                    onResetSuccess = {
                        navController.navigate(AppRoute.Login()) {
                            popUpTo(AppRoute.PasswordReset) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
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

            composable<AppRoute.Profile> {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onForgotPassword = { navController.navigate(AppRoute.PasswordReset) }
                )
            }

            composable<AppRoute.Devices> {
                DevicesScreen(onBack = { navController.popBackStack() })
            }

            composable<AppRoute.NotificationCalibration> {
                NotificationCalibrationScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AppRoute.Register> {
                RegisterScreen(
                    onRegistered = {
                        navController.navigate(AppRoute.Login()) {
                            popUpTo(AppRoute.Register) {
                                inclusive = true
                            }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // ۴. ساختار اصلی برنامه پس از لاگین موفق
            composable<AppRoute.MainStructure> {
                val appNavController = rememberNavController()
                val appBackStackEntry by appNavController.currentBackStackEntryAsState()
                val appCurrentRoute = appBackStackEntry?.destination?.route

                val context = LocalContext.current
                val isPersian = LocaleHelper.getLanguage(context) == "fa"

                // ===== یادآوری ملایمِ مجوزهای داده‌نشده (Bottom Sheet، نه دیالوگ مسدودکننده) =====
                // فقط مجوزهای «remindable» که هنوز داده نشده‌اند و زمانشان رسیده باعث نمایش شیت می‌شوند؛
                // ولی داخل شیت همهٔ مجوزهای مهم (با وضعیت زنده) دیده می‌شوند تا پیشرفت معنا داشته باشد.
                var showPermissionSheet by remember { mutableStateOf(false) }
                var reminderItems by remember { mutableStateOf<List<PermissionReminderItem>>(emptyList()) }

                LaunchedEffect(permissionRefreshTrigger) {
                    // مجوزی که کاربر صریحاً گفته «دیگه نشون نده» (و هنوز داده نشده) اصلاً در شیت نمی‌آید.
                    val remindable = onboardingPermissions(Build.VERSION.SDK_INT).filter {
                        it.remindable &&
                                (isPermissionGranted(it) || !PermissionReminderPreferences.isDismissedForever(context, it.key))
                    }
                    reminderItems = remindable.map { perm ->
                        PermissionReminderItem(
                            permission = perm,
                            granted = isPermissionGranted(perm),
                            permanentlyDenied = isPermanentlyDenied(perm)
                        )
                    }

                    if (!showPermissionSheet &&
                        OnboardingPreferences.isCompleted(context) &&
                        OnboardingPreferences.isReminderGracePeriodOver(context)
                    ) {
                        val due = reminderItems.filter {
                            !it.granted && PermissionReminderPreferences.shouldRemindNow(context, it.permission.key)
                        }
                        if (due.isNotEmpty()) {
                            // شیت همهٔ مجوزهای هنوز-داده‌نشده را با هم نشان می‌دهد؛ پس زمان‌بندی همه هم‌زمان می‌ماند.
                            reminderItems.filter { !it.granted }.forEach {
                                PermissionReminderPreferences.markShownNow(context, it.permission.key)
                            }
                            showPermissionSheet = true
                        }
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
                                onProfileClick = { navController.navigate(AppRoute.Profile) },
                                onForgotPassword = { navController.navigate(AppRoute.PasswordReset) },
                                onAddScreenClick = {
                                    navController.navigate(AppRoute.AddScreen(highlightId = "category"))
                                },
                                onNotificationCalibrationClick = {
                                    navController.navigate(AppRoute.NotificationCalibration)
                                },
                                onDevicesClick = {
                                    navController.navigate(AppRoute.Devices)
                                },
                                onLoginClick = {
                                    authScope.launch {
                                        app.container.authRepository.logout()
                                        navController.navigate(AppRoute.Login()) {
                                            popUpTo(AppRoute.MainStructure) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // ===== شیت یادآوری مجوز =====
                    if (showPermissionSheet && reminderItems.isNotEmpty()) {
                        PermissionReminderSheet(
                            isPersian = isPersian,
                            items = reminderItems,
                            onAllow = { perm -> requestOrOpenSettings(perm) },
                            onLater = {
                                // «بعداً» (و کشیدن/لمس بیرون/Back): زمان‌بندیِ مجوزهای هنوز-داده‌نشده
                                reminderItems.filter { !it.granted }.forEach {
                                    PermissionReminderPreferences.snooze(context, it.permission.key)
                                }
                                showPermissionSheet = false
                            },
                            onNeverAskAgain = {
                                reminderItems.filter { !it.granted }.forEach {
                                    PermissionReminderPreferences.dismissForever(context, it.permission.key)
                                }
                                showPermissionSheet = false
                            },
                            onAllGranted = { showPermissionSheet = false }
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