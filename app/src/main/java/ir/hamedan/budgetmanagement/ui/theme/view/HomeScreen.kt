package ir.hamedan.budgetmanagement.ui.theme.view

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.hamedan.budgetmanagement.item.AuroraBackground
import ir.hamedan.budgetmanagement.item.BottomNavItem
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.getFormattedEnglishDate
import ir.hamedan.budgetmanagement.utils.getFormattedPersianDate
import java.text.NumberFormat
import java.util.Locale

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

@Composable
fun HomeScreen(
    onTransactionClick: (Transaction) -> Unit = {},
    onThemeToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = LocaleHelper.getLanguage(context)
    val isPersian = currentLang == "fa"

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(
            if (isPersian) Locale("fa", "IR") else Locale.US
        )
    }

    // 🚀 تغییر لایه اصلی به Box به جای Scaffold برای آزادی مطلق لایه‌ها
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        AuroraBackground()

        // ۱. لیست محتوای اصلی (پایین‌ترین لایه، اسکرول آزاد از سقف تا کف)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 💡 اصلاح فاصله اول لیست برای حل مشکل مخفی شدن زیر کپسول
            item {
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding() // ۱. به صورت هوشمند ارتفاع ساعت و باتری گوشی را رد می‌کند
                        .height(55.dp)       // ۲. دقیقاً به اندازه مجموع ارتفاع کپسول و فاصله‌هایش فضا باز می‌کند
                )
            }

            // بخش خوش‌آمدگویی
            item {
                Column {
                    Text(
                        text = if (isPersian) "سلام، سینا 👋" else "Hi, Cenna 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
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
                            Text("↑ 12%", color = MaterialTheme.colorScheme.primary)
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
                        // کارت درآمد
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // کارت هزینه
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // کارت تعداد تراکنش‌ها تمام‌قد
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTransactionClick(transaction) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = MaterialTheme.typography.headlineLarge.fontSize)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) transaction.title else transaction.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPersian) transaction.date else transaction.dateEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${numberFormatter.format(transaction.amount)}${if (isPersian) " تومان" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (transaction.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 💡 ضربه‌گیر پایینی: افزایش ارتفاع به 110.dp تا لیست پشت داک کپسول و FAB پایینی گیر نکند
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        // ۲. 🚀 لایه بالایی (تاپ‌بار کپسولی شناور - با چیدمان اختصاصی روی Box)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding() // فاصله هوشمند از نوار بالای گوشی
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            val shape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), shape) // افزایش اندک آلفا برای خوانایی بهتر متن‌های زیر آن موقع اسکرول
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)
                    .clip(shape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // دکمه تغییر تم
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "تغییر تم",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // عنوان صفحه
                Text(
                    text = if (isPersian) "مدیریت هزینه‌ها" else "Expense Manager",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )

                // دکمه تغییر زبان
                IconButton(onClick = {
                    val newLang = if (isPersian) "en" else "fa"
                    LocaleHelper.setLocale(context, newLang)
                    (context as? Activity)?.recreate()
                }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "تغییر زبان",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getDummyTransactions(): List<Transaction> = listOf(
    Transaction("1", "رستوران و غذا", "Restaurant & Food", -250000, "۱:۳۰ • امروز", "1:30 • Today", "غذا", "Food"),
    Transaction("2", "سوخت", "Fuel", -800000, "۱۸:۴۵ • دیروز", "18:45 • Yesterday", "سوخت", "Fuel"),
    Transaction("3", "حقوق", "Salary", 25000000, "۹:۰۰ • ۲ روز پیش", "9:00 • 2 days ago", "حقوق", "Salary"),
    Transaction("4", "خرید", "Shopping", -430000, "۱۶:۲۰ • ۳ روز پیش", "16:20 • 3 days ago", "خرید", "Shopping"),
    Transaction("5", "اینترنت و تلفن", "Internet & Phone", -120000, "۱۱:۱۰ • ۳ روز پیش", "11:10 • 3 days ago", "اینترنت", "Internet")
)