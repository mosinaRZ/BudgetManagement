package ir.hamedan.budgetmanagement.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// -----------------------------------------------------------------------------
// VisualTransformation جهت تفکیک سه رقمی اعداد
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
fun PendingTransactionsBottomSheet(
    pendingList: List<PendingTransactionEntity>,
    categories: List<CategoryEntity>,
    isPersian: Boolean,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onConfirmFinal: (pending: PendingTransactionEntity, title: String, amount: Long, category: String, isExpense: Boolean, note: String) -> Unit,
    onIgnore: (pending: PendingTransactionEntity) -> Unit,
    onCategoriesClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    var selectedPendingForConfirm by remember { mutableStateOf<PendingTransactionEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle
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

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPersian) "تراکنش‌های در انتظار بررسی" else "Pending Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (pendingList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPersian) "${pendingList.size} تراکنش" else "${pendingList.size} Items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (pendingList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (isPersian) "تراکنش پیامکی جدیدی وجود ندارد" else "No pending transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(pendingList, key = { it.id }) { item ->
                            val itemShape = RoundedCornerShape(20.dp)
                            val isExpense = item.type == "EXPENSE"
                            val displayAmount = if (currencyUnit == "IRR") (item.amount * 10).toLong() else item.amount.toLong()
                            val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), itemShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), itemShape)
                                    .clip(itemShape)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.suggestedTitle.ifEmpty { if (isPersian) "تراکنش پیامکی" else "SMS Transaction" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "${if (isExpense) "-" else "+"}${numberFormatter.format(displayAmount)} $currencyText",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (item.rawMessage.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.rawMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onIgnore(item) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isPersian) "رد" else "Ignore", fontSize = 12.sp)
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Button(
                                        onClick = { selectedPendingForConfirm = item },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isPersian) "تایید" else "Confirm", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPendingForConfirm?.let { pending ->
        PendingConfirmBottomSheet(
            pending = pending,
            categories = categories,
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onDismiss = { selectedPendingForConfirm = null },
            onCategoriesClick = onCategoriesClick,
            onConfirmFinal = { title, amount, category, isExpense, note ->
                onConfirmFinal(pending, title, amount, category, isExpense, note)
                selectedPendingForConfirm = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingConfirmBottomSheet(
    pending: PendingTransactionEntity,
    categories: List<CategoryEntity>,
    isPersian: Boolean,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onCategoriesClick: () -> Unit = {},
    onConfirmFinal: (title: String, amount: Long, category: String, isExpense: Boolean, note: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialAmount = remember(pending.id, currencyUnit) {
        if (currencyUnit == "IRR") (pending.amount * 10).toLong().toString() else pending.amount.toLong().toString()
    }

    // فیلدهای فرم
    var transactionTitle by remember(pending.id) {
        mutableStateOf(pending.suggestedTitle.ifEmpty { if (isPersian) "تراکنش پیامکی" else "SMS Transaction" })
    }
    var transactionAmount by remember(pending.id) { mutableStateOf(initialAmount) }
    var selectedCategoryKey by remember(pending.id) { mutableStateOf(pending.suggestedCategory) }
    var isExpense by remember(pending.id) { mutableStateOf(pending.type == "EXPENSE") }
    var transactionNote by remember(pending.id) { mutableStateOf("") }

    val maxDigitsLength = 12 // حداکثر ۱۲ رقم برای مبلغ

    // یافتن دسته‌بندی انتخاب‌شده برای استخراج ایموجی
    val selectedCategoryObj = categories.find { it.title.equals(selectedCategoryKey, ignoreCase = true) }

    // وضعیت خطای اعتبارسنجی فیلدها
    var titleError by remember(pending.id) { mutableStateOf(false) }
    var amountError by remember(pending.id) { mutableStateOf(false) }
    var categoryError by remember(pending.id) { mutableStateOf(false) }

    // وضعیت کنترل منوی کشویی دسته‌بندی
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // وضعیت انیمیشن موفقیت و ریست شدن سوییپ
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var swipeResetTrigger by remember { mutableStateOf(false) }

    // اورلی انیمیشن موفقیت دقیقا مطابق سورس شما
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
                        text = if (isPersian) "تراکنش با موفقیت تایید شد" else "Transaction confirmed successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = if (isPersian) "تکمیل و ثبت تراکنش" else "Confirm Transaction",
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

            // فیلد عنوان (محدود به ۴۰ کاراکتر) + ویس
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
                trailingIcon = {
                    VoiceInputButton(
                        onResult = { spoken ->
                            transactionTitle = spoken.take(40)
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
                            text = "${transactionTitle.length}/40",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // فیلد مبلغ همراه با VisualTransformation تفکیک ۳ رقمی (بدون ویس)
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
                    val filteredCategories = categories.filter { it.isExpense == isExpense }

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
                            onDismiss()
                            onCategoriesClick()
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // فیلد یادداشت (محدود به ۱۲۰ کاراکتر) + ویس
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
                trailingIcon = {
                    VoiceInputButton(
                        onResult = { spoken ->
                            transactionNote = spoken.take(120)
                        },
                        language = if (isPersian) "fa-IR" else "en-US"
                    )
                },
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

            // دکمه تایید به‌صورت کشیدنی دقیقاً مطابق استایل سورس شما
            SwipeToConfirmButton(
                text = if (isPersian) "برای ذخیره بکشید" else "Swipe to Save",
                isPersian = isPersian,
                resetTrigger = swipeResetTrigger,
                onConfirm = {
                    val parsedAmount = transactionAmount.toLongOrNull() ?: 0L

                    val amount: Long =
                        if (currencyUnit == "IRR") {
                            parsedAmount / 10L
                        } else {
                            parsedAmount
                        }

                    titleError = transactionTitle.isBlank()
                    amountError = amount <= 0L
                    categoryError = selectedCategoryKey.isBlank()

                    val isFormValid = !titleError && !amountError && !categoryError

                    if (isFormValid) {
                        scope.launch {
                            showSuccessAnimation = true
                            delay(4000)

                            showSuccessAnimation = false

                            sheetState.hide()

                            onConfirmFinal(
                                transactionTitle.trim(),
                                amount,
                                selectedCategoryKey,
                                isExpense,
                                transactionNote.trim()
                            )
                        }
                    } else {
                        swipeResetTrigger = !swipeResetTrigger
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}