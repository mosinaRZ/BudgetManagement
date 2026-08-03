package ir.hamedan.budgetmanagement.ui.screens.debtCredit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.add.ThousandsSeparatorTransformation
import ir.hamedan.budgetmanagement.ui.screens.goals.AmountActionDialog
import ir.hamedan.budgetmanagement.ui.viewmodels.DebtCreditViewModel
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtCreditScreen(
    onBackClick: () -> Unit = {},
    viewModel: DebtCreditViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val debtCreditList by viewModel.debtCreditList.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // گوش دادن به خطای عدم وجود بالانس کافی
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var itemForDeposit by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var itemForWithdraw by remember { mutableStateOf<DebtCreditEntity?>(null) }

    // داده‌های موقت ذخیره‌سازی جهت نمایش دیالوگ بالانس
    var pendingSaveData by remember { mutableStateOf<PendingSaveData?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // هدر صفحه
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
                    text = if (isPersian) "بدهی‌ها و بستانکاری‌ها" else "Debts & Credits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(44.dp))
            }

            if (debtCreditList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val emptyCardShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                emptyCardShape
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                emptyCardShape
                            )
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🤝", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isPersian) "هیچ بدهی یا طلبی ثبت نشده!" else "No Debts or Credits Yet!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPersian) "برای تسویه‌حساب‌های بهتر و پیگیری اقساط، موارد جدید اضافه کنید."
                                else "Start tracking your debts and credits by adding a new record.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    selectedItemForEdit = null
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPersian) "افزودن مورد جدید" else "Add New Record",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(debtCreditList, key = { it.id }) { item ->
                        DebtCreditItemCard(
                            item = item,
                            isPersian = isPersian,
                            onDepositClick = { itemForDeposit = item },
                            onWithdrawClick = { itemForWithdraw = item },
                            onEditClick = {
                                selectedItemForEdit = item
                                showAddDialog = true
                            },
                            onDeleteClick = { itemToDelete = item },
                            onToggleSettled = { viewModel.toggleSettled(item.id, item.isSettled) }
                        )
                    }
                }
            }
        }

        if (debtCreditList.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    selectedItemForEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", modifier = Modifier.size(28.dp))
            }
        }

        // دیالوگ واریز
        itemForDeposit?.let { item ->
            AmountActionDialog(
                title = if (isPersian) "ثبت واریزی/پرداختی برای «${item.personName}»" else "Deposit for '${item.personName}'",
                isPersian = isPersian,
                currencyUnit = "IRT",
                isDeposit = true,
                onDismiss = { itemForDeposit = null },
                onConfirm = { amount ->
                    viewModel.deposit(item.id, amount)
                    itemForDeposit = null
                }
            )
        }

        // دیالوگ برداشت
        itemForWithdraw?.let { item ->
            AmountActionDialog(
                title = if (isPersian) "کاهش مبلغ برای «${item.personName}»" else "Withdraw for '${item.personName}'",
                isPersian = isPersian,
                currencyUnit = "IRT",
                isDeposit = false,
                onDismiss = { itemForWithdraw = null },
                onConfirm = { amount ->
                    viewModel.withdraw(item.id, amount)
                    itemForWithdraw = null
                }
            )
        }

        if (showAddDialog) {
            AddOrEditDebtCreditDialog(
                initialItem = selectedItemForEdit,
                isPersian = isPersian,
                onDismiss = {
                    showAddDialog = false
                    selectedItemForEdit = null
                },
                onSave = { id, type, personName, totalAmount, isMonthly, monthlyAmount, dueDay, oneTimeDueDateMillis, note ->
                    showAddDialog = false
                    if (id != null) {
                        viewModel.saveOrUpdate(
                            id = id,
                            type = type,
                            personName = personName,
                            totalAmount = totalAmount,
                            isMonthly = isMonthly,
                            monthlyAmount = monthlyAmount,
                            dueDay = dueDay,
                            oneTimeDueDateMillis = oneTimeDueDateMillis,
                            note = note,
                            addToBalance = false
                        )
                        selectedItemForEdit = null
                    } else {
                        pendingSaveData = PendingSaveData(
                            id = null,
                            type = type,
                            personName = personName,
                            totalAmount = totalAmount,
                            isMonthly = isMonthly,
                            monthlyAmount = monthlyAmount,
                            dueDay = dueDay,
                            oneTimeDueDateMillis = oneTimeDueDateMillis,
                            note = note
                        )
                    }
                }
            )
        }

        // دیالوگ پرسش ثبت تراکنش در بالانس حساب اصلی
        pendingSaveData?.let { data ->
            val dialogShape = RoundedCornerShape(24.dp)

            Dialog(onDismissRequest = {}) {
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = if (isPersian) "محاسبه در بالانس حساب" else "Calculate in Main Balance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        val actionText = if (data.type == "DEBT") {
                            if (isPersian) "به بالانس حساب شما اضافه گردد؟" else "be added to your main balance?"
                        } else {
                            if (isPersian) "از بالانس حساب شما کسر گردد؟" else "be deducted from your main balance?"
                        }

                        Text(
                            text = if (isPersian)
                                "آیا می‌خواهید مبلغ «%,.0f» مربوط به «${data.personName}» $actionText".format(data.totalAmount)
                            else
                                "Do you want the amount of '%,.0f' for '${data.personName}' $actionText".format(data.totalAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveOrUpdate(
                                        id = data.id,
                                        type = data.type,
                                        personName = data.personName,
                                        totalAmount = data.totalAmount,
                                        isMonthly = data.isMonthly,
                                        monthlyAmount = data.monthlyAmount,
                                        dueDay = data.dueDay,
                                        oneTimeDueDateMillis = data.oneTimeDueDateMillis,
                                        note = data.note,
                                        addToBalance = true
                                    )
                                    pendingSaveData = null
                                    selectedItemForEdit = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = if (isPersian) "بله، محاسبه شود" else "Yes, calculate",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.saveOrUpdate(
                                        id = data.id,
                                        type = data.type,
                                        personName = data.personName,
                                        totalAmount = data.totalAmount,
                                        isMonthly = data.isMonthly,
                                        monthlyAmount = data.monthlyAmount,
                                        dueDay = data.dueDay,
                                        oneTimeDueDateMillis = data.oneTimeDueDateMillis,
                                        note = data.note,
                                        addToBalance = false
                                    )
                                    pendingSaveData = null
                                    selectedItemForEdit = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isPersian) "خیر، فقط ثبت شود" else "No, record only")
                            }
                        }
                    }
                }
            }
        }

        itemToDelete?.let { item ->
            val dialogShape = RoundedCornerShape(28.dp)
            Dialog(onDismissRequest = { itemToDelete = null }) {
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
                            text = if (isPersian) "حذف بدهی / طلب" else "Delete Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isPersian)
                                "آیا از حذف رکورد مربوط به «${item.personName}» اطمینان دارید؟"
                            else
                                "Are you sure you want to delete the record for '${item.personName}'?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { itemToDelete = null },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isPersian) "انصراف" else "Cancel")
                            }

                            Button(
                                onClick = {
                                    viewModel.delete(item.id)
                                    itemToDelete = null
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isPersian) "حذف" else "Delete", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PendingSaveData(
    val id: String?,
    val type: String,
    val personName: String,
    val totalAmount: Double,
    val isMonthly: Boolean,
    val monthlyAmount: Double,
    val dueDay: Int,
    val oneTimeDueDateMillis: Long,
    val note: String?
)

@Composable
fun DebtCreditItemCard(
    item: DebtCreditEntity,
    isPersian: Boolean,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleSettled: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val isDebt = item.type == "DEBT"
    val remainingAmount = (item.totalAmount - item.paidAmount).coerceAtLeast(0.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                cardShape
            )
            .border(
                1.dp,
                if (item.isSettled) MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                cardShape
            )
            .clip(cardShape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isDebt) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isDebt) (if (isPersian) "بدهی به" else "Debt to") else (if (isPersian) "طلب از" else "Credit from"),
                            color = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (isPersian) "مبلغ کل:" else "Total Amount:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "%,.0f".format(item.totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (isPersian) "پرداخت/دریافت شده:" else "Paid/Received:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "%,.0f".format(item.paidAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (isPersian) "باقی‌مانده:" else "Remaining:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "%,.0f".format(remainingAmount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            if (item.dueDateMillis > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = if (isPersian) "مهلت تسویه / سررسید:" else "Expiration Date:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DateUtils.formatTimestamp(item.dueDateMillis, isPersian),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPersian) "واریز / دریافت" else "Deposit", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPersian) "برداشت / اصلاح" else "Withdraw", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleSettled() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = item.isSettled, onCheckedChange = { onToggleSettled() })
                Text(
                    text = if (isPersian) "تسویه شده / منقضی شده" else "Settled / Expired",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (item.isSettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditDebtCreditDialog(
    initialItem: DebtCreditEntity?,
    isPersian: Boolean,
    currencyUnit: String = "IRT",
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        type: String,
        personName: String,
        totalAmount: Double,
        isMonthly: Boolean,
        monthlyAmount: Double,
        dueDay: Int,
        oneTimeDueDateMillis: Long,
        note: String?
    ) -> Unit
) {
    val maxNameLength = 40
    val maxDigitsLength = 12

    var type by remember { mutableStateOf(initialItem?.type ?: "DEBT") }
    var personName by remember { mutableStateOf(initialItem?.personName ?: "") }

    val initialTotalDigits = remember(initialItem, currencyUnit) {
        if (initialItem == null || initialItem.totalAmount <= 0) ""
        else {
            val amount = if (currencyUnit == "IRR") (initialItem.totalAmount * 10).toLong() else initialItem.totalAmount.toLong()
            amount.toString().take(maxDigitsLength)
        }
    }
    var totalAmountDigits by remember { mutableStateOf(initialTotalDigits) }

    var isMonthly by remember { mutableStateOf(initialItem?.isMonthly ?: false) }

    val initialMonthlyDigits = remember(initialItem, currencyUnit) {
        if (initialItem == null || initialItem.monthlyAmount <= 0) ""
        else {
            val amount = if (currencyUnit == "IRR") (initialItem.monthlyAmount * 10).toLong() else initialItem.monthlyAmount.toLong()
            amount.toString().take(maxDigitsLength)
        }
    }
    var monthlyAmountDigits by remember { mutableStateOf(initialMonthlyDigits) }

    var selectedDay by remember { mutableIntStateOf(initialItem?.dueDay ?: 1) }
    var oneTimeDueDateMillis by remember { mutableLongStateOf(initialItem?.dueDateMillis ?: System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var note by remember { mutableStateOf(initialItem?.note ?: "") }

    val dayListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedDay - 1).coerceAtLeast(0)
    )

    LaunchedEffect(dayListState.isScrollInProgress) {
        if (!dayListState.isScrollInProgress) {
            val centerIndex = dayListState.firstVisibleItemIndex +
                    if (dayListState.firstVisibleItemScrollOffset > 30) 1 else 0
            val newDay = (centerIndex + 1).coerceIn(1, 31)
            if (newDay != selectedDay) {
                selectedDay = newDay
            }
        }
    }

    val labelTotalCurrency = if (isPersian) (if (currencyUnit == "IRR") "مبلغ کل (ریال)" else "مبلغ کل (تومان)")
    else (if (currencyUnit == "IRR") "Total Amount (Rial)" else "Total Amount (Toman)")

    val labelMonthlyCurrency = if (isPersian) (if (currencyUnit == "IRR") "قسط ماهانه (ریال)" else "قسط ماهانه (تومان)")
    else (if (currencyUnit == "IRR") "Monthly Installment (Rial)" else "Monthly Installment (Toman)")

    // دیالوگ انتخاب تاریخ سررسید یکباره با اعمال غیرفعالسازی روزهای گذشته
    if (showDatePickerDialog) {
        val todayStartMillis = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = oneTimeDueDateMillis.coerceAtLeast(todayStartMillis),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= todayStartMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { oneTimeDueDateMillis = it }
                    showDatePickerDialog = false
                }) {
                    Text(if (isPersian) "تایید" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                    text = if (initialItem != null) {
                        if (isPersian) "ویرایش بدهی / طلب" else "Edit Debt / Credit"
                    } else {
                        if (isPersian) "افزودن بدهی / طلب جدید" else "New Debt / Credit"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    FilterChip(
                        selected = type == "DEBT",
                        onClick = { type = "DEBT" },
                        label = {
                            Text(
                                if (isPersian) "بدهی (پرداختی)" else "Debt",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = type == "CREDIT",
                        onClick = { type = "CREDIT" },
                        label = {
                            Text(
                                if (isPersian) "طلب (دریافتی)" else "Credit",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = personName,
                    onValueChange = { input ->
                        if (input.length <= maxNameLength) personName = input
                    },
                    label = { Text(if (isPersian) "نام طرف حساب" else "Person Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalAmountDigits,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= maxDigitsLength) totalAmountDigits = digitsOnly
                    },
                    label = { Text(labelTotalCurrency) },
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMonthly = !isMonthly },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isMonthly, onCheckedChange = { isMonthly = it })
                    Text(
                        text = if (isPersian) "پرداخت به صورت ماهانه/اقساطی است" else "Monthly / Installment payment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isMonthly) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = monthlyAmountDigits,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= maxDigitsLength) monthlyAmountDigits = digitsOnly
                        },
                        label = { Text(labelMonthlyCurrency) },
                        singleLine = true,
                        visualTransformation = ThousandsSeparatorTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isPersian) "روز سررسید قسط در ماه" else "Due Day of Month",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            LazyColumn(
                                state = dayListState,
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(vertical = 36.dp)
                            ) {
                                items(31) { index ->
                                    val day = index + 1
                                    val isSelected = day == selectedDay
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isPersian) "روز $day" else "Day $day",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian)
                                "مهلت تسویه: ${DateUtils.formatTimestamp(oneTimeDueDateMillis, isPersian)}"
                            else
                                "Due Date: ${DateUtils.formatTimestamp(oneTimeDueDateMillis, isPersian)}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isPersian) "یادداشت (اختیاری)" else "Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

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

                    val rawTotal = totalAmountDigits.toDoubleOrNull() ?: 0.0
                    val rawMonthly = monthlyAmountDigits.toDoubleOrNull() ?: 0.0

                    val finalTotalInToman = if (currencyUnit == "IRR") rawTotal / 10.0 else rawTotal
                    val finalMonthlyInToman = if (currencyUnit == "IRR") rawMonthly / 10.0 else rawMonthly

                    Button(
                        onClick = {
                            onSave(
                                initialItem?.id,
                                type,
                                personName.trim(),
                                finalTotalInToman,
                                isMonthly,
                                if (isMonthly) finalMonthlyInToman else 0.0,
                                selectedDay,
                                oneTimeDueDateMillis,
                                note.ifBlank { null }
                            )
                        },
                        enabled = personName.isNotBlank() && rawTotal > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (initialItem != null) (if (isPersian) "ذخیره" else "Save")
                            else (if (isPersian) "ایجاد" else "Create")
                        )
                    }
                }
            }
        }
    }
}