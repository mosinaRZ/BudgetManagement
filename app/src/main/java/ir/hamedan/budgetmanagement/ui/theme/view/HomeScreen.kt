package ir.hamedan.budgetmanagement.ui.theme.view

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Language // اضافه شدن آیکون مناسب زبان
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.item.BottomNavItem
import ir.hamedan.budgetmanagement.item.CapsuleBottomNavigation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTransactionClick: (Transaction) -> Unit = {},
    onFabClick: () -> Unit = {},
    onBottomNavItemClick: (BottomNavItem) -> Unit = {},
    onThemeToggle: () -> Unit = {}
) {
    val context = LocalContext.current

    // حل مشکل تعریف دوگانه: فقط یک بار و بر اساس لوکال هلپر خوانده می‌شود
    val currentLang = LocaleHelper.getLanguage(context)
    val isPersian = currentLang == "fa"

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(
            if (isPersian) Locale("fa", "IR") else Locale.US
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
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
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )

                // دکمه تغییر زبان (آیکون به Language تغییر یافت)
                IconButton(onClick = {
                    val newLang = if (isPersian) "en" else "fa"
                    LocaleHelper.setLocale(context, newLang)
                    // بازسازی اکتیویتی برای اعمال آنی زبان، فونت و جهت لایه‌ها
                    (context as? Activity)?.recreate()
                }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "تغییر زبان",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (isPersian) "افزودن تراکنش" else "Add Transaction"
                )
            }
        },
        bottomBar = {
            CapsuleBottomNavigation(
                currentRoute = "home",
                onItemSelected = onBottomNavItemClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ۱. بخش خوش‌آمدگویی
            item {
                Column {
                    Text(
                        text = if (isPersian) "سلام، سینا 👋" else "Hi, Sina 👋",
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

            // ۲. کارت موجودی اصلی
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

            // ۳. ردیف کارت‌های خلاصه (درآمد، هزینه زیر هم + تراکنش‌ها جلو)
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
                                    text = "${numberFormatter.format(25800000)}${if (isPersian) " تومان" else ""}",
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
                                    text = "${numberFormatter.format(-13260000)}${if (isPersian) " تومان" else ""}",
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

            // ۴. لیست تراکنش‌ها
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

            item { Spacer(modifier = Modifier.height(100.dp)) }
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