package ir.hamedan.budgetmanagement.ui.theme.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import kotlinx.coroutines.delay

data class AddOptionItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val icon: ImageVector,
    val descriptionFa: String,
    val descriptionEn: String,
    val route: String
)

@Composable
fun AddScreen(
    highlightId: String? = null, // دریافت پارامتر فوکوس
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    // وضعیت برای کنترل زمان انیمیشن (بعد از چند ثانیه پالس زدن متوقف شود)
    var isAnimationActive by remember { mutableStateOf(highlightId != null) }

    // تایمر برای اتمام خودکار انیمیشن پالس بعد از ۴ ثانیه
    LaunchedEffect(highlightId) {
        if (highlightId != null) {
            delay(4000)
            isAnimationActive = false
        }
    }

    val addOptions = remember {
        listOf(
            // ترکیب دو دکمه خرید و فروش در یک کارت تراکنش
            AddOptionItem(
                "transaction", "ثبت تراکنش", "Add Transaction",
                Icons.Default.SwapHoriz, "ثبت خرید یا درآمد جدید", "Add expense or income", "add_transaction"
            ),
            AddOptionItem(
                "piggy", "مدیریت قلک‌ها", "Manage Goals",
                Icons.Default.StarBorder, "ایجاد یا ویرایش اهداف", "Create or edit savings targets", "add_goal"
            ),
            AddOptionItem(
                "category", "مدیریت دسته‌بندی‌ها", "Manage Categories",
                Icons.Default.Category, "مدیریت دسته‌ها", "Manage categories", "add_category"
            ),
            AddOptionItem(
                "limit", "مدیریت محدودیت‌ها", "Manage Limits",
                Icons.Default.Warning, "تعیین یا ویرایش سقف بودجه", "Set or edit budget ceilings", "add_limit"
            ),
            AddOptionItem(
                "due", "مدیریت موعدها", "Manage Due Dates",
                Icons.Default.Event, "یادآور پرداخت قسط/بدهی", "Installment/Debt reminders", "add_due"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        // 🚀 ۱. محتوای اسکرول‌پذیر (گرید کارت‌ها)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 160.dp, // فاصله کافی برای عبور زیر متون هدر
                bottom = 110.dp // فاصله کافی برای عبور بالای دکمه انصراف
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // استفاده از پارامتر span برای تشخیص دکمه تراکنش
            items(
                items = addOptions,
                span = { option ->
                    if (option.id == "transaction") GridItemSpan(2) else GridItemSpan(1)
                }
            ) { option ->
                val optionShape = RoundedCornerShape(24.dp)
                val isWide = option.id == "transaction" // آیا این همان دکمه عریض است؟

                val isTargetHighlight = (option.id == highlightId ||
                        (option.id == "transaction" && (highlightId == "buy" || highlightId == "sell"))) && isAnimationActive

                // انیمیشن‌ها
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val borderAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
                    label = "alpha"
                )

                val borderColor = if (isTargetHighlight) MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                val borderWidth = if (isTargetHighlight) 2.5.dp else 1.dp
                val contentColor = if (isWide) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // اگر دکمه عریض است، ارتفاع ثابت بده، وگرنه مربعی بماند
                        .then(if (isWide) Modifier.height(140.dp) else Modifier.aspectRatio(1f))
                        .background(
                            if (isTargetHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            optionShape
                        )
                        .border(borderWidth, borderColor, optionShape)
                        .clip(optionShape)
                        .clickable { /* هدایت به صفحه تراکنش */ }
                        .padding(16.dp)
                ) {
                    // چیدمان داخل دکمه عریض (افقی) در مقابل مربعی (عمودی)
                    if (isWide) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(option.icon, null, modifier = Modifier.size(40.dp), tint = if (isTargetHighlight) MaterialTheme.colorScheme.primary else contentColor)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(if (isPersian) option.titleFa else option.titleEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isTargetHighlight) MaterialTheme.colorScheme.primary else contentColor)
                                Text(if (isPersian) option.descriptionFa else option.descriptionEn, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(option.icon, null, modifier = Modifier.size(48.dp), tint = if (isTargetHighlight) MaterialTheme.colorScheme.primary else contentColor)
                            Spacer(Modifier.height(12.dp))
                            Text(if (isPersian) option.titleFa else option.titleEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (isTargetHighlight) MaterialTheme.colorScheme.primary else contentColor, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // 🚀 ۲. عنوان و توضیحات اصلی (غیرقابل اسکرول و بدون پس‌زمینه کادر اضافه)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 8.dp)
        ) {
            Text(
                text = if (isPersian) "مدیریت و افزودن آیتم‌ها" else "Add & Manage Items",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPersian) "یکی از گزینه‌های زیر را انتخاب کنید" else "Select one of the options below",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 🚀 ۳. دکمه انصراف شیشه‌ای و غیرقابل اسکرول (پایین صفحه)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp)
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { onBackClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isPersian) "انصراف" else "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
            }
        }
    }
}