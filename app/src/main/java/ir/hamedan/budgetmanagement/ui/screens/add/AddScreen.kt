package ir.hamedan.budgetmanagement.ui.screens.add

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.AppHapticType
import ir.hamedan.budgetmanagement.utils.rememberAppHaptics
import kotlinx.coroutines.delay

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
// (در فایل‌های دیگر import می‌شود؛ تغییر امضا/نام ممنوع)
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
 * - کارت اصلی (ثبت تراکنش) دو دکمهٔ صریح «هزینه» و «درآمد» دارد؛ خودِ کارت دیگر کلیک‌پذیر نیست
 *   تا لمس اتفاقی، یک هزینه‌ی ناخواسته شروع نکند.
 * - بخش‌های مدیریتی (قلک، دسته‌بندی، محدودیت، بدهی و طلب) به‌صورت ردیف‌های کامل‌عرض نمایش داده
 *   می‌شوند تا عنوان و توضیح فارسی هیچ‌وقت بریده نشود.
 * - تمام رنگ‌ها از [MaterialTheme.colorScheme] می‌آید (هزینه = error، درآمد = primary؛ مثل صفحهٔ خانه).
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
    val haptic = rememberAppHaptics()

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

    val transactionOption = remember {
        AddOptionItem(
            "transaction", "ثبت تراکنش", "Add Transaction",
            R.drawable.ic_transaction,
            "هزینه یا درآمد جدید را در چند ثانیه ثبت کنید",
            "Record an expense or income in seconds",
            "add_transaction"
        )
    }

    val manageOptions = remember {
        listOf(
            AddOptionItem(
                "piggy", "مدیریت قلک‌ها", "Manage Goals",
                R.drawable.ic_goal, "ایجاد یا ویرایش اهداف پس‌انداز", "Create or edit savings targets", "add_goal"
            ),
            AddOptionItem(
                "category", "مدیریت دسته‌بندی‌ها", "Manage Categories",
                R.drawable.ic_category, "دسته‌های هزینه و درآمد خود را بسازید", "Organize your expense and income categories", "add_category"
            ),
            AddOptionItem(
                "limit", "مدیریت محدودیت‌های مالی", "Manage Budget Limits",
                R.drawable.ic_limit, "برای خرج‌کردن سقف تعیین کنید", "Set spending ceilings that keep you on track", "add_limit"
            ),
            AddOptionItem(
                "debtcredit", "بدهی و طلب", "Debts & Credits",
                R.drawable.ic_debtcredit, "بدهی‌ها و طلب‌های خود را پیگیری کنید", "Keep track of what you owe and are owed", "add_debtcredit"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        AuroraBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            AddTopBar(isPersian = isPersian, onBackClick = {
                haptic.perform(AppHapticType.Click)
                onBackClick()
            })

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "transaction") {
                    val highlighted = isAnimationActive &&
                            (highlightId == "transaction" || highlightId == "buy" || highlightId == "sell")
                    TransactionHeroCard(
                        option = transactionOption,
                        isPersian = isPersian,
                        highlighted = highlighted,
                        onAdd = { isExpense ->
                            haptic.perform(AppHapticType.Click)
                            onAddTransactionClick(isExpense)
                        }
                    )
                }

                item(key = "section_title") {
                    SectionTitle(
                        text = if (isPersian) "مدیریت مالی" else "Manage your finances",
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }

                items(items = manageOptions, key = { it.id }) { option ->
                    OptionRow(
                        option = option,
                        isPersian = isPersian,
                        highlighted = isAnimationActive && option.id == highlightId,
                        onClick = {
                            haptic.perform(AppHapticType.Click)
                            when (option.id) {
                                "debtcredit" -> onDebtClick()
                                "piggy" -> onGoalsClick()
                                "category" -> onCategoriesClick()
                                "limit" -> onLimitsClick()
                            }
                        }
                    )
                }

                item(key = "bottom_inset") {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

// ───────────────────────────── اجزای صفحه ─────────────────────────────

@Composable
private fun AddTopBar(isPersian: Boolean, onBackClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .background(scheme.surfaceVariant.copy(alpha = 0.75f), CircleShape)
                .border(1.dp, scheme.outline.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isPersian) "بازگشت" else "Back",
                tint = scheme.onSurface
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPersian) "افزودن و مدیریت" else "Add & Manage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isPersian) "همه‌چیز برای مدیریت پول شما، یک‌جا"
                else "Everything for your money, in one place",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(scheme.outline.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )
    }
}

/** نبض مرز برای آیتم هایلایت‌شده؛ فقط وقتی [active] است انیمیشن اجرا می‌شود. */
@Composable
private fun rememberHighlightPulse(active: Boolean): Float {
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "highlight")
    val value by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "highlightAlpha"
    )
    return value
}

/** بازخورد لمسی: کارت هنگام فشردن کمی کوچک می‌شود و با فنر برمی‌گردد. */
@Composable
private fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** کارت اصلی؛ شامل دو میانبر مستقیم به جریان ثبت هزینه یا درآمد. */
@Composable
private fun TransactionHeroCard(
    option: AddOptionItem,
    isPersian: Boolean,
    highlighted: Boolean,
    onAdd: (isExpense: Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val pulse = rememberHighlightPulse(highlighted)
    val borderBrush = if (highlighted) {
        Brush.linearGradient(listOf(scheme.primary.copy(alpha = pulse), scheme.primary.copy(alpha = pulse)))
    } else {
        Brush.linearGradient(
            listOf(scheme.primary.copy(alpha = 0.45f), scheme.primary.copy(alpha = 0.08f))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primaryContainer.copy(alpha = 0.95f),
                        scheme.primaryContainer.copy(alpha = 0.55f)
                    )
                )
            )
            .border(if (highlighted) 2.5.dp else 1.dp, borderBrush, shape)
    ) {
        // نشان محوِ آیکون تراکنش؛ فقط یک عنصر تزئینی و کم‌رنگ برای عمق
        Icon(
            painter = painterResource(id = option.iconRes),
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-22).dp)
                .size(150.dp)
                .alpha(0.07f)
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = option.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = scheme.onPrimary
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) option.titleFa else option.titleEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isPersian) option.descriptionFa else option.descriptionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroPill(
                    label = if (isPersian) "هزینه جدید" else "New expense",
                    icon = Icons.Filled.ArrowUpward,
                    container = scheme.error,
                    content = scheme.onError,
                    onClick = { onAdd(true) },
                    modifier = Modifier.weight(1f)
                )
                HeroPill(
                    label = if (isPersian) "درآمد جدید" else "New income",
                    icon = Icons.Filled.ArrowDownward,
                    container = scheme.primary,
                    content = scheme.onPrimary,
                    onClick = { onAdd(false) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(50.dp)
            .pressScale(source, 0.95f)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ردیف مدیریتی؛ هم‌سبک کارت‌های تنظیمات (surfaceVariant + حاشیهٔ ظریف) با آیکون اختصاصی. */
@Composable
private fun OptionRow(
    option: AddOptionItem,
    isPersian: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val pulse = rememberHighlightPulse(highlighted)
    val source = remember { MutableInteractionSource() }

    val borderStroke = if (highlighted) {
        BorderStroke(2.5.dp, scheme.primary.copy(alpha = pulse))
    } else {
        BorderStroke(1.dp, scheme.outline.copy(alpha = 0.12f))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clip(shape)
            .background(
                if (highlighted) scheme.primary.copy(alpha = 0.10f)
                else scheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(borderStroke, shape)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.primary.copy(alpha = if (highlighted) 0.20f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = option.iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = scheme.primary
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPersian) option.titleFa else option.titleEn,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isPersian) option.descriptionFa else option.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(scheme.onSurface.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // در RTL خودکار به سمت چپ برمی‌گردد
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}