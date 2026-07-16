package ir.hamedan.budgetmanagement.ui.theme.view.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.item.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.LocaleHelper

// ۱. مدیریت وضعیت منوهای کشویی (فقط یک منو در لحظه باز می‌ماند)
enum class SettingsMenu {
    LANGUAGE, CURRENCY, SECURITY, EXPORT, ABOUT, NONE
}

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit = {}, // هدایت کاربر به صفحه مجزای مدیریت دسته‌بندی‌ها
    onCurrencyChanged: (String) -> Unit = {}  // تغییر واحد پولی ("IRT" یا "IRR")
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()

    // وضعیت جستجو در تنظیمات
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // وضعیت فعال بودن کدام منوی کشویی (بای‌دیفالت هیچ‌کدام باز نیستند)
    var activeMenu by remember { mutableStateOf(SettingsMenu.NONE) }

    // وضعیت‌های نمونه برای سوییچ‌های بخش امنیت و واحد پول
    var isBiometricEnabled by remember { mutableStateOf(false) }
    var isPasswordEnabled by remember { mutableStateOf(false) }
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
        AuroraBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // تاپ‌بار اختصاصی به همراه دکمه سرچ
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // هماهنگ‌سازی کلیک آکاردئونی: با باز شدن هر آیتم، بقیه به کمک activeMenu بسته می‌شوند.

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
                                activeMenu = SettingsMenu.NONE // بستن کشویی‌ها قبل تغییر صفحه
                                onNavigateToCategories()
                            }
                        )
                    }
                }

                // ۴. بخش امنیت (کشویی با سوییچ دوقلو)
                if (matchesSearch(
                        titleFa = "امنیت برنامه", titleEn = "App Security",
                        subtitleFa = "تنظیم رمز ورود و ویژگی‌های بیومتریک", subtitleEn = "Configure passcode and biometric login"
                    )) {
                    item {
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
                                SecuritySwitchRow(
                                    title = if (isPersian) "فعال‌سازی گذرواژه" else "Enable Passcode",
                                    checked = isPasswordEnabled,
                                    onCheckedChange = { isPasswordEnabled = it }
                                )
                                SecuritySwitchRow(
                                    title = if (isPersian) "ورود با اثر انگشت / چهره" else "Biometric Login",
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { isBiometricEnabled = it }
                                )
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
        }
    }
}

// کامپوننت پایه برای آیتم‌های منبسط شونده با انیمیشن روان ورتیکال کامپوز
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

        // پیاده‌سازی انیمیشن انقباض و انبساط کشویی شبیه آکاردئون با حفظ فلو پرفورمنس
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

// کامپوننت پایه برای آیتم‌های ساده تک کلیکی مثل هدایت به صفحه جدید
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            // کادر مرکزی (نمایش پویای تایتل یا فیلد سرچ متناسب با کلیک روی ذره‌بین)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), barShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), barShape)
                    .clip(barShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchExpanded) {
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

            // دکمه سرچ
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

// تابع کمکی جهت باز کردن امن آدرس‌های اینترنتی
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}