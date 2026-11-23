package ir.hamedan.budgetmanagement.ui.screens.settings

import android.app.Activity
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarDefaults
import androidx.compose.material3.SnackbarHostState
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role



import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import ir.hamedan.budgetmanagement.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthContext
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthErrorBanner
import ir.hamedan.budgetmanagement.ui.screens.auth.PasswordPolicy
import ir.hamedan.budgetmanagement.ui.screens.auth.PasswordStrengthMeter
import ir.hamedan.budgetmanagement.ui.screens.auth.authErrorMessage
import ir.hamedan.budgetmanagement.ui.screens.auth.credentialTextStyle
import ir.hamedan.budgetmanagement.data.security.RememberedLoginStore
import ir.hamedan.budgetmanagement.data.guest.GuestModeStore
import ir.hamedan.budgetmanagement.ui.screens.guest.GuestDeleteWorkspaceDialog
import ir.hamedan.budgetmanagement.ui.screens.guest.GuestPasscodeDialog
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.preferences.SharedPreferences
import ir.hamedan.budgetmanagement.data.security.ScreenSecurity
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.platform.biometric.BiometricPromptManager
import ir.hamedan.budgetmanagement.data.export.ExportFormat
import ir.hamedan.budgetmanagement.data.export.ExportManager
import ir.hamedan.budgetmanagement.data.export.ExportPeriod
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.utils.WebLinks
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import ir.hamedan.budgetmanagement.BuildConfig
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.ui.components.AppConfirmDialog
import ir.hamedan.budgetmanagement.ui.screens.profile.ProfileCompletion
import kotlinx.coroutines.flow.flowOf

enum class SettingsMenu {
    LANGUAGE, CURRENCY, SECURITY, EXPORT, ABOUT, NOTIFICATION, NONE
}

/** یک مورد قابل‌جستجوی تنظیمات؛ همان متن در ردیف نمایش داده می‌شود تا جستجو و نمایش هرگز ناهماهنگ نشوند. */
private data class SettingsEntry(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String
) {
    fun title(isPersian: Boolean) = if (isPersian) titleFa else titleEn
    fun subtitle(isPersian: Boolean) = if (isPersian) subtitleFa else subtitleEn
}

private val ALL_SETTINGS_ITEMS = listOf(
    SettingsEntry("profile", "پروفایل من", "My Profile", "اطلاعات شخصی و امنیت حساب", "Personal information and account security"),
    SettingsEntry("theme", "ظاهر برنامه", "App Theme", "تغییر حالت تاریک و روشن", "Switch between Dark and Light mode"),
    SettingsEntry("language", "زبان برنامه", "App Language", "انتخاب زبان کاربری (فارسی / انگلیسی)", "Choose UI language (Persian / English)"),
    SettingsEntry("currency", "واحد پولی", "Currency", "نمایش مبالغ بر اساس تومان یا ریال", "Display amounts in Toman or Rial"),
    SettingsEntry("notifications", "تنظیمات اعلان‌ها", "Notification Settings", "کنترل نحوه دریافت اعلان‌های برنامه", "Control how you receive app notifications"),
    SettingsEntry("categories", "مدیریت دسته‌بندی‌ها", "Manage Categories", "ویرایش، حذف یا ایجاد دسته‌های خرید و فروش", "Edit, delete, or create transaction categories"),
    SettingsEntry("devices", "دستگاه‌های حساب", "Account Devices", "مدیریت دستگاه‌های متصل به حساب", "Manage devices connected to your account"),
    SettingsEntry("biometric", "ورود با اثر انگشت", "Biometric Login", "باز کردن برنامه با اثر انگشت", "Unlock the app with your fingerprint"),
    SettingsEntry("screenProtection", "محافظت از محتوای صفحه", "Screen content protection", "جلوگیری از اسکرین‌شات و نمایش محتوا در برنامه‌های اخیر", "Block screenshots and hide content in Recents"),
    SettingsEntry("password", "تغییر گذرواژه", "Change Password", "گذرواژه ورود به حساب را عوض کنید", "Update your sign-in password"),
    SettingsEntry("exportExcel", "خروجی Excel", "Excel export", "جدول قابل‌ویرایش از تراکنش‌ها (XLSX)", "An editable spreadsheet of your transactions (XLSX)"),
    SettingsEntry("exportPdf", "خروجی PDF", "PDF report", "گزارش خوانا برای چاپ و اشتراک‌گذاری", "A readable report to print or share"),
    SettingsEntry("backup", "پشتیبان‌گیری", "Backup", "نسخه‌ی رمزنگاری‌شده از تمام اطلاعات شما", "An encrypted copy of all your data"),
    SettingsEntry("email", "ارسال ایمیل به پشتیبانی", "Email Support", "cidna.app@gmail.com", "cidna.app@gmail.com"),
    SettingsEntry("whatsapp", "واتس‌اپ توسعه‌دهنده", "WhatsApp Contact", "گفتگوی مستقیم با توسعه‌دهنده", "Chat with the developer"),
    SettingsEntry("twitter", "ایکس (توییتر)", "X (Twitter)", "@CidnaApp", "@CidnaApp"),
    SettingsEntry("instagram", "اینستاگرام", "Instagram", "@cidnaapp", "@cidnaapp"),
    SettingsEntry("privacy", "حریم خصوصی", "Privacy Policy", "نحوه‌ی نگهداری و استفاده از اطلاعات شما", "How your information is handled"),
    SettingsEntry("securityPage", "امنیت و رمزنگاری", "Security & Encryption", "جزئیات رمزنگاری و امنیت داده‌ها", "Encryption and data-security details"),
    SettingsEntry("terms", "قوانین و مقررات", "Terms of Service", "شرایط استفاده از برنامه", "Terms for using the app"),
    SettingsEntry("licenses", "مجوزهای متن‌باز", "Open-source licenses", "کتابخانه‌های استفاده‌شده در برنامه", "Libraries used in the app"),
    SettingsEntry("logout", "خروج از حساب کاربری", "Log Out", "خروج از این دستگاه", "Sign out of this device"),
    SettingsEntry("deleteAccount", "حذف حساب کاربری", "Delete Account", "پاک‌کردن دائمی حساب و داده‌ها", "Permanently erase your account and data")
)

private val SETTINGS_ENTRY_BY_ID = ALL_SETTINGS_ITEMS.associateBy { it.id }

private const val SHEET_THEME = "theme"
private const val SHEET_LANGUAGE = "language"
private const val SHEET_CURRENCY = "currency"
private const val SHEET_NOTIFICATIONS = "notifications"

/** فهرست ردیف‌هایی که جستجو نشانشان می‌دهد؛ جفت‌های `(قابل‌نمایش؟, ردیف)`. */
private fun settingsRows(vararg entries: Pair<Boolean, @Composable () -> Unit>): List<@Composable () -> Unit> =
    entries.filter { it.first }.map { it.second }

private fun initialsOf(first: String, last: String, isPersian: Boolean): String {
    val a = first.trim().firstOrNull { it.isLetter() }
    val b = last.trim().firstOrNull { it.isLetter() }
    // نیم‌فاصله مانع چسبیدن دو حرف فارسی به هم می‌شود.
    return listOfNotNull(a, b).joinToString(if (isPersian) "\u200C" else "") { it.uppercase() }
}

private fun maskedPhone(phone: String, isPersian: Boolean): String {
    if (phone.length < 7) return phone
    val masked = phone.take(4) + " ••• " + phone.takeLast(3)
    // LRM: ارقام و نقطه‌ها در چیدمان راست‌به‌چپ هم به ترتیب درست نمایش داده می‌شوند.
    return "\u200E" + if (isPersian) TransactionShareFormatter.toPersianDigits(masked) else masked
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onCurrencyChanged: (String) -> Unit = {},  // "IRT" یا "IRR"
    onLoginClick: () -> Unit = {},
    onAddScreenClick: () -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onNotificationCalibrationClick: () -> Unit = {},
    onDevicesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    /** Called after the account was deleted and this device was signed out of it. */
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var activeSheet by rememberSaveable { mutableStateOf<String?>(null) }

    val app = context.applicationContext as BudgetApp
    // Guest mode has its own passcode and fingerprint, separate from the account's.
    val isGuest = remember(context) { app.container.guestModeStore.isSessionActive }
    val guestStore = remember(context) { app.container.guestModeStore }

    var isBiometricEnabled by remember(context) {
        mutableStateOf(
            if (isGuest) guestStore.isBiometricEnabled() else SharedPreferences.getBiometricEnabled(context)
        )
    }
    // A stale opt-in without a usable Keystore credential must not advertise biometric login.
    LaunchedEffect(isGuest) {
        if (!isGuest && isBiometricEnabled &&
            (!app.container.rememberedLoginStore.hasBiometricCredential() ||
                    !app.container.rememberedLoginStore.canUseStrongBiometric())) {
            isBiometricEnabled = false
            SharedPreferences.setBiometricEnabled(context, false)
        }
    }

    var isScreenProtected by remember(context) { mutableStateOf(ScreenSecurity.isEnabled(context)) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showBiometricPasswordDialog by remember { mutableStateOf(false) }
    var biometricSetupError by remember { mutableStateOf<String?>(null) }
    val rememberedLoginStore = remember(context) { RememberedLoginStore(context) }
    var themeMode by remember(context) { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // به‌روزرسانی مقدار تم در بدو باز شدن شیت
    LaunchedEffect(activeSheet) {
        if (activeSheet == SHEET_THEME) {
            themeMode = ThemePreferences.getThemeMode(context)
        }
    }

    // واحد پولی ذخیره‌شده یا مقدار پیش‌فرض
    var currentCurrencyCode by remember(context) {
        mutableStateOf(CurrencySharedPreferences.getCurrency(context))
    }
    var notifMode by remember { mutableStateOf(NotificationPreferences.getMode(context)) }

    var showExportSheet by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf<ExportFormat?>(null) } // PDF یا XLSX
    val exportScope = rememberCoroutineScope()
    val settingsSnackbarHostState = remember { SnackbarHostState() }

    // ─── پروفایل برای کارت بالای صفحه (فقط خواندن از دیتابیس محلی؛ درخواست شبکه‌ای نمی‌زند) ───
    val profileUserId = remember(context, isGuest) {
        if (isGuest) GuestModeStore.GUEST_USER_ID else app.container.authSessionStore.userId()
    }
    val profileFlow = remember(profileUserId) {
        profileUserId?.takeIf { it.isNotBlank() }
            ?.let { app.container.profileRepository.observeLocalProfile(it) }
            ?: flowOf<UserEntity?>(null)
    }
    val profile by profileFlow.collectAsState(initial = null)

    val hasBiometricHardware = remember(context) {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                result != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    val systemInDark = isSystemInDarkTheme()
    val effectiveDark = when (themeMode) {
        ThemePreferences.MODE_DARK -> true
        ThemePreferences.MODE_LIGHT -> false
        else -> systemInDark
    }
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    fun matchesSearch(id: String): Boolean {
        if (searchQuery.isBlank()) return true
        val entry = SETTINGS_ENTRY_BY_ID[id] ?: return false
        val query = normalizeForSearch(searchQuery)
        return normalizeForSearch(entry.title(isPersian)).contains(query) ||
                normalizeForSearch(entry.subtitle(isPersian)).contains(query)
    }

    fun visible(id: String): Boolean = matchesSearch(id)

    // آیا هیچ آیتمی با جستجوی فعلی مطابقت دارد؟
    val hasAnyMatch = remember(searchQuery, isPersian) {
        if (searchQuery.isBlank()) true
        else {
            val query = normalizeForSearch(searchQuery)
            ALL_SETTINGS_ITEMS.any { item ->
                normalizeForSearch(item.title(isPersian)).contains(query) ||
                        normalizeForSearch(item.subtitle(isPersian)).contains(query)
            }
        }
    }

    val suggestion = remember(searchQuery, isPersian, hasAnyMatch) {
        if (!hasAnyMatch) findSuggestion(searchQuery, isPersian) else null
    }

    fun title(id: String) = SETTINGS_ENTRY_BY_ID.getValue(id).title(isPersian)
    fun subtitle(id: String) = SETTINGS_ENTRY_BY_ID.getValue(id).subtitle(isPersian)

    // ─── منطق روشن/خاموش‌کردن اثر انگشت (بدون تغییر نسبت به قبل) ───
    val onBiometricToggle: (Boolean) -> Unit = { targetChecked ->

        val activity = context as? FragmentActivity
        if (isGuest) {
            biometricSetupError = null
            when {
                activity == null -> biometricSetupError = if (isPersian) "امکان احراز هویت در این صفحه وجود ندارد." else "Biometric authentication is unavailable on this screen."
                !targetChecked -> BiometricPromptManager.showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        guestStore.disableBiometric()
                        isBiometricEnabled = false
                    }
                )
                !guestStore.hasPasscode() -> {
                    // Fingerprint needs a passcode to fall back on, so set that first.
                    biometricSetupError = if (isPersian) "ابتدا گذرواژه مهمان را تعیین کنید؛ سپس اثر انگشت را فعال کنید." else "Set a guest passcode first, then enable fingerprint."
                    showChangePasswordDialog = true
                }
                else -> enrollGuestBiometric(activity, guestStore, isPersian) { ok, error ->
                    isBiometricEnabled = guestStore.isBiometricEnabled()
                    biometricSetupError = error
                }
            }
        } else if (activity == null) {
            biometricSetupError = if (isPersian) "امکان احراز هویت در این صفحه وجود ندارد." else "Biometric authentication is unavailable on this screen."
        } else if (!targetChecked) {
            // خاموش کردن فقط پس از احراز هویت انجام می‌شود تا تغییر تنظیم امنیتی بدون تأیید کاربر ممکن نباشد.
            BiometricPromptManager.showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    isBiometricEnabled = false
                    SharedPreferences.setBiometricEnabled(context, false)
                    NotificationHelper.send(
                        context = context,
                        notificationType = NotificationType.SETTINGS_CHANGED,
                        type = "SYSTEM",
                        titleFa = "تنظیمات به‌روزرسانی شد",
                        titleEn = "Settings Updated",
                        descFa = "ورود با اثر انگشت غیرفعال شد.",
                        descEn = "Biometric login has been disabled.",
                        tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                    )
                }
            )
        } else {
            // اگر Credential قبلاً ساخته شده باشد فقط تأیید بیومتریک لازم است.
            // در غیر این صورت ابتدا رمز محلی بررسی و سپس Credential در Keystore ساخته می‌شود.
            biometricSetupError = null
            if (rememberedLoginStore.hasBiometricCredential() && rememberedLoginStore.canUseStrongBiometric()) {
                BiometricPromptManager.showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        isBiometricEnabled = true
                        SharedPreferences.setBiometricEnabled(context, true)
                        NotificationHelper.send(
                            context = context,
                            notificationType = NotificationType.SETTINGS_CHANGED,
                            type = "SYSTEM",
                            titleFa = "ورود با اثر انگشت فعال شد",
                            titleEn = "Biometric login enabled",
                            descFa = "از این پس می‌توانید بدون وارد کردن رمز عبور وارد شوید.",
                            descEn = "You can now unlock the app without typing your password.",
                            tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                        )
                    }
                )
            } else {
                showBiometricPasswordDialog = true
            }
        }
    }

    // ─── ردیف‌های هر گروه ───
    val prefRows = settingsRows(
        visible("theme") to @Composable {
            SettingsRow(
                title = title("theme"),
                subtitle = subtitle("theme"),
                icon = if (effectiveDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
                accent = SettingsAccent.VIOLET,
                onClick = { activeSheet = SHEET_THEME },
                trailing = { SettingsValueTrailing(if (effectiveDark) (if (isPersian) "تاریک" else "Dark") else (if (isPersian) "روشن" else "Light")) }
            )
        },
        visible("language") to @Composable {
            SettingsRow(
                title = title("language"),
                subtitle = subtitle("language"),
                icon = Icons.Default.Language,
                accent = SettingsAccent.SKY,
                onClick = { activeSheet = SHEET_LANGUAGE },
                trailing = { SettingsValueTrailing(if (isPersian) "فارسی" else "English") }
            )
        },
        visible("currency") to @Composable {
            SettingsRow(
                title = title("currency"),
                subtitle = subtitle("currency"),
                iconRes = R.drawable.ic_currency,
                accent = SettingsAccent.AMBER,
                onClick = { activeSheet = SHEET_CURRENCY },
                trailing = {
                    SettingsValueTrailing(
                        if (currentCurrencyCode == "IRR") (if (isPersian) "ریال" else "Rial") else (if (isPersian) "تومان" else "Toman")
                    )
                }
            )
        },
        visible("notifications") to @Composable {
            SettingsRow(
                title = title("notifications"),
                subtitle = subtitle("notifications"),
                icon = Icons.Default.Notifications,
                accent = SettingsAccent.ROSE,
                onClick = { activeSheet = SHEET_NOTIFICATIONS },
                trailing = {
                    SettingsValueTrailing(
                        if (notifMode == NotificationPreferences.MODE_IN_APP) (if (isPersian) "درون‌برنامه" else "In-app")
                        else (if (isPersian) "برنامه + سیستم" else "App + system")
                    )
                }
            )
        }
    )

    val manageRows = settingsRows(
        visible("categories") to @Composable {
            SettingsRow(
                title = title("categories"),
                subtitle = subtitle("categories"),
                iconRes = R.drawable.ic_category,
                accent = SettingsAccent.TEAL,
                onClick = { onAddScreenClick() },
                trailing = { SettingsChevron() }
            )
        },
        visible("devices") to @Composable {
            SettingsRow(
                title = title("devices"),
                subtitle = subtitle("devices"),
                icon = Icons.Default.Devices,
                accent = SettingsAccent.INDIGO,
                onClick = onDevicesClick,
                trailing = { SettingsChevron() }
            )
        }
    )

    val securityRows = settingsRows(
        (hasBiometricHardware && visible("biometric")) to @Composable {
            SettingsSwitchRow(
                title = title("biometric"),
                subtitle = subtitle("biometric"),
                icon = Icons.Default.Fingerprint,
                accent = SettingsAccent.TEAL,
                checked = isBiometricEnabled,
                onCheckedChange = onBiometricToggle
            )
        },
        visible("screenProtection") to @Composable {
            SettingsSwitchRow(
                title = title("screenProtection"),
                subtitle = subtitle("screenProtection"),
                icon = Icons.Default.VisibilityOff,
                accent = SettingsAccent.INDIGO,
                checked = isScreenProtected,
                onCheckedChange = { target ->
                    isScreenProtected = target
                    ScreenSecurity.setEnabled(context, target)
                }
            )
        },
        visible("password") to @Composable {
            SettingsRow(
                title = if (isGuest) (if (isPersian) "گذرواژه مهمان" else "Guest passcode") else title("password"),
                subtitle = if (isGuest) (if (isPersian) "تعیین یا تغییر گذرواژه‌ی حالت مهمان" else "Set or change the guest passcode") else subtitle("password"),
                icon = Icons.Default.Password,
                accent = SettingsAccent.AMBER,
                onClick = { showChangePasswordDialog = true },
                trailing = { SettingsChevron() }
            )
        }
    )

    val dataRows = settingsRows(
        visible("exportExcel") to @Composable {
            SettingsRow(
                title = title("exportExcel"),
                subtitle = subtitle("exportExcel"),
                icon = Icons.Default.TableChart,
                accent = SettingsAccent.TEAL,
                onClick = {
                    exportFormat = ExportFormat.XLSX
                    showExportSheet = true
                },
                trailing = { SettingsChevron() }
            )
        },
        visible("exportPdf") to @Composable {
            SettingsRow(
                title = title("exportPdf"),
                subtitle = subtitle("exportPdf"),
                icon = Icons.Default.PictureAsPdf,
                accent = SettingsAccent.ROSE,
                onClick = {
                    exportFormat = ExportFormat.PDF
                    showExportSheet = true
                },
                trailing = { SettingsChevron() }
            )
        },
        // Backup is a guest-only feature: guest data lives only on this phone.
        // An account's data is stored (encrypted) on the server and comes back
        // after sign-in, so signed-in users get no local backup at all.
        (isGuest && visible("backup")) to @Composable {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGroupLabel(
                    title = title("backup"),
                    subtitle = subtitle("backup")
                )
                LocalBackupButtons(
                    snackbarHostState = settingsSnackbarHostState,
                    isPersian = isPersian
                )
            }
        }
    )

    val supportRows = settingsRows(
        visible("email") to @Composable {
            SettingsRow(
                title = title("email"),
                subtitle = subtitle("email"),
                iconRes = R.drawable.ic_gmail,
                tint = settingsBrandColor(Color(0xFFC5221F), Color(0xFFFF8A80)),
                onClick = {
                    val email = "cidna.app@gmail.com"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        setPackage("com.google.android.gm")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$email")
                        }
                        try {
                            context.startActivity(Intent.createChooser(fallbackIntent, if (isPersian) "ارسال ایمیل" else "Send Email"))
                        } catch (e2: Exception) {
                            exportScope.launch {
                                settingsSnackbarHostState.showError(
                                    if (isPersian) "برنامه‌ای برای ارسال ایمیل پیدا نشد."
                                    else "No app found to send an email."
                                )
                            }
                        }
                    }
                },
                trailing = { SettingsChevron() }
            )
        },
        visible("whatsapp") to @Composable {
            SettingsRow(
                title = title("whatsapp"),
                subtitle = subtitle("whatsapp"),
                iconRes = R.drawable.ic_whatsapp,
                tint = settingsBrandColor(Color(0xFF128C7E), Color(0xFF25D366)),
                onClick = { openUrl(context, "https://wa.me/989180500841") },
                trailing = { SettingsChevron() }
            )
        },
        visible("twitter") to @Composable {
            SettingsRow(
                title = title("twitter"),
                subtitle = subtitle("twitter"),
                iconRes = R.drawable.ic_x,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { openUrl(context, "https://twitter.com/CidnaApp") },
                trailing = { SettingsChevron() }
            )
        },
        visible("instagram") to @Composable {
            SettingsRow(
                title = title("instagram"),
                subtitle = subtitle("instagram"),
                iconRes = R.drawable.ic_instagram,
                tint = settingsBrandColor(Color(0xFFD6005B), Color(0xFFFF6FA5)),
                onClick = { openUrl(context, "https://www.instagram.com/cidnaapp") },
                trailing = { SettingsChevron() }
            )
        }
    )

    // Legal and security pages come from the official website (same text as the store listings),
    // shown in a Custom Tab with the app's light/dark scheme.
    val legalRows = settingsRows(
        visible("privacy") to @Composable {
            SettingsRow(
                title = title("privacy"),
                subtitle = subtitle("privacy"),
                iconRes = R.drawable.ic_legal,
                accent = SettingsAccent.SLATE,
                onClick = { WebLinks.open(context, WebLinks.Page.PRIVACY, isPersian, darkTheme) },
                trailing = { SettingsChevron() }
            )
        },
        visible("securityPage") to @Composable {
            SettingsRow(
                title = title("securityPage"),
                subtitle = subtitle("securityPage"),
                iconRes = R.drawable.ic_legal,
                accent = SettingsAccent.SLATE,
                onClick = { WebLinks.open(context, WebLinks.Page.SECURITY, isPersian, darkTheme) },
                trailing = { SettingsChevron() }
            )
        },
        visible("terms") to @Composable {
            SettingsRow(
                title = title("terms"),
                subtitle = subtitle("terms"),
                iconRes = R.drawable.ic_legal,
                accent = SettingsAccent.SLATE,
                onClick = { WebLinks.open(context, WebLinks.Page.TERMS, isPersian, darkTheme) },
                trailing = { SettingsChevron() }
            )
        },
        visible("licenses") to @Composable {
            SettingsRow(
                title = title("licenses"),
                subtitle = subtitle("licenses"),
                iconRes = R.drawable.ic_legal,
                accent = SettingsAccent.SLATE,
                onClick = { WebLinks.open(context, WebLinks.Page.LICENSES, isPersian, darkTheme) },
                trailing = { SettingsChevron() }
            )
        }
    )

    val dangerRows = settingsRows(
        visible("logout") to @Composable {
            SettingsRow(
                title = if (isGuest) (if (isPersian) "خروج از حالت مهمان" else "Leave guest mode") else title("logout"),
                subtitle = if (isGuest) (if (isPersian) "بازگشت به صفحه‌ی ورود" else "Return to the sign-in screen") else subtitle("logout"),
                icon = Icons.Default.Logout,
                accent = SettingsAccent.DANGER,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showLogoutDialog = true }
            )
        },
        visible("deleteAccount") to @Composable {
            SettingsRow(
                title = if (isGuest) (if (isPersian) "حذف داده‌های مهمان" else "Delete guest data") else title("deleteAccount"),
                subtitle = if (isGuest) (if (isPersian) "پاک‌کردن دائمی همه‌ی اطلاعات این گوشی" else "Permanently erase everything on this phone") else subtitle("deleteAccount"),
                icon = Icons.Default.DeleteForever,
                accent = SettingsAccent.DANGER,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteAccountDialog = true }
            )
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 75.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding().height(5.dp))
            }

            if (suggestion != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable { searchQuery = suggestion }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = settingsBrandText(),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPersian)
                                "منظورت «$suggestion» بود؟"
                            else
                                "Did you mean \"$suggestion\"?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = settingsBrandText(),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!hasAnyMatch) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 22.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = settingsBrandText(),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPersian) "نتیجه‌ای پیدا نشد" else "No results found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian)
                                "برای «$searchQuery» موردی در تنظیمات پیدا نشد. عبارت دیگری را امتحان کنید."
                            else
                                "Nothing in Settings matches «$searchQuery». Try another search term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = { searchQuery = "" }) {
                            Text(if (isPersian) "نمایش همه تنظیمات" else "Show all settings")
                        }
                    }
                }
            }

            // کارت پروفایل
            if (visible("profile")) {
                item(key = "hero") {
                    val user = profile
                    val displayName = user?.let {
                        it.fullName.ifBlank { "${it.firstName} ${it.lastName}".trim() }
                    }.orEmpty().ifBlank {
                        if (isGuest) (if (isPersian) "کاربر مهمان" else "Guest user")
                        else (if (isPersian) "کاربر سیدنا" else "Cidna user")
                    }
                    SettingsProfileHero(
                        isPersian = isPersian,
                        isGuest = isGuest,
                        name = displayName,
                        subtitle = if (isGuest) {
                            if (isPersian) "داده‌ها فقط روی همین گوشی ذخیره می‌شوند" else "Your data stays on this phone only"
                        } else {
                            user?.phoneNumber?.takeIf { it.isNotBlank() }?.let { maskedPhone(it, isPersian) }
                                ?: subtitle("profile")
                        },
                        initials = user?.let { initialsOf(it.firstName, it.lastName, isPersian) }.orEmpty(),
                        completionPercent = if (isGuest) null else user?.let { ProfileCompletion.of(it).percent },
                        onClick = onProfileClick
                    )
                }
            }

            if (prefRows.isNotEmpty()) {
                item(key = "prefs") {
                    SettingsGroup(
                        rows = prefRows,
                        title = if (isPersian) "شخصی‌سازی" else "Personalization"
                    )
                }
            }
            if (manageRows.isNotEmpty()) {
                item(key = "manage") {
                    SettingsGroup(
                        rows = manageRows,
                        title = if (isPersian) "مدیریت" else "Manage"
                    )
                }
            }
            if (securityRows.isNotEmpty()) {
                item(key = "security") {
                    SettingsGroup(
                        rows = securityRows,
                        title = if (isPersian) "امنیت و حریم خصوصی" else "Security & Privacy"
                    )
                }
            }
            if (dataRows.isNotEmpty()) {
                item(key = "data") {
                    SettingsGroup(
                        rows = dataRows,
                        title = if (isPersian) "داده‌ها و گزارش‌ها" else "Data & Reports"
                    )
                }
            }
            if (supportRows.isNotEmpty()) {
                item(key = "support") {
                    SettingsGroup(
                        rows = supportRows,
                        title = if (isPersian) "پشتیبانی و ارتباط با ما" else "Support & Contact"
                    )
                }
            }
            if (legalRows.isNotEmpty()) {
                item(key = "legal") {
                    SettingsGroup(
                        rows = legalRows,
                        title = if (isPersian) "قوانین و شفافیت" else "Legal & Transparency"
                    )
                }
            }
            if (dangerRows.isNotEmpty()) {
                item(key = "danger") {
                    SettingsGroup(
                        rows = dangerRows,
                        title = if (isPersian) "حساب کاربری" else "Account",
                        danger = true
                    )
                }
            }

            if (searchQuery.isBlank()) {
                item(key = "footer") {
                    SettingsFooter(isPersian = isPersian, versionName = BuildConfig.VERSION_NAME)
                }
            }
        }

        SettingsTopBar(
            isPersian = isPersian,
            searchQuery = searchQuery,
            isSearchExpanded = isSearchExpanded,
            onSearchQueryChange = { searchQuery = it },
            onSearchToggle = {
                isSearchExpanded = !isSearchExpanded
                if (!isSearchExpanded) searchQuery = ""
            }
        )

        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                isPersian = isPersian,
                onDismiss = { showLogoutDialog = false },
                onConfirm = { onLoginClick() }
            )
        }

        if (showDeleteAccountDialog && isGuest) {
            GuestDeleteWorkspaceDialog(
                isPersian = isPersian,
                hasPasscode = guestStore.hasPasscode(),
                onDismiss = { showDeleteAccountDialog = false },
                onSubmit = { passcode ->
                    if (guestStore.hasPasscode() && !guestStore.verifyPasscode(passcode)) {
                        Result.failure<Unit>(IllegalArgumentException("Incorrect guest passcode."))
                    } else {
                        app.container.guestModeManager.deleteWorkspace()
                    }
                },
                onSuccess = {
                    showDeleteAccountDialog = false
                    isBiometricEnabled = false
                    onAccountDeleted()
                }
            )
        }

        if (showDeleteAccountDialog && !isGuest) {
            val authRepository = remember(context) { (context.applicationContext as BudgetApp).container.authRepository }
            DeleteAccountDialog(
                isPersian = isPersian,
                onDismiss = { showDeleteAccountDialog = false },
                onSubmit = { password -> authRepository.deleteAccount(password) },
                onSuccess = {
                    showDeleteAccountDialog = false
                    // The fingerprint credential belonged to the deleted account and was wiped.
                    isBiometricEnabled = false
                    SharedPreferences.setBiometricEnabled(context, false)
                    onAccountDeleted()
                }
            )
        }

        if (showBiometricPasswordDialog) {
            EnableBiometricLoginDialog(
                isPersian = isPersian,
                errorMessage = biometricSetupError,
                onDismiss = {
                    showBiometricPasswordDialog = false
                    biometricSetupError = null
                },
                onConfirm = { currentPassword ->
                    if (!rememberedLoginStore.verifyPassword(currentPassword)) {
                        biometricSetupError = if (isPersian) "رمز عبور فعلی نادرست است." else "The current password is incorrect."
                    } else {
                        val activity = context as? FragmentActivity
                        val cipher = rememberedLoginStore.prepareEncryptionCipher()
                        if (activity == null || cipher == null) {
                            biometricSetupError = if (isPersian) "بیومتریک دستگاه آماده استفاده نیست." else "Biometric authentication is not available on this device."
                        } else {
                            val prompt = BiometricPrompt(
                                activity,
                                ContextCompat.getMainExecutor(context),
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        result.cryptoObject?.cipher?.let { authenticatedCipher ->
                                            rememberedLoginStore.enrollBiometric(authenticatedCipher)
                                            isBiometricEnabled = rememberedLoginStore.hasBiometricCredential()
                                            SharedPreferences.setBiometricEnabled(context, isBiometricEnabled)
                                            if (isBiometricEnabled) {
                                                showBiometricPasswordDialog = false
                                                biometricSetupError = null
                                                NotificationHelper.send(
                                                    context = context,
                                                    notificationType = NotificationType.SETTINGS_CHANGED,
                                                    type = "SYSTEM",
                                                    titleFa = "ورود با اثر انگشت فعال شد",
                                                    titleEn = "Biometric login enabled",
                                                    descFa = "از این پس می‌توانید بدون وارد کردن رمز عبور وارد شوید.",
                                                    descEn = "You can now unlock the app without typing your password.",
                                                    tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                                                )
                                            } else {
                                                biometricSetupError = if (isPersian) "فعال‌سازی ورود با اثر انگشت انجام نشد." else "Biometric login could not be enabled."
                                            }
                                        } ?: run {
                                            biometricSetupError = if (isPersian) "احراز هویت بیومتریک کامل نشد." else "Biometric authentication could not be completed."
                                        }
                                    }

                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        biometricSetupError = errString.toString().takeIf { it.isNotBlank() }
                                    }
                                }
                            )

                            prompt.authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle(if (isPersian) "فعال‌سازی ورود با اثر انگشت" else "Enable biometric login")
                                    .setSubtitle(if (isPersian) "برای فعال‌سازی، بیومتریک خود را تأیید کنید" else "Confirm your biometric to enable login")
                                    .setNegativeButtonText(if (isPersian) "انصراف" else "Cancel")
                                    .build(),
                                BiometricPrompt.CryptoObject(cipher)
                            )
                        }
                    }
                }
            )
        }

        if (showChangePasswordDialog && isGuest) {
            GuestPasscodeDialog(
                isPersian = isPersian,
                hasPasscode = guestStore.hasPasscode(),
                onDismiss = { showChangePasswordDialog = false },
                onSubmit = { current, new ->
                    runCatching {
                        if (guestStore.hasPasscode() && !guestStore.verifyPasscode(current)) {
                            error("Incorrect guest passcode.")
                        }
                        guestStore.savePasscode(new)
                    }
                },
                onSuccess = {
                    showChangePasswordDialog = false
                    biometricSetupError = null
                }
            )
        }

        if (showChangePasswordDialog && !isGuest) {
            val authRepository = remember(context) { (context.applicationContext as BudgetApp).container.authRepository }
            ChangePasswordDialog(
                isPersian = isPersian,
                onDismiss = { showChangePasswordDialog = false },
                onSubmit = { currentPassword, newPassword -> authRepository.changePassword(currentPassword, newPassword) },
                onForgotPassword = {
                    showChangePasswordDialog = false
                    onForgotPassword()
                },
                onSuccess = {
                    showChangePasswordDialog = false
                    // The fingerprint credential wrapped the old password and was dropped by the repository.
                    isBiometricEnabled = false
                    SharedPreferences.setBiometricEnabled(context, false)
                    NotificationHelper.send(
                        context = context,
                        notificationType = NotificationType.SETTINGS_CHANGED,
                        type = "SYSTEM",
                        titleFa = "تنظیمات به‌روزرسانی شد",
                        titleEn = "Settings Updated",
                        descFa = "گذرواژه با موفقیت تغییر کرد. برای ورود با اثر انگشت، آن را دوباره فعال کنید.",
                        descEn = "Password changed successfully. Re-enable fingerprint login if you use it.",
                        tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                    )
                }
            )
        }

        // ─── شیت‌های انتخاب ───
        if (activeSheet == SHEET_THEME) {
            SettingsOptionSheet(
                title = if (isPersian) "انتخاب ظاهر برنامه" else "Select App Theme",
                subtitle = if (isPersian) "ظاهر مناسب خودت را انتخاب کن" else "Pick the look that suits you",
                icon = Icons.Default.DarkMode,
                accent = SettingsAccent.VIOLET,
                options = listOf(
                    SettingsOption(
                        id = "light",
                        title = if (isPersian) "روشن" else "Light",
                        description = if (isPersian) "مناسب روز و نور محیط" else "Best in daylight and bright rooms",
                        icon = Icons.Default.WbSunny
                    ),
                    SettingsOption(
                        id = "dark",
                        title = if (isPersian) "تاریک" else "Dark",
                        description = if (isPersian) "راحت‌تر برای چشم در شب" else "Easier on the eyes at night",
                        icon = Icons.Default.NightsStay
                    )
                ),
                selectedId = if (effectiveDark) "dark" else "light",
                onSelect = { id ->
                    val mode = if (id == "light") ThemePreferences.MODE_LIGHT else ThemePreferences.MODE_DARK
                    if (themeMode != mode) {
                        themeMode = mode
                        saveThemeMode(context, mode)
                        onThemeToggle()

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                        }
                    }
                    activeSheet = null
                },
                onDismiss = { activeSheet = null }
            )
        }

        if (activeSheet == SHEET_LANGUAGE) {
            SettingsOptionSheet(
                title = if (isPersian) "زبان برنامه" else "App Language",
                subtitle = if (isPersian) "زبان رابط کاربری را انتخاب کن" else "Choose the interface language",
                icon = Icons.Default.Language,
                accent = SettingsAccent.SKY,
                options = listOf(
                    SettingsOption(
                        id = "fa",
                        title = "پارسی (FA)",
                        description = if (isPersian) "رابط راست‌به‌چپ و تقویم شمسی" else "Right-to-left layout, Persian calendar",
                        icon = Icons.Default.Translate
                    ),
                    SettingsOption(
                        id = "en",
                        title = "English (EN)",
                        description = if (isPersian) "رابط چپ‌به‌راست" else "Left-to-right layout",
                        icon = Icons.Default.Language
                    )
                ),
                selectedId = if (isPersian) "fa" else "en",
                onSelect = { id ->
                    activeSheet = null
                    val toPersian = id == "fa"
                    if (toPersian != isPersian) {
                        LocaleHelper.setLocale(context, if (toPersian) "fa" else "en")
                        ir.hamedan.budgetmanagement.platform.locale.ShortcutsHelper.update(context)
                        (context as? Activity)?.recreate()

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                        }
                    }
                },
                onDismiss = { activeSheet = null }
            )
        }

        if (activeSheet == SHEET_CURRENCY) {
            SettingsOptionSheet(
                title = if (isPersian) "واحد پولی" else "Currency",
                subtitle = if (isPersian) "مبالغ در کل برنامه با این واحد نمایش داده می‌شوند" else "Amounts across the app use this unit",
                icon = null,
                iconRes = R.drawable.ic_currency,
                accent = SettingsAccent.AMBER,
                options = listOf(
                    SettingsOption(
                        id = "IRT",
                        title = if (isPersian) "تومان" else "Toman (IRT)",
                        description = if (isPersian) "پیش‌فرض؛ ۱ تومان = ۱۰ ریال" else "Default; 1 Toman = 10 Rials",
                        icon = Icons.Default.Payments
                    ),
                    SettingsOption(
                        id = "IRR",
                        title = if (isPersian) "ریال" else "Rial (IRR)",
                        description = if (isPersian) "نمایش مبالغ به ریال" else "Show amounts in Rials",
                        icon = Icons.Default.Payments
                    )
                ),
                selectedId = currentCurrencyCode,
                onSelect = { code ->
                    activeSheet = null
                    if (currentCurrencyCode != code) {
                        currentCurrencyCode = code
                        CurrencySharedPreferences.setCurrency(context, code)
                        onCurrencyChanged(code)

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                        }

                        NotificationHelper.send(
                            context = context,
                            notificationType = NotificationType.SETTINGS_CHANGED,
                            type = "SYSTEM",
                            titleFa = "تنظیمات به‌روزرسانی شد",
                            titleEn = "Settings Updated",
                            descFa = "واحد پولی برنامه با موفقیت تغییر کرد.",
                            descEn = "Currency have been updated successfully.",
                            tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                        )
                    }
                },
                onDismiss = { activeSheet = null }
            )
        }

        if (activeSheet == SHEET_NOTIFICATIONS) {
            SettingsOptionSheet(
                title = if (isPersian) "تنظیمات اعلان‌ها" else "Notification Settings",
                subtitle = if (isPersian) "اعلان‌ها کجا نمایش داده شوند؟" else "Where should notifications appear?",
                icon = Icons.Default.Notifications,
                accent = SettingsAccent.ROSE,
                options = listOf(
                    SettingsOption(
                        id = NotificationPreferences.MODE_IN_APP,
                        title = if (isPersian) "فقط درون برنامه‌ای" else "In-App Only",
                        description = if (isPersian) "اعلان‌ها فقط داخل برنامه و در بخش اعلان‌ها نمایش داده می‌شوند"
                        else "Notifications appear only inside the app",
                        icon = Icons.Default.PhoneAndroid
                    ),
                    SettingsOption(
                        id = NotificationPreferences.MODE_BOTH,
                        title = if (isPersian) "درون برنامه + سیستمی" else "In-App + System",
                        description = if (isPersian) "علاوه بر برنامه، اعلان در نوار وضعیت گوشی هم نمایش داده می‌شود"
                        else "Also shows alerts in your phone's notification shade",
                        icon = Icons.Default.NotificationsActive
                    )
                ),
                selectedId = notifMode,
                onSelect = { mode ->
                    // انتخاب دوبارهٔ همان حالت نباید اعلان «تنظیمات به‌روزرسانی شد» بفرستد.
                    if (notifMode != mode) {
                        notifMode = mode
                        NotificationPreferences.setMode(context, mode)
                        NotificationHelper.send(
                            context = context,
                            notificationType = NotificationType.SETTINGS_CHANGED,
                            type = "SYSTEM",
                            titleFa = "تنظیمات به‌روزرسانی شد",
                            titleEn = "Settings Updated",
                            descFa = "نحوه ارسال اعلان برنامه با موفقیت تغییر کرد.",
                            descEn = "Way of sending notification have been updated successfully.",
                            tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                        )
                    }
                },
                onDismiss = { activeSheet = null },
                footer = {
                    // ورود به یک صفحهٔ جدید ← کارت جداگانه با فلش، متفاوت از گزینه‌های بالا
                    Spacer(Modifier.height(4.dp))
                    SettingsNavigationCard(
                        title = if (isPersian) "شخصی‌سازی و مدیریت دقیق اعلان‌ها" else "Customize Notifications",
                        description = if (isPersian) "هر نوع اعلان را جداگانه روشن یا خاموش کنید"
                        else "Turn each notification type on or off",
                        icon = Icons.Default.Tune,
                        onClick = {
                            activeSheet = null
                            onNotificationCalibrationClick()
                        }
                    )
                }
            )
        }

        if (showExportSheet && exportFormat != null) {
            val isExcel = exportFormat == ExportFormat.XLSX
            SettingsFloatingSheet(onDismiss = { showExportSheet = false }) {
                run {
                    SettingsSheetHeader(
                        title = if (isPersian) "بازه زمانی گزارش را انتخاب کنید" else "Select report period",
                        subtitle = if (isExcel) title("exportExcel") else title("exportPdf"),
                        icon = if (isExcel) Icons.Default.TableChart else Icons.Default.PictureAsPdf,
                        accent = if (isExcel) SettingsAccent.TEAL else SettingsAccent.ROSE
                    )
                    SettingsGroup(
                        onSheet = true,
                        rows = ExportPeriod.entries.map { period ->
                            @Composable {
                                SettingsRow(
                                    title = if (isPersian) period.titleFa else period.titleEn,
                                    icon = Icons.Default.CalendarMonth,
                                    accent = SettingsAccent.TEAL,
                                    onClick = {
                                        showExportSheet = false
                                        val selectedFormat = exportFormat
                                        exportScope.launch {
                                            if (selectedFormat == null) return@launch
                                            try {
                                                ExportManager.export(
                                                    context = context,
                                                    format = selectedFormat,
                                                    period = period,
                                                    isPersian = isPersian
                                                )
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                android.util.Log.e("SettingsScreen", "Export failed", e)
                                                settingsSnackbarHostState.showError(
                                                    if (isPersian) "ساخت گزارش ناموفق بود. دوباره تلاش کنید."
                                                    else "Creating the report failed. Please try again."
                                                )
                                            }
                                        }
                                    },
                                    trailing = { SettingsChevron() }
                                )
                            }
                        }
                    )
                }
            }
        }

        AppSnackbarHost(
            hostState = settingsSnackbarHostState,
            isPersian = isPersian,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = AppSnackbarDefaults.AboveBottomBar, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
fun LogoutConfirmationDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppConfirmDialog(
        title = if (isPersian) "خروج از حساب" else "Log Out",
        message = if (isPersian)
            "آیا مطمئن هستید که می‌خواهید از حساب کاربری خود خارج شوید؟ برای ورود مجدد به رمز عبور نیاز خواهید داشت."
        else
            "Are you sure you want to log out? You will need your passcode to sign in again.",
        confirmText = if (isPersian) "خروج" else "Log Out",
        cancelText = if (isPersian) "انصراف" else "Cancel",
        destructive = true,
        icon = Icons.Default.Logout,
        confirmIcon = Icons.Default.Logout,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
internal fun EnableBiometricLoginDialog(
    isPersian: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (isPersian) "فعال‌سازی ورود با اثر انگشت" else "Enable biometric login",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isPersian)
                        "برای فعال‌سازی ورود بیومتریک، یک‌بار رمز عبور فعلی خود را وارد کنید. رمز عبور به‌صورت متن ساده ذخیره نمی‌شود."
                    else
                        "Enter your current password once to enable biometric login. Your password is not stored in plaintext.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isPersian) "رمز عبور فعلی" else "Current password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text(if (isPersian) "ادامه" else "Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSubmit: suspend (currentPassword: String, newPassword: String) -> Result<Unit>,
    onSuccess: () -> Unit,
    onForgotPassword: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isCurrentVisible by remember { mutableStateOf(false) }
    var isNewVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val englishKeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        hintLocales = LocaleList(Locale("en"))
    )

    fun submit() {
        if (isSubmitting) return
        val problem = when {
            currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() ->
                if (isPersian) "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
            newPassword != confirmPassword ->
                if (isPersian) "گذرواژه جدید و تکرار آن مطابقت ندارند" else "New passwords do not match"
            newPassword == currentPassword ->
                if (isPersian) "گذرواژه جدید باید با گذرواژه فعلی متفاوت باشد" else "The new password must differ from the current one"
            else -> PasswordPolicy.validate(newPassword, isPersian)
        }
        if (problem != null) {
            errorMessage = problem
            return
        }
        isSubmitting = true
        errorMessage = null
        scope.launch {
            onSubmit(currentPassword, newPassword)
                .onSuccess { onSuccess() }
                .onFailure { errorMessage = authErrorMessage(it, isPersian, AuthContext.CHANGE_PASSWORD) }
            isSubmitting = false
        }
    }

    AlertDialog(
        // The request cannot be cancelled half-way, so the dialog stays until it finishes.
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (isPersian) "تغییر گذرواژه حساب" else "Change account password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isPersian)
                        "با تغییر گذرواژه، سایر دستگاه‌های متصل از حساب خارج می‌شوند و باید دوباره وارد شوند."
                    else
                        "Changing your password signs every other device out of your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it.take(256); errorMessage = null },
                    enabled = !isSubmitting,
                    label = { Text(if (isPersian) "گذرواژه فعلی" else "Current password") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = englishKeyboardOptions,
                    visualTransformation = if (isCurrentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isCurrentVisible = !isCurrentVisible }) {
                            Icon(
                                imageVector = if (isCurrentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it.take(PasswordPolicy.MAX_LENGTH + 1); errorMessage = null },
                    enabled = !isSubmitting,
                    label = { Text(if (isPersian) "گذرواژه جدید" else "New password") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = englishKeyboardOptions,
                    visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isNewVisible = !isNewVisible }) {
                            Icon(
                                imageVector = if (isNewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordStrengthMeter(password = newPassword, isPersian = isPersian)

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it.take(PasswordPolicy.MAX_LENGTH + 1); errorMessage = null },
                    enabled = !isSubmitting,
                    label = { Text(if (isPersian) "تکرار گذرواژه جدید" else "Confirm new password") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    keyboardOptions = englishKeyboardOptions.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isPersian) "حداقل ${PasswordPolicy.MIN_LENGTH} کاراکتر؛ فقط حروف انگلیسی، عدد و نماد (بدون فاصله)."
                    else "At least ${PasswordPolicy.MIN_LENGTH} characters; English letters, digits and symbols only (no spaces).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { AuthErrorBanner(message = it) }
                }
                if (onForgotPassword != null) {
                    TextButton(
                        onClick = onForgotPassword,
                        enabled = !isSubmitting,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(if (isPersian) "رمز عبورم را فراموش کرده‌ام" else "I forgot my password")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isPersian) "تغییر گذرواژه" else "Change password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}

/**
 * Confirms and performs account deletion. The password is required so a stolen unlocked phone
 * cannot delete the account; the server re-verifies it. Deletion is a soft delete: the account can
 * be restored by signing in again during the grace period.
 */
@Composable
fun DeleteAccountDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSubmit: suspend (password: String) -> Result<Unit>,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (isSubmitting) return
        if (password.isEmpty()) {
            errorMessage = if (isPersian) "گذرواژه را وارد کنید" else "Enter your password"
            return
        }
        isSubmitting = true
        errorMessage = null
        scope.launch {
            onSubmit(password)
                .onSuccess { onSuccess() }
                .onFailure { errorMessage = authErrorMessage(it, isPersian, AuthContext.DELETE_ACCOUNT) }
            isSubmitting = false
        }
    }

    AlertDialog(
        // The request cannot be cancelled half-way, so the dialog stays until it finishes.
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (isPersian) "حذف حساب کاربری" else "Delete account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isPersian)
                        "با حذف حساب، از همه‌ی دستگاه‌ها خارج می‌شوید و اطلاعات این دستگاه پاک می‌شود. اطلاعات همگام‌شده‌ی شما تا ۳۰ روز روی سرور نگه داشته می‌شود؛ اگر در این مدت دوباره وارد شوید می‌توانید حساب را بازیابی کنید، وگرنه برای همیشه پاک می‌شود."
                    else
                        "Deleting your account signs you out of every device and erases the data on this device. Your synced data is kept on the server for 30 days: sign in again within that time to restore the account, otherwise it is erased permanently.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = if (isPersian) TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(256); errorMessage = null },
                    enabled = !isSubmitting,
                    label = { Text(if (isPersian) "گذرواژه حساب" else "Account password") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        hintLocales = LocaleList(Locale("en")),
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { AuthErrorBanner(message = it) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text(if (isPersian) "حذف حساب" else "Delete account")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}

@Composable
private fun SettingsTopBar(
    isPersian: Boolean,
    searchQuery: String,
    isSearchExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit
) {
    val barShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), barShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), barShape)
                    .clip(barShape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchExpanded) {
                    val focusManager = LocalFocusManager.current

                    TextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            if (newValue.length <= 40) {          // مثلاً حداکثر ۴۰ کاراکتر
                                onSearchQueryChange(newValue)
                            }
                        },
                        placeholder = {
                            Text(
                                text = if (isPersian) "جستجو در تنظیمات..." else "Search settings...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = if (isPersian) "پاک کردن" else "Clear",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                VoiceInputButton(
                                    onResult = { spokenText ->
                                        val query = spokenText.take(40)
                                        onSearchQueryChange(query)
                                    },
                                    language = if (isPersian) "fa-IR" else "en-US"
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = if (isPersian) "تنظیمات" else "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), barShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), barShape)
                    .clip(barShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isPersian) "جستجو" else "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.w("SettingsScreen", "Could not open $url", e)
        val isFa = ir.hamedan.budgetmanagement.platform.locale.LocaleHelper.getLanguage(context) == "fa"
        android.widget.Toast.makeText(
            context,
            if (isFa) "برنامه‌ای برای باز کردن این لینک پیدا نشد" else "No app found to open this link",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    val prev = IntArray(b.length + 1) { it }
    val curr = IntArray(b.length + 1)

    for (i in a.indices) {
        curr[0] = i + 1
        for (j in b.indices) {
            val cost = if (a[i].equals(b[j], ignoreCase = true)) 0 else 1
            curr[j + 1] = minOf(
                curr[j] + 1,
                prev[j + 1] + 1,
                prev[j] + cost
            )
        }
        for (j in prev.indices) prev[j] = curr[j]
    }
    return curr[b.length]
}

/**
 * نزدیک‌ترین عنوان را برمی‌گرداند؛ اگر شباهت کافی نباشد null
 */
private fun normalizeForSearch(s: String): String {
    return s.trim()
        .lowercase()
        .replace("\u200c", " ") // نیم‌فاصله -> فاصله
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace(Regex("\\s+"), " ")
}

/**
 * نزدیک‌ترین عنوان را برمی‌گرداند؛ اگر شباهت کافی نباشد null
 * مقایسه در سطح کلمه انجام می‌شود، نه کل عبارت، تا typo روی یک کلمه هم تشخیص داده شود.
 */
private fun findSuggestion(query: String, isPersian: Boolean): String? {
    val normalizedQuery = normalizeForSearch(query)
    if (normalizedQuery.length < 2) return null

    var bestLabel: String? = null
    var bestDistance = Int.MAX_VALUE

    for (item in ALL_SETTINGS_ITEMS) {
        val title = if (isPersian) item.titleFa else item.titleEn
        val subtitle = if (isPersian) item.subtitleFa else item.subtitleEn

        // کلمات عنوان + زیرنویس را با هم بررسی می‌کنیم تا حتی typo توی توضیحات هم پیدا شود
        val words = normalizeForSearch("$title $subtitle")
            .split(Regex("[\\s/,،]+"))
            .filter { it.length >= 2 }

        for (word in words) {
            // مقایسه‌ی طول‌ها: اگر خیلی با هم فرق دارند، اصلاً بررسی نکن (بهینه‌سازی + جلوگیری از match های بی‌معنی)
            if (kotlin.math.abs(word.length - normalizedQuery.length) > 3) continue

            val distance = levenshtein(normalizedQuery, word)
            // آستانه: حداکثر ۴۰٪ طول کلمه، حداقل ۱، حداکثر ۳
            val threshold = maxOf(1, (word.length * 0.4).toInt()).coerceAtMost(3)

            if (distance in 1..threshold && distance < bestDistance) {
                bestDistance = distance
                bestLabel = title
            }
        }
    }
    return bestLabel
}

/**
 * Enrolls the fingerprint for guest mode: the Keystore key is created here and the guest marker is
 * encrypted with it once the user verified the fingerprint.
 */
internal fun enrollGuestBiometric(
    activity: FragmentActivity,
    store: GuestModeStore,
    isPersian: Boolean,
    onResult: (Boolean, String?) -> Unit
) {
    val cipher = store.prepareEnrollCipher()
    if (cipher == null) {
        onResult(false, if (isPersian) "بیومتریک دستگاه آماده استفاده نیست." else "Biometric authentication is not available on this device.")
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val enabled = result.cryptoObject?.cipher?.let { store.completeBiometricEnrollment(it) } == true
                onResult(
                    enabled,
                    if (enabled) null
                    else if (isPersian) "فعال‌سازی ورود با اثر انگشت انجام نشد." else "Biometric login could not be enabled."
                )
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val userStopped = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                onResult(false, if (userStopped) null else errString.toString().takeIf { it.isNotBlank() })
            }
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isPersian) "فعال‌سازی ورود با اثر انگشت" else "Enable biometric login")
            .setSubtitle(if (isPersian) "برای فعال‌سازی، اثر انگشت خود را تأیید کنید" else "Confirm your fingerprint to enable login")
            .setNegativeButtonText(if (isPersian) "انصراف" else "Cancel")
            .build(),
        BiometricPrompt.CryptoObject(cipher)
    )
}