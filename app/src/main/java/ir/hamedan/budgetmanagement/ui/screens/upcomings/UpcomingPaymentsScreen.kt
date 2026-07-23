package ir.hamedan.budgetmanagement.ui.screens.payments

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.PaymentDateUtils
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
fun UpcomingPaymentsScreen(
    onBackClick: () -> Unit = {},
    viewModel: UpcomingPaymentViewModel = viewModel(factory = UpcomingPaymentViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    val paymentsList by viewModel.payments.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<UpcomingPaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<UpcomingPaymentEntity?>(null) }

    val pendingPayments = remember(paymentsList) { paymentsList.filter { !it.isPaid } }
    val paidPayments = remember(paymentsList) { paymentsList.filter { it.isPaid } }

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
            // Top Bar
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
                    text = if (isPersian) "پرداخت‌های پیش‌رو" else "Upcoming Payments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(44.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (paymentsList.isEmpty()) {
                    item {
                        val emptyShape = RoundedCornerShape(24.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), emptyShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), emptyShape)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📅", fontSize = 56.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isPersian) "پرداختی ثبت نشده است" else "No Payments Scheduled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isPersian) "اقساط، قبوض و هزینه‌های دوره‌ای خود را اضافه کنید تا موعد آن‌ها را فراموش نکنید."
                                    else "Add your bills and upcoming installments to stay on track.",
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
                                        text = if (isPersian) "افزودن پرداخت جدید" else "Add New Payment",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (pendingPayments.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isPersian) "در انتظار پرداخت" else "Pending Payments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(pendingPayments, key = { it.id }) { payment ->
                            PaymentItemCard(
                                payment = payment,
                                isPersian = isPersian,
                                currencyUnit = currencyUnit,
                                numberFormatter = numberFormatter,
                                onTogglePaid = { viewModel.togglePaymentStatus(payment) },
                                onEditClick = { paymentToEdit = payment },
                                onDeleteClick = { paymentToDelete = payment }
                            )
                        }
                    }

                    if (paidPayments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPersian) "پرداخت‌شده‌ها" else "Paid",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }

                        items(paidPayments, key = { it.id }) { payment ->
                            PaymentItemCard(
                                payment = payment,
                                isPersian = isPersian,
                                currencyUnit = currencyUnit,
                                numberFormatter = numberFormatter,
                                onTogglePaid = { viewModel.togglePaymentStatus(payment) },
                                onEditClick = { paymentToEdit = payment },
                                onDeleteClick = { paymentToDelete = payment }
                            )
                        }
                    }
                }
            }
        }

        if (paymentsList.isNotEmpty()) {
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
                Icon(Icons.Default.Add, contentDescription = "Add Payment", modifier = Modifier.size(28.dp))
            }
        }

        if (showAddDialog || paymentToEdit != null) {
            AddOrEditPaymentDialog(
                paymentToEdit = paymentToEdit,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                onDismiss = {
                    showAddDialog = false
                    paymentToEdit = null
                },
                onConfirm = { payment ->
                    viewModel.addOrUpdatePayment(payment)
                    showAddDialog = false
                    paymentToEdit = null
                }
            )
        }

        paymentToDelete?.let { payment ->
            AlertDialog(
                onDismissRequest = { paymentToDelete = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = if (isPersian) "حذف پرداخت" else "Delete Payment",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isPersian)
                            "آیا از حذف «${payment.title}» اطمینان دارید؟"
                        else
                            "Are you sure you want to delete '${payment.title}'?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePayment(payment.id)
                            paymentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPersian) "حذف" else "Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { paymentToDelete = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PaymentItemCard(
    payment: UpcomingPaymentEntity,
    isPersian: Boolean,
    currencyUnit: String,
    numberFormatter: NumberFormat,
    onTogglePaid: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val cardAlpha by animateFloatAsState(targetValue = if (payment.isPaid) 0.55f else 1.0f, label = "CardAlpha")

    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "Toman")
    val amountToDisplay = if (currencyUnit == "IRR") payment.amount * 10 else payment.amount
    val formattedAmount = numberFormatter.format(amountToDisplay.toLong())

    val isOverdue = !payment.isPaid && payment.dueDate < System.currentTimeMillis()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), cardShape)
            .border(
                width = 1.dp,
                color = when {
                    payment.isPaid -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                },
                shape = cardShape
            )
            .clip(cardShape)
            .clickable { onTogglePaid() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onTogglePaid,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (payment.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (payment.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = payment.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (payment.isPaid) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DateUtils.formatTimestamp(payment.dueDate, isPersian),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$formattedAmount $currencyText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (payment.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    if (!payment.isPaid) {
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditPaymentDialog(
    paymentToEdit: UpcomingPaymentEntity? = null,
    isPersian: Boolean,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (UpcomingPaymentEntity) -> Unit
) {
    val maxTitleLength = 40
    val maxDigitsLength = 12

    var title by remember { mutableStateOf(paymentToEdit?.title ?: "") }

    val initialRawDigits = remember(paymentToEdit, currencyUnit) {
        if (paymentToEdit == null) ""
        else {
            val amount = if (currencyUnit == "IRR") (paymentToEdit.amount * 10).toLong() else paymentToEdit.amount.toLong()
            amount.toString().take(maxDigitsLength)
        }
    }
    var rawAmountDigits by remember { mutableStateOf(initialRawDigits) }

    // روز سررسید (پیش‌فرض ۲۶)
    var selectedDay by remember { mutableIntStateOf(paymentToEdit?.dueDay ?: 26) }
    var expandedDayMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, dialogShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), dialogShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (paymentToEdit != null) {
                        if (isPersian) "ویرایش پرداخت" else "Edit Payment"
                    } else {
                        if (isPersian) "افزودن پرداخت پیش‌رو" else "New Upcoming Payment"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // فیلد عنوان با محدودیت ۴۰ کاراکتر
                OutlinedTextField(
                    value = title,
                    onValueChange = { input ->
                        if (input.length <= maxTitleLength) {
                            title = input
                        }
                    },
                    label = { Text(if (isPersian) "عنوان (مثلاً اجاره، قسط وام)" else "Title (e.g. Rent, Loan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "${title.length}/$maxTitleLength",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val labelCurrency = if (isPersian) (if (currencyUnit == "IRR") "مبلغ (ریال)" else "مبلغ (تومان)")
                else (if (currencyUnit == "IRR") "Amount (Rial)" else "Amount (Toman)")

                // فیلد مبلغ با محدودیت ۱۲ رقم و تفکیک ۳ رقمی
                OutlinedTextField(
                    value = rawAmountDigits,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= maxDigitsLength) {
                            rawAmountDigits = digitsOnly
                        }
                    },
                    label = { Text(labelCurrency) },
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // جایگزینی اسلایدر با ExposedDropdownMenuBox برای انتخاب دقیق روز
                ExposedDropdownMenuBox(
                    expanded = expandedDayMenu,
                    onExpandedChange = { expandedDayMenu = !expandedDayMenu }
                ) {
                    OutlinedTextField(
                        value = if (isPersian) "روز $selectedDay هر ماه" else "Day $selectedDay of month",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isPersian) "روز سررسید" else "Due Day") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDayMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDayMenu,
                        onDismissRequest = { expandedDayMenu = false },
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        (1..31).forEach { day ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isPersian) "روز $day ماه" else "Day $day",
                                        fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal,
                                        color = if (day == selectedDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedDay = day
                                    expandedDayMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }

                    val cleanNumber = rawAmountDigits.toDoubleOrNull() ?: 0.0

                    Button(
                        onClick = {
                            val finalAmountInToman = if (currencyUnit == "IRR") cleanNumber / 10.0 else cleanNumber

                            // محاسبه تاریخ میلی‌ثانیه‌ای سررسید بر اساس روز انتخابی
                            val calculatedDueDate = PaymentDateUtils.calculateNextDueDate(selectedDay)

                            val payment = paymentToEdit?.copy(
                                title = title.trim(),
                                amount = finalAmountInToman,
                                dueDay = selectedDay,
                                dueDate = calculatedDueDate
                            ) ?: UpcomingPaymentEntity(
                                title = title.trim(),
                                amount = finalAmountInToman,
                                dueDay = selectedDay,
                                dueDate = calculatedDueDate
                            )

                            onConfirm(payment)
                        },
                        enabled = title.isNotBlank() && cleanNumber > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (paymentToEdit != null) (if (isPersian) "ذخیره" else "Save") else (if (isPersian) "ایجاد" else "Create"))
                    }
                }
            }
        }
    }
}