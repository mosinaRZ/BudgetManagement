package ir.hamedan.budgetmanagement.ui.screens.transactions

import ir.hamedan.budgetmanagement.di.appViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.screens.add.ThousandsSeparatorTransformation
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransactionClick: () -> Unit = {},
    viewModel: TransactionViewModel = appViewModel()
) {
    val isPersian = isPersianLocale()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardVisible = imeBottom > 0

    val showScrollToTopButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val transactionsList by viewModel.filteredTransactions.collectAsState()

    val currencyUnit by viewModel.currencyUnit.collectAsState(initial = "IRT")

    var showFilterSheet by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val groupedTransactions = remember(transactionsList, isPersian) {
        transactionsList.groupBy { transaction ->
            getRelativeDateHeader(transaction.timestamp, isPersian)
        }
    }

    val isTrulyEmpty = !isLoading && transactionsList.isEmpty() &&
            searchQuery.isBlank() &&
            !filterState.isCustomFilterActive &&
            filterState.timeFilter == TimeFilter.ALL

    val noSearchResults = !isLoading && searchQuery.isNotBlank() && transactionsList.isEmpty()

    val noFilterResults = !isLoading && transactionsList.isEmpty() &&
            searchQuery.isBlank() &&
            (filterState.isCustomFilterActive || filterState.timeFilter != TimeFilter.ALL)

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        when {
            // حالت بارگذاری اسکلتون
            isLoading -> {
                TransactionsSkeletonScreen()
            }

            isTrulyEmpty -> {
                val emptyCardShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 170.dp, start = 24.dp, end = 24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), emptyCardShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), emptyCardShape)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "💸", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isPersian) "هنوز هیچ تراکنشی ثبت نشده!" else "No Transactions Yet!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPersian) "برای مدیریت دقیق‌تر هزینه‌ها و درآمدها، اولین تراکنش خود را همین حالا ثبت کنید."
                            else "Start tracking your finances by adding your very first transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onAddTransactionClick() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "ثبت اولین تراکنش" else "Add First Transaction",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            noSearchResults -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 170.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPersian)
                            "برای «$searchQuery» چیزی یافت نشد"
                        else
                            "Nothing found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            noFilterResults -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 170.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPersian) "تراکنشی با این فیلتر پیدا نشد" else "No Transactions Match This Filter",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian)
                                "فیلترهای انتخاب‌شده را تغییر بده یا پاکشون کن تا تراکنش‌ها نمایش داده شوند."
                            else
                                "Try adjusting or clearing the selected filters to see your transactions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(
                        top = if (keyboardVisible) 310.dp else 170.dp,
                        bottom = 190.dp
                    ),
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
                                currencyUnit = currencyUnit,
                                numberFormatter = numberFormatter,
                                onEdit = { transactionToEdit = transaction },
                                onDelete = { transactionToDelete = transaction }
                            )
                        }
                    }
                }
            }
        }

        // بخش تاپ‌بار و فیلترها
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            TransactionsTopBar(
                isPersian = isPersian,
                isFilterActive = filterState.isCustomFilterActive,
                showFilterButton = !isTrulyEmpty && !isLoading,
                onFilterClick = { showFilterSheet = true }
            )

            if (!isTrulyEmpty && !isLoading) {
                if (filterState.isCustomFilterActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.clearFilter() },
                            label = {
                                Text(
                                    if (isPersian) "حذف فیلترها ✕" else "Clear Filters ✕",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                } else {
                    TimeFilterSelector(
                        selectedFilter = filterState.timeFilter,
                        isPersian = isPersian,
                        onFilterSelected = { newTimeFilter ->
                            val previousTimeFilter = filterState.timeFilter
                            if (newTimeFilter != previousTimeFilter) {
                                viewModel.setQuickTimeFilter(newTimeFilter)

                                coroutineScope.launch {
                                    val newTitle = if (isPersian) newTimeFilter.titleFa else newTimeFilter.titleEn
                                    snackbarHostState.showSnackbar(
                                        message = if (isPersian) "فیلتر زمان به «$newTitle» تغییر یافت" else "Filter set to \"$newTitle\"",
                                        duration = SnackbarDuration.Indefinite
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // سرچ و دکمه بازگشت به بالا
        if (!isTrulyEmpty && !noFilterResults && !isLoading) {
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
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                CircleShape
                            )
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
                    onValueChange = { newValue ->
                        if (newValue.length <= 40) {
                            viewModel.onSearchQueryChanged(newValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            searchShape
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            searchShape
                        ),
                    placeholder = {
                        Text(
                            text = if (isPersian) "جستجو در لیست .." else "Search in list ..",
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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
                                    viewModel.onSearchQueryChanged(query)
                                },
                                language = if (isPersian) "fa-IR" else "en-US"
                            )
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

        // اسنک‌بار پیام اختصاصی همراه با تایمر دایره‌ای معکوس برای Undo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 140.dp, start = 16.dp, end = 16.dp)
        ) { data ->
            CircularCountdownSnackbar(
                snackbarData = data,
                isPersian = isPersian,
                totalSeconds = 5
            )
        }

        // ۱. باتم شیت شناور فیلترینگ
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilterState = filterState,
                isPersian = isPersian,
                onDismiss = { showFilterSheet = false },
                onApply = { timeFilter, typeFilter, sortOrder, start, end ->
                    viewModel.applyCustomFilter(timeFilter, typeFilter, sortOrder, start, end)
                    showFilterSheet = false
                }
            )
        }

        // ۲. باتم شیت شناور ویرایش تراکنش
        transactionToEdit?.let { tx ->
            EditTransactionBottomSheet(
                transaction = tx,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                viewModel = viewModel,
                onDismiss = { transactionToEdit = null },
                onConfirm = { updatedTx ->
                    viewModel.updateTransaction(updatedTx)
                    transactionToEdit = null
                }
            )
        }

        // ۳. دیالوگ تایید حذف
        transactionToDelete?.let { tx ->
            // متغیرهای وضعیت برای تشخیص نگه‌داشتن دکمه و پر شدن انیمیشن
            var isPressed by remember { mutableStateOf(false) }

            // انیمیشن محو شدن و جمع شدن دکمه انصراف (تغییر وزن از ۱ به ۰)
            val cancelWeight by animateFloatAsState(
                targetValue = if (isPressed) 0.001f else 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                label = "CancelWeight"
            )

            // انیمیشن پر شدن دکمه حذف (از ۰ تا ۱ در طی ۱.۵ ثانیه)
            val progress by animateFloatAsState(
                targetValue = if (isPressed) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isPressed) 1500 else 300, // زمان لازم برای نگه داشتن (۱.۵ ثانیه)
                    easing = LinearEasing
                ),
                label = "HoldProgress"
            )

            // وقتی انیمیشن پر شدن به ۱۰۰٪ رسید، عملیات حذف انجام می‌شود
            LaunchedEffect(progress) {
                if (progress >= 1f && isPressed) {
                    isPressed = false // ریست کردن وضعیت
                    val deletedTx = tx
                    viewModel.deleteTransaction(deletedTx)
                    transactionToDelete = null

                    coroutineScope.launch {
                        val displayTitle = deletedTx.title.ifEmpty {
                            StringMapper.getCategoryName(deletedTx.category, isPersian)
                        }
                        val result = snackbarHostState.showSnackbar(
                            message = if (isPersian) "تراکنش «$displayTitle» حذف شد" else "Transaction \"$displayTitle\" deleted",
                            actionLabel = if (isPersian) "بازگردانی" else "Undo",
                            duration = SnackbarDuration.Indefinite
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreTransaction(deletedTx)
                        } else {
                            viewModel.commitDeleteTransaction(deletedTx)
                        }
                    }
                }
            }

            val dialogShape = RoundedCornerShape(28.dp)
            Dialog(onDismissRequest = { transactionToDelete = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            shape = dialogShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = dialogShape
                        )
                        .clip(dialogShape)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = if (isPersian) "حذف تراکنش" else "Delete Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isPersian)
                                "آیا از حذف این تراکنش اطمینان دارید؟"
                            else
                                "Are you sure you want to delete this transaction?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // دکمه انصراف (با تغییر وزن و پدینگ نرم کنار می‌رود)
                            if (cancelWeight > 0.01f) {
                                OutlinedButton(
                                    onClick = { transactionToDelete = null },
                                    modifier = Modifier
                                        .weight(cancelWeight)
                                        .height(48.dp)
                                        .padding(end = (12 * cancelWeight).dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isPersian) "انصراف" else "Cancel",
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                }
                            }

                            // دکمه حذف سفارشی (تشخیص نگه داشتن انگشت و پر شدن پس‌زمینه)
                            Box(
                                modifier = Modifier
                                    .weight(1f) // همیشه فضای باقیمانده را پر می‌کند
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true // کاربر انگشت را گذاشت
                                                tryAwaitRelease() // منتظر برداشتن انگشت می‌مانیم
                                                isPressed = false // کاربر انگشت را برداشت
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // لایه پر شونده سفید رنگ (از سمت چپ به راست)
                                if (progress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.coerceAtLeast(0.001f))
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )
                                }

                                // محتوای دکمه (آیکون و متن)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isPressed) {
                                            if (isPersian) "در حال حذف..." else "Deleting..."
                                        } else {
                                            if (isPersian) "حذف" else "Hold to Delete"
                                        },
                                        color = MaterialTheme.colorScheme.onError,
                                        maxLines = 1
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

// -----------------------------------------------------------------------------
// کامپوننت‌ها و انیمیشن‌های Skeleton Loading (Shimmer Effect)
// -----------------------------------------------------------------------------

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslation"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    return this.background(brush)
}

@Composable
private fun TransactionsSkeletonScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 170.dp, bottom = 190.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بخش ۱: هدر اسکلتون
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(14.dp)
                    .width(70.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
        items(3) {
            TransactionSkeletonRow()
        }

        // بخش ۲: هدر اسکلتون دوم
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(14.dp)
                    .width(90.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
        items(2) {
            TransactionSkeletonRow()
        }
    }
}

@Composable
private fun TransactionSkeletonRow() {
    val rowShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), rowShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), rowShape)
            .clip(rowShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // آیکون دایره‌ای
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.width(12.dp))

        // عنوان و دسته‌بندی
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.55f)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.35f)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // مبلغ
        Box(
            modifier = Modifier
                .height(18.dp)
                .width(75.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )
    }
}

// -----------------------------------------------------------------------------
// سایر کامپوننت‌های کمکی
// -----------------------------------------------------------------------------

@Composable
private fun CircularCountdownSnackbar(
    snackbarData: SnackbarData,
    isPersian: Boolean,
    totalSeconds: Int = 5
) {
    val animatedProgress = remember { Animatable(1f) }

    LaunchedEffect(snackbarData) {
        animatedProgress.snapTo(1f)
        animatedProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = totalSeconds * 1000,
                easing = LinearEasing
            )
        )
        snackbarData.dismiss()
    }

    val secondsLeft = kotlin.math.ceil(animatedProgress.value * totalSeconds).toInt()

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            snackbarData.visuals.actionLabel?.let { actionLabel ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { snackbarData.performAction() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = numberFormatter.format(secondsLeft),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentFilterState: FilterState,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onApply: (TimeFilter, TransactionTypeFilter, SortOrder, Long?, Long?) -> Unit
) {
    var selectedTime by remember { mutableStateOf(currentFilterState.timeFilter) }
    var selectedType by remember { mutableStateOf(currentFilterState.typeFilter) }
    var selectedSort by remember { mutableStateOf(currentFilterState.sortOrder) }

    var startDate by remember { mutableStateOf(currentFilterState.startDate) }
    var endDate by remember { mutableStateOf(currentFilterState.endDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isPersian) "فیلتر پیشرفته" else "Advanced Filter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(if (isPersian) "نوع تراکنش:" else "Type:", fontWeight = FontWeight.SemiBold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(TransactionTypeFilter.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(if (isPersian) type.titleFa else type.titleEn) }
                    )
                }
            }

            Text(if (isPersian) "بازه زمانی سفارشی:" else "Custom Date Range:", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (startDate != null) DateUtils.formatTimestamp(startDate!!, isPersian) else (if (isPersian) "از تاریخ" else "From Date"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (endDate != null) DateUtils.formatTimestamp(endDate!!, isPersian) else (if (isPersian) "تا تاریخ" else "To Date"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(if (isPersian) "ترتیب نمایش:" else "Sort By:", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SortOrder.values().forEach { sort ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSort = sort }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selectedSort == sort,
                            onClick = { selectedSort = sort }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPersian) sort.titleFa else sort.titleEn)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
                Button(
                    onClick = { onApply(selectedTime, selectedType, selectedSort, startDate, endDate) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPersian) "اعمال فیلتر" else "Apply")
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text(if (isPersian) "تایید" else "OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text(if (isPersian) "تایید" else "OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formattedText = StringBuilder()
        val length = originalText.length

        for (i in 0 until length) {
            formattedText.append(originalText[i])
            if ((length - 1 - i) % 3 == 0 && i != length - 1) {
                formattedText.append(",")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val commas = (offset - 1) / 3
                return (offset + commas).coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val commas = (offset - 1) / 4
                return (offset - commas).coerceAtMost(originalText.length)
            }
        }

        return TransformedText(
            androidx.compose.ui.text.AnnotatedString(formattedText.toString()),
            offsetMapping
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionBottomSheet(
    transaction: TransactionEntity,
    isPersian: Boolean,
    currencyUnit: String,
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(transaction.title) }

    val initialAmount = remember(transaction.amount, currencyUnit) {
        if (currencyUnit == "IRR") (transaction.amount * 10).toLong().toString()
        else transaction.amount.toLong().toString()
    }
    var rawAmountText by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var category by remember { mutableStateOf(transaction.category) }
    var type by remember { mutableStateOf(transaction.type) } // "EXPENSE" or "INCOME"

    val maxTitleLength = 40
    val maxDigitsLength = 12
    val maxNoteLength = 120

    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    val currentCategories = remember(type, expenseCategories, incomeCategories) {
        val rawList = if (type == "EXPENSE") expenseCategories else incomeCategories
        rawList.filter { cat ->
            cat.title != "دسته‌بندی نشده" &&
                    cat.title != "دسته بندی نشده" &&
                    !cat.title.equals("Uncategorized", ignoreCase = true)
        }
    }

    val selectedCategoryObj = currentCategories.find { it.title == category }

    LaunchedEffect(type, currentCategories) {
        if (currentCategories.isNotEmpty() && currentCategories.none { it.title == category }) {
            category = currentCategories.first().title
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isPersian) "ویرایش تراکنش" else "Edit Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // انتخاب نوع تراکنش (هزینه / درآمد)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { type = "EXPENSE" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == "EXPENSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPersian) "هزینه" else "Expense", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { type = "INCOME" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPersian) "درآمد" else "Income", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // فیلد عنوان
            OutlinedTextField(
                value = title,
                onValueChange = { input ->
                    if (input.length <= maxTitleLength) {
                        title = input
                        if (titleError) titleError = false
                    }
                },
                label = { Text(if (isPersian) "عنوان تراکنش" else "Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                isError = titleError,
                trailingIcon = {
                    VoiceInputButton(
                        onResult = { spoken ->
                            title = spoken.take(maxTitleLength)
                            if (titleError) titleError = false
                        },
                        language = if (isPersian) "fa-IR" else "en-US"
                    )
                },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (titleError) (if (isPersian) "عنوان تراکنش نمی‌تواند خالی باشد" else "Title cannot be empty") else "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${title.length}/$maxTitleLength",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // فیلد مبلغ
            val amountLabel = if (isPersian) {
                if (currencyUnit == "IRR") "مبلغ (ریال)" else "مبلغ (تومان)"
            } else {
                if (currencyUnit == "IRR") "Amount (Rial)" else "Amount (Toman)"
            }

            OutlinedTextField(
                value = rawAmountText,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length <= maxDigitsLength) {
                        rawAmountText = digitsOnly
                        if (amountError) amountError = false
                    }
                },
                label = { Text(amountLabel) },
                singleLine = true,
                visualTransformation = ThousandsSeparatorTransformation(), // توجه: از همون کلاسی که در AddScreen گذاشتید استفاده کنید
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = amountError,
                supportingText = {
                    if (amountError) {
                        Text(
                            text = if (isPersian) "مبلغ معتبر (بزرگتر از ۰) وارد کنید" else "Enter a valid amount (> 0)",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // دراپ‌داون دسته‌بندی
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = StringMapper.getCategoryName(category, isPersian),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isPersian) "انتخاب دسته‌بندی" else "Select Category") },
                    leadingIcon = {
                        if (selectedCategoryObj != null) {
                            Text(
                                text = selectedCategoryObj.iconEmoji,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isError = categoryError,
                    supportingText = {
                        if (categoryError) {
                            Text(
                                text = if (isPersian) "لطفاً یک دسته‌بندی انتخاب کنید" else "Please select a category",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false },
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (currentCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isPersian) "دسته‌بندی یافت نشد" else "No categories found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { isCategoryDropdownExpanded = false }
                        )
                    } else {
                        currentCategories.forEach { catItem ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(text = catItem.iconEmoji, fontSize = 22.sp)
                                        Text(
                                            text = StringMapper.getCategoryName(catItem.title, isPersian),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    category = catItem.title
                                    isCategoryDropdownExpanded = false
                                    categoryError = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // فیلد یادداشت
            OutlinedTextField(
                value = note,
                onValueChange = { input ->
                    if (input.length <= maxNoteLength) {
                        note = input
                    }
                },
                label = { Text(if (isPersian) "یادداشت (اختیاری)" else "Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                trailingIcon = {
                    VoiceInputButton(
                        onResult = { spoken ->
                            note = spoken.take(maxNoteLength)
                        },
                        language = if (isPersian) "fa-IR" else "en-US"
                    )
                },
                supportingText = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "${note.length}/$maxNoteLength",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // دکمه تایید اسکرولی (Swipe To Confirm)
            SwipeToConfirmButton(
                text = if (isPersian) "برای ذخیره بکشید" else "Swipe to Save",
                isPersian = isPersian,
                resetTrigger = title.isEmpty() && rawAmountText.isEmpty(),
                onConfirm = {
                    val parsedAmount = rawAmountText.toDoubleOrNull() ?: 0.0
                    val isTitleValid = title.isNotBlank()
                    val isAmountValid = parsedAmount > 0
                    val isCategoryValid = category.isNotBlank()

                    titleError = !isTitleValid
                    amountError = !isAmountValid
                    categoryError = !isCategoryValid

                    if (isTitleValid && isAmountValid && isCategoryValid) {
                        val finalAmountInToman = if (currencyUnit == "IRR") parsedAmount / 10.0 else parsedAmount

                        val updatedTransaction = transaction.copy(
                            title = title.trim(),
                            amount = finalAmountInToman,
                            category = category,
                            type = type,
                            note = note.trim()
                        )

                        showSuccessAnimation = true
                        scope.launch {
                            delay(4000) // زمان نمایش دیالوگ موفقیت
                            showSuccessAnimation = false
                            onConfirm(updatedTransaction)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // اورلی انیمیشن موفقیت
    if (showSuccessAnimation) {
        Dialog(onDismissRequest = { }) {
            val successShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, successShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), successShape)
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.success_anim)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = 1
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(130.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPersian) "انجام شد!" else "Done!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) "تغییرات با موفقیت ذخیره شد" else "Changes saved successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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
        TimeFilter.values().forEach { filter ->
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha * 0.15f))
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
    transaction: TransactionEntity,
    isPersian: Boolean,
    currencyUnit: String,
    numberFormatter: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rowShape = RoundedCornerShape(20.dp)
    val isExpense = transaction.type == "EXPENSE"
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

    val formattedAmount = remember(transaction.amount, isPersian, currencyUnit) {
        val calculatedAmount = if (currencyUnit == "IRR") transaction.amount.toLong() * 10 else transaction.amount.toLong()
        if (isPersian && isExpense) {
            "-${numberFormatter.format(calculatedAmount)}"
        } else {
            numberFormatter.format(if (isExpense) -calculatedAmount else calculatedAmount)
        }
    }

    val currencySuffix = if (isPersian) {
        if (currencyUnit == "IRR") "ریال" else "تومان"
    } else {
        if (currencyUnit == "IRR") "IRR" else "T"
    }

    val formattedTime = remember(transaction.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    val categoryTitle = StringMapper.getCategoryName(transaction.category, isPersian)

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
                .graphicsLayer { translationX = animatedOffset.toPx() }
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
                    text = transaction.title.ifEmpty { categoryTitle },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryTitle • $formattedTime",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Text(
                text = "$formattedAmount $currencySuffix",
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
    isFilterActive: Boolean,
    onFilterClick: () -> Unit,
    showFilterButton: Boolean = true,
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

            if (showFilterButton) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isFilterActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            smallShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                        .clip(smallShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = if (isPersian) "فیلتر" else "Filter",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getRelativeDateHeader(timestamp: Long, isPersian: Boolean): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    // Calendar-day difference, not raw millis — otherwise a 30-hour gap that
    // crosses midnight twice gets miscounted relative to "Today"/"Yesterday".
    val diffDays = ChronoUnit.DAYS.between(date, today)

    val isSameMonth = if (isPersian) {
        // Persian months don't line up with Gregorian months, so this has to
        // go through the app's own Jalali conversion, not Calendar.MONTH.
        val (todayYear, todayMonth, _) = DateUtils.toJalali(today)
        val (dateYear, dateMonth, _) = DateUtils.toJalali(date)
        todayYear == dateYear && todayMonth == dateMonth
    } else {
        today.year == date.year && today.monthValue == date.monthValue
    }

    return when {
        diffDays == 0L -> {
            if (isPersian) "امروز" else "Today"
        }

        diffDays == 1L -> {
            if (isPersian) "دیروز" else "Yesterday"
        }

        diffDays in 2..6 -> {
            if (isPersian) "$diffDays روز پیش" else "$diffDays days ago"
        }

        isSameMonth -> {
            if (isPersian) "این ماه" else "This month"
        }

        else -> {
            DateUtils.formatTimestamp(timestamp, isPersian)
        }
    }
}