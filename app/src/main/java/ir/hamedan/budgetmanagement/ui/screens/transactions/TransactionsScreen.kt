package ir.hamedan.budgetmanagement.ui.screens.transactions

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel = viewModel(factory = TransactionViewModel.Factory(LocalContext.current))
) {
    val isPersian = isPersianLocale()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardVisible = imeBottom > 0

    val showScrollToTopButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val transactionsList by viewModel.filteredTransactions.collectAsState()

    // دریافت واحد پول (IRT یا IRR) از ViewModel
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

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        if (transactionsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPersian) "تراکنشی یافت نشد" else "No transactions found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
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
                onFilterClick = { showFilterSheet = true }
            )

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
                    onFilterSelected = { viewModel.setQuickTimeFilter(it) }
                )
            }
        }

        // سرچ و دکمه بازگشت به بالا
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
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), searchShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), searchShape),
                placeholder = {
                    Text(
                        text = if (isPersian) "جستجو در لیست فیلترشده..." else "Search in filtered list...",
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
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text(if (isPersian) "حذف تراکنش" else "Delete Transaction") },
                text = { Text(if (isPersian) "آیا از حذف این تراکنش اطمینان دارید؟" else "Are you sure you want to delete this transaction?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(tx.id)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isPersian) "حذف" else "Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { transactionToDelete = null }) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }
                }
            )
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

    val dateFormater = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

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

            // ۱. نوع تراکنش
            Text(if (isPersian) "نوع تراکنش:" else "Type:", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionTypeFilter.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(if (isPersian) type.titleFa else type.titleEn) }
                    )
                }
            }

            // ۲. فیلتر بازه زمانی سفارشی
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
                        text = if (startDate != null) dateFormater.format(Date(startDate!!)) else (if (isPersian) "از تاریخ" else "From Date"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (endDate != null) dateFormater.format(Date(endDate!!)) else (if (isPersian) "تا تاریخ" else "To Date"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ۳. مرتب‌سازی
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

            // دکمه‌های تایید و انصراف
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

private fun getCategoryIconVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "food", "restaurant" -> Icons.Default.Restaurant
        "transport", "car" -> Icons.Default.DirectionsCar
        "shopping", "bag" -> Icons.Default.ShoppingBag
        "bills", "receipt" -> Icons.Default.Receipt
        "salary", "money" -> Icons.Default.AttachMoney
        "entertainment", "game" -> Icons.Default.SportsEsports
        "health" -> Icons.Default.MedicalServices
        "education" -> Icons.Default.School
        else -> Icons.Default.Category
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
    var title by remember { mutableStateOf(transaction.title) }

    // تبدیل مقدار پیش‌فرض مبلغ بر اساس واحد پول انتخاب‌شده
    val initialAmount = remember(transaction.amount, currencyUnit) {
        if (currencyUnit == "IRR") (transaction.amount * 10).toLong().toString()
        else transaction.amount.toLong().toString()
    }
    var rawAmountText by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var category by remember { mutableStateOf(transaction.category) }
    var type by remember { mutableStateOf(transaction.type) }

    // وضعیت‌های خطا برای Validation
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }

    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    val currentCategories = if (type == "EXPENSE") expenseCategories else incomeCategories

    LaunchedEffect(type) {
        if (currentCategories.isNotEmpty() && currentCategories.none { it.title == category }) {
            category = currentCategories.first().title
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isPersian) "ویرایش تراکنش" else "Edit Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // ۱. نوع تراکنش
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == "EXPENSE",
                    onClick = { type = "EXPENSE" },
                    label = {
                        Text(
                            text = if (isPersian) "برداشتی (هزینه)" else "Expense",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (type == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                FilterChip(
                    selected = type == "INCOME",
                    onClick = { type = "INCOME" },
                    label = {
                        Text(
                            text = if (isPersian) "واریزی (درآمد)" else "Income",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // ۲. عنوان (اجباری)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text(if (isPersian) "عنوان *" else "Title *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(if (isPersian) "عنوان نمی‌تواند خالی باشد" else "Title is required") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ۳. مبلغ (اجباری)
            val currencyLabel = if (isPersian) {
                if (currencyUnit == "IRR") "مبلغ (ریال) *" else "مبلغ (تومان) *"
            } else {
                if (currencyUnit == "IRR") "Amount (Rial) *" else "Amount (Toman) *"
            }

            OutlinedTextField(
                value = rawAmountText,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length <= 13) {
                        rawAmountText = digitsOnly
                        amountError = false
                    }
                },
                label = { Text(currencyLabel) },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text(if (isPersian) "مبلغ معتبر وارد کنید" else "Enter a valid amount") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // ۴. یادداشت (اختیاری)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isPersian) "یادداشت (اختیاری)" else "Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // ۵. دسته‌بندی (اجباری)
            Column {
                Text(
                    text = if (isPersian) "دسته‌بندی * (${if (type == "EXPENSE") "هزینه‌ها" else "درآمدها"}):" else "Category *:",
                    fontWeight = FontWeight.SemiBold,
                    color = if (categoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (currentCategories.isEmpty()) {
                    Text(
                        text = if (isPersian) "دسته‌بندی یافت نشد" else "No categories found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        items(currentCategories, key = { it.id }) { catItem ->
                            val isSelected = category == catItem.title

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    category = catItem.title
                                    categoryError = false
                                },
                                label = { Text("${catItem.iconEmoji} ${catItem.title}") }
                            )
                        }
                    }
                }
                if (categoryError) {
                    Text(
                        text = if (isPersian) "لطفا یک دسته‌بندی انتخاب کنید" else "Please select a category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // ۶. دکمه‌های ثبت
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
                    onClick = {
                        // Validate Fields
                        val parsedAmount = rawAmountText.toDoubleOrNull()
                        val isTitleValid = title.isNotBlank()
                        val isAmountValid = parsedAmount != null && parsedAmount > 0
                        val isCategoryValid = category.isNotBlank()

                        titleError = !isTitleValid
                        amountError = !isAmountValid
                        categoryError = !isCategoryValid

                        if (isTitleValid && isAmountValid && isCategoryValid) {
                            // در صورت انتخاب ریال، مبلغ به تومان (پایه دیتابیس) تبدیل و ذخیره می‌شود
                            val finalAmountInToman = if (currencyUnit == "IRR") parsedAmount!! / 10.0 else parsedAmount!!
                            onConfirm(
                                transaction.copy(
                                    title = title.trim(),
                                    amount = finalAmountInToman,
                                    category = category,
                                    type = type,
                                    note = note
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPersian) "ذخیره تغییرات" else "Save")
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

    // محاسبه مبلغ و واحد پول بر اساس ریال/تومان
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

                // نمایش یادداشت در صورت وجود
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

private fun getRelativeDateHeader(timestamp: Long, isPersian: Boolean): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    val diffDays = ((now.timeInMillis - time.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

    return when {
        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR) -> {
            if (isPersian) "امروز" else "Today"
        }

        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1 -> {
            if (isPersian) "دیروز" else "Yesterday"
        }

        diffDays in 2..6 -> {
            if (isPersian) "$diffDays روز پیش" else "$diffDays days ago"
        }

        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == time.get(Calendar.WEEK_OF_YEAR) -> {
            if (isPersian) "این هفته" else "This week"
        }

        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == time.get(Calendar.MONTH) -> {
            if (isPersian) "این ماه" else "This month"
        }

        else -> {
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}