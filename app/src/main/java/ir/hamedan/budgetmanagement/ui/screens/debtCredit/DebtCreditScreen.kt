package ir.hamedan.budgetmanagement.ui.screens.debtCredit

import ir.hamedan.budgetmanagement.ui.components.CountUpText

import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.showError
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AppLogoIcon
import ir.hamedan.budgetmanagement.ui.components.StatusBarAuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmountFromToman
import ir.hamedan.budgetmanagement.ui.screens.goals.AmountActionDialog
import ir.hamedan.budgetmanagement.ui.screens.debtCredit.DebtCreditViewModel
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtCreditScreen(
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    viewModel: DebtCreditViewModel = appViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val debtCreditList by viewModel.debtCreditList.collectAsState()
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { msg ->
            snackbarHostState.showError(msg)
        }
    }

    var itemToDelete by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var itemForDeposit by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var itemForWithdraw by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var itemForSettlement by remember { mutableStateOf<DebtCreditEntity?>(null) }

    // محتوای شیت موفقیت؛ null یعنی شیت نمایش داده نمی‌شود
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StatusBarAuroraBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 120.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (debtCreditList.isEmpty()) {
                item {
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
                            Icon(painterResource(R.drawable.ic_debtcredit), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
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
                                onClick = onAddClick,
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
                items(debtCreditList, key = { it.id }) { item ->
                    DebtCreditItemCard(
                        item = item,
                        isPersian = isPersian,
                        currencyUnit = currencyUnit,
                        onDepositClick = { itemForDeposit = item },
                        onWithdrawClick = { itemForWithdraw = item },
                        onEditClick = { onEditClick(item.id) },
                        onDeleteClick = { itemToDelete = item },
                        onToggleSettled = {
                            if (item.isSettled) {
                                viewModel.toggleSettled(item.id, true)
                            } else {
                                itemForSettlement = item
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val shape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), shape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), shape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)
                    .clip(shape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPersian) "بدهی‌ها و بستانکاری‌ها" else "Debts & Credits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (debtCreditList.isNotEmpty()) {
            FloatingActionButton(
                onClick = onAddClick,
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

        AppSnackbarHost(
            hostState = snackbarHostState,
            isPersian = isPersian,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )

        itemForDeposit?.let { item ->
            AmountActionDialog(
                title = if (isPersian) "ثبت واریزی/پرداختی برای «${item.personName}»" else "Deposit for '${item.personName}'",
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isDeposit = true,
                onDismiss = { itemForDeposit = null },
                onConfirm = { amount ->
                    viewModel.deposit(item.id, amount) { saved ->
                        if (saved) {
                            successSheet = debtCreditPaymentContent(
                                type = item.type,
                                personName = item.personName,
                                amountToman = amount,
                                currencyUnit = currencyUnit,
                                isPersian = isPersian
                            )
                        }
                    }
                    itemForDeposit = null
                }
            )
        }

        itemForSettlement?.let { item ->
            val remaining = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)
            val isDebt = item.type == "DEBT"
            val accent = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            Dialog(onDismissRequest = { itemForSettlement = null }) {
                ThemedDialogCard {
                    DialogIconBadge(
                        icon = AppLogoIcon,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (isPersian) "تسویه «${item.personName}»" else "Settle '${item.personName}'",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isPersian) "مانده قابل تسویه" else "Remaining balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CountUpText(
                                target = remaining,
                                key = "debt.${item.id}.detailRemaining.$currencyUnit",
                                format = { formatDebtCreditAmount(it, currencyUnit, isPersian) },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = if (isPersian)
                            "با تأیید، مبلغ مانده به‌عنوان تراکنش ${if (isDebt) "برداشت" else "واریز"} در تراز حساب ثبت می‌شود."
                        else
                            "Confirming will record the remaining amount as a ${if (isDebt) "withdrawal" else "income"} transaction in your account balance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { itemForSettlement = null },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPersian) "انصراف" else "Cancel", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                viewModel.settleWithRemainingBalance(item.id)
                                itemForSettlement = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "تسویه کن" else "Settle",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        itemForWithdraw?.let { item ->
            AmountActionDialog(
                title = if (isPersian) "کاهش مبلغ برای «${item.personName}»" else "Withdraw for '${item.personName}'",
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isDeposit = false,
                onDismiss = { itemForWithdraw = null },
                onConfirm = { amount ->
                    viewModel.withdraw(item.id, amount) { saved ->
                        if (saved) {
                            successSheet = debtCreditAdjustmentContent(
                                type = item.type,
                                personName = item.personName,
                                amountToman = amount,
                                currencyUnit = currencyUnit,
                                isPersian = isPersian
                            )
                        }
                    }
                    itemForWithdraw = null
                }
            )
        }

        // ------------------ شیت موفقیت (تمام‌صفحه) ------------------
        successSheet?.let { content ->
            SuccessBottomSheet(
                content = content,
                isPersian = isPersian,
                onDismiss = { successSheet = null }
            )
        }

        itemToDelete?.let { item ->
            var isPressed by remember { mutableStateOf(false) }
            val dialogShape = RoundedCornerShape(28.dp)

            val cancelWeight by animateFloatAsState(
                targetValue = if (isPressed) 0.001f else 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                label = "CancelWeight"
            )

            val progress by animateFloatAsState(
                targetValue = if (isPressed) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isPressed) 1500 else 300,
                    easing = LinearEasing
                ),
                label = "HoldProgress"
            )

            LaunchedEffect(progress) {
                if (progress >= 1f && isPressed) {
                    isPressed = false
                    val targetItem = item
                    viewModel.softDelete(targetItem)
                    itemToDelete = null

                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (isPersian) "اطلاعات مربوط به «${targetItem.personName}» حذف شد" else "Record for '${targetItem.personName}' deleted",
                            actionLabel = if (isPersian) "بازگردانی" else "Undo",
                            duration = SnackbarDuration.Indefinite
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restore(targetItem)
                        } else {
                            viewModel.commitDelete(targetItem.id)
                        }
                    }
                }
            }

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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cancelWeight > 0.01f) {
                                OutlinedButton(
                                    onClick = { itemToDelete = null },
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

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                tryAwaitRelease()
                                                isPressed = false
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (progress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.coerceAtLeast(0.001f))
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )
                                }

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
                                            if (isPersian) "حذف" else "Delete"
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

// ---------------------------------------------------------------------------
// متن شیت‌های موفقیت
// ---------------------------------------------------------------------------

private fun debtCreditPaymentContent(
    type: String,
    personName: String,
    amountToman: Long,
    currencyUnit: String,
    isPersian: Boolean
): SuccessSheetContent {
    val amount = formatSuccessAmountFromToman(amountToman, currencyUnit, isPersian)
    val isDebt = type == "DEBT"
    return if (isPersian) {
        SuccessSheetContent(
            title = if (isDebt) "پرداخت ثبت شد" else "دریافتی ثبت شد",
            message = if (isDebt) "پرداخت مبلغ $amount به «$personName» با موفقیت ثبت شد."
            else "دریافت مبلغ $amount از «$personName» با موفقیت ثبت شد.",
            hint = "موجودی حساب و مانده‌ی این مورد به‌روز است."
        )
    } else {
        SuccessSheetContent(
            title = if (isDebt) "Payment recorded" else "Amount received",
            message = if (isDebt) "Your payment of $amount to “$personName” was recorded successfully."
            else "The $amount received from “$personName” was recorded successfully.",
            hint = "Your account balance and the remaining amount are up to date."
        )
    }
}

private fun debtCreditAdjustmentContent(
    type: String,
    personName: String,
    amountToman: Long,
    currencyUnit: String,
    isPersian: Boolean
): SuccessSheetContent {
    val amount = formatSuccessAmountFromToman(amountToman, currencyUnit, isPersian)
    val isDebt = type == "DEBT"
    return if (isPersian) {
        SuccessSheetContent(
            title = "اصلاح ثبت شد",
            message = if (isDebt) "مبلغ $amount از پرداخت‌های ثبت‌شده برای «$personName» کسر شد."
            else "مبلغ $amount از دریافتی‌های ثبت‌شده از «$personName» کسر شد.",
            hint = "موجودی حساب و مانده‌ی این مورد به‌روز است."
        )
    } else {
        SuccessSheetContent(
            title = "Adjustment recorded",
            message = if (isDebt) "$amount was deducted from the payments recorded for “$personName”."
            else "$amount was deducted from the amounts recorded as received from “$personName”.",
            hint = "Your account balance and the remaining amount are up to date."
        )
    }
}

/** کارت یکپارچه دیالوگ‌ها؛ هم‌رنگ و هم‌فرم با دیالوگ حذف (surfaceVariant، گوشه ۲۸، حاشیه ظریف). */
@Composable
private fun ThemedDialogCard(content: @Composable ColumnScope.() -> Unit) {
    val dialogShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), dialogShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), dialogShape)
            .clip(dialogShape)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun DialogIconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

private fun formatDebtCreditAmount(amountInToman: Long, currencyUnit: String, isPersian: Boolean): String {
    val displayedAmount = MoneyContract.displayFromStorage(amountInToman, currencyUnit)
    val locale = if (isPersian) Locale("fa", "IR") else Locale.US
    val unit = when {
        currencyUnit == MoneyContract.DISPLAY_RIAL && isPersian -> "ریال"
        currencyUnit == MoneyContract.DISPLAY_RIAL -> "Rial"
        isPersian -> "تومان"
        else -> "Toman"
    }
    return "${NumberFormat.getNumberInstance(locale).format(displayedAmount)} $unit"
}

@Composable
fun DebtCreditItemCard(
    item: DebtCreditEntity,
    isPersian: Boolean,
    currencyUnit: String,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleSettled: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val isDebt = item.type == "DEBT"
    val remainingAmount = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)

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
                modifier = Modifier
                    .fillMaxWidth(),
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
                CountUpText(target = item.totalAmount, key = "debt.${item.id}.total.$currencyUnit", format = { formatDebtCreditAmount(it, currencyUnit, isPersian) }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (isPersian) "پرداخت/دریافت شده:" else "Paid/Received:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CountUpText(target = item.paidAmount, key = "debt.${item.id}.paid.$currencyUnit", format = { formatDebtCreditAmount(it, currencyUnit, isPersian) }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (isPersian) "باقی‌مانده:" else "Remaining:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CountUpText(
                    target = remainingAmount,
                    key = "debt.${item.id}.remaining.$currencyUnit",
                    format = { formatDebtCreditAmount(it, currencyUnit, isPersian) },
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPersian) "برداشت / اصلاح" else "Withdraw", style = MaterialTheme.typography.bodySmall)
                }
            }

            val isExpiredAndSettled = item.isSettled
                    && item.dueDateMillis > 0
                    && item.dueDateMillis < System.currentTimeMillis()

            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (!isExpiredAndSettled) onToggleSettled()
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isSettled,
                    onCheckedChange = { _ ->
                        if (!isExpiredAndSettled) onToggleSettled()
                    }
                )
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