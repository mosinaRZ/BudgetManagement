package ir.hamedan.budgetmanagement.ui.components

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** شیت نیمه‌شفاف مثل شیت‌های موفقیت برنامه؛ همان شفق و همان ظاهر شدن پله‌ایِ متن‌ها. */
private const val UpdateSheetSurfaceAlpha = 0.84f
private const val UpdateAuroraIntensityLight = 2.3f
private const val UpdateAuroraIntensityDark = 1.8f

private const val UpdateTitleDelay = 380
private const val UpdateMessageDelay = 560
private const val UpdateVersionsDelay = 740
private const val UpdateButtonDelay = 1100L
private const val UpdateRevealDuration = 950

private val UpdateRevealEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * شیت اجباریِ «به‌روزرسانی» برای نسخه‌ای که دیگر پشتیبانی نمی‌شود.
 *
 * بسته نمی‌شود: ضربدر ندارد، کشیدن به پایین و لمس بیرونِ شیت اثری ندارد و دکمه‌ی بازگشت فقط
 * برنامه را به پس‌زمینه می‌برد. تنها راه رفتن، به‌روزرسانی است. شیت وقتی از صفحه برمی‌خیزد که
 * پدرش دیگر آن را نمایش ندهد (یعنی نسخه‌ی نصب‌شده دیگر قدیمی نباشد)؛ لمس دکمه فقط فروشگاه را
 * باز می‌کند و چیزی را نمی‌بندد.
 *
 * @param latestVersion آخرین نسخه‌ی منتشرشده؛ اگر هنوز معلوم نیست null.
 * @param checking درخواست بررسی سرور در جریان است.
 * @param onUpdateClick باز کردن صفحه‌ی به‌روزرسانی در فروشگاه.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForceUpdateBottomSheet(
    isPersian: Boolean,
    installedVersion: String,
    latestVersion: String?,
    checking: Boolean,
    onUpdateClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val context = LocalContext.current
    val animate = animationsEnabled()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // هیچ مسیری (کشیدن، لمس بیرون، back) نباید شیت را به حالت Hidden ببرد.
        confirmValueChange = { target -> target != SheetValue.Hidden }
    )
    var isShown by remember { mutableStateOf(false) }
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }.first { it == SheetValue.Expanded }
        isShown = true
    }
    var buttonVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isShown, animate) {
        if (isShown) {
            if (animate) delay(UpdateButtonDelay)
            buttonVisible = true
        }
    }

    val textShadow = if (isDark) {
        Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 2f), blurRadius = 10f)
    } else null

    fun digits(text: String): String =
        if (isPersian) TransactionShareFormatter.toPersianDigits(text) else text

    val title = if (isPersian) "به‌روزرسانی ضروری است" else "An update is required"
    val message = if (isPersian) {
        "این نسخه از سیدنا دیگر پشتیبانی نمی‌شود. برای ادامه و امنیت اطلاعاتتان، برنامه را به آخرین نسخه به‌روز کنید. اطلاعات شما سر جایش می‌ماند."
    } else {
        "This version of Cidna is no longer supported. Update to the latest version to continue and keep your data safe. Your data stays exactly where it is."
    }
    val buttonLabel = when {
        latestVersion != null && isPersian -> "به‌روزرسانی به نسخه‌ی ${digits(latestVersion)}"
        latestVersion != null -> "Update to version $latestVersion"
        isPersian -> "به‌روزرسانی برنامه"
        else -> "Update the app"
    }
    val hint = when {
        checking && isPersian -> "در حال بررسی…"
        checking -> "Checking…"
        isPersian -> "بعد از پایان نصب، برنامه به‌طور خودکار آماده‌ی استفاده است."
        else -> "Once the installation finishes, the app is ready to use."
    }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        containerColor = Color.Transparent,
        contentColor = colors.onSurface,
        scrimColor = colors.scrim.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        // Back must not strand the user, nor leave an unusable screen open: send the app to the background.
        BackHandler(enabled = true) { (context as? Activity)?.moveTaskToBack(true) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(colors.background.copy(alpha = UpdateSheetSurfaceAlpha))
        ) {
            AuroraBackground(
                intensity = if (isDark) UpdateAuroraIntensityDark else UpdateAuroraIntensityLight,
                drawBase = false
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UpdateBadge(started = isShown, animate = animate)

                Spacer(Modifier.height(28.dp))

                UpdateRevealText(
                    text = title,
                    started = isShown,
                    delayMillis = UpdateTitleDelay,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.35.em,
                        shadow = textShadow
                    ),
                    color = colors.onSurface,
                    // TalkBack announces the title when the sheet appears.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )

                Spacer(Modifier.height(14.dp))

                UpdateRevealText(
                    text = message,
                    started = isShown,
                    delayMillis = UpdateMessageDelay,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 1.7.em,
                        shadow = textShadow
                    ),
                    color = colors.onSurface.copy(alpha = 0.85f)
                )

                Spacer(Modifier.height(20.dp))

                UpdateRevealBox(started = isShown, delayMillis = UpdateVersionsDelay) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            VersionLabel(
                                caption = if (isPersian) "نسخه‌ی شما" else "Your version",
                                value = digits(installedVersion)
                            )
                            if (latestVersion != null) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                VersionLabel(
                                    caption = if (isPersian) "نسخه‌ی جدید" else "New version",
                                    value = digits(latestVersion),
                                    emphasized = true
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = buttonVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // فقط فروشگاه را باز می‌کند؛ هیچ‌چیز را نمی‌بندد.
                        Button(
                            onClick = onUpdateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = buttonLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.onSurface.copy(alpha = 0.62f)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 1.6.em,
                                    shadow = textShadow
                                ),
                                color = colors.onSurface.copy(alpha = 0.62f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/** دایره‌ی گرادیانی با فلش رو به بالا و دو حلقه‌ی نرمِ نبضی دورش. */
@Composable
private fun UpdateBadge(started: Boolean, animate: Boolean) {
    val colors = MaterialTheme.colorScheme
    val appear = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(started, animate) {
        if (started && animate) appear.animateTo(1f, tween(700, easing = UpdateRevealEasing))
    }
    val pulse by rememberInfiniteTransition(label = "update-badge").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "update-badge-pulse"
    )

    Box(
        modifier = Modifier
            .size(156.dp)
            .graphicsLayer {
                val p = appear.value
                alpha = p
                scaleX = 0.8f + 0.2f * p
                scaleY = 0.8f + 0.2f * p
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            if (animate) {
                repeat(2) { ring ->
                    val p = (pulse + ring * 0.5f) % 1f
                    drawCircle(
                        color = colors.primary.copy(alpha = (1f - p) * 0.28f),
                        radius = maxRadius * (0.55f + 0.45f * p)
                    )
                }
            } else {
                drawCircle(color = colors.primary.copy(alpha = 0.14f), radius = maxRadius * 0.8f)
            }
        }
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(
                    Brush.linearGradient(listOf(colors.primary, lerp(colors.primary, colors.tertiary, 0.6f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun VersionLabel(caption: String, value: String, emphasized: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) colors.primary else colors.onSurface
        )
    }
}

/** محو و بالا آمدنِ نرم برای هر محتوایی (همان حرکت متن‌ها). */
@Composable
private fun UpdateRevealBox(started: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    val animate = animationsEnabled()
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(started, animate) {
        if (started && animate) {
            delay(delayMillis.toLong())
            progress.animateTo(1f, tween(durationMillis = UpdateRevealDuration, easing = UpdateRevealEasing))
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * 18.dp.toPx()
        }
    ) { content() }
}

/**
 * ظاهر شدنِ متن از حالت تار و نامرئی (تاری فقط روی Android 12+). اگر انیمیشن‌های سیستم خاموش
 * باشد متن بی‌درنگ نمایش داده می‌شود.
 */
@Composable
private fun UpdateRevealText(
    text: String,
    started: Boolean,
    delayMillis: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animate = animationsEnabled()
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(started, animate) {
        if (started && animate) {
            delay(delayMillis.toLong())
            progress.animateTo(1f, tween(durationMillis = UpdateRevealDuration, easing = UpdateRevealEasing))
        }
    }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = progress.value
                alpha = p
                translationY = (1f - p) * 18.dp.toPx()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && p < 1f) {
                    val radius = (1f - p) * 16.dp.toPx()
                    renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                }
            }
    )
}