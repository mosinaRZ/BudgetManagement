package ir.hamedan.budgetmanagement.ui.screens.add

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AddOptionItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val icon: ImageVector,
    val descriptionFa: String,
    val descriptionEn: String,
    val route: String
)

// -----------------------------------------------------------------------------
// VisualTransformation دقیقاً مشابه فایل BudgetLimitScreen
// -----------------------------------------------------------------------------
class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = StringBuilder()
        for (i in originalText.indices) {
            formattedText.append(originalText[i])
            if ((originalText.length - 1 - i) % 3 == 0 && i != originalText.length - 1) {
                formattedText.append(",")
            }
        }

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val commasBefore = (offset - 1) / 3
                return (offset + commasBefore).coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val commasBefore = offset / 4
                return (offset - commasBefore).coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText.toString()), numberOffsetTranslator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    highlightId: String? = null,
    onBackClick: () -> Unit = {},
    onGoalsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onLimitsClick: () -> Unit = {},
    onDueClick: () -> Unit = {},
    viewModel: AddViewModel = viewModel(factory = AddViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val scope = rememberCoroutineScope()

    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()

    var isAnimationActive by remember { mutableStateOf(false) }
    var consumedHighlightId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(highlightId) {
        if (highlightId != null && highlightId != consumedHighlightId) {
            consumedHighlightId = highlightId
            isAnimationActive = true
            delay(4700)
            isAnimationActive = false
        }
    }

    var showTransactionBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // فیلدهای فرم
    var transactionTitle by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") } // فقط ارقام ذخیره می‌شوند
    var selectedCategoryKey by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var transactionNote by remember { mutableStateOf("") }

    val maxDigitsLength = 12 // حداکثر ۱۲ رقم برای مبلغ

    // یافتن دسته‌بندی انتخاب‌شده برای استخراج ایموجی
    val selectedCategoryObj = categoriesList.find { it.title == selectedCategoryKey }

    // وضعیت خطای اعتبارسنجی فیلدها
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }

    // وضعیت انیمیشن موفقیت
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // وضعیت کنترل منوی کشویی دسته‌بندی
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val addOptions = remember {
        listOf(
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
                "limit", "مدیریت محدودیت‌های مالی", "Manage Budget Limits",
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

        // ۱. گرید گزینه‌ها
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 160.dp,
                bottom = 110.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = addOptions,
                span = { option ->
                    if (option.id == "transaction") GridItemSpan(2) else GridItemSpan(1)
                }
            ) { option ->
                val optionShape = RoundedCornerShape(24.dp)
                val isWide = option.id == "transaction"

                val isTargetHighlight = (option.id == highlightId ||
                        (option.id == "transaction" && (highlightId == "buy" || highlightId == "sell"))) && isAnimationActive

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
                        .then(if (isWide) Modifier.height(140.dp) else Modifier.aspectRatio(1f))
                        .background(
                            if (isTargetHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            optionShape
                        )
                        .border(borderWidth, borderColor, optionShape)
                        .clip(optionShape)
                        .clickable {
                            when (option.id) {
                                "transaction" -> showTransactionBottomSheet = true
                                "piggy" -> onGoalsClick()
                                "category" -> onCategoriesClick()
                                "limit" -> onLimitsClick()
                                "due" -> onDueClick()
                            }
                        }
                        .padding(16.dp)
                ) {
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

        // ۲. هدر
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

        // ۳. دکمه انصراف
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

        // ۴. باتم‌شیت ثبت تراکنش
        if (showTransactionBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTransactionBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        text = if (isPersian) "ثبت تراکنش جدید" else "New Transaction",
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
                            onClick = {
                                isExpense = true
                                selectedCategoryKey = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isExpense) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isPersian) "هزینه" else "Expense", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isExpense = false
                                selectedCategoryKey = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isExpense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isExpense) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isPersian) "درآمد" else "Income", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // فیلد عنوان (محدود به ۴۰ کاراکتر)
                    OutlinedTextField(
                        value = transactionTitle,
                        onValueChange = { input ->
                            if (input.length <= 40) {
                                transactionTitle = input
                                if (titleError) titleError = false
                            }
                        },
                        label = { Text(if (isPersian) "عنوان تراکنش" else "Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        isError = titleError,
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
                                    text = "${transactionTitle.length}/40",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // فیلد مبلغ همراه با VisualTransformation تفکیک ۳ رقمی
                    val amountLabel = if (isPersian) {
                        if (currencyUnit == "IRR") "مبلغ (ریال)" else "مبلغ (تومان)"
                    } else {
                        if (currencyUnit == "IRR") "Amount (Rial)" else "Amount (Toman)"
                    }

                    OutlinedTextField(
                        value = transactionAmount,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= maxDigitsLength) {
                                transactionAmount = digitsOnly
                                if (amountError) amountError = false
                            }
                        },
                        label = { Text(amountLabel) },
                        singleLine = true,
                        visualTransformation = ThousandsSeparatorTransformation(),
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

                    // منوی کشویی انتخاب دسته‌بندی
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = StringMapper.getCategoryName(selectedCategoryKey, isPersian),
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
                            val filteredCategories = categoriesList.filter { it.isExpense == isExpense }

                            if (filteredCategories.isEmpty()) {
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
                                filteredCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(text = category.iconEmoji, fontSize = 22.sp)
                                                Text(
                                                    text = StringMapper.getCategoryName(category.title, isPersian),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCategoryKey = category.title
                                            isCategoryDropdownExpanded = false
                                            categoryError = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isPersian) "مدیریت دسته‌بندی‌ها..." else "Manage Categories...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                onClick = {
                                    isCategoryDropdownExpanded = false
                                    showTransactionBottomSheet = false
                                    onCategoriesClick()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // فیلد یادداشت (محدود به ۱۲۰ کاراکتر)
                    OutlinedTextField(
                        value = transactionNote,
                        onValueChange = { input ->
                            if (input.length <= 120) {
                                transactionNote = input
                            }
                        },
                        label = { Text(if (isPersian) "یادداشت (اختیاری)" else "Note (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 3,
                        supportingText = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = "${transactionNote.length}/120",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // دکمه ذخیره
                    Button(
                        onClick = {
                            val parsedAmount = transactionAmount.toDoubleOrNull() ?: 0.0
                            val amount = if (currencyUnit == "IRR") parsedAmount / 10.0 else parsedAmount

                            titleError = transactionTitle.isBlank()
                            amountError = parsedAmount <= 0.0
                            categoryError = selectedCategoryKey.isBlank()

                            val isFormValid = !titleError && !amountError && !categoryError

                            if (isFormValid) {
                                viewModel.addTransaction(
                                    title = transactionTitle.trim(),
                                    amount = amount,
                                    categoryKey = selectedCategoryKey,
                                    isExpense = isExpense,
                                    note = transactionNote.trim()
                                )

                                showSuccessAnimation = true
                                scope.launch {
                                    delay(4000)
                                    showSuccessAnimation = false
                                    sheetState.hide()
                                    showTransactionBottomSheet = false

                                    // ریست فیلدها
                                    transactionTitle = ""
                                    transactionAmount = ""
                                    selectedCategoryKey = ""
                                    transactionNote = ""
                                    isExpense = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isPersian) "ذخیره تراکنش" else "Save Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ۵. اورلی انیمیشن موفقیت
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            text = if (isPersian) "تراکنش با موفقیت ثبت شد" else "Transaction added successfully",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}