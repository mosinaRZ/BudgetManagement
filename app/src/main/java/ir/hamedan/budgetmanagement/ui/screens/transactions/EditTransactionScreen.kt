package ir.hamedan.budgetmanagement.ui.screens.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.screens.add.AddTransactionFormState
import ir.hamedan.budgetmanagement.ui.screens.add.AmountHero
import ir.hamedan.budgetmanagement.ui.screens.add.CategoryTile
import ir.hamedan.budgetmanagement.ui.screens.add.DateRow
import ir.hamedan.budgetmanagement.ui.screens.add.MAX_AMOUNT_DIGITS
import ir.hamedan.budgetmanagement.ui.screens.add.NumericKeypad
import ir.hamedan.budgetmanagement.ui.screens.add.PrimaryActionButton
import ir.hamedan.budgetmanagement.ui.screens.add.TypeToggle
import ir.hamedan.budgetmanagement.ui.screens.add.appendDigits
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.utils.CategoryRules
import ir.hamedan.budgetmanagement.utils.CategorySuggestionHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import ir.hamedan.budgetmanagement.utils.SystemTransactionText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * ویرایش تراکنش به‌صورت صفحه‌ی تمام‌صفحه (جایگزین باتم‌شیت قدیمی).
 *
 * - مبلغ بزرگ و لمس‌پذیر است؛ با لمسش کیبورد اختصاصی باز می‌شود.
 * - دسته‌ها یک ردیف افقی هستند و دسته‌ی فعلی خودکار نمایان می‌شود.
 * - دکمه‌ی سوایپ تا وقتی تغییری ایجاد نشده غیرفعال است.
 * - تراکنش‌های قلک دسته‌ی قفل‌شده دارند (مثل رفتار قبلی).
 */
@Composable
fun EditTransactionScreen(
    transactionId: String,
    onClose: () -> Unit = {},
    viewModel: TransactionViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    var original by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(transactionId) {
        val found = viewModel.findTransaction(transactionId)
        // تراکنش (مثلاً بین دو صفحه) حذف شده؛ بی‌صدا برمی‌گردیم.
        if (found == null) onClose() else original = found
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()
        val tx = original
        if (tx == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditorContent(tx = tx, isPersian = isPersian, viewModel = viewModel, onClose = onClose)
        }
    }
}

@Composable
private fun EditorContent(
    tx: TransactionEntity,
    isPersian: Boolean,
    viewModel: TransactionViewModel,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleFocus = remember { FocusRequester() }

    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val allCategories by viewModel.categories.collectAsState()

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }

    // متن‌های اولیه (متن تراکنش‌های سیستمی بر اساس زبان محلی‌سازی می‌شود).
    val initialTitle = remember(tx.id, isPersian) { SystemTransactionText.localizeTitle(tx.title, isPersian) }
    val initialNote = remember(tx.id, isPersian) { SystemTransactionText.localizeNote(tx.note, isPersian) ?: "" }
    val initialAmountText = remember(tx.id, currencyUnit) {
        if (currencyUnit == "IRR") (tx.amount * 10).toLong().toString() else tx.amount.toLong().toString()
    }
    val initialOffset = remember(tx.id) {
        val day = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
        ChronoUnit.DAYS.between(day, today).toInt().coerceAtLeast(0)
    }

    val form = rememberSaveable(tx.id, saver = AddTransactionFormState.Saver) {
        AddTransactionFormState(
            isExpense = tx.type == "EXPENSE",
            amount = initialAmountText,
            title = initialTitle,
            note = initialNote,
            dayOffset = initialOffset
        )
    }
    var categoryId by rememberSaveable(tx.id) { mutableStateOf(tx.categoryId) }
    var editingAmount by rememberSaveable { mutableStateOf(false) }
    var noteOpen by rememberSaveable { mutableStateOf(initialNote.isNotBlank()) }
    var showTitleError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var savedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showShare by remember { mutableStateOf(false) }

    // تراکنش‌های قلک باید همان دسته‌ی سیستمی را نگه دارند و دسته‌شان قفل است.
    val isSavingGoalTransaction = remember(tx.categoryId, allCategories) {
        allCategories.any { it.id == tx.categoryId && !CategoryRules.isUserSelectable(it) }
    }
    val categories = remember(form.isExpense, expenseCategories, incomeCategories) {
        (if (form.isExpense) expenseCategories else incomeCategories).filter { cat ->
            CategoryRules.isUserSelectable(cat) &&
                    cat.title != "دسته‌بندی نشده" &&
                    cat.title != "دسته بندی نشده" &&
                    !cat.title.equals("Uncategorized", ignoreCase = true)
        }
    }
    val selectedCategory = if (isSavingGoalTransaction) {
        allCategories.find { it.id == tx.categoryId }
    } else {
        categories.find { it.id == categoryId }
    }

    // بعد از عوض‌شدن نوع، دسته‌ی نامعتبر با پیشنهاد (یا اولین دسته) جایگزین می‌شود.
    LaunchedEffect(form.isExpense, categories) {
        if (!isSavingGoalTransaction && categories.isNotEmpty() && categories.none { it.id == categoryId }) {
            val suggestedTitle = CategorySuggestionHelper.suggestCategory(form.title, categories, isPersian)
            categoryId = categories.firstOrNull { it.title == suggestedTitle }?.id ?: categories.first().id
        }
    }

    val accent by animateColorAsState(
        targetValue = if (form.isExpense) scheme.tertiary else scheme.primary,
        animationSpec = tween(300),
        label = "accent"
    )
    val onAccent by animateColorAsState(
        targetValue = if (form.isExpense) scheme.onTertiary else scheme.onPrimary,
        animationSpec = tween(300),
        label = "onAccent"
    )

    // مقادیر نهایی و تشخیص تغییر
    val parsedAmount = form.amount.toDoubleOrNull() ?: 0.0
    val newStoredAmount = MoneyContract.inputToStorage(parsedAmount, currencyUnit)
    // اگر فقط تاریخ عوض نشده باشد، ساعت و زمان اصلی دست نمی‌خورد.
    val newTimestamp = if (form.dayOffset == initialOffset) {
        tx.timestamp
    } else {
        val time = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalTime()
        today.minusDays(form.dayOffset.toLong()).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
    val hasChanges = form.isExpense != (tx.type == "EXPENSE") ||
            newStoredAmount != tx.amount ||
            (selectedCategory?.id ?: categoryId) != tx.categoryId ||
            form.title.trim() != initialTitle.trim() ||
            form.note.trim() != initialNote.trim() ||
            newTimestamp != tx.timestamp

    fun setType(expense: Boolean) {
        if (form.isExpense == expense) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        form.isExpense = expense
        if (!isSavingGoalTransaction) categoryId = ""
    }

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun save() {
        val titleOk = form.title.isNotBlank()
        val amountOk = parsedAmount > 0
        val categoryOk = isSavingGoalTransaction || categoryId.isNotBlank()
        if (!titleOk || !amountOk || !categoryOk) {
            // دستگیره‌ی سوایپ به ابتدا برمی‌گردد و مشکل نشان داده می‌شود.
            resetKey++
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            when {
                !amountOk -> {
                    editingAmount = true
                    toast(tr(isPersian, "مبلغ رو وارد کن", "Enter an amount"))
                }
                !titleOk -> {
                    showTitleError = true
                    titleFocus.requestFocus()
                }
                else -> toast(tr(isPersian, "یک دسته‌بندی انتخاب کن", "Pick a category"))
            }
            return
        }
        val updated = tx.copy(
            // متن بدون تغییر، همان مقدار ذخیره‌شده می‌ماند تا متن سیستمی خراب نشود.
            title = if (form.title.trim() == initialTitle.trim()) tx.title else form.title.trim(),
            amount = newStoredAmount,
            categoryId = if (isSavingGoalTransaction) tx.categoryId else categoryId,
            type = if (form.isExpense) "EXPENSE" else "INCOME",
            timestamp = newTimestamp,
            note = if (form.note.trim() == initialNote.trim()) tx.note else form.note.trim()
        )
        isSaving = true
        viewModel.updateTransaction(updated) { saved ->
            if (saved) {
                savedTx = updated
                successSheet = transactionUpdatedContent(
                    title = form.title.trim(),
                    amountToman = updated.amount,
                    currencyUnit = currencyUnit,
                    isPersian = isPersian
                )
            } else {
                isSaving = false
                resetKey++
                scope.launch {
                    snackbarHostState.showError(
                        tr(isPersian, "ذخیره‌ی تغییرات انجام نشد", "Couldn't save the changes")
                    )
                }
            }
        }
    }

    BackHandler(enabled = editingAmount && successSheet == null) { editingAmount = false }

    Box(modifier = Modifier.fillMaxSize()) {
        // هاله‌ی رنگیِ بالا که با نوع تراکنش عوض می‌شود.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            EditTopBar(
                title = tr(isPersian, "ویرایش تراکنش", "Edit transaction"),
                closeLabel = tr(isPersian, "بستن", "Close"),
                onClose = { if (editingAmount) editingAmount = false else onClose() }
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                TypeToggle(isExpense = form.isExpense, isPersian = isPersian, onSetType = ::setType)
            }

            // مبلغ: لمس‌پذیر؛ با لمس، کیبورد اختصاصی جای فیلدها را می‌گیرد.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !editingAmount) {
                        focusManager.clearFocus()
                        editingAmount = true
                    }
                    .padding(vertical = 8.dp)
            ) {
                AmountHero(form = form, accent = accent, currencyUnit = currencyUnit, isPersian = isPersian)
                if (!editingAmount) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(isPersian, "برای تغییر مبلغ لمس کن", "Tap to change the amount"),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }

            AnimatedContent(
                targetState = editingAmount,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    (slideInVertically(tween(260)) { it / 6 } + fadeIn(tween(260)))
                        .togetherWith(slideOutVertically(tween(200)) { it / 6 } + fadeOut(tween(160)))
                },
                label = "editorPanel"
            ) { editing ->
                if (editing) {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        NumericKeypad(
                            isPersian = isPersian,
                            onDigits = { digits -> form.amount = appendDigits(form.amount, digits) },
                            onBackspace = { form.amount = form.amount.dropLast(1) },
                            onClear = { form.amount = "" }
                        )
                        Spacer(Modifier.height(12.dp))
                        PrimaryActionButton(
                            text = tr(isPersian, "تأیید مبلغ", "Confirm amount"),
                            enabled = form.amount.toLongOrNull()?.let { it > 0L } == true &&
                                    form.amount.length <= MAX_AMOUNT_DIGITS,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { editingAmount = false }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    DetailsPanel(
                        form = form,
                        categories = categories,
                        categoryId = categoryId,
                        selectedCategoryName = selectedCategory?.let { StringMapper.getCategoryName(it.title, isPersian) },
                        selectedCategoryEmoji = selectedCategory?.iconEmoji,
                        isSavingGoalTransaction = isSavingGoalTransaction,
                        accent = accent,
                        onAccent = onAccent,
                        isPersian = isPersian,
                        noteOpen = noteOpen,
                        onNoteOpen = { noteOpen = true },
                        showTitleError = showTitleError,
                        onTitleChanged = { input ->
                            if (input.length <= 40) {
                                form.title = input
                                if (input.isNotBlank()) showTitleError = false
                            }
                        },
                        titleFocus = titleFocus,
                        onCategoryPicked = { category ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            categoryId = category.id
                        }
                    ) {
                        SwipeToConfirmButton(
                            text = tr(isPersian, "برای ذخیره بکشید", "Swipe to save"),
                            isPersian = isPersian,
                            resetTrigger = resetKey,
                            onConfirm = ::save,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasChanges && !isSaving,
                            accentColor = accent,
                            onBlockedAttempt = {
                                if (!isSaving) {
                                    toast(tr(isPersian, "هنوز تغییری ایجاد نکردی", "Nothing has changed yet"))
                                }
                            }
                        )
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            isPersian = isPersian,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp)
        )
    }

    successSheet?.let { content ->
        SuccessBottomSheet(
            content = content,
            isPersian = isPersian,
            onDismiss = {
                successSheet = null
                onClose()
            },
            onShare = savedTx?.let { { showShare = true } }
        )
    }

    if (showShare) {
        savedTx?.let { shared ->
            val category = allCategories.find { it.id == shared.categoryId }
            TransactionShareSheet(
                transaction = shared,
                categoryTitle = category?.let { StringMapper.getCategoryName(it.title, isPersian) }
                    ?: StringMapper.getCategoryName(shared.categoryId, isPersian),
                categoryEmoji = category?.iconEmoji ?: "📁",
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                onDismiss = { showShare = false }
            )
        }
    }
}

@Composable
private fun EditTopBar(title: String, closeLabel: String, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = closeLabel, tint = scheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        // هم‌وزن دکمه‌ی بستن تا عنوان وسط بماند.
        Spacer(Modifier.width(48.dp))
    }
}

/** فیلدهای قابل‌ویرایش؛ دکمه‌ی ذخیره از بیرون در [action] داده می‌شود تا زیر اسکرول ثابت بماند. */
@Composable
private fun DetailsPanel(
    form: AddTransactionFormState,
    categories: List<ir.hamedan.budgetmanagement.data.local.models.CategoryEntity>,
    categoryId: String,
    selectedCategoryName: String?,
    selectedCategoryEmoji: String?,
    isSavingGoalTransaction: Boolean,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    noteOpen: Boolean,
    onNoteOpen: () -> Unit,
    showTitleError: Boolean,
    onTitleChanged: (String) -> Unit,
    titleFocus: FocusRequester,
    onCategoryPicked: (ir.hamedan.budgetmanagement.data.local.models.CategoryEntity) -> Unit,
    action: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // دسته‌ی فعلی در ردیف افقی بیرون از دید نماند.
    LaunchedEffect(categories.size) {
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index > 0) listState.scrollToItem(index)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = form.title,
                onValueChange = onTitleChanged,
                label = { Text(tr(isPersian, "عنوان", "Title")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                isError = showTitleError,
                supportingText = if (showTitleError) {
                    {
                        Text(
                            tr(
                                isPersian,
                                "عنوان رو فراموش کردی؛ یه اسم کوتاه بنویس",
                                "You forgot the title; add a short name"
                            )
                        )
                    }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    focusedLabelColor = accent,
                    cursorColor = accent
                ),
                trailingIcon = {
                    VoiceInputButton(
                        onResult = { spoken -> onTitleChanged(spoken.take(40)) },
                        language = if (isPersian) "fa-IR" else "en-US"
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = tr(isPersian, "دسته‌بندی", "Category"),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (isSavingGoalTransaction) {
                // تراکنش قلک: دسته قفل است.
                val shape = RoundedCornerShape(16.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.15f), shape)
                        .padding(14.dp)
                ) {
                    Text(text = selectedCategoryEmoji ?: "🐷", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCategoryName.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = tr(
                                isPersian,
                                "🔒 این تراکنش مربوط به قلک است؛ دسته قابل تغییر نیست",
                                "🔒 This is a savings-goal transaction; its category is locked"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    // فاصله‌ی اطراف جای بزرگ‌شدن کارتِ انتخاب‌شده است تا بریده نشود.
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    items(items = categories, key = { it.id }) { category ->
                        Box(modifier = Modifier.width(100.dp)) {
                            CategoryTile(
                                emoji = category.iconEmoji,
                                name = StringMapper.getCategoryName(category.title, isPersian),
                                selected = category.id == categoryId,
                                suggested = false,
                                accent = accent,
                                onAccent = onAccent,
                                suggestedLabel = "",
                                onClick = { onCategoryPicked(category) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            DateRow(
                dayOffset = form.dayOffset,
                accent = accent,
                isPersian = isPersian,
                onSelectOffset = { form.dayOffset = it }
            )

            Spacer(Modifier.height(4.dp))

            if (!noteOpen) {
                Text(
                    text = tr(isPersian, "＋ افزودن یادداشت", "＋ Add a note"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNoteOpen)
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                )
            } else {
                OutlinedTextField(
                    value = form.note,
                    onValueChange = { if (it.length <= 120) form.note = it },
                    label = { Text(tr(isPersian, "یادداشت (اختیاری)", "Note (optional)")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        focusedLabelColor = accent,
                        cursorColor = accent
                    ),
                    trailingIcon = {
                        VoiceInputButton(
                            onResult = { spoken -> form.note = spoken.take(120) },
                            language = if (isPersian) "fa-IR" else "en-US"
                        )
                    },
                    supportingText = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "${form.note.length}/120",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        action()
        Spacer(Modifier.height(16.dp))
    }
}