package ir.hamedan.budgetmanagement.ui.theme.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import ir.hamedan.budgetmanagement.data.SharedPreferences // اضافه شدن شرد پرفرنسز
import ir.hamedan.budgetmanagement.data.ThemePreferences
import ir.hamedan.budgetmanagement.data.ThemePreferences.saveThemeMode
import ir.hamedan.budgetmanagement.item.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.LocaleHelper

// ۱. مدیریت وضعیت منوهای کشویی (فقط یک منو در لحظه باز می‌ماند)
enum class SettingsMenu {
    LANGUAGE, CURRENCY, SECURITY, EXPORT, ABOUT, NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onCurrencyChanged: (String) -> Unit = {},  // تغییر واحد پولی ("IRT" یا "IRR")
    onLoginClick: () -> Unit = {},
    onAddScreenClick: () -> Unit = {},
    onThemeToggle: () -> Unit = {}
    ) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()

    // وضعیت جستجو در تنظیمات
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // وضعیت فعال بودن کدام منوی کشویی (بای‌دیفالت هیچ‌کدام باز نیستند)
    var activeMenu by remember { mutableStateOf(SettingsMenu.NONE) }

    // 🚀 تغییر مهم: خواندن وضعیت اولیه اثر انگشت از حافظه
    var isBiometricEnabled by remember {
        mutableStateOf(SharedPreferences.getBiometricEnabled(context))
    }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showThemeBottomSheet by remember { mutableStateOf(false) }
    // خواندن مقدار اولیه تم از SharedPreferences و تبدیل آن به یک State قابله مشاهده در کامپوز
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var currentCurrency by remember { mutableStateOf(if (isPersian) "تومان" else "IRT") }

    // تابع کمکی محلی برای بررسی مطابقت عنوان‌ها و توضیحات با عبارت سرچ شده
    fun matchesSearch(titleFa: String, titleEn: String, subtitleFa: String, subtitleEn: String): Boolean {
        if (searchQuery.isBlank()) return true
        val query = searchQuery.trim()
        val targetTitle = if (isPersian) titleFa else titleEn
        val targetSubtitle = if (isPersian) subtitleFa else subtitleEn
        return targetTitle.contains(query, ignoreCase = true) ||
                targetSubtitle.contains(query, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // لایه اول (پایین‌ترین): پس‌زمینه افکت شفق قطبی
        AuroraBackground()

        // لایه دوم (وسط): لیست محتوا و آیتم‌های تنظیمات
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 75.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding().height(5.dp))
            }

            // ۴. مدیریت تم و ظاهر برنامه (باز کردن باتم‌شیت)
            if (matchesSearch(
                    titleFa = "ظاهر برنامه", titleEn = "App Theme",
                    subtitleFa = "تغییر حالت تاریک و روشن", subtitleEn = "Switch between Dark and Light mode"
                )) {
                item {
                    SettingsSimpleItem(
                        title = if (isPersian) "ظاهر برنامه" else "App Theme",
                        subtitle = if (isPersian) "تغییر حالت تاریک و روشن" else "Switch between Dark and Light mode",
                        icon = Icons.Default.DarkMode, // حتماً این ایمپورت اضافه شود
                        onClick = {
                            showThemeBottomSheet = true
                        }
                    )
                }
            }

            // ۱. زبان برنامه (کشویی)
            if (matchesSearch(
                    titleFa = "زبان برنامه", titleEn = "App Language",
                    subtitleFa = "انتخاب زبان کاربری (فارسی / انگلیسی)", subtitleEn = "Choose UI language (Persian / English)"
                )) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "زبان برنامه" else "App Language",
                        subtitle = if (isPersian) "انتخاب زبان کاربری (فارسی / انگلیسی)" else "Choose UI language (Persian / English)",
                        icon = Icons.Default.Language,
                        isExpanded = activeMenu == SettingsMenu.LANGUAGE,
                        onClick = {
                            activeMenu = if (activeMenu == SettingsMenu.LANGUAGE) SettingsMenu.NONE else SettingsMenu.LANGUAGE
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LanguageOptionButton(
                                title = "پارسی (FA)",
                                isSelected = isPersian,
                                modifier = Modifier.weight(1f)
                            ) {
                                LocaleHelper.setLocale(context, "fa")
                                (context as? Activity)?.recreate()
                            }
                            LanguageOptionButton(
                                title = "English (EN)",
                                isSelected = !isPersian,
                                modifier = Modifier.weight(1f)
                            ) {
                                LocaleHelper.setLocale(context, "en")
                                (context as? Activity)?.recreate()
                            }
                        }
                    }
                }
            }

            // ۲. واحد پولی (کشویی)
            if (matchesSearch(
                    titleFa = "واحد پولی", titleEn = "Currency",
                    subtitleFa = "نمایش مبالغ بر اساس تومان یا ریال", subtitleEn = "Display amounts in Toman or Rial"
                )) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "واحد پولی" else "Currency",
                        subtitle = if (isPersian) "نمایش مبالغ بر اساس تومان یا ریال" else "Display amounts in Toman or Rial",
                        icon = Icons.Default.CurrencyExchange,
                        isExpanded = activeMenu == SettingsMenu.CURRENCY,
                        onClick = {
                            activeMenu = if (activeMenu == SettingsMenu.CURRENCY) SettingsMenu.NONE else SettingsMenu.CURRENCY
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                if (isPersian) "تومان" else "IRT",
                                if (isPersian) "ریال" else "IRR"
                            ).forEach { currency ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            currentCurrency = currency
                                            onCurrencyChanged(if (currency == "تومان" || currency == "IRT") "IRT" else "IRR")
                                            activeMenu = SettingsMenu.NONE
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentCurrency == currency,
                                        onClick = {
                                            currentCurrency = currency
                                            onCurrencyChanged(if (currency == "تومان" || currency == "IRT") "IRT" else "IRR")
                                            activeMenu = SettingsMenu.NONE
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currency,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ۳. مدیریت دسته‌بندی‌ها (صفحه مجزا - بدون کشویی)
            if (matchesSearch(
                    titleFa = "مدیریت دسته‌بندی‌ها", titleEn = "Manage Categories",
                    subtitleFa = "ویرایش، حذف یا ایجاد دسته‌های خرید و فروش", subtitleEn = "Edit, delete, or create transaction categories"
                )) {
                item {
                    SettingsSimpleItem(
                        title = if (isPersian) "مدیریت دسته‌بندی‌ها" else "Manage Categories",
                        subtitle = if (isPersian) "ویرایش، حذف یا ایجاد دسته‌های خرید و فروش" else "Edit, delete, or create transaction categories",
                        icon = Icons.Default.Category,
                        onClick = {
                            onAddScreenClick()
                        }
                    )
                }
            }

            // ۴. بخش امنیت (کشویی با سوییچ دوقلو، تغییر رمز و خروج)
            if (matchesSearch(
                    titleFa = "امنیت برنامه", titleEn = "App Security",
                    subtitleFa = "تنظیم رمز ورود و ویژگی‌های بیومتریک", subtitleEn = "Configure passcode and biometric login"
                )) {
                item {
                    // 🔍 بررسی وجود سخت‌افزار بیومتریک/اثر انگشت
                    val hasBiometricHardware = remember(context) {
                        val biometricManager = androidx.biometric.BiometricManager.from(context)
                        val result = biometricManager.canAuthenticate(
                            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        result != androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                                result != androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    }

                    SettingsAccordionItem(
                        title = if (isPersian) "امنیت برنامه" else "App Security",
                        subtitle = if (isPersian) "تنظیم رمز ورود و ویژگی‌های بیومتریک" else "Configure passcode and biometric login",
                        icon = Icons.Default.Lock,
                        isExpanded = activeMenu == SettingsMenu.SECURITY,
                        onClick = {
                            activeMenu = if (activeMenu == SettingsMenu.SECURITY) SettingsMenu.NONE else SettingsMenu.SECURITY
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 🖐️ سوییچ اثر انگشت (فقط در صورت وجود سخت‌افزار نمایش داده می‌شود)
                            if (hasBiometricHardware) {
                                SecuritySwitchRow(
                                    title = if (isPersian) "ورود با اثر انگشت" else "Biometric Login",
                                    icon = Icons.Default.Fingerprint,
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { checked ->
                                        isBiometricEnabled = checked
                                        SharedPreferences.setBiometricEnabled(context, checked)
                                    }
                                )
                            }

                            // 🔐 ردیف: تغییر رمز عبور برنامه
                            SecurityActionRow(
                                title = if (isPersian) "تغییر رمز عبور برنامه" else "Change App Passcode",
                                icon = Icons.Default.Password
                            ) {
                                showChangePasswordDialog = true // پاپ‌آپ باز می‌شود
                            }

                            // 🚪 ردیف: خروج از حساب کاربری (Logout)
                            SecurityActionRow(
                                title = if (isPersian) "خروج از حساب کاربری" else "Log Out",
                                icon = Icons.Default.Logout,
                                iconTint = MaterialTheme.colorScheme.error, // رنگ قرمز برای هشدار خروج
                                titleColor = MaterialTheme.colorScheme.error
                            ) {
                                showLogoutDialog = true
                            }
                        }
                    }
                }
            }

            // ۵. دریافت داده‌ها (کشویی با گزینه‌های دانلود فایل)
            if (matchesSearch(
                    titleFa = "دریافت اطلاعات و گزارش‌ها", titleEn = "Export Data & Reports",
                    subtitleFa = "خروجی گرفتن از تراکنش‌ها در قالب PDF یا Excel", subtitleEn = "Export transactions to PDF or Excel formats"
                )) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "دریافت اطلاعات و گزارش‌ها" else "Export Data & Reports",
                        subtitle = if (isPersian) "خروجی گرفتن از تراکنش‌ها در قالب PDF یا Excel" else "Export transactions to PDF or Excel formats",
                        icon = Icons.Default.Download,
                        isExpanded = activeMenu == SettingsMenu.EXPORT,
                        onClick = {
                            activeMenu = if (activeMenu == SettingsMenu.EXPORT) SettingsMenu.NONE else SettingsMenu.EXPORT
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExportButton(
                                title = "Excel (XLSX)",
                                icon = Icons.Default.TableChart,
                                modifier = Modifier.weight(1f)
                            ) {
                                // منطق اکسپورت اکسل
                            }
                            ExportButton(
                                title = "PDF Document",
                                icon = Icons.Default.PictureAsPdf,
                                modifier = Modifier.weight(1f)
                            ) {
                                // منطق اکسپورت پی‌دی‌اف
                            }
                        }
                    }
                }
            }

            // ۶. درباره ما (کشویی با لینک‌های ارتباطی داینامیک)
            if (matchesSearch(
                    titleFa = "درباره ما و پشتیبانی", titleEn = "About Us & Support",
                    subtitleFa = "راه‌های ارتباطی، تلگرام، اینستاگرام و ایمیل", subtitleEn = "Contact channels, Telegram, Instagram, & Support"
                )) {
                item {
                    SettingsAccordionItem(
                        title = if (isPersian) "درباره ما و پشتیبانی" else "About Us & Support",
                        subtitle = if (isPersian) "راه‌های ارتباطی، تلگرام، اینستاگرام و ایمیل" else "Contact channels, Telegram, Instagram, & Support",
                        icon = Icons.Default.Info,
                        isExpanded = activeMenu == SettingsMenu.ABOUT,
                        onClick = {
                            activeMenu = if (activeMenu == SettingsMenu.ABOUT) SettingsMenu.NONE else SettingsMenu.ABOUT
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SocialLinkRow(
                                title = if (isPersian) "کانال تلگرام" else "Telegram Channel",
                                icon = Icons.Default.Send,
                                color = Color(0xFF229ED9)
                            ) {
                                openUrl(context, "https://t.me/your_channel")
                            }
                            SocialLinkRow(
                                title = if (isPersian) "صفحه اینستاگرام" else "Instagram Page",
                                icon = Icons.Default.CameraAlt,
                                color = Color(0xFFE1306C)
                            ) {
                                openUrl(context, "https://instagram.com/your_profile")
                            }
                            SocialLinkRow(
                                title = if (isPersian) "پشتیبانی جیمیل" else "Gmail Support",
                                icon = Icons.Default.Email,
                                color = Color(0xFFD44638)
                            ) {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@yourdomain.com")
                                }
                                context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                            }
                            SocialLinkRow(
                                title = if (isPersian) "واتس‌اپ توسعه‌دهنده" else "WhatsApp Contact",
                                icon = Icons.Default.Phone,
                                color = Color(0xFF25D366)
                            ) {
                                openUrl(context, "https://wa.me/989123456789")
                            }
                        }
                    }
                }
            }
        }

        // لایه سوم (بالاترین): تاپ‌بار جزیره‌ای شناور با کادر سرچ
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

        // قرار دادن کامپوننت دیالوگ در انتهای اسکرین
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                isPersian = isPersian,
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    onLoginClick()
                }
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                isPersian = isPersian,
                onDismiss = { showChangePasswordDialog = false },
                onConfirm = { oldPassword, secureNewPassword ->

                }
            )
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
                        // --- گزینه حالت روشن (Light Mode = 1) ---
                        val isLightSelected = themeMode == ThemePreferences.MODE_LIGHT
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

                        // --- گزینه حالت تاریک (Dark Mode = 2) ---
                        val isDarkSelected = themeMode == ThemePreferences.MODE_DARK
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
        }    }
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
private fun LanguageOptionButton(
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
private fun SecuritySwitchRow(
    title: String,
    icon: ImageVector, // اضافه شدن پارامتر آیکون
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
        // ترکیب آیکون و متن در کنار هم
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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
    onConfirm: (String, String) -> Unit // حالا رمز قبلی و جدید رو باهم می‌فرسته
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // محاسبه معیارهای سنجش قدرت رمز عبور
    val hasMinLength = newPassword.length >= 8
    val hasUpperCase = newPassword.any { it.isUpperCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecialChar = newPassword.any { !it.isLetterOrDigit() }

    // تعداد معیارهای پاس شده (بین 0 تا 4)
    val strengthScore = listOf(hasMinLength, hasUpperCase, hasDigit, hasSpecialChar).count { it }

    // تعیین رنگ و متن نوار قدرت بر اساس امتیاز
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

    // اجبار کیبورد به زبان انگلیسی
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
                    text = if (isPersian) "رمزهای ورود فقط باید شامل کاراکترهای انگلیسی باشند." else "Passcodes must contain English characters only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // ۱. فیلد رمز عبور فعلی
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = null
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

                // ۲. فیلد رمز عبور جدید
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
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

                // 📊 نوار میزان قدرتمند بودن گذرواژه (کامپوننت جدید)
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
                        // نوار پیشرفت افقی دگرگون شونده
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

                // ۳. فیلد تایید رمز عبور جدید
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
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

                // نمایش پیام‌های خطا
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
                        text.any { it.code > 127 } // بررسی ساده برای کاراکترهای غیر ASCII (مثل فارسی)
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
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = if (isPersian) "جستجو در تنظیمات..." else "Search settings...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true,
                        // 🚀 اضافه کردن اکشن Search به کیبورد
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        // 🚀 بستن کیبورد هنگام لمس دکمه جستجوی کیبورد
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