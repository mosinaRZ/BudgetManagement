package ir.hamedan.budgetmanagement.ui.theme.view

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.item.AuroraBackground
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.getFormattedEnglishDate
import ir.hamedan.budgetmanagement.utils.getFormattedPersianDate
import java.text.NumberFormat
import java.util.Locale

// ساختار داده جدید مخصوص اعلان‌ها و پیام‌های سیستم
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String,
    val timeFa: String,
    val timeEn: String,
    val isRead: Boolean = false
)

enum class NotificationType {
    SUCCESS,  // مثل ثبت جدید، موفقیت‌آمیز بودن تراکنش
    ERROR,    // خطاها، اتمام بودجه، هشدارهای امنیتی
    REWARD,   // جوایز، کمپین‌ها، تخفیف‌ها
    SYSTEM    // اخبار، بروزرسانی‌ها، پیام‌های پشتیبانی
}

data class Transaction(
    val id: String,
    val title: String,
    val titleEn: String,
    val amount: Long,
    val date: String,
    val dateEn: String,
    val category: String,
    val categoryEn: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTransactionClick: (Transaction) -> Unit = {},
    onThemeToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = LocaleHelper.getLanguage(context)
    val isPersian = currentLang == "fa"

    // مدیریت خروج با دکمه بک
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            val message = if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    // 🔥 مدیریت وضعیت باتم شیت اعلان‌ها
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    // داده‌های نمونه برای نوتیفیکیشن‌ها و پیام‌ها
    val notifications = remember { getDummyNotifications() }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        // ۱. لیست محتوای اصلی هوم‌اسکرین
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding().height(55.dp))
            }

            // بخش خوش‌آمدگویی
            item {
                Column {
                    Text(
                        text = if (isPersian) "سلام، سینا 👋" else "Hi, Cenna 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPersian) getFormattedPersianDate() else getFormattedEnglishDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // کارت موجودی اصلی
            item {
                val balanceShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), balanceShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), balanceShape)
                        .clip(balanceShape)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = if (isPersian) "تراز این ماه" else "This Month Balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "${numberFormatter.format(12540000)} ${if (isPersian) "تومان" else "T"}",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↑ 12%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isPersian) " نسبت به ماه قبل" else " vs last month",
                                modifier = Modifier.padding(start = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ردیف کارت‌های خلاصه
            item {
                val summaryCardShape = RoundedCornerShape(20.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), summaryCardShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), summaryCardShape)
                                .clip(summaryCardShape)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isPersian) "درآمد این ماه" else "Income",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${numberFormatter.format(25800000)}${if (isPersian) " تومان" else " T"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), summaryCardShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), summaryCardShape)
                                .clip(summaryCardShape)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isPersian) "هزینه این ماه" else "Expense",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${numberFormatter.format(-13260000)}${if (isPersian) " تومان" else " T"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), summaryCardShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), summaryCardShape)
                            .clip(summaryCardShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isPersian) "تراکنش‌ها" else "Transactions",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "42",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // عنوان لیست تراکنش‌ها
            item {
                Text(
                    text = if (isPersian) "آخرین تراکنش‌ها" else "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // لیست تراکنش‌ها
            items(getDummyTransactions(), key = { it.id }) { transaction ->
                val emoji = when (transaction.category) {
                    "غذا", "Food" -> "🍔"
                    "سوخت", "Fuel" -> "⛽"
                    "حقوق", "Salary" -> "💼"
                    "خرید", "Shopping" -> "🛒"
                    "اینترنت", "Internet" -> "📞"
                    else -> "📌"
                }

                val rowShape = RoundedCornerShape(20.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), rowShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), rowShape)
                        .clip(rowShape)
                        .clickable { onTransactionClick(transaction) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) transaction.title else transaction.titleEn,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPersian) transaction.date else transaction.dateEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "${numberFormatter.format(transaction.amount)} ${if (isPersian) "تومان" else "T"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        // ۲. تاپ‌بار جزیره‌ای سه تیکه شناور
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            val smallShape = RoundedCornerShape(24.dp)
            val centerShape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // تیکه چپ: تغییر تم
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), smallShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                        .clip(smallShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = if (isPersian) "تغییر تم" else "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // تیکه وسط: عنوان
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), centerShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), centerShape)
                        .clip(centerShape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPersian) "مدیریت هزینه‌ها" else "Expense Manager",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // تیکه راست: زنگوله اعلان‌ها (باز کردن باتم‌شیت شناور فوقِ معلق)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), smallShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                        .clip(smallShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = if (isPersian) "اعلان‌ها" else "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ==========================================
        // 🌟 پیاده‌سازی باتم‌شیت کاملاً معلق و مستقل (خالی از کف و طرفین)
        // ==========================================
        if (showBottomSheet) {
            val density = LocalDensity.current
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val screenHeightPx = with(density) { screenHeight.toPx() }

            // مانیتور کردن افست لحظه‌ای شیت
            val rawOffset = try { sheetState.requireOffset() } catch (_: Exception) { screenHeightPx / 2f }
            val expansionProgress = (1f - (rawOffset / screenHeightPx)).coerceIn(0f, 1f)

            // اگر به سقف یا خیلی بالا رفت، لبه‌های گرد و فواصل حذف شوند
            val isFullyExpanded = expansionProgress > 0.82f

            // فواصل نرم از اطراف و پایین (Bottom Padding اضافه شد تا از کف زمین معلق بماند)
            val animatedPaddingHorizontal by animateDpAsState(
                targetValue = if (isFullyExpanded) 0.dp else 16.dp,
                label = "PaddingHorizontal"
            )
            val animatedPaddingBottom by animateDpAsState(
                targetValue = if (isFullyExpanded) 0.dp else 36.dp, // ۳۶ دی‌پی فاصله از پایین صفحه برای تعلیق کامل
                label = "PaddingBottom"
            )
            val animatedCornerRadius by animateIntAsState(
                targetValue = if (isFullyExpanded) 0 else 32,
                label = "CornerRadius"
            )

            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                // کانتینر اصلی را کاملاً شفاف می‌کنیم تا استایل شناور خود را بکشیم
                containerColor = Color.Transparent,
                scrimColor = Color.Black.copy(alpha = 0.4f),
                dragHandle = null
                // 💡 پارامتر windowInsets برای سازگاری کامل با همه نسخه‌های Material 3 حذف شد
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = animatedPaddingHorizontal,
                            end = animatedPaddingHorizontal,
                            bottom = animatedPaddingBottom
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(
                                topStart = animatedCornerRadius.dp,
                                topEnd = animatedCornerRadius.dp,
                                bottomStart = if (isFullyExpanded) 0.dp else animatedCornerRadius.dp,
                                bottomEnd = if (isFullyExpanded) 0.dp else animatedCornerRadius.dp
                            )
                        )
                        .border(
                            width = if (isFullyExpanded) 0.dp else 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(
                                topStart = animatedCornerRadius.dp,
                                topEnd = animatedCornerRadius.dp,
                                bottomStart = if (isFullyExpanded) 0.dp else animatedCornerRadius.dp,
                                bottomEnd = if (isFullyExpanded) 0.dp else animatedCornerRadius.dp
                            )
                        )
                        // 🚀 پدینگ‌ها سیستم‌بار و دکمه‌های ناوبری به صورت خودکار و ایمن اینجا مدیریت می‌شوند
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // درگ هندل مینیمال
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        // هدر بخش پیام‌ها
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPersian) "اعلان‌ها و رویدادها" else "System Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isPersian) "۴ پیام جدید" else "4 New",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // لیست نوتیفیکیشن‌ها و اخبار
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                        ) {
                            items(notifications, key = { it.id }) { item ->
                                val itemShape = RoundedCornerShape(18.dp)

                                val (icon, iconColor, bgColor) = when (item.type) {
                                    NotificationType.SUCCESS -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.08f))
                                    NotificationType.ERROR -> Triple(Icons.Default.Error, Color(0xFFE53935), Color(0xFFE53935).copy(alpha = 0.08f))
                                    NotificationType.REWARD -> Triple(Icons.Default.CardGiftcard, Color(0xFFFFB300), Color(0xFFFFB300).copy(alpha = 0.08f))
                                    NotificationType.SYSTEM -> Triple(Icons.Default.Info, Color(0xFF1E88E5), Color(0xFF1E88E5).copy(alpha = 0.08f))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), itemShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), itemShape)
                                        .clip(itemShape)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(bgColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isPersian) item.titleFa else item.titleEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isPersian) item.timeFa else item.timeEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (isPersian) item.descFa else item.descEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.15
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// تولید داده‌های دوزبانه فیلتر شده و تست شده برای نوتیفیکیشن‌ها
private fun getDummyNotifications(): List<NotificationItem> = listOf(
    NotificationItem(
        id = "n1",
        type = NotificationType.SUCCESS,
        titleFa = "ثبت تراکنش موفقیت‌آمیز",
        titleEn = "Transaction Registered",
        descFa = "تراکنش مربوط به خرید اینترنت با موفقیت در پایگاه داده ذخیره و ترازنامه ماهانه شما به‌روزرسانی شد.",
        descEn = "Your internet purchase transaction has been successfully recorded and the monthly balance updated.",
        timeFa = "۱۰ دقیقه پیش",
        timeEn = "10m ago"
    ),
    NotificationItem(
        id = "n2",
        type = NotificationType.REWARD,
        titleFa = "جایزه چالش پس‌انداز ماهانه 🏆",
        titleEn = "Savings Challenge Reward 🏆",
        descFa = "تبریک! به دلیل ثبت منظم هزینه‌ها و رعایت سقف مجاز، از طرف پشتیبانی مرکزی کد تخفیف اختصاصی خرید از دیجی‌کالا دریافت کردید.",
        descEn = "Congratulations! For regular expense tracking, you have received an exclusive shopping discount code.",
        timeFa = "۱ ساعت پیش",
        timeEn = "1h ago"
    ),
    NotificationItem(
        id = "n3",
        type = NotificationType.ERROR,
        titleFa = "هشدار عبور از سقف بودجه!",
        titleEn = "Budget Threshold Alert!",
        descFa = "هزینه‌های مربوط به دسته بندی «رستوران و غذا» به ۸۵٪ سقف تعیین شده رسیده است. لطفا تراکنش‌های بعدی خود را مدیریت کنید.",
        descEn = "Your expenses in \"Food & Dining\" category have reached 85% of your defined monthly limit.",
        timeFa = "دیروز",
        timeEn = "Yesterday"
    ),
    NotificationItem(
        id = "n4",
        type = NotificationType.SYSTEM,
        titleFa = "بروزرسانی نسخه جدید برنامه‌ریزی مالی",
        titleEn = "New Feature Release",
        descFa = "نسخه جدید هسته مدیریت هوشمند بودجه منتشر شد. اکنون می‌توانید ویجت‌های شیشه‌ای اختصاصی به صفحه اصلی گوشی اضافه کنید.",
        descEn = "Version 2.4 is live. You can now add beautiful glassmorphism widgets directly to your phone's home screen.",
        timeFa = "۲ روز پیش",
        timeEn = "2 days ago"
    )
)

private fun getDummyTransactions(): List<Transaction> = listOf(
    Transaction("1", "رستوران و غذا", "Restaurant & Food", -250000, "۱:۳۰ • امروز", "1:30 • Today", "غذا", "Food"),
    Transaction("2", "سوخت", "Fuel", -800000, "۱۸:۴۵ • دیروز", "18:45 • Yesterday", "سوخت", "Fuel"),
    Transaction("3", "حقوق", "Salary", 25000000, "۹:۰۰ • ۲ روز پیش", "9:00 • 2 days ago", "حقوق", "Salary"),
    Transaction("4", "خرید", "Shopping", -430000, "۱۶:۲۰ • ۳ روز پیش", "16:20 • 3 days ago", "خرید", "Shopping"),
    Transaction("5", "اینترنت و تلفن", "Internet & Phone", -120000, "۱۱:۱۰ • ۳ روز پیش", "11:10 • 3 days ago", "اینترنت", "Internet")
)