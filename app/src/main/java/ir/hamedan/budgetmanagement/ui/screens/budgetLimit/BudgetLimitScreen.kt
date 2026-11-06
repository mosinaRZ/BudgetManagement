package ir.hamedan.budgetmanagement.ui.screens.budgetLimit

import ir.hamedan.budgetmanagement.ui.components.CountUpPairText

import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.showError
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.StatusBarAuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitUiModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@Composable
fun BudgetLimitScreen(
    onBackClick: () -> Unit = {},
    onAddLimitClick: () -> Unit = {},
    onEditLimitClick: (limitId: String) -> Unit = {},
    viewModel: BudgetLimitViewModel = appViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            snackbarHostState.showError(message)
        }
    }
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

    var limitToDelete by remember { mutableStateOf<BudgetLimitUiModel?>(null) }

    val limitsList = limitsListState ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StatusBarAuroraBackground()

        // ------------------ Main Content List ------------------
        if (limitsListState != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // تنظیم فاصله افقی برای کل لیست و آیتم‌ها
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    // ایجاد فضای خالی در بالا برای اینکه محتوا زیر تاپ‌بار پنهان نشود
                    top = 120.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                Icon(painterResource(R.drawable.ic_limit), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
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
                                    onClick = onAddLimitClick,
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
                    items(limitsList, key = { it.entity.id }) { item ->
                        BudgetLimitItemCard(
                            item = item,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit,
                            numberFormatter = numberFormatter,
                            categoryName = categories.find { it.id == item.entity.categoryId }?.title.orEmpty(),
                            onToggleActive = { isActive ->
                                viewModel.updateLimitStatus(item.entity.id, isActive)
                            },
                            onEditClick = { onEditLimitClick(item.entity.id) },
                            onDeleteClick = { limitToDelete = item },
                            onRenewClick = { viewModel.renewLimit(item.entity.id) }
                        )
                    }
                }
            }
        }

        // ------------------ Top Bar (Floating on top) ------------------
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
                    modifier = Modifier
                        .size(44.dp)
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
                    text = if (isPersian) "محدودیت‌های بودجه" else "Budget Limits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ------------------ Floating Action Button ------------------
        if (limitsListState != null && limitsList.isNotEmpty()) {
            FloatingActionButton(
                onClick = onAddLimitClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isPersian) "افزودن سقف" else "Add limit", modifier = Modifier.size(28.dp))
            }
        }

        // ------------------ Snackbar Host (Overlay) ------------------
        AppSnackbarHost(
            hostState = snackbarHostState,
            isPersian = isPersian,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
        )

        // ------------------ Delete Dialog (Delete) ------------------
        limitToDelete?.let { item ->
            val mappedCategoryName = StringMapper.getCategoryName(categories.find { it.id == item.entity.categoryId }?.title.orEmpty(), isPersian)
            var isPressed by remember { mutableStateOf(false) }
            val dialogShape = RoundedCornerShape(28.dp)

            // انیمیشن محو شدن و جمع شدن دکمه انصراف
            val cancelWeight by animateFloatAsState(
                targetValue = if (isPressed) 0.001f else 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                label = "CancelWeight"
            )

            // انیمیشن پر شدن دکمه حذف (۱.۵ ثانیه)
            val progress by animateFloatAsState(
                targetValue = if (isPressed) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isPressed) 1500 else 300,
                    easing = LinearEasing
                ),
                label = "HoldProgress"
            )

            // وقتی پر شدن دکمه به ۱۰۰٪ رسید
            LaunchedEffect(progress) {
                if (progress >= 1f && isPressed) {
                    isPressed = false
                    val deletedEntity = item.entity
                    viewModel.deleteBudgetLimit(deletedEntity.id)
                    limitToDelete = null

                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (isPersian) "محدودیت «$mappedCategoryName» حذف شد" else "Limit '$mappedCategoryName' deleted",
                            actionLabel = if (isPersian) "بازگردانی" else "Undo",
                            duration = SnackbarDuration.Indefinite
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreLimit(deletedEntity)
                        } else {
                            viewModel.commitDeleteLimit(deletedEntity)
                        }
                    }
                }
            }

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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // دکمه انصراف با قابلیت جمع شدن
                            if (cancelWeight > 0.01f) {
                                OutlinedButton(
                                    onClick = { limitToDelete = null },
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

                            // دکمه حذف با قابلیت فشردن و نگه داشتن (Delete)
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
                                // لایه پیشرفت پرشونده روی دکمه
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

@Composable
fun BudgetLimitItemCard(
    item: BudgetLimitUiModel,
    isPersian: Boolean,
    currencyUnit: String,
    numberFormatter: NumberFormat,
    categoryName: String,
    onToggleActive: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenewClick: () -> Unit
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

    val categoryDisplayName = StringMapper.getCategoryName(categoryName, isPersian)

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
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onRenewClick,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPersian) "تمدید ۳۰ روزه" else "Renew for 30 days", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountUpPairText(
                    first = spentDisp.toLong(), firstKey = "limit.${item.entity.id}.spent.$currencyUnit",
                    second = maxDisp.toLong(), secondKey = "limit.${item.entity.id}.max.$currencyUnit",
                    format = { a, b -> "${numberFormatter.format(a)} / ${numberFormatter.format(b)} $currencyText" },
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