package ir.hamedan.budgetmanagement.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.SharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.BiometricPromptManager
import ir.hamedan.budgetmanagement.utils.ExportFormat
import ir.hamedan.budgetmanagement.utils.ExportManager
import ir.hamedan.budgetmanagement.utils.ExportPeriod
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.launch

enum class SettingsMenu {
    LANGUAGE, CURRENCY, SECURITY, EXPORT, ABOUT, NOTIFICATION, NONE
}

private data class SettingsSearchItem(
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String
)

private val ALL_SETTINGS_ITEMS = listOf(
    SettingsSearchItem("ظاهر برنامه", "App Theme", "تغییر حالت تاریک و روشن", "Switch between Dark and Light mode"),
    SettingsSearchItem("زبان برنامه", "App Language", "انتخاب زبان کاربری (فارسی / انگلیسی)", "Choose UI language (Persian / English)"),
    SettingsSearchItem("واحد پولی", "Currency", "نمایش مبالغ بر اساس تومان یا ریال", "Display amounts in Toman or Rial"),
    SettingsSearchItem("تنظیمات اعلان‌ها", "Notification Settings", "کنترل نحوه دریافت اعلان‌های برنامه", "Control how you receive app notifications"),
    SettingsSearchItem("مدیریت دسته‌بندی‌ها", "Manage Categories", "ویرایش، حذف یا ایجاد دسته‌های خرید و فروش", "Edit, delete, or create transaction categories"),
    SettingsSearchItem("امنیت برنامه", "App Security", "تنظیم رمز ورود و ویژگی‌های بیومتریک", "Configure passcode and biometric login"),
    SettingsSearchItem("دریافت اطلاعات و گزارش‌ها", "Export Data & Reports", "خروجی گرفتن از تراکنش‌ها در قالب PDF یا Excel", "Export transactions to PDF or Excel formats"),
    SettingsSearchItem("درباره ما و پشتیبانی", "About Us & Support", "راه‌های ارتباطی، واتس‌اپ و ایمیل", "Contact channels, Gmail, WhatsApp & Support")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onCurrencyChanged: (String) -> Unit = {},  // "IRT" یا "IRR"
    onLoginClick: () -> Unit = {},
    onAddScreenClick: () -> Unit = {},
    onThemeToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf(SettingsMenu.NONE) }

    var isBiometricEnabled by remember(context) {
        mutableStateOf(SharedPreferences.getBiometricEnabled(context))
    }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showThemeBottomSheet by remember { mutableStateOf(false) }
    var themeMode by remember(context) { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // به‌روزرسانی مقدار تم در بدو باز شدن باتم‌شیت
    LaunchedEffect(showThemeBottomSheet) {
        if (showThemeBottomSheet) {
            themeMode = ThemePreferences.getThemeMode(context)
        }
    }

    // واحد پولی ذخیره‌شده یا مقدار پیش‌فرض
    var currentCurrencyCode by remember(context) {
        mutableStateOf(CurrencySharedPreferences.getCurrency(context))
    }

    var showExportSheet by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf<ExportFormat?>(null) } // PDF یا XLSX
    val exportScope = rememberCoroutineScope()

    fun matchesSearch(titleFa: String, titleEn: String, subtitleFa: String, subtitleEn: String): Boolean {
        if (searchQuery.isBlank()) return true
        val query = searchQuery.trim()
        val targetTitle = if (isPersian) titleFa else titleEn
        val targetSubtitle = if (isPersian) subtitleFa else subtitleEn
        return targetTitle.contains(query, ignoreCase = true) ||
                targetSubtitle.contains(query, ignoreCase = true)
    }

    // آیا هیچ آیتمی با جستجوی فعلی مطابقت دارد؟
    val hasAnyMatch = remember(searchQuery, isPersian) {
        if (searchQuery.isBlank()) true
        else ALL_SETTINGS_ITEMS.any { item ->
            matchesSearch(item.titleFa, item.titleEn, item.subtitleFa, item.subtitleEn)
        }
    }

    val suggestion = remember(searchQuery, isPersian, hasAnyMatch) {
        if (!hasAnyMatch) findSuggestion(searchQuery, isPersian) else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 75.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding().height(5.dp))
            }

            // داخل LazyColumn، بعد از Spacer اول:
            if (suggestion != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable { searchQuery = suggestion!! }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPersian)
                                "منظورت «$suggestion» بود؟"
                            else
                                "Did you mean \"$suggestion\"?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ظاهر برنامه
            if (matchesSearch("ظاهر برنامه", "App Theme", "تغییر حالت تاریک و روشن", "Switch between Dark and Light mode")) {
                item {
                    SettingsSimpleItem(
                        title = if (isPersian) "ظاهر برنامه" else "App Theme",
                        subtitle = if (isPersian) "تغییر حالت تاریک و روشن" else "Switch between Dark and Light mode",
                        icon = Icons.Default.DarkMode,
                        onClick = { showThemeBottomSheet = true }
                    )
                }
            }

            // ۱. زبان برنامه
            if (matchesSearch("زبان برنامه", "App Language", "انتخاب زبان کاربری (فارسی / انگلیسی)", "Choose UI language (Persian / English)")) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "زبان برنامه" else "App Language",
                        subtitle = if (isPersian) "انتخاب زبان کاربری (فارسی / انگلیسی)" else "Choose UI language (Persian / English)",
                        icon = Icons.Default.Language,
                        isExpanded = activeMenu == SettingsMenu.LANGUAGE,
                        onClick = { activeMenu = if (activeMenu == SettingsMenu.LANGUAGE) SettingsMenu.NONE else SettingsMenu.LANGUAGE }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CurrencyOrLanguageOptionButton(
                                title = "پارسی (FA)",
                                isSelected = isPersian,
                                modifier = Modifier.weight(1f)
                            ) {
                                LocaleHelper.setLocale(context, "fa")
                                (context as? Activity)?.recreate()

                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                }
                            }
                            CurrencyOrLanguageOptionButton(
                                title = "English (EN)",
                                isSelected = !isPersian,
                                modifier = Modifier.weight(1f)
                            ) {
                                LocaleHelper.setLocale(context, "en")
                                (context as? Activity)?.recreate()

                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                }
                            }
                        }
                    }
                }
            }

            // ۲. واحد پولی (استفاده از ۲ دکمه به جای RadioButton)
            if (matchesSearch("واحد پولی", "Currency", "نمایش مبالغ بر اساس تومان یا ریال", "Display amounts in Toman or Rial")) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "واحد پولی" else "Currency",
                        subtitle = if (isPersian) "نمایش مبالغ بر اساس تومان یا ریال" else "Display amounts in Toman or Rial",
                        icon = Icons.Default.CurrencyExchange,
                        isExpanded = activeMenu == SettingsMenu.CURRENCY,
                        onClick = { activeMenu = if (activeMenu == SettingsMenu.CURRENCY) SettingsMenu.NONE else SettingsMenu.CURRENCY }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CurrencyOrLanguageOptionButton(
                                title = if (isPersian) "تومان" else "Toman (IRT)",
                                isSelected = currentCurrencyCode == "IRT",
                                modifier = Modifier.weight(1f)
                            ) {
                                currentCurrencyCode = "IRT"
                                CurrencySharedPreferences.setCurrency(context, "IRT")
                                onCurrencyChanged("IRT")

                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                }

                                NotificationHelper.send(
                                    context,
                                    type = "SYSTEM",
                                    titleFa = "تنظیمات به‌روزرسانی شد",
                                    titleEn = "Settings Updated",
                                    descFa = "واحد پولی برنامه با موفقیت تغییر کرد.",
                                    descEn = "Currency have been updated successfully.",
                                    tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                                )
                            }
                            CurrencyOrLanguageOptionButton(
                                title = if (isPersian) "ریال" else "Rial (IRR)",
                                isSelected = currentCurrencyCode == "IRR",
                                modifier = Modifier.weight(1f)
                            ) {
                                currentCurrencyCode = "IRR"
                                CurrencySharedPreferences.setCurrency(context, "IRR")
                                onCurrencyChanged("IRR")

                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                }

                                NotificationHelper.send(
                                    context,
                                    type = "SYSTEM",
                                    titleFa = "تنظیمات به‌روزرسانی شد",
                                    titleEn = "Settings Updated",
                                    descFa = "واحد پولی برنامه با موفقیت تغییر کرد.",
                                    descEn = "Currency have been updated successfully.",
                                    tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                                )
                            }
                        }
                    }
                }
            }

            // ۰. تنظیمات اعلان‌ها
            if (matchesSearch("تنظیمات اعلان‌ها", "Notification Settings",
                    "کنترل نحوه دریافت اعلان‌های برنامه", "Control how you receive app notifications")) {
                item {
                    var notifMode by remember {
                        mutableStateOf(NotificationPreferences.getMode(context))
                    }

                    SettingsAccordionItem(
                        title = if (isPersian) "تنظیمات اعلان‌ها" else "Notification Settings",
                        subtitle = if (isPersian) "کنترل نحوه دریافت اعلان‌های برنامه"
                        else "Control how you receive app notifications",
                        icon = Icons.Default.Notifications,
                        isExpanded = activeMenu == SettingsMenu.NOTIFICATION,
                        onClick = { activeMenu = if (activeMenu == SettingsMenu.NOTIFICATION)
                            SettingsMenu.NONE else SettingsMenu.NOTIFICATION }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyOrLanguageOptionButton(
                                title = if (isPersian) "فقط درون برنامه‌ای" else "In-App Only",
                                isSelected = notifMode == NotificationPreferences.MODE_IN_APP,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                notifMode = NotificationPreferences.MODE_IN_APP
                                NotificationPreferences.setMode(context, NotificationPreferences.MODE_IN_APP)

                                NotificationHelper.send(
                                    context,
                                    type = "SYSTEM",
                                    titleFa = "تنظیمات به‌روزرسانی شد",
                                    titleEn = "Settings Updated",
                                    descFa = "نحوه ارسال اعلان برنامه با موفقیت تغییر کرد.",
                                    descEn = "Way of sending notification have been updated successfully.",
                                    tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                                )
                            }
                            CurrencyOrLanguageOptionButton(
                                title = if (isPersian) "درون برنامه + سیستمی" else "In-App + System",
                                isSelected = notifMode == NotificationPreferences.MODE_BOTH,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                notifMode = NotificationPreferences.MODE_BOTH
                                NotificationPreferences.setMode(context, NotificationPreferences.MODE_BOTH)

                                NotificationHelper.send(
                                    context,
                                    type = "SYSTEM",
                                    titleFa = "تنظیمات به‌روزرسانی شد",
                                    titleEn = "Settings Updated",
                                    descFa = "نحوه ارسال اعلان برنامه با موفقیت تغییر کرد.",
                                    descEn = "Way of sending notification have been updated successfully.",
                                    tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                                )
                            }
                        }
                    }
                }
            }

            // ۳. مدیریت دسته‌بندی‌ها
            if (matchesSearch("مدیریت دسته‌بندی‌ها", "Manage Categories", "ویرایش، حذف یا ایجاد دسته‌های خرید و فروش", "Edit, delete, or create transaction categories")) {
                item {
                    SettingsSimpleItem(
                        title = if (isPersian) "مدیریت دسته‌بندی‌ها" else "Manage Categories",
                        subtitle = if (isPersian) "ویرایش، حذف یا ایجاد دسته‌های خرید و فروش" else "Edit, delete, or create transaction categories",
                        icon = Icons.Default.Category,
                        onClick = { onAddScreenClick() }
                    )
                }
            }

            // ۴. بخش امنیت (همراه با احراز هویت اثر انگشت برای سوییچ)
            if (matchesSearch("امنیت برنامه", "App Security", "تنظیم رمز ورود و ویژگی‌های بیومتریک", "Configure passcode and biometric login")) {
                item {
                    val hasBiometricHardware = remember(context) {
                        val biometricManager = BiometricManager.from(context)
                        val result = biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                                result != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    }

                    SettingsAccordionItem(
                        title = if (isPersian) "امنیت برنامه" else "App Security",
                        subtitle = if (isPersian) "تنظیم رمز ورود و ویژگی‌های بیومتریک" else "Configure passcode and biometric login",
                        icon = Icons.Default.Lock,
                        isExpanded = activeMenu == SettingsMenu.SECURITY,
                        onClick = { activeMenu = if (activeMenu == SettingsMenu.SECURITY) SettingsMenu.NONE else SettingsMenu.SECURITY }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasBiometricHardware) {
                                SecuritySwitchRow(
                                    title = if (isPersian) "ورود با اثر انگشت" else "Biometric Login",
                                    icon = Icons.Default.Fingerprint,
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { targetChecked ->
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricPromptManager.showBiometricPrompt(
                                                activity = activity,
                                                onSuccess = {
                                                    isBiometricEnabled = targetChecked
                                                    SharedPreferences.setBiometricEnabled(context, targetChecked)

                                                    NotificationHelper.send(
                                                        context,
                                                        type = "SYSTEM",
                                                        titleFa = "تنظیمات به‌روزرسانی شد",
                                                        titleEn = "Settings Updated",
                                                        descFa = "تنظیمات بیومتریک برنامه با موفقیت تغییر کرد.",
                                                        descEn = "Biometric login have been updated successfully.",
                                                        tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                )
                            }

                            SecurityActionRow(
                                title = if (isPersian) "تغییر رمز عبور برنامه" else "Change App Passcode",
                                icon = Icons.Default.Password
                            ) {
                                showChangePasswordDialog = true
                            }

                            SecurityActionRow(
                                title = if (isPersian) "خروج از حساب کاربری" else "Log Out",
                                icon = Icons.Default.Logout,
                                iconTint = MaterialTheme.colorScheme.error,
                                titleColor = MaterialTheme.colorScheme.error
                            ) {
                                showLogoutDialog = true
                            }
                        }
                    }
                }
            }

            // ۵. دریافت داده‌ها
            if (matchesSearch("دریافت اطلاعات و گزارش‌ها", "Export Data & Reports", "خروجی گرفتن از تراکنش‌ها در قالب PDF یا Excel", "Export transactions to PDF or Excel formats")) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "دریافت اطلاعات و گزارش‌ها" else "Export Data & Reports",
                        subtitle = if (isPersian) "خروجی گرفتن از تراکنش‌ها در قالب PDF یا Excel" else "Export transactions to PDF or Excel formats",
                        icon = Icons.Default.Download,
                        isExpanded = activeMenu == SettingsMenu.EXPORT,
                        onClick = { activeMenu = if (activeMenu == SettingsMenu.EXPORT) SettingsMenu.NONE else SettingsMenu.EXPORT }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExportButton(
                                title = "Excel (XLSX)",
                                icon = Icons.Default.TableChart,
                                modifier = Modifier.weight(1f)
                            ) {
                                exportFormat = ExportFormat.XLSX
                                showExportSheet = true
                            }
                            ExportButton(
                                title = "PDF Document",
                                icon = Icons.Default.PictureAsPdf,
                                modifier = Modifier.weight(1f)
                            ) {
                                exportFormat = ExportFormat.PDF
                                showExportSheet = true
                            }
                        }
                    }
                }
            }

            // ۶. درباره ما
            if (matchesSearch("درباره ما و پشتیبانی", "About Us & Support", "راه‌های ارتباطی،واتس‌اپ و ایمیل", "Contact channels, Gmail, WhatsApp & Support")) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "درباره ما و پشتیبانی" else "About Us & Support",
                        subtitle = if (isPersian) "راه‌های ارتباطی، واتس‌اپ و ایمیل" else "Contact channels, Gmail, WhatsApp & Support",
                        icon = Icons.Default.Info,
                        isExpanded = activeMenu == SettingsMenu.ABOUT,
                        onClick = { activeMenu = if (activeMenu == SettingsMenu.ABOUT) SettingsMenu.NONE else SettingsMenu.ABOUT }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SocialLinkRow(
                                title = if (isPersian) "پشتیبانی جیمیل" else "Gmail Support",
                                icon = Icons.Default.Email,
                                color = Color(0xFFD44638)
                            ) {
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
                                    context.startActivity(Intent.createChooser(fallbackIntent, if (isPersian) "ارسال ایمیل" else "Send Email"))
                                }
                            }
                            SocialLinkRow(
                                title = if (isPersian) "واتس‌اپ توسعه‌دهنده" else "WhatsApp Contact",
                                icon = Icons.Default.Phone,
                                color = Color(0xFF25D366)
                            ) { openUrl(context, "https://wa.me/989180500841") }
                        }
                    }
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

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                isPersian = isPersian,
                onDismiss = { showChangePasswordDialog = false },
                onConfirm = { oldPassword, secureNewPassword ->
                    NotificationHelper.send(
                        context,
                        type = "SYSTEM",
                        titleFa = "تنظیمات به‌روزرسانی شد",
                        titleEn = "Settings Updated",
                        descFa = "گذرواژه با موفقیت تغییر کرد.",
                        descEn = "Password have been updated successfully.",
                        tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
                    )
                }
            )
        }

        if (showExportSheet && exportFormat != null) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = if (isPersian) "بازه زمانی گزارش را انتخاب کنید" else "Select report period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ExportPeriod.entries.forEach { period ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    showExportSheet = false
                                    exportScope.launch {
                                        ExportManager.export(
                                            context = context,
                                            format = exportFormat!!,
                                            period = period,
                                            isPersian = isPersian
                                        )
                                    }
                                }
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isPersian) period.titleFa else period.titleEn,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }

        if (showThemeBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPersian) "انتخاب ظاهر برنامه" else "Select App Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // محاسبه حالت تم فعلی (در نظر گرفتن تم سیستم در صورت استفاده از MODE_SYSTEM)
                        val systemInDark = isSystemInDarkTheme()
                        val currentActiveMode = if (themeMode == ThemePreferences.MODE_SYSTEM) {
                            if (systemInDark) ThemePreferences.MODE_DARK else ThemePreferences.MODE_LIGHT
                        } else {
                            themeMode
                        }

                        val isLightSelected = currentActiveMode == ThemePreferences.MODE_LIGHT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isLightSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = if (isLightSelected) 2.dp else 1.dp,
                                    color = if (isLightSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (themeMode != ThemePreferences.MODE_LIGHT) {
                                        themeMode = ThemePreferences.MODE_LIGHT
                                        saveThemeMode(context, ThemePreferences.MODE_LIGHT)
                                        onThemeToggle()

                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                        }
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = if (isLightSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = isLightSelected,
                                        onClick = {
                                            if (themeMode != ThemePreferences.MODE_LIGHT) {
                                                themeMode = ThemePreferences.MODE_LIGHT
                                                saveThemeMode(context, ThemePreferences.MODE_LIGHT)
                                                onThemeToggle()

                                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                    ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                                }
                                            }
                                        }
                                    )
                                    Text(
                                        text = if (isPersian) "روشن" else "Light",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        val isDarkSelected = currentActiveMode == ThemePreferences.MODE_DARK
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isDarkSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = if (isDarkSelected) 2.dp else 1.dp,
                                    color = if (isDarkSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (themeMode != ThemePreferences.MODE_DARK) {
                                        themeMode = ThemePreferences.MODE_DARK
                                        saveThemeMode(context, ThemePreferences.MODE_DARK)
                                        onThemeToggle()

                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                        }
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = if (isDarkSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = isDarkSelected,
                                        onClick = {
                                            if (themeMode != ThemePreferences.MODE_DARK) {
                                                themeMode = ThemePreferences.MODE_DARK
                                                saveThemeMode(context, ThemePreferences.MODE_DARK)
                                                onThemeToggle()

                                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                    ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget(context)
                                                }
                                            }
                                        }
                                    )
                                    Text(
                                        text = if (isPersian) "تاریک" else "Dark",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// دکمه مشترک دوگانه برای انتخاب واحد پولی و زبان
@Composable
private fun CurrencyOrLanguageOptionButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = shape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsAccordionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSimpleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SecuritySwitchRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SecurityActionRow(
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun LogoutConfirmationDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (isPersian) "خروج از حساب" else "Log Out",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = if (isPersian)
                    "آیا مطمئن هستید که می‌خواهید از حساب کاربری خود خارج شوید؟ برای ورود مجدد به رمز عبور نیاز خواهید داشت."
                else
                    "Are you sure you want to log out? You will need your passcode to sign in again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isPersian) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isPersian) "خروج" else "Log Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isPersian) "انصراف" else "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val maxPasswordLength = 32

    val hasMinLength = newPassword.length >= 8
    val hasUpperCase = newPassword.any { it.isUpperCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecialChar = newPassword.any { !it.isLetterOrDigit() }

    val strengthScore = listOf(hasMinLength, hasUpperCase, hasDigit, hasSpecialChar).count { it }

    val (strengthColor, strengthText) = remember(strengthScore, newPassword) {
        if (newPassword.isEmpty()) {
            Color.Transparent to ""
        } else {
            when (strengthScore) {
                1 -> Color(0xFFE57373) to (if (isPersian) "ضعیف" else "Weak")
                2 -> Color(0xFFFFB74D) to (if (isPersian) "متوسط" else "Medium")
                3 -> Color(0xFFFFF176) to (if (isPersian) "خوب" else "Good")
                4 -> Color(0xFF81C784) to (if (isPersian) "قوی" else "Strong")
                else -> Color(0xFFE57373) to (if (isPersian) "خیلی ضعیف" else "Very Weak")
            }
        }
    }

    val englishKeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        hintLocales = LocaleList(Locale("en"))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = if (isPersian) "تغییر رمز عبور" else "Change Passcode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isPersian) "رمزهای ورود فقط باید شامل کاراکترهای انگلیسی (حداکثر $maxPasswordLength کاراکتر) باشند." else "Passcodes must contain English characters only (max $maxPasswordLength chars).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { input ->
                        if (input.length <= maxPasswordLength && input.all { it.code <= 127 }) {
                            currentPassword = input
                            errorMessage = null
                        }
                    },
                    label = { Text(if (isPersian) "رمز عبور فعلی" else "Current Passcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = englishKeyboardOptions,
                    visualTransformation = if (isCurrentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }) {
                            Icon(
                                imageVector = if (isCurrentPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { input ->
                        if (input.length <= maxPasswordLength && input.all { it.code <= 127 }) {
                            newPassword = input
                            errorMessage = null
                        }
                    },
                    label = { Text(if (isPersian) "رمز عبور جدید" else "New Passcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = englishKeyboardOptions,
                    visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                            Icon(
                                imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (newPassword.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPersian) "امنیت رمز عبور:" else "Password Strength:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = strengthText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = strengthColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (strengthScore / 4f).coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(strengthColor, CircleShape)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { input ->
                        if (input.length <= maxPasswordLength && input.all { it.code <= 127 }) {
                            confirmPassword = input
                            errorMessage = null
                        }
                    },
                    label = { Text(if (isPersian) "تایید رمز عبور جدید" else "Confirm New Passcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = englishKeyboardOptions,
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hasNonEnglish = listOf(currentPassword, newPassword, confirmPassword).any { text ->
                        text.any { it.code > 127 }
                    }

                    when {
                        currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                            errorMessage = if (isPersian) "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
                        }
                        hasNonEnglish -> {
                            errorMessage = if (isPersian) "لطفاً فقط از حروف و اعداد انگلیسی استفاده کنید" else "Please use English characters only"
                        }
                        newPassword != confirmPassword -> {
                            errorMessage = if (isPersian) "رمز عبور جدید و تایید آن مطابقت ندارند" else "New passwords do not match"
                        }
                        strengthScore < 2 -> {
                            errorMessage = if (isPersian) "رمز عبور خیلی ضعیف است" else "Password is too weak"
                        }
                        else -> {
                            onConfirm(currentPassword, newPassword)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isPersian) "تایید" else "Confirm")
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
private fun ExportButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SocialLinkRow(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
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
        e.printStackTrace()
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