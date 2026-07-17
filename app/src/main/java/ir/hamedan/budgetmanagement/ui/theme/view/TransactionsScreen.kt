package ir.hamedan.budgetmanagement.ui.theme.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.item.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// کلاس داده کاملاً دوزبانه برای تراکنش‌ها
data class TransactionItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val categoryFa: String,
    val categoryEn: String,
    val amount: Long, // منفی برای هزینه، مثبت برای درآمد
    val time: String,
    val dateFa: String,
    val dateEn: String
)

// بهترین آبجکت برای مدیریت حالت‌های فیلتر زمانی به صورت دوزبانه
enum class TimeFilter(val titleFa: String, val titleEn: String) {
    DAILY("روزانه", "Daily"),
    WEEKLY("هفتگی", "Weekly"),
    MONTHLY("ماهانه", "Monthly"),
    ALL("کل", "All")
}

@Composable
fun TransactionsScreen(
    onBackClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onEditTransaction: (TransactionItem) -> Unit = {},
    onDeleteTransaction: (TransactionItem) -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    // بهینه‌سازی دکمه بازگشت به بالا با derivedStateOf برای جلوگیری از Recomposition اضافه
    val showScrollToTopButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    var searchQuery by remember { mutableStateOf("") }

    // فیلتر انتخابی زمان (بای‌دیفالت روی "کل")
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.ALL) }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val sampleTransactions = remember {
        listOf(
            TransactionItem(
                id = "1",
                titleFa = "خرید سوپرمارکتی", titleEn = "Grocery Shopping",
                categoryFa = "خوراکی", categoryEn = "Food",
                amount = -450000L, time = "14:20",
                dateFa = "امروز", dateEn = "Today"
            ),
            TransactionItem(
                id = "2",
                titleFa = "حقوق ماهانه", titleEn = "Monthly Salary",
                categoryFa = "درآمد", categoryEn = "Income",
                amount = 32000000L, time = "08:00",
                dateFa = "امروز", dateEn = "Today"
            ),
            TransactionItem(
                id = "3",
                titleFa = "کافه با دوستان", titleEn = "Cafe with Friends",
                categoryFa = "تفریح", categoryEn = "Leisure",
                amount = -280000L, time = "21:15",
                dateFa = "دیروز", dateEn = "Yesterday"
            ),
            TransactionItem(
                id = "4",
                titleFa = "بنزین خودرو", titleEn = "Gasoline",
                categoryFa = "حمل و نقل", categoryEn = "Transport",
                amount = -150000L, time = "10:30",
                dateFa = "دیروز", dateEn = "Yesterday"
            ),
            TransactionItem(
                id = "5",
                titleFa = "شهریه باشگاه", titleEn = "Gym Membership",
                categoryFa = "سلامت", categoryEn = "Health",
                amount = -900000L, time = "18:00",
                dateFa = "۳ روز پیش", dateEn = "3 days ago"
            ),
            TransactionItem(
                id = "6",
                titleFa = "اجاره خانه", titleEn = "House Rent",
                categoryFa = "مسکن", categoryEn = "Bills",
                amount = -8500000L, time = "12:00",
                dateFa = "۳ روز پیش", dateEn = "3 days ago"
            ),
            TransactionItem(
                id = "7",
                titleFa = "فروش لپ‌تاپ قدیمی", titleEn = "Sold Old Laptop",
                categoryFa = "درآمد متفرقه", categoryEn = "Income",
                amount = 14500000L, time = "15:45",
                dateFa = "هفته گذشته", dateEn = "Last week"
            ),
            TransactionItem(
                id = "8",
                titleFa = "اشتراک فیلیمو", titleEn = "Streaming Subscription",
                categoryFa = "رسانه", categoryEn = "Entertainment",
                amount = -120000L, time = "23:10",
                dateFa = "هفته گذشته", dateEn = "Last week"
            ),
            TransactionItem(
                id = "9",
                titleFa = "خرید کتاب", titleEn = "Book Purchase",
                categoryFa = "آموزش", categoryEn = "Education",
                amount = -180000L, time = "11:15",
                dateFa = "هفته گذشته", dateEn = "Last week"
            ),
            TransactionItem(
                id = "10",
                titleFa = "ویزیت دندان‌پزشکی", titleEn = "Dental Clinic",
                categoryFa = "سلامت", categoryEn = "Health",
                amount = -2400000L, time = "17:30",
                dateFa = "هفته گذشته", dateEn = "Last week"
            )
        )
    }

    // ✅ منطق فیلترینگ دوتایی (هم متنی هم زمانی) در داخل derivedStateOf برای پرفورمنس حداکثری
    val groupedTransactions by remember(searchQuery, selectedTimeFilter, isPersian) {
        derivedStateOf {
            sampleTransactions
                .filter { item ->
                    // ۱. اعمال فیلتر متنی
                    val title = if (isPersian) item.titleFa else item.titleEn
                    val category = if (isPersian) item.categoryFa else item.categoryEn
                    val matchesSearch = title.contains(searchQuery, ignoreCase = true) ||
                            category.contains(searchQuery, ignoreCase = true)

                    // ۲. اعمال فیلتر زمانی بر اساس آبجکت انتخابی
                    val matchesTime = when (selectedTimeFilter) {
                        TimeFilter.DAILY -> {
                            if (isPersian) item.dateFa == "امروز" || item.dateFa == "دیروز"
                            else item.dateEn == "Today" || item.dateEn == "Yesterday"
                        }
                        TimeFilter.WEEKLY -> {
                            if (isPersian) item.dateFa != "هفته گذشته"
                            else item.dateEn != "Last week"
                        }
                        TimeFilter.MONTHLY -> true // در داده‌های نمونه فعلی همه در بازه ماه هستند
                        TimeFilter.ALL -> true
                    }

                    matchesSearch && matchesTime
                }
                .groupBy {
                    if (isPersian) it.dateFa else it.dateEn
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            // تغییر Padding بالا برای باز شدن فضا جهت قرارگیری فیلترهای زمانی زیر TopBar
            contentPadding = PaddingValues(top = 185.dp, bottom = 165.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedTransactions.forEach { (dateHeader, items) ->
                item(key = dateHeader) {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                items(items, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        isPersian = isPersian,
                        numberFormatter = numberFormatter,
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = { onDeleteTransaction(transaction) }
                    )
                }
            }
        }

        // بخش بالایی صفحه شامل هدر اصلی و ردیف گزینه‌های فیلتر زمانی
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            TransactionsTopBar(
                isPersian = isPersian,
                onFilterClick = onFilterClick
            )

            // ردیف گزینه‌های فیلتر زمانی (روزانه، هفتگی، ماهانه، کل)
            TimeFilterSelector(
                selectedFilter = selectedTimeFilter,
                isPersian = isPersian,
                onFilterSelected = { selectedTimeFilter = it }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isPersian) "برگشت به بالا" else "Scroll to Top",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            val searchShape = RoundedCornerShape(20.dp)
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), searchShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), searchShape),
                placeholder = {
                    Text(
                        text = if (isPersian) "جستجو در تراکنش‌ها..." else "Search transactions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = if (isPersian) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isPersian) "پاک کردن" else "Clear",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                shape = searchShape
            )
        }
    }
}

// کامپوننت دکمه‌های فیلتر زمانی با دیزاین مدرن و واکنش‌گرا
@Composable
private fun TimeFilterSelector(
    selectedFilter: TimeFilter,
    isPersian: Boolean,
    onFilterSelected: (TimeFilter) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // لیست کردن فیلترها براساس چیدمان راست‌به‌چپ یا چپ‌به‌راست زبان سیستم
        val filters = remember { TimeFilter.values() }

        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val title = if (isPersian) filter.titleFa else filter.titleEn

            val backgroundAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                label = "TabBgAlpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(shape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha * 0.15f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                        shape = shape
                    )
                    .clickable { onFilterSelected(filter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionItem,
    isPersian: Boolean,
    numberFormatter: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rowShape = RoundedCornerShape(20.dp)
    val isExpense = transaction.amount < 0
    var isRevealed by remember { mutableStateOf(false) }

    val revealOffsetDp = if (isPersian) 120.dp else (-120).dp
    val animatedOffset by animateDpAsState(
        targetValue = if (isRevealed) revealOffsetDp else 0.dp,
        label = "RevealAnimation"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        label = "AlphaAnimation"
    )

    val formattedAmount = remember(transaction.amount, isPersian) {
        if (isPersian && isExpense) {
            "-${numberFormatter.format(java.lang.Math.abs(transaction.amount))}"
        } else {
            numberFormatter.format(transaction.amount)
        }
    }

    val title = if (isPersian) transaction.titleFa else transaction.titleEn
    val category = if (isPersian) transaction.categoryFa else transaction.categoryEn

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(animatedAlpha)
                .background(Color.Transparent, rowShape)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            // تغییر فقط در این خط اعمال شد:
            // در حالت انگلیسی (LTR) مقدار End دکمه‌ها را به سمت راست صفحه می‌برد.
            // در حالت پارسی (RTL) مقدار End دکمه‌ها را به سمت چپ صفحه می‌برد.
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    onEdit()
                    isRevealed = false
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isPersian) "ویرایش" else "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    onDelete()
                    isRevealed = false
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = if (isPersian) "حذف" else "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
        }

        Row(
            modifier = Modifier
                .graphicsLayer {
                    translationX = animatedOffset.toPx()
                }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), rowShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), rowShape)
                .clip(rowShape)
                .clickable { isRevealed = !isRevealed }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isExpense) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$category • ${transaction.time}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Text(
                text = "$formattedAmount ${if (isPersian) "تومان" else "T"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { isRevealed = !isRevealed },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isRevealed) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = if (isPersian) "گزینه‌ها" else "Options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionsTopBar(
    isPersian: Boolean,
    onFilterClick: () -> Unit
) {
    val smallShape = RoundedCornerShape(24.dp)
    val centerShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), centerShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), centerShape)
                    .clip(centerShape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPersian) "جزئیات تراکنش‌ها" else "Transaction Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), smallShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                    .clip(smallShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = if (isPersian) "فیلتر" else "Filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}