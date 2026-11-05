package ir.hamedan.budgetmanagement.ui.screens.add

import androidx.annotation.DrawableRes
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import ir.hamedan.budgetmanagement.di.appViewModel

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmount
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.utils.CategoryRules
import ir.hamedan.budgetmanagement.utils.CategorySuggestionHelper
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AddOptionItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    @DrawableRes val iconRes: Int,
    val descriptionFa: String,
    val descriptionEn: String,
    val route: String
)

// -----------------------------------------------------------------------------
// VisualTransformation دقیقاً مشابه فایل BudgetLimitScreen
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

/**
 * هاب «افزودن و مدیریت».
 *
 * ثبت تراکنش دیگر باتم‌شیت نیست؛ کارت اصلی با دو دکمه‌ی «هزینه» و «درآمد» مستقیم به جریان
 * تمام‌صفحه‌ی [AddTransactionScreen] می‌رود و نوع تراکنش از همین‌جا از پیش انتخاب می‌شود.
 */
@Composable
fun AddScreen(
    highlightId: String? = null,
    onBackClick: () -> Unit = {},
    onGoalsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onLimitsClick: () -> Unit = {},
    onDebtClick: () -> Unit = {},
    onAddTransactionClick: (isExpense: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val scheme = MaterialTheme.colorScheme

    var isAnimationActive by remember { mutableStateOf(false) }
    var consumedHighlightId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(highlightId) {
        if (highlightId != null && highlightId != consumedHighlightId) {
            consumedHighlightId = highlightId
            isAnimationActive = true
            delay(3200)
            isAnimationActive = false
        }
    }

    // یک انیمیشن نبض مشترک برای کارتِ هایلایت‌شده (نه یکی برای هر کارت).
    val pulse = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    val addOptions = remember {
        listOf(
            AddOptionItem(
                "transaction", "ثبت تراکنش", "Add Transaction",
                R.drawable.ic_transaction, "هزینه یا درآمد جدید را در چند ثانیه ثبت کنید", "Record an expense or income in seconds", "add_transaction"
            ),
            AddOptionItem(
                "piggy", "مدیریت قلک‌ها", "Manage Goals",
                R.drawable.ic_goal, "ایجاد یا ویرایش اهداف", "Create or edit savings targets", "add_goal"
            ),
            AddOptionItem(
                "category", "مدیریت دسته‌بندی‌ها", "Manage Categories",
                R.drawable.ic_category, "مدیریت دسته‌ها", "Manage categories", "add_category"
            ),
            AddOptionItem(
                "limit", "مدیریت محدودیت‌های مالی", "Manage Budget Limits",
                R.drawable.ic_limit, "تعیین یا ویرایش سقف بودجه", "Set or edit budget ceilings", "add_limit"
            ),
            AddOptionItem(
                "debtcredit", "بدهی و طلب", "Debts & Credits",
                R.drawable.ic_debtcredit, "مدیریت بدهی‌ها و طلب‌ها", "Manage debts and credits", "add_debtcredit"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        AuroraBackground()

        // ۱. گرید گزینه‌ها
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 180.dp, bottom = 140.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = addOptions,
                key = { it.id },
                span = { option ->
                    if (option.id == "transaction") GridItemSpan(2) else GridItemSpan(1)
                }
            ) { option ->
                val isTargetHighlight = (option.id == highlightId ||
                        (option.id == "transaction" && (highlightId == "buy" || highlightId == "sell"))) && isAnimationActive

                if (option.id == "transaction") {
                    TransactionHeroCard(
                        option = option,
                        isPersian = isPersian,
                        highlighted = isTargetHighlight,
                        borderAlpha = borderAlpha,
                        onAdd = onAddTransactionClick
                    )
                } else {
                    OptionTile(
                        option = option,
                        isPersian = isPersian,
                        highlighted = isTargetHighlight,
                        borderAlpha = borderAlpha,
                        onClick = {
                            when (option.id) {
                                "debtcredit" -> onDebtClick()
                                "piggy" -> onGoalsClick()
                                "category" -> onCategoriesClick()
                                "limit" -> onLimitsClick()
                            }
                        }
                    )
                }
            }
        }

        // ۲. هدر
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 8.dp)
        ) {
            Text(
                text = if (isPersian) "مدیریت و افزودن آیتم‌ها" else "Add & Manage Items",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPersian) "یکی از گزینه‌های زیر را انتخاب کنید" else "Select one of the options below",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }

        // ۳. بازگشت (ملایم؛ این یک دکمه‌ی ناوبری است، نه اکشن مخرب)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(scheme.background.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp)
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, scheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isPersian) "بازگشت" else "Back",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
            }
        }
    }
}

/** کارت اصلی؛ شامل دو میانبر مستقیم به جریان ثبت هزینه یا درآمد. */
@Composable
private fun TransactionHeroCard(
    option: AddOptionItem,
    isPersian: Boolean,
    highlighted: Boolean,
    borderAlpha: Float,
    onAdd: (isExpense: Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val borderColor = if (highlighted) scheme.primary.copy(alpha = borderAlpha) else scheme.primary.copy(alpha = 0.25f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primary.copy(alpha = 0.22f), scheme.tertiary.copy(alpha = 0.14f))
                )
            )
            .border(if (highlighted) 2.5.dp else 1.dp, borderColor, shape)
            .clickable { onAdd(true) }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                AddOptionIcon(option = option, tint = scheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) option.titleFa else option.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isPersian) option.descriptionFa else option.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroPill(
                label = if (isPersian) "هزینه جدید" else "New expense",
                container = scheme.tertiary,
                content = scheme.onTertiary,
                onClick = { onAdd(true) },
                modifier = Modifier.weight(1f)
            )
            HeroPill(
                label = if (isPersian) "درآمد جدید" else "New income",
                container = scheme.primary,
                content = scheme.onPrimary,
                onClick = { onAdd(false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1
        )
    }
}

/** کارت‌های مدیریتی؛ حالا با توضیح کوتاه زیر عنوان. */
@Composable
private fun OptionTile(
    option: AddOptionItem,
    isPersian: Boolean,
    highlighted: Boolean,
    borderAlpha: Float,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val borderColor = if (highlighted) scheme.primary.copy(alpha = borderAlpha) else scheme.outline.copy(alpha = 0.15f)
    val tint = if (highlighted) scheme.primary else scheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(
                if (highlighted) scheme.primary.copy(alpha = 0.08f) else scheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(if (highlighted) 2.5.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            AddOptionIcon(option = option, tint = tint)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isPersian) option.titleFa else option.titleEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isPersian) option.descriptionFa else option.descriptionEn,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AddOptionIcon(option: AddOptionItem, tint: Color) {
    Icon(
        painter = painterResource(id = option.iconRes),
        contentDescription = null,
        modifier = Modifier.size(36.dp),
        tint = tint
    )
}