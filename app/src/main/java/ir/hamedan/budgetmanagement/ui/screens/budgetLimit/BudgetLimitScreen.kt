package ir.hamedan.budgetmanagement.ui.screens.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.util.*

// -----------------------------------------------------------------------------
// VisualTransformation برای تفکیک ۳ رقمی بدون جابه‌جا شدن Cursor
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

@Composable
fun BudgetLimitScreen(
    onBackClick: () -> Unit = {},
    viewModel: BudgetLimitViewModel = viewModel(factory = BudgetLimitViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    val limitsListState by viewModel.budgetLimitsWithSpent.collectAsState()
    val rawCategories by viewModel.expenseCategories.collectAsState()
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()

    val categories = remember(rawCategories) {
        rawCategories.filter { it.title.uppercase() != "UNCATEGORIZED" }
    }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var limitToEdit by remember { mutableStateOf<BudgetLimitUiModel?>(null) }
    var limitToDelete by remember { mutableStateOf<BudgetLimitUiModel?>(null) }

    val limitsList = limitsListState ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isPersian) "محدودیت‌های بودجه" else "Budget Limits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(44.dp))
            }

            if (limitsListState != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    if (limitsList.isEmpty()) {
                        item {
                            val emptyCardShape = RoundedCornerShape(24.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), emptyCardShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), emptyCardShape)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "⚠️", fontSize = 56.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (isPersian) "هنوز هیچ محدودیتی تعیین نکردی!" else "No Budget Limits Yet!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isPersian) "برای کنترل بهتر هزینه‌ها، برای دسته‌بندی‌های مختلف سقف تعیین کن."
                                        else "Set limits for your categories to prevent overspending.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showAddDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isPersian) "افزودن اولین سقف بودجه" else "Add First Limit",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = if (isPersian) "محدودیت‌ها" else "Limits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(limitsList, key = { it.entity.id }) { item ->
                            BudgetLimitItemCard(
                                item = item,
                                isPersian = isPersian,
                                currencyUnit = currencyUnit,
                                numberFormatter = numberFormatter,
                                onToggleActive = { isActive ->
                                    viewModel.updateLimitStatus(item.entity.id, isActive)
                                },
                                onEditClick = { limitToEdit = item },
                                onDeleteClick = { limitToDelete = item }
                            )
                        }
                    }
                }
            }
        }

        if (limitsListState != null && limitsList.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Limit", modifier = Modifier.size(28.dp))
            }
        }

        if (showAddDialog || limitToEdit != null) {
            AddOrEditLimitDialog(
                limitToEdit = limitToEdit,
                categories = categories,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                onDismiss = {
                    showAddDialog = false
                    limitToEdit = null
                },
                onConfirm = { categoryName, maxLimit, startDate, endDate ->
                    viewModel.saveBudgetLimit(categoryName, maxLimit, startDate, endDate)
                    showAddDialog = false
                    limitToEdit = null
                }
            )
        }

        // ---------------------------------------------------------------------
        // دیالوگ حذف
        // ---------------------------------------------------------------------
        limitToDelete?.let { item ->
            val mappedCategoryName = StringMapper.getCategoryName(item.entity.categoryName, isPersian)
            val dialogShape = RoundedCornerShape(28.dp)

            Dialog(onDismissRequest = { limitToDelete = null }) {
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
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
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
                            text = if (isPersian) "حذف محدودیت بودجه" else "Delete Budget Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isPersian)
                                "آیا از حذف محدودیت دسته‌بندی «$mappedCategoryName» اطمینان دارید؟"
                            else
                                "Are you sure you want to delete limit for '$mappedCategoryName'?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { limitToDelete = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPersian) "انصراف" else "Cancel")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.deleteBudgetLimit(item.entity.id)
                                    limitToDelete = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPersian) "حذف" else "Delete", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetLimitItemCard(
    item: BudgetLimitUiModel,
    isPersian: Boolean,
    currencyUnit: String,
    numberFormatter: NumberFormat,
    onToggleActive: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)

    val rawRatio = if (item.entity.maxLimit > 0) (item.currentSpent / item.entity.maxLimit).toFloat() else 0f
    val percentText = (rawRatio * 100).toInt()

    val visualProgress = rawRatio.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (item.isActive || item.isExpired) visualProgress else 0f,
        label = "LimitProgress"
    )

    val isWarning = rawRatio >= 0.8f
    val progressColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "Toman")
    val spentDisp = if (currencyUnit == "IRR") item.currentSpent * 10 else item.currentSpent
    val maxDisp = if (currencyUnit == "IRR") item.entity.maxLimit * 10 else item.entity.maxLimit

    val categoryDisplayName = StringMapper.getCategoryName(item.entity.categoryName, isPersian)

    val startDateFormatted = DateUtils.formatTimestamp(item.entity.startDate, isPersian)
    val endDateFormatted = DateUtils.formatTimestamp(item.entity.endDate, isPersian)
    val periodText = "$startDateFormatted - $endDateFormatted"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isActive) 1.0f else 0.6f)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), cardShape)
            .clip(cardShape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = item.categoryEmoji, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = categoryDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = periodText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.isActive,
                        enabled = !item.isExpired,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.scale(0.85f)
                    )

                    if (!item.isExpired) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (item.isExpired) {
                val isSuccess = item.isSuccessful
                val statusColor = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                val statusText = if (isSuccess) {
                    if (isPersian) "🟢 پایان دوره - موفقیت‌آمیز" else "🟢 Expired - Successful"
                } else {
                    if (isPersian) "🔴 پایان دوره - ناموفق (تخطی از سقف)" else "🔴 Expired - Exceeded"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${numberFormatter.format(spentDisp.toLong())} / ${numberFormatter.format(maxDisp.toLong())} $currencyText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.isExpired) (if (isPersian) "منقضی شده" else "Expired")
                    else if (item.isActive) "$percentText%"
                    else (if (isPersian) "غیرفعال" else "Inactive"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isActive || item.isExpired) progressColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (item.isActive || item.isExpired) progressColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                trackColor = progressColor.copy(alpha = 0.12f)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// دیالوگ افزودن/ویرایش با اعمال محدودیت ۱۲ رقم
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditLimitDialog(
    limitToEdit: BudgetLimitUiModel? = null,
    categories: List<CategoryEntity>,
    isPersian: Boolean,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (categoryName: String, maxLimit: Double, startDate: Long, endDate: Long) -> Unit
) {
    val maxDigitsLength = 12 // حداکثر ۱۲ رقم برای مبلغ

    var selectedCategoryKey by remember {
        mutableStateOf(limitToEdit?.entity?.categoryName ?: categories.firstOrNull()?.title ?: "")
    }
    var expanded by remember { mutableStateOf(false) }

    val initialRawDigits = remember(limitToEdit, currencyUnit) {
        if (limitToEdit == null) ""
        else {
            val amount = if (currencyUnit == "IRR") (limitToEdit.entity.maxLimit * 10).toLong() else limitToEdit.entity.maxLimit.toLong()
            amount.toString().take(maxDigitsLength)
        }
    }
    var rawAmountDigits by remember { mutableStateOf(initialRawDigits) }

    var startDateMillis by remember { mutableStateOf(limitToEdit?.entity?.startDate ?: System.currentTimeMillis()) }
    var endDateMillis by remember { mutableStateOf(limitToEdit?.entity?.endDate ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { startDateMillis = it }
                    showStartDatePicker = false
                }) { Text(if (isPersian) "تایید" else "OK") }
            }
        ) { DatePicker(state = startDatePickerState) }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { endDateMillis = it }
                    showEndDatePicker = false
                }) { Text(if (isPersian) "تایید" else "OK") }
            }
        ) { DatePicker(state = endDatePickerState) }
    }

    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(28.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
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
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (limitToEdit != null) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = if (limitToEdit != null) {
                        if (isPersian) "ویرایش سقف بودجه" else "Edit Budget Limit"
                    } else {
                        if (isPersian) "تعریف سقف جدید" else "Set New Budget Limit"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = StringMapper.getCategoryName(selectedCategoryKey, isPersian),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isPersian) "دسته‌بندی" else "Category") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.iconEmoji} ${StringMapper.getCategoryName(category.title, isPersian)}") },
                                onClick = {
                                    selectedCategoryKey = category.title
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                val labelCurrency = if (isPersian) (if (currencyUnit == "IRR") "سقف مجاز (ریال)" else "سقف مجاز (تومان)")
                else (if (currencyUnit == "IRR") "Max Limit (Rial)" else "Max Limit (Toman)")

                // فیلد ورودی مبلغ همراه با شرط محدودیت ارقام
                OutlinedTextField(
                    value = rawAmountDigits,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= maxDigitsLength) {
                            rawAmountDigits = digitsOnly
                        }
                    },
                    label = { Text(labelCurrency) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = if (isPersian) "تاریخ شروع" else "Start Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = DateUtils.formatTimestamp(startDateMillis, isPersian),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = if (isPersian) "تاریخ پایان" else "End Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = DateUtils.formatTimestamp(endDateMillis, isPersian),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPersian) "انصراف" else "Cancel")
                        }
                    }

                    val cleanNumber = rawAmountDigits.toDoubleOrNull() ?: 0.0

                    Button(
                        onClick = {
                            val finalMaxInToman = if (currencyUnit == "IRR") cleanNumber / 10.0 else cleanNumber
                            onConfirm(selectedCategoryKey, finalMaxInToman, startDateMillis, endDateMillis)
                        },
                        enabled = selectedCategoryKey.isNotBlank() && cleanNumber > 0 && endDateMillis > startDateMillis,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (limitToEdit != null) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (limitToEdit != null) (if (isPersian) "ذخیره" else "Save") else (if (isPersian) "ایجاد" else "Create"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}