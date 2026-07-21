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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class AddOptionItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val icon: ImageVector,
    val descriptionFa: String,
    val descriptionEn: String,
    val route: String
)

// تابع کمکی جهت فرمت‌دهی ۳ رقم ۳ رقم
fun formatAmountWithCommas(rawInput: String): String {
    val cleanString = rawInput.replace(",", "").replace(" ", "")
    if (cleanString.isEmpty()) return ""

    val parts = cleanString.split(".")
    val numberPart = parts[0].toLongOrNull() ?: return ""

    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
    val formattedNumber = formatter.format(numberPart)

    return if (parts.size > 1) {
        "$formattedNumber.${parts[1]}"
    } else {
        formattedNumber
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

    val categoriesList by viewModel.categories.collectAsState()

    var isAnimationActive by remember { mutableStateOf(highlightId != null) }

    LaunchedEffect(highlightId) {
        if (highlightId != null) {
            delay(4700)
            isAnimationActive = false
        }
    }

    var showTransactionBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // فیلدهای فرم
    var transactionTitle by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") }
    var selectedCategoryKey by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var transactionNote by remember { mutableStateOf("") }

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
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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

                    // نوع تراکنش (با تغییر نوع، انتخاب دسته‌بندی قبلی ریست می‌شود)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isPersian) "هزینه" else "Expense")
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isPersian) "درآمد" else "Income")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // عنوان
                    OutlinedTextField(
                        value = transactionTitle,
                        onValueChange = {
                            transactionTitle = it
                            if (titleError) titleError = false
                        },
                        label = { Text(if (isPersian) "عنوان تراکنش" else "Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = titleError,
                        supportingText = {
                            if (titleError) {
                                Text(
                                    text = if (isPersian) "عنوان تراکنش نمی‌تواند خالی باشد" else "Title cannot be empty",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // مبلغ با جداسازی ۳ رقم ۳ رقم
                    OutlinedTextField(
                        value = transactionAmount,
                        onValueChange = { input ->
                            var filtered = input.filter { it.isDigit() || it == '.' }
                            val dotCount = filtered.count { it == '.' }
                            if (dotCount > 1) {
                                val firstDotIndex = filtered.indexOf('.')
                                filtered = filtered.substring(0, firstDotIndex + 1) +
                                        filtered.substring(firstDotIndex + 1).replace(".", "")
                            }
                            transactionAmount = formatAmountWithCommas(filtered)
                            if (amountError) amountError = false
                        },
                        label = { Text(if (isPersian) "مبلغ" else "Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = amountError,
                        supportingText = {
                            if (amountError) {
                                Text(
                                    text = if (isPersian) "مبلغ معتبر وارد کنید" else "Enter a valid amount",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // منوی کشویی انتخاب دسته‌بندی همراه با ایموجی
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
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = categoryError,
                            supportingText = {
                                if (categoryError) {
                                    Text(
                                        text = if (isPersian) "یک دسته‌بندی انتخاب کنید" else "Please select a category",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            // فیلتر دسته‌بندی‌ها متناسب با هزینه‌ای یا درآمدی بودن
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

                            // 👇 این بخش CTA به انتهای لیست اضافه می‌شود 👇
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Manage Categories",
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
                                    showTransactionBottomSheet = false // بستن باتم‌شیت فعلی
                                    onCategoriesClick() // هدایت به صفحه مدیریت دسته‌بندی‌ها
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // فیلد یادداشت
                    OutlinedTextField(
                        value = transactionNote,
                        onValueChange = { transactionNote = it },
                        label = { Text(if (isPersian) "یادداشت (اختیاری)" else "Note (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // دکمه ذخیره
                    Button(
                        onClick = {
                            val cleanAmountString = transactionAmount.replace(",", "")
                            val amount = cleanAmountString.toDoubleOrNull() ?: 0.0

                            titleError = transactionTitle.isBlank()
                            amountError = amount <= 0.0
                            categoryError = selectedCategoryKey.isBlank()

                            val isFormValid = !titleError && !amountError && !categoryError

                            if (isFormValid) {
                                viewModel.addTransaction(
                                    title = transactionTitle,
                                    amount = amount,
                                    categoryKey = selectedCategoryKey,
                                    isExpense = isExpense,
                                    note = transactionNote
                                )

                                showSuccessAnimation = true
                                scope.launch {
                                    delay(4500)
                                    showSuccessAnimation = false
                                    sheetState.hide()
                                    showTransactionBottomSheet = false

                                    transactionTitle = ""
                                    transactionAmount = ""
                                    selectedCategoryKey = ""
                                    transactionNote = ""
                                    isExpense = true

                                    onBackClick()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
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