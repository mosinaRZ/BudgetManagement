package ir.hamedan.budgetmanagement.ui.screens.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.data.preferences.CategoryAddHintPolicy
import ir.hamedan.budgetmanagement.data.preferences.CategoryHintPreferences
import kotlinx.coroutines.delay

/**
 * دکمه‌ی شناور «+» دسته‌بندی‌ها همراه با نکته‌ی دوره‌ای.
 *
 * هر از چندگاهی (طبق [CategoryAddHintPolicy]) دو موج نرم از دکمه پخش می‌شود و حبابی با فلشِ
 * اشاره‌کننده بالای آن باز می‌شود که می‌گوید از همین‌جا دسته‌ی جدید بساز؛ بعد از چند ثانیه خودش
 * جمع می‌شود. لمس دکمه یا حباب هر دو مستقیم به ساخت دسته می‌رود.
 *
 * @param hintEnabled وقتی روی صفحه دیالوگ/شیت باز است یا چیزی مهم‌تر در جریان است، false بدهید.
 */
@Composable
internal fun AddCategoryFab(
    isExpense: Boolean,
    isPersian: Boolean,
    hintEnabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hintVisible by remember { mutableStateOf(false) }
    val latestOnClick by rememberUpdatedState(onClick)

    // با عوض‌شدن hintEnabled (مثلاً باز شدن دیالوگ حذف) زمان‌بندی از اول شروع می‌شود و
    // نکته‌ی باز بلافاصله بسته می‌شود.
    LaunchedEffect(hintEnabled) {
        hintVisible = false
        if (!hintEnabled) return@LaunchedEffect
        var shownThisVisit = 0
        while (CategoryAddHintPolicy.shouldShow(
                shownThisVisit,
                CategoryHintPreferences.totalShown(context),
                CategoryHintPreferences.fabUses(context)
            )
        ) {
            delay(CategoryAddHintPolicy.delayBeforeNext(shownThisVisit))
            hintVisible = true
            shownThisVisit++
            CategoryHintPreferences.recordShown(context)
            delay(CategoryAddHintPolicy.VISIBLE_MS)
            hintVisible = false
        }
    }

    fun open() {
        hintVisible = false
        CategoryHintPreferences.recordFabUse(context)
        latestOnClick()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        AddHintBubble(
            visible = hintVisible,
            text = if (isPersian) {
                if (isExpense) "اینجا می‌تونی یه دسته‌بندی هزینه‌ی جدید بسازی" else "اینجا می‌تونی یه دسته‌بندی درآمد جدید بسازی"
            } else {
                if (isExpense) "Tap here to add a new expense category" else "Tap here to add a new income category"
            },
            onClick = ::open
        )

        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hintVisible) {
                PulseRings(color = containerColor, modifier = Modifier.matchParentSize())
            }
            FloatingActionButton(
                onClick = ::open,
                containerColor = containerColor,
                contentColor = contentColor,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isPersian) "افزودن دسته‌بندی" else "Add category",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/** دو موجِ دایره‌ایِ پخش‌شونده پشت دکمه (بیرون از کادر دکمه هم کشیده می‌شود، چون برش ندارد). */
@Composable
private fun PulseRings(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fabPulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "fabPulsePhase"
    )
    Spacer(
        modifier = modifier.drawBehind {
            val base = size.minDimension / 2f
            for (shift in listOf(0f, 0.5f)) {
                val eased = FastOutSlowInEasing.transform((phase + shift) % 1f)
                drawCircle(
                    color = color.copy(alpha = 0.38f * (1f - eased)),
                    radius = base * (1f + 0.85f * eased)
                )
            }
        }
    )
}

@Composable
private fun AddHintBubble(visible: Boolean, text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val origin = TransformOrigin(pivotFractionX = if (rtl) 0f else 1f, pivotFractionY = 1f)
    val shape = RoundedCornerShape(22.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + scaleIn(tween(360, easing = FastOutSlowInEasing), initialScale = 0.55f, transformOrigin = origin),
        exit = fadeOut(tween(200)) + scaleOut(tween(240), targetScale = 0.7f, transformOrigin = origin)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Row(
                modifier = Modifier
                    .widthIn(max = 264.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = scheme.primary.copy(alpha = 0.22f),
                        spotColor = scheme.primary.copy(alpha = 0.30f)
                    )
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                scheme.surface.copy(alpha = 0.97f),
                                scheme.surfaceVariant.copy(alpha = 0.94f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                scheme.primary.copy(alpha = 0.50f),
                                scheme.onSurface.copy(alpha = 0.10f),
                                scheme.primary.copy(alpha = 0.24f)
                            )
                        ),
                        shape = shape
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    color = scheme.onSurface,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // فلشِ حباب؛ مرکزش دقیقاً بالای مرکز دکمه‌ی ۵۶dp است (۲۸dp از لبه‌ی انتهایی).
            val arrowColor = scheme.surfaceVariant.copy(alpha = 0.94f)
            val arrowBorder = scheme.primary.copy(alpha = 0.30f)
            Box(
                modifier = Modifier
                    .padding(end = 20.dp)
                    .width(16.dp)
                    .height(8.dp)
                    .drawBehind {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, arrowColor)
                        val stroke = 1.dp.toPx()
                        drawLine(arrowBorder, Offset(0f, 0f), Offset(size.width / 2f, size.height), stroke)
                        drawLine(arrowBorder, Offset(size.width, 0f), Offset(size.width / 2f, size.height), stroke)
                    }
                    .clearAndSetSemantics { }
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}