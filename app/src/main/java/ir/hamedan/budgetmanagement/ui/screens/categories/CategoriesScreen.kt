package ir.hamedan.budgetmanagement.ui.screens.categories

import ir.hamedan.budgetmanagement.ui.components.CountUpText
import ir.hamedan.budgetmanagement.ui.components.CountUpPairText
import androidx.compose.animation.core.Animatable
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.components.showInfo
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AppLogoIcon
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.StatusBarAuroraBackground
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlin.math.ceil

@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit = {},
    /** باز کردن ویرایشگر تمام‌صفحه: [categoryId] خالی = دسته‌ی جدید با نوع [isExpense]. */
    onOpenEditor: (categoryId: String?, isExpense: Boolean) -> Unit = { _, _ -> },
    categoryViewModel: CategoriesViewModel = appViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        categoryViewModel.errorMessage.collect { message ->
            snackbarHostState.showError(message)
        }
    }
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    val categoriesState by categoryViewModel.categories.collectAsState()
    val transactionCounts by categoryViewModel.transactionCountsMap.collectAsState()
    val transactionTotals by categoryViewModel.transactionTotalsMap.collectAsState()
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()

    // با rememberSaveable: ویرایشگر صفحه‌ی جداست و این صفحه موقع رفتن به آن از composition خارج می‌شود؛
    // بدون ذخیره، بعد از ثبت یک دسته‌ی «درآمد» تب دوباره روی «هزینه‌ها» برمی‌گشت.
    var selectedTabState by rememberSaveable { mutableStateOf(0) }
    val isExpenseTab = selectedTabState == 0

    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val hiddenCategories by categoryViewModel.hiddenCategories.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StatusBarAuroraBackground()
        // ------------------ Main Content (Pushed down to avoid overlapping the expanded floating header) ------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 175.dp) // تنظیم فاصله از بالا متناسب با ارتفاع جدید هدر شناور
        ) {
            // ------------------ Category Grid Content ------------------
            if (categoriesState != null) {
                val filteredCategories = categoriesState!!.filter { category ->
                    category.isExpense == isExpenseTab &&
                            category.title != "دسته‌بندی نشده" &&
                            category.title != "دسته بندی نشده" &&
                            !category.title.equals("Uncategorized", ignoreCase = true) &&
                            !hiddenCategories.contains(category.id)
                }

                if (filteredCategories.isEmpty()) {
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
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), emptyCardShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), emptyCardShape)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_category), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isPersian) {
                                        if (isExpenseTab) "هیچ دسته‌بندی هزینه‌ای نداری!" else "هیچ دسته‌بندی درآمدی نداری!"
                                    } else {
                                        if (isExpenseTab) "No Expense Categories Yet!" else "No Income Categories Yet!"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isPersian) {
                                        "برای مدیریت بهتر تراکنش‌ها و گزارش‌گیری دقیق، همین الان اولین دسته‌بندی خودت رو اضافه کن."
                                    } else {
                                        "Start organizing your transactions by adding a first custom category."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { onOpenEditor(null, isExpenseTab) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isExpenseTab) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPersian) "ساخت اولین دسته‌بندی" else "Create First Category",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 24.dp, end = 24.dp, top = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredCategories, key = { it.id }) { category ->
                            val count = transactionCounts[category.id] ?: 0
                            val totalAmount = transactionTotals[category.id] ?: 0L
                            CategoryItemCard(
                                category = category,
                                transactionCount = count,
                                transactionTotal = totalAmount,
                                currencyUnit = currencyUnit,
                                isPersian = isPersian,
                                onEditClick = { onOpenEditor(category.id, category.isExpense) },
                                onDeleteClick = { categoryToDelete = category }
                            )
                        }
                    }
                }
            }
        }

        // ------------------ Floating Header (Top Bar & Tab Switcher combined) ------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Row: Back Button & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = if (isPersian) "مدیریت دسته‌بندی‌ها" else "Manage Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Switcher inside the floating header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
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
                    onClick = { selectedTabState = 0 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpenseTab) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                        contentColor = if (isExpenseTab) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPersian) "هزینه‌ها" else "Expenses", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedTabState = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isExpenseTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                        contentColor = if (!isExpenseTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPersian) "درآمدها" else "Incomes", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ------------------ Floating Action Button (FAB) ------------------
        val currentCategories = categoriesState
        val hasCategories = currentCategories?.any { category ->
            category.isExpense == isExpenseTab &&
                    category.title != "دسته‌بندی نشده" &&
                    category.title != "دسته بندی نشده" &&
                    !category.title.equals("Uncategorized", ignoreCase = true)
        } ?: false

        if (hasCategories) {
            AddCategoryFab(
                isExpense = isExpenseTab,
                isPersian = isPersian,
                // وقتی دیالوگ حذف باز است نکته نمی‌آید تا حواس کاربر پرت نشود.
                hintEnabled = categoryToDelete == null,
                containerColor = if (isExpenseTab) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (isExpenseTab) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                onClick = { onOpenEditor(null, isExpenseTab) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp)
            )
        }

        // ------------------ Countdown Snackbar Host ------------------
        AppSnackbarHost(
            hostState = snackbarHostState,
            isPersian = isPersian,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )

        // ------------------ Delete Dialog ------------------
        categoryToDelete?.let { category ->
            var transactionCount by remember { mutableIntStateOf(0) }
            var budgetLimitCount by remember { mutableIntStateOf(0) }

            // متغیرهای وضعیت برای تشخیص نگه‌داشتن دکمه و پر شدن انیمیشن
            var isPressed by remember { mutableStateOf(false) }

            LaunchedEffect(category.title) {
                transactionCount = categoryViewModel.getTransactionCount(category.id)
                budgetLimitCount = categoryViewModel.getBudgetLimitCount(category.id)
            }

            val categoryName = StringMapper.getCategoryName(category.title, isPersian)
            val uncategorizedName = StringMapper.getCategoryName("UNCATEGORIZED", isPersian)
            val dialogShape = RoundedCornerShape(28.dp)

            // انیمیشن محو شدن و جمع شدن دکمه انصراف (تغییر وزن از ۱ به ۰)
            val cancelWeight by animateFloatAsState(
                targetValue = if (isPressed) 0.001f else 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                label = "CancelWeight"
            )

            // انیمیشن پر شدن دکمه حذف (از ۰ تا ۱ در طی ۱.۵ ثانیه)
            val progress by animateFloatAsState(
                targetValue = if (isPressed) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isPressed) 1500 else 300, // زمان لازم برای نگه داشتن
                    easing = LinearEasing
                ),
                label = "HoldProgress"
            )

            // وقتی انیمیشن پر شدن به ۱۰۰٪ رسید، عملیات حذف انجام می‌شود
            LaunchedEffect(progress) {
                if (progress >= 1f && isPressed) {
                    isPressed = false // ریست کردن وضعیت
                    val targetCategory = category

                    categoryViewModel.softDeleteCategory(targetCategory)
                    categoryToDelete = null

                    coroutineScope.launch {
                        var undone = false
                        try {
                            val result = snackbarHostState.showSnackbar(
                                message = if (isPersian) "دسته‌بندی «$categoryName» حذف شد" else "Category '$categoryName' deleted",
                                actionLabel = if (isPersian) "بازگردانی" else "Undo",
                                duration = SnackbarDuration.Indefinite
                            )
                            undone = result == SnackbarResult.ActionPerformed
                        } finally {
                            // finally اجرا می‌شود حتی اگر کاربر با اسنک‌بارِ باز به ویرایشگر برود و این صفحه
                            // (و coroutine) لغو شود؛ وگرنه حذف هرگز نهایی نمی‌شد و دسته فقط مخفی می‌ماند.
                            if (undone) {
                                categoryViewModel.restoreCategory(targetCategory)
                            } else {
                                categoryViewModel.commitDeleteCategory(targetCategory)
                            }
                        }
                    }
                }
            }

            Dialog(onDismissRequest = { categoryToDelete = null }) {
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
                            text = if (isPersian) "حذف دسته‌بندی" else "Delete Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val transactionMessage = if (transactionCount > 0) {
                            if (isPersian)
                                "دسته‌بندی «$categoryName» دارای $transactionCount تراکنش ثبت‌شده است. با حذف آن، این تراکنش‌ها به دسته‌بندی «$uncategorizedName» منتقل می‌شوند."
                            else
                                "'$categoryName' has $transactionCount active transactions. Deleting it will move these transactions to '$uncategorizedName'."
                        } else null

                        val budgetLimitMessage = if (budgetLimitCount > 0) {
                            if (isPersian)
                                "برای دسته‌بندی «$categoryName» یک محدودیت خرج‌کرد (بودجه) هم تعریف شده است. با حذف این دسته‌بندی، آن محدودیت هم حذف خواهد شد."
                            else
                                "A budget limit is set for '$categoryName'. Deleting this category will also delete that budget limit."
                        } else null

                        val confirmMessage = if (isPersian)
                            "آیا از حذف دسته‌بندی «$categoryName» اطمینان دارید؟"
                        else
                            "Are you sure you want to delete '$categoryName'?"

                        val fullMessage = listOfNotNull(transactionMessage, budgetLimitMessage, confirmMessage)
                            .joinToString("\n\n")

                        Text(
                            text = fullMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // دکمه انصراف
                            if (cancelWeight > 0.01f) {
                                OutlinedButton(
                                    onClick = { categoryToDelete = null },
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

                            // دکمه حذف سفارشی (تشخیص نگه داشتن انگشت)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true // انگشت روی دکمه قرار گرفت
                                                tryAwaitRelease() // منتظر برداشتن انگشت
                                                isPressed = false // انگشت برداشته شد
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // لایه پر شونده
                                if (progress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.coerceAtLeast(0.001f))
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )
                                }

                                // محتوای دکمه (آیکون و متن)
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
                                            if (isPersian) " حذف" else "Delete"
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
fun CategoryItemCard(
    category: CategoryEntity,
    transactionCount: Int,
    transactionTotal: Long,
    currencyUnit: String,
    isPersian: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                cardShape
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                cardShape
            )
            .clip(cardShape)
            .clickable(enabled = !category.isSystem) { onEditClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = category.iconEmoji, fontSize = 28.sp)

                if (!category.isSystem) {
                    Row {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "System Category",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = StringMapper.getCategoryName(category.title, isPersian),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPersian) "${numberFormatter.format(transactionCount)} تراکنش" else "${numberFormatter.format(transactionCount)} txns",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            tint = if (category.isExpense) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        val displayAmount = if (currencyUnit == "IRR") transactionTotal * 10L else transactionTotal
                        val currencyText = if (isPersian) {
                            if (currencyUnit == "IRR") "ریال" else "تومان"
                        } else {
                            if (currencyUnit == "IRR") "Rial" else "Toman"
                        }
                        CountUpText(
                            target = displayAmount,
                            key = "category.${category.id}.total.$currencyUnit",
                            format = { "${numberFormatter.format(it)} $currencyText" },
                            durationScale = 0.8f,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}