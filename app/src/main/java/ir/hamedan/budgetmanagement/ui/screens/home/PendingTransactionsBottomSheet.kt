package ir.hamedan.budgetmanagement.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.window.DialogProperties
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.utils.StringMapper
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
    onConfirmFinal: (pending: PendingTransactionEntity, title: String, amount: Double, category: String, isExpense: Boolean, note: String) -> Unit,
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
        PendingConfirmDialog(
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
private fun PendingConfirmDialog(
    pending: PendingTransactionEntity,
    categories: List<CategoryEntity>,
    isPersian: Boolean,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onCategoriesClick: () -> Unit = {},
    onConfirmFinal: (title: String, amount: Double, category: String, isExpense: Boolean, note: String) -> Unit
) {
    val initialAmount = if (currencyUnit == "IRR") (pending.amount * 10).toLong().toString() else pending.amount.toLong().toString()

    var title by remember { mutableStateOf(pending.suggestedTitle.ifEmpty { if (isPersian) "تراکنش پیامکی" else "SMS Transaction" }) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var noteText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(pending.type == "EXPENSE") }
    var selectedCategoryKey by remember { mutableStateOf(pending.suggestedCategory) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // وضعیت کلیک روی دکمه ثبت (برای کنترل زمان نمایش ارورها)
    var isSubmitted by remember { mutableStateOf(false) }

    // جستجوی دسته‌بندی
    val selectedCategoryObj = categories.find { it.title.equals(selectedCategoryKey, ignoreCase = true) }

    val maxDigitsLength = 12

    // بررسی صحت فیلدها
    val isTitleValid = title.trim().isNotEmpty()
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = amountText.isNotEmpty() && parsedAmount > 0
    val isCategoryValid = selectedCategoryKey.isNotEmpty()

    // نمایش خطاها فقط پس از تلاش کاربر برای ثبت فرم
    val showTitleError = isSubmitted && !isTitleValid
    val showAmountError = isSubmitted && !isAmountValid
    val showCategoryError = isSubmitted && !isCategoryValid

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // هدر
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "تکمیل و ثبت تراکنش" else "Confirm Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // تغییر نوع تراکنش (هزینه / درآمد)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpense) MaterialTheme.colorScheme.error.copy(alpha = 0.85f) else Color.Transparent)
                            .clickable {
                                isExpense = true
                                selectedCategoryKey = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPersian) "برداشتی (هزینه)" else "Expense",
                            color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isExpense) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Transparent)
                            .clickable {
                                isExpense = false
                                selectedCategoryKey = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPersian) "واریزی (درآمد)" else "Income",
                            color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // عنوان
                OutlinedTextField(
                    value = title,
                    onValueChange = { input ->
                        if (input.length <= 40) {
                            title = input
                        }
                    },
                    label = { Text(if (isPersian) "عنوان تراکنش *" else "Title *") },
                    isError = showTitleError,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (showTitleError) {
                                Text(
                                    text = if (isPersian) "عنوان نمی‌تواند خالی باشد" else "Title is required",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text(
                                text = "${title.length}/40",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // مبلغ
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= maxDigitsLength) {
                            amountText = digitsOnly
                        }
                    },
                    label = {
                        val unit = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else currencyUnit
                        Text("${if (isPersian) "مبلغ *" else "Amount *"} ($unit)")
                    },
                    isError = showAmountError,
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = if (showAmountError) {
                        {
                            Text(
                                text = if (isPersian) "مبلغ معتبری وارد کنید" else "Enter a valid amount",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else null
                )

                // منوی کشویی انتخاب دسته‌بندی
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                ) {
                    val categoryDisplayText = if (selectedCategoryKey.isEmpty()) {
                        if (isPersian) "لطفاً دسته‌بندی را انتخاب کنید" else "Select a category"
                    } else {
                        StringMapper.getCategoryName(selectedCategoryKey, isPersian)
                    }

                    OutlinedTextField(
                        value = categoryDisplayText,
                        onValueChange = {},
                        readOnly = true,
                        isError = showCategoryError,
                        label = { Text(if (isPersian) "دسته‌بندی *" else "Category *") },
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        supportingText = if (showCategoryError) {
                            {
                                Text(
                                    text = if (isPersian) "انتخاب دسته‌بندی الزامی است" else "Category is required",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else null
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false },
                        modifier = Modifier
                            .heightIn(max = 320.dp)
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
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(text = cat.iconEmoji, fontSize = 22.sp)
                                            Text(
                                                text = StringMapper.getCategoryName(cat.title, isPersian),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryKey = cat.title
                                        isCategoryDropdownExpanded = false
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

                // یادداشت (اختیاری - بدون اعتبارسنجی)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { input ->
                        if (input.length <= 120) {
                            noteText = input
                        }
                    },
                    label = { Text(if (isPersian) "یادداشت (اختیاری)" else "Note (Optional)") },
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "${noteText.length}/120",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // دکمه‌ها
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }

                    Button(
                        onClick = {
                            isSubmitted = true // کلیک کاربر روی تایید ثبت شد

                            // اگر همه فیلدها معتبر باشند ثبت نهایی انجام می‌شود
                            if (isTitleValid && isAmountValid && isCategoryValid) {
                                val finalAmount = if (currencyUnit == "IRR") parsedAmount / 10 else parsedAmount
                                onConfirmFinal(title.trim(), finalAmount, selectedCategoryKey, isExpense, noteText.trim())
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPersian) "تایید نهایی" else "Save")
                    }
                }
            }
        }
    }
}