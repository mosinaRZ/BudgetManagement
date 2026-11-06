package ir.hamedan.budgetmanagement.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * انیمیشن Lottie مشترک همه‌ی شیت‌های موفقیت (فایل داخل res/raw).
 * برای عوض کردن انیمیشن فقط همین یک خط را عوض کنید.
 */
private val SuccessLottieRes: Int get() = R.raw.success

/** شفافیت سطح شیت: هرچه کمتر، صفحه‌ی پشت شیت بیشتر دیده می‌شود. */
private const val SheetSurfaceAlpha = 0.84f

/** شدت شفق داخل شیت نسبت به شفق عادی برنامه (۱ = همان شدت صفحه‌های دیگر). */
private const val AuroraIntensityLight = 2.3f
private const val AuroraIntensityDark = 1.8f

/** ترتیب و فاصله‌ی ظاهر شدن خط‌های متن (میلی‌ثانیه، بعد از باز شدن کامل شیت). */
private const val TitleRevealDelay = 380
private const val MessageRevealDelay = 560
private const val HintRevealDelay = 740
private const val RevealDuration = 950

private val RevealEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * محتوای متنی شیت موفقیت.
 *
 * @property title تیتر کوتاه و قاطع («تراکنش ثبت شد»)
 * @property message توضیح یک‌جمله‌ای که دقیقاً می‌گوید چه چیزی ثبت شد
 * @property hint جمله‌ی کوچک و آرام پایانی (اختیاری)
 */
@Immutable
data class SuccessSheetContent(
    val title: String,
    val message: String,
    val hint: String? = null
)

/**
 * شیت موفقیت تمام‌صفحه.
 *
 * - تا بالاترین نقطه باز می‌شود و مثل یک صفحه‌ی مستقل دیده می‌شود.
 * - انیمیشن Lottie فقط یک‌بار، بعد از باز شدن کامل شیت، پخش می‌شود.
 * - متن‌ها به‌صورت پله‌ای (محو شدن + بالا آمدن + شفاف شدن از حالت تار) ظاهر می‌شوند.
 * - با دکمه‌ی ضربدر، یک‌بار زدن دکمه‌ی برگشت و کشیدن به پایین بسته می‌شود.
 *
 * @param onDismiss بعد از بسته شدن کامل شیت صدا زده می‌شود؛ والد باید state نمایش را false کند.
 * @param onFullyShown یک‌بار، لحظه‌ای که شیت کاملاً باز شد و فرم زیرین کاملاً پوشانده شده است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessBottomSheet(
    content: SuccessSheetContent,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onFullyShown: () -> Unit = {},
    // اگر داده شود، زیر متن‌ها یک دکمه‌ی «اشتراک‌گذاری» نمایش داده می‌شود.
    onShare: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val latestOnFullyShown by rememberUpdatedState(onFullyShown)
    var isShown by remember { mutableStateOf(false) }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }.first { it == SheetValue.Expanded }
        isShown = true
        latestOnFullyShown()
    }

    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    // روی شفق پررنگ، متن روشنِ تم تیره کنتراستش کم می‌شود؛ یک سایه‌ی نرم خوانایی را نگه می‌دارد.
    val textShadow = if (isDark) {
        Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 2f), blurRadius = 10f)
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = colors.onSurface,
        scrimColor = colors.scrim.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(colors.background.copy(alpha = SheetSurfaceAlpha))
        ) {
            AuroraBackground(
                intensity = if (isDark) AuroraIntensityDark else AuroraIntensityLight,
                drawBase = false
            )

            // نوار بالای شیت (دستگیره + دکمه‌ی بستن)؛ زیر نوار وضعیت می‌ماند، حتی اگر شیت تا لبه‌ی صفحه باز شود.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // دستگیره‌ی کشیدن
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(colors.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )

                // دکمه‌ی بستن
                IconButton(
                    onClick = closeSheet,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 20.dp)
                        .size(40.dp)
                        .border(1.dp, colors.outline.copy(alpha = 0.15f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = colors.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isPersian) "بستن" else "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(SuccessLottieRes)
                )

                // انیمیشن در لایه‌ی زیرین متن قرار می‌گیرد.
                val lottieProgress by animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = isShown,
                    restartOnPlay = false,
                    iterations = 1
                )

                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                // متن‌ها روی انیمیشن نمایش داده می‌شوند.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    RevealText(
                        text = content.title,
                        started = isShown,
                        delayMillis = TitleRevealDelay,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.35.em,
                            shadow = textShadow
                        ),
                        color = colors.onSurface
                    )

                    Spacer(Modifier.height(14.dp))

                    RevealText(
                        text = content.message,
                        started = isShown,
                        delayMillis = MessageRevealDelay,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 1.7.em,
                            shadow = textShadow
                        ),
                        color = colors.onSurface.copy(alpha = 0.85f)
                    )

                    content.hint?.let { hint ->
                        Spacer(Modifier.height(12.dp))
                        RevealText(
                            text = hint,
                            started = isShown,
                            delayMillis = HintRevealDelay,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 1.6.em,
                                shadow = textShadow
                            ),
                            color = colors.onSurface.copy(alpha = 0.62f)
                        )
                    }

                    if (onShare != null) {
                        // دکمه بعد از ظاهر شدن متن‌ها نرم وارد می‌شود تا حواس را از پیام اصلی پرت نکند.
                        var shareVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(isShown) {
                            if (isShown) {
                                delay(HintRevealDelay.toLong() + 350L)
                                shareVisible = true
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                        AnimatedVisibility(
                            visible = shareVisible,
                            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 }
                        ) {
                            FilledTonalButton(
                                onClick = onShare,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isPersian) "اشتراک‌گذاری" else "Share")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

/**
 * ظاهر شدن متن به سبک صفحه‌ی «به‌روزرسانی نرم‌افزار» سامسونگ:
 * از حالت تار و نامرئی، کمی پایین‌تر از جای نهایی، نرم محو می‌شود و در جای خود می‌نشیند.
 * تاری فقط روی Android 12 به بالا اعمال می‌شود؛ در نسخه‌های پایین‌تر بقیه‌ی افکت بدون تاری اجرا می‌شود.
 * اگر کاربر انیمیشن‌های سیستم را خاموش کرده باشد متن بی‌درنگ نمایش داده می‌شود.
 */
@Composable
private fun RevealText(
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
            progress.animateTo(1f, tween(durationMillis = RevealDuration, easing = RevealEasing))
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

/** مبلغ را (بر حسب واحد نمایشی کاربر، تومان یا ریال) همراه با نام واحد فرمت می‌کند. */
fun formatSuccessAmount(displayAmount: Long, currencyUnit: String, isPersian: Boolean): String {
    val locale = if (isPersian) Locale("fa", "IR") else Locale.US
    val unit = when {
        currencyUnit == MoneyContract.DISPLAY_RIAL && isPersian -> "ریال"
        currencyUnit == MoneyContract.DISPLAY_RIAL -> "Rial"
        isPersian -> "تومان"
        else -> "Toman"
    }
    return "${NumberFormat.getNumberInstance(locale).format(displayAmount)} $unit"
}

/** مبلغِ ذخیره‌شده (تومان) را به واحد نمایشی کاربر تبدیل و فرمت می‌کند. */
fun formatSuccessAmountFromToman(amountInToman: Long, currencyUnit: String, isPersian: Boolean): String =
    formatSuccessAmount(MoneyContract.displayFromStorage(amountInToman, currencyUnit), currencyUnit, isPersian)