package ir.hamedan.budgetmanagement.ui.screens.categories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.screens.add.PrimaryActionButton
import ir.hamedan.budgetmanagement.ui.screens.add.formatNumber
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.ui.screens.categories.sphere.IconSphere
import ir.hamedan.budgetmanagement.ui.screens.categories.sphere.IconSphereLabels
import ir.hamedan.budgetmanagement.utils.CategoryIconCatalog
import ir.hamedan.budgetmanagement.utils.CategoryNameRules
import ir.hamedan.budgetmanagement.utils.CategorySuggestion
import ir.hamedan.budgetmanagement.utils.CategoryTitleSuggestions

private const val STEP_NAME = 0
private const val STEP_ICON = 1
private const val STEP_REVIEW = 2

/**
 * وضعیت فرم دسته‌بندی. با [Saver] در چرخش صفحه حفظ می‌شود.
 * [emoji] خالی یعنی هنوز آیکونی انتخاب نشده (کره در این حالت خودش می‌چرخد).
 */
@Stable
class CategoryFormState(
    title: String = "",
    emoji: String = "",
    step: Int = STEP_NAME,
    iconManual: Boolean = false,
    loaded: Boolean = false
) {
    var title by mutableStateOf(title)
    var emoji by mutableStateOf(emoji)
    var step by mutableStateOf(step)

    /** کاربر خودش آیکون را از روی کره انتخاب کرده؛ دیگر عنوان پیشنهادی آیکون را عوض نمی‌کند. */
    var iconManual by mutableStateOf(iconManual)

    /** مقدارهای اولیه‌ی حالت ویرایش یک‌بار در فرم ریخته شده‌اند. */
    var loaded by mutableStateOf(loaded)

    val hasIcon: Boolean get() = emoji.isNotBlank()

    companion object {
        val Saver = listSaver<CategoryFormState, Any>(
            save = { listOf(it.title, it.emoji, it.step, it.iconManual, it.loaded) },
            restore = {
                CategoryFormState(
                    title = it[0] as String,
                    emoji = it[1] as String,
                    step = it[2] as Int,
                    iconManual = it[3] as Boolean,
                    loaded = it[4] as Boolean
                )
            }
        )
    }
}

/**
 * ثبت و ویرایش تمام‌صفحه‌ی دسته‌بندی به‌صورت جریان سه‌مرحله‌ای (جایگزین دیالوگ قدیمی):
 * ۱) نام (با عنوان‌های پیشنهادی) ← ۲) آیکون روی کره‌ی سه‌بعدی ← ۳) مرور و ثبت با کشیدن.
 *
 * @param categoryId خالی = دسته‌ی جدید؛ مقدار = ویرایش آن دسته
 * @param isExpense نوع دسته‌ی جدید (در ویرایش، نوع خودِ دسته ملاک است و قابل تغییر نیست)
 * @param onClose خروج بدون ذخیره
 * @param onFinished بعد از ذخیره‌ی موفق و بسته شدن شیت موفقیت
 */
@Composable
fun CategoryEditorScreen(
    categoryId: String? = null,
    isExpense: Boolean = true,
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    viewModel: CategoriesViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme

    val categories by viewModel.categories.collectAsState()
    val all = categories.orEmpty()
    val editing = categoryId?.let { id -> all.find { it.id == id } }
    val isEditMode = categoryId != null
    val expense = editing?.isExpense ?: isExpense

    val form = rememberSaveable(saver = CategoryFormState.Saver) { CategoryFormState() }
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message -> snackbarHostState.showError(message) }
    }

    // حالت ویرایش: مقدارهای فعلی دسته یک‌بار در فرم ریخته می‌شود؛ اگر دسته در این بین حذف شده بود برمی‌گردیم.
    LaunchedEffect(categories, categoryId) {
        if (categoryId == null || categories == null) return@LaunchedEffect
        val current = categories.orEmpty().find { it.id == categoryId }
        if (current == null) {
            onClose()
        } else if (!form.loaded) {
            form.title = current.title
            form.emoji = current.iconEmoji
            form.iconManual = true
            form.loaded = true
        }
    }
    val ready = !isEditMode || form.loaded

    val accent = if (expense) scheme.tertiary else scheme.primary
    val onAccent = if (expense) scheme.onTertiary else scheme.onPrimary

    val trimmedTitle = form.title.trim()
    val duplicate = CategoryNameRules.isDuplicate(trimmedTitle, all, editing?.id, isPersian)
    val suggestions = remember(all, expense, isPersian, isEditMode) {
        if (isEditMode) emptyList() else CategoryTitleSuggestions.forType(expense, isPersian, all)
    }

    fun goTo(step: Int) {
        focusManager.clearFocus()
        form.step = step.coerceIn(STEP_NAME, STEP_REVIEW)
    }

    fun applySuggestion(suggestion: CategorySuggestion) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        form.title = suggestion.title(isPersian).take(CategoryNameRules.MAX_TITLE_LENGTH)
        // آیکون پیشنهادی فقط وقتی جایگزین می‌شود که کاربر خودش آیکونی را دستی انتخاب نکرده باشد.
        if (!form.iconManual) form.emoji = suggestion.emoji
    }

    val canSave = !isSaving && ready && trimmedTitle.isNotEmpty() && !duplicate && form.hasIcon

    fun save() {
        if (!canSave) return
        val savedTitle = trimmedTitle
        val savedEmoji = form.emoji
        isSaving = true
        if (editing != null) {
            viewModel.updateCategory(editing, savedTitle, savedEmoji) { saved ->
                if (saved) successSheet = categoryUpdatedContent(savedTitle, savedEmoji, isPersian)
                else isSaving = false
            }
        } else {
            viewModel.addCategory(savedTitle, savedEmoji, expense) { saved ->
                if (saved) {
                    successSheet = categoryAddedContent(savedTitle, savedEmoji, expense, isPersian)
                } else {
                    isSaving = false
                }
            }
        }
    }

    BackHandler(enabled = form.step > STEP_NAME && successSheet == null) { goTo(form.step - 1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        AuroraBackground()

        // هاله‌ی رنگیِ بالای صفحه؛ رنگش نوع دسته (هزینه/درآمد) را نشان می‌دهد.
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
            EditorTopBar(
                step = form.step,
                accent = accent,
                isPersian = isPersian,
                onBack = { if (form.step == STEP_NAME) onClose() else goTo(form.step - 1) }
            )

            if (!ready) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            } else {
                AnimatedContent(
                    targetState = form.step,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    transitionSpec = {
                        val dir = if (targetState > initialState) {
                            AnimatedContentTransitionScope.SlideDirection.Start
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.End
                        }
                        (slideIntoContainer(dir, tween(260), initialOffset = { it / 4 }) + fadeIn(tween(260)))
                            .togetherWith(
                                slideOutOfContainer(dir, tween(260), targetOffset = { it / 4 }) + fadeOut(tween(160))
                            )
                    },
                    label = "categoryStep"
                ) { step ->
                    when (step) {
                        STEP_NAME -> NameStep(
                            form = form,
                            isExpense = expense,
                            isEditMode = isEditMode,
                            suggestions = suggestions,
                            duplicate = duplicate,
                            accent = accent,
                            onAccent = onAccent,
                            isPersian = isPersian,
                            onPickSuggestion = ::applySuggestion,
                            onContinue = { goTo(STEP_ICON) }
                        )

                        STEP_ICON -> IconStep(
                            form = form,
                            accent = accent,
                            onAccent = onAccent,
                            isPersian = isPersian,
                            onContinue = { goTo(STEP_REVIEW) }
                        )

                        else -> ReviewStep(
                            form = form,
                            isExpense = expense,
                            isEditMode = isEditMode,
                            accent = accent,
                            isPersian = isPersian,
                            canSave = canSave,
                            onEditName = { goTo(STEP_NAME) },
                            onEditIcon = { goTo(STEP_ICON) },
                            onConfirm = ::save
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
                onFinished()
            }
        )
    }
}

// -----------------------------------------------------------------------------
// نوار بالا
// -----------------------------------------------------------------------------

@Composable
private fun EditorTopBar(step: Int, accent: Color, isPersian: Boolean, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            if (step == STEP_NAME) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = tr(isPersian, "بستن", "Close"),
                    tint = scheme.onSurface
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr(isPersian, "مرحله‌ی قبل", "Previous step"),
                    tint = scheme.onSurface
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val color by animateColorAsState(
                    targetValue = if (index <= step) accent else scheme.onSurface.copy(alpha = 0.12f),
                    animationSpec = tween(300),
                    label = "categoryProgress$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
        // هم‌وزن با دکمه‌ی سمت مقابل تا نوار پیشرفت وسط بماند.
        Spacer(Modifier.width(48.dp))
    }
}

/** برچسب کوچکِ بالای هر مرحله: «دسته‌بندی جدید · هزینه» / «ویرایش دسته‌بندی · درآمد». */
@Composable
private fun TypeBadge(isExpense: Boolean, isEditMode: Boolean, accent: Color, isPersian: Boolean) {
    val kind = if (isExpense) tr(isPersian, "هزینه", "Expense") else tr(isPersian, "درآمد", "Income")
    val prefix = if (isEditMode) tr(isPersian, "ویرایش دسته‌بندی", "Edit category") else tr(isPersian, "دسته‌بندی جدید", "New category")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "$prefix · $kind",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۱: نام
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameStep(
    form: CategoryFormState,
    isExpense: Boolean,
    isEditMode: Boolean,
    suggestions: List<CategorySuggestion>,
    duplicate: Boolean,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onPickSuggestion: (CategorySuggestion) -> Unit,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val titleFocus = remember { FocusRequester() }
    // خطای «خالی» فقط بعد از زدن «ادامه» می‌آید؛ خطای «تکراری» همان لحظه‌ی تایپ دیده می‌شود.
    var showEmptyError by remember { mutableStateOf(false) }

    fun updateTitle(input: String) {
        if (input.length <= CategoryNameRules.MAX_TITLE_LENGTH) form.title = input
        if (input.isNotBlank()) showEmptyError = false
    }

    val errorText = when {
        duplicate -> tr(
            isPersian,
            "این نام قبلاً استفاده شده؛ یه اسم دیگه انتخاب کن",
            "This name is already in use; pick another one"
        )
        showEmptyError -> tr(
            isPersian,
            "اسم دسته‌بندی رو فراموش کردی؛ یه اسم کوتاه بنویس",
            "You forgot the name; add a short one"
        )
        else -> null
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
            TypeBadge(isExpense, isEditMode, accent, isPersian)
            Spacer(Modifier.height(14.dp))
            Text(
                text = tr(isPersian, "اسمش چیه؟", "What's it called?"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = form.title,
                onValueChange = ::updateTitle,
                label = { Text(tr(isPersian, "عنوان دسته‌بندی", "Category title")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                isError = errorText != null,
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = errorText.orEmpty(),
                            color = scheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${formatNumber(form.title.length.toLong(), isPersian)}/" +
                                    formatNumber(CategoryNameRules.MAX_TITLE_LENGTH.toLong(), isPersian),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    focusedLabelColor = accent,
                    cursorColor = accent
                ),
                trailingIcon = {
                    VoiceInputButton(
                        onResult = { spoken -> updateTitle(spoken.take(CategoryNameRules.MAX_TITLE_LENGTH)) },
                        language = if (isPersian) "fa-IR" else "en-US"
                    )
                }
            )

            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = tr(isPersian, "پیشنهادها؛ یکی رو بزن تا سریع‌تر پیش بری", "Suggestions: tap one to move faster"),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        val title = suggestion.title(isPersian)
                        SuggestionChip(
                            emoji = suggestion.emoji,
                            title = title,
                            selected = title == form.title.trim(),
                            accent = accent,
                            onClick = { onPickSuggestion(suggestion) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = true,
            accent = accent,
            onAccent = onAccent,
            onClick = {
                when {
                    form.title.isBlank() -> {
                        showEmptyError = true
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        titleFocus.requestFocus()
                    }
                    duplicate -> {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        titleFocus.requestFocus()
                    }
                    else -> onContinue()
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SuggestionChip(
    emoji: String,
    title: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else scheme.outline.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۲: آیکون روی کره
// -----------------------------------------------------------------------------

@Composable
private fun IconStep(
    form: CategoryFormState,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var showIconError by remember { mutableStateOf(false) }

    // آیکونِ ویرایش که بیرون از فهرست است هم روی کره می‌آید؛ فهرست فقط با عوض‌شدن آن دوباره ساخته می‌شود.
    val initialEmoji = remember { form.emoji }
    val icons = remember(initialEmoji) { CategoryIconCatalog.withExtra(initialEmoji) }
    val labels = remember(isPersian) {
        IconSphereLabels(
            description = tr(
                isPersian,
                "کره‌ی آیکون‌ها؛ برای چرخاندن بکشید و برای انتخاب لمس کنید",
                "Icon sphere: drag to rotate, tap to choose"
            ),
            next = tr(isPersian, "آیکون بعدی", "Next icon"),
            previous = tr(isPersian, "آیکون قبلی", "Previous icon")
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = tr(isPersian, "یه آیکون انتخاب کن", "Pick an icon"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            SelectedIconPill(
                emoji = form.emoji,
                title = form.title.trim(),
                accent = accent,
                isPersian = isPersian
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (showIconError) {
                        tr(isPersian, "یک آیکون انتخاب کن", "Pick an icon")
                    } else {
                        tr(isPersian, "هر طرف که خواستی بچرخون؛ لمس کن تا انتخاب بشه", "Spin it any way you like; tap to choose")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showIconError) scheme.error else scheme.onSurfaceVariant
                )
            }
        }

        // کره تمام عرض صفحه را می‌گیرد (بدون حاشیه‌ی ۲۴dp) تا آیکون‌های کناری بریده نشوند.
        IconSphere(
            icons = icons,
            selected = form.emoji.takeIf { it.isNotBlank() },
            accent = accent,
            labels = labels,
            onSelect = { emoji ->
                form.emoji = emoji
                form.iconManual = true
                showIconError = false
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                text = tr(isPersian, "ادامه", "Continue"),
                enabled = true,
                accent = accent,
                onAccent = onAccent,
                onClick = {
                    if (!form.hasIcon) {
                        showIconError = true
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    } else {
                        onContinue()
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** پیش‌نمایش زنده‌ی آیکون و نام کنار هم؛ تا آیکونی انتخاب نشده، جای‌نما نشان می‌دهد. */
@Composable
private fun SelectedIconPill(emoji: String, title: String, accent: Color, isPersian: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(accent.copy(alpha = if (emoji.isBlank()) 0.05f else 0.12f))
            .border(1.dp, accent.copy(alpha = if (emoji.isBlank()) 0.15f else 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(scheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji.ifBlank { "？" }, fontSize = 20.sp, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (emoji.isBlank()) {
                tr(isPersian, "هنوز آیکونی انتخاب نشده", "No icon chosen yet")
            } else {
                title
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (emoji.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۳: مرور و ثبت
// -----------------------------------------------------------------------------

@Composable
private fun ReviewStep(
    form: CategoryFormState,
    isExpense: Boolean,
    isEditMode: Boolean,
    accent: Color,
    isPersian: Boolean,
    canSave: Boolean,
    onEditName: () -> Unit,
    onEditIcon: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val kind = if (isExpense) tr(isPersian, "هزینه", "Expense") else tr(isPersian, "درآمد", "Income")

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
            Text(
                text = tr(isPersian, "همه‌چی درسته؟", "Look good?"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            // کارت خلاصه؛ لمس آن به ویرایش آیکون می‌رود.
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(accent.copy(alpha = 0.10f))
                    .border(1.dp, accent.copy(alpha = 0.25f), cardShape)
                    .clickable(onClick = onEditIcon)
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.5.dp, accent.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = form.emoji, fontSize = 44.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = form.title.trim(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isEditMode) {
                        tr(isPersian, "دسته‌بندی $kind", "$kind category")
                    } else {
                        tr(isPersian, "دسته‌بندی جدید $kind", "New $kind category")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            ReviewRow(
                label = tr(isPersian, "نام", "Name"),
                value = form.title.trim(),
                onClick = onEditName
            )
            ReviewRow(
                label = tr(isPersian, "آیکون", "Icon"),
                value = tr(isPersian, "تغییر آیکون", "Change icon"),
                leading = form.emoji,
                onClick = onEditIcon
            )
            ReviewRow(
                label = tr(isPersian, "نوع", "Type"),
                value = kind,
                onClick = null
            )
        }

        Spacer(Modifier.height(12.dp))

        SwipeToConfirmButton(
            text = if (isEditMode) {
                tr(isPersian, "برای ذخیره بکشید", "Swipe to save")
            } else {
                tr(isPersian, "برای ثبت بکشید", "Swipe to add")
            },
            isPersian = isPersian,
            resetTrigger = canSave,
            onConfirm = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            accentColor = accent
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReviewRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    leading: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            if (leading != null) {
                Text(text = leading, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
    }
}

// ---------------------------------------------------------------------------
// متن شیت‌های موفقیت
// ---------------------------------------------------------------------------

internal fun categoryAddedContent(
    title: String,
    emoji: String,
    isExpense: Boolean,
    isPersian: Boolean
): SuccessSheetContent =
    if (isPersian) {
        SuccessSheetContent(
            title = "دسته‌بندی اضافه شد",
            message = "دسته‌بندی «$emoji $title» به‌عنوان ${if (isExpense) "هزینه" else "درآمد"} با موفقیت اضافه شد.",
            hint = "از همین حالا هنگام ثبت تراکنش می‌توانید آن را انتخاب کنید."
        )
    } else {
        SuccessSheetContent(
            title = "Category added",
            message = "The ${if (isExpense) "expense" else "income"} category “$emoji $title” was added successfully.",
            hint = "You can pick it right away when recording a transaction."
        )
    }

internal fun categoryUpdatedContent(title: String, emoji: String, isPersian: Boolean): SuccessSheetContent =
    if (isPersian) {
        SuccessSheetContent(
            title = "دسته‌بندی به‌روزرسانی شد",
            message = "تغییرات دسته‌بندی «$emoji $title» با موفقیت ذخیره شد."
        )
    } else {
        SuccessSheetContent(
            title = "Category updated",
            message = "Your changes to “$emoji $title” were saved successfully."
        )
    }