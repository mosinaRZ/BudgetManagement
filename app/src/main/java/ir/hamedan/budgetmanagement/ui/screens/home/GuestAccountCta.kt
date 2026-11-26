package ir.hamedan.budgetmanagement.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

/**
 * Call-to-action shown on the home screen of a guest only. It never goes away on its own and has
 * no close button: the guest workspace lives on this phone alone, so the card keeps pointing out
 * what an account adds (a cloud copy that survives a lost phone, a reinstall or a new device) and
 * moves the guest straight to registration with one tap.
 *
 * It is personal where it can be: once the guest has recorded transactions the headline names how
 * many of them exist only on this phone, which is the real reason to create an account.
 *
 * Motion is subtle and constant: a light sweep across the card, a breathing badge and a nudging
 * arrow on the button.
 *
 * @param entryCount number of transactions the guest has recorded (0 shows the generic copy).
 */
@Composable
fun GuestAccountCtaCard(
    isPersian: Boolean,
    entryCount: Int,
    numberFormatter: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val transition = rememberInfiniteTransition(label = "guest_cta")

    // A soft band of light that crosses the card, then rests for a moment.
    val sweep by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4200
                -0.4f at 0 using LinearEasing
                1.4f at 1700 using FastOutSlowInEasing
                1.4f at 4200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    // The badge breathes.
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    // The arrow on the button nudges toward the reading direction.
    val nudge by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nudge"
    )

    val gradient = Brush.linearGradient(listOf(colors.primary, colors.secondary))
    val onGradient = colors.onPrimary

    val headline = when {
        entryCount > 0 -> if (isPersian) {
            "${numberFormatter.format(entryCount.toLong())} تراکنش تو فقط روی همین گوشیه!"
        } else {
            "${numberFormatter.format(entryCount.toLong())} of your entries live only on this phone!"
        }
        else -> if (isPersian) "حسابت رو بساز و دادهٔ مالی‌ات رو بیمه کن" else "Create an account and insure your data"
    }
    val subline = if (isPersian) {
        "گوشی گم شه یا برنامه پاک شه، همه‌چیز می‌پره. با یک حساب، دادهٔ فعلی‌ات منتقل و امن می‌مونه."
    } else {
        "Lose the phone or reinstall the app and it is all gone. With an account, your current data moves over and stays safe."
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .drawWithContent {
                drawContent()
                // The sweeping band of light, painted over the card content.
                val bandWidth = size.width * 0.35f
                val startX = size.width * sweep - bandWidth / 2f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(startX, 0f),
                        end = androidx.compose.ui.geometry.Offset(startX + bandWidth, size.height * 0.35f)
                    )
                )
            }
    ) {
        // Decorative translucent rings in the corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(120.dp)
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 28.dp)
                .size(76.dp)
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = breathe
                            scaleY = breathe
                        }
                        .background(Color.White.copy(alpha = 0.20f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = onGradient,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = onGradient
                    )
                    Text(
                        text = subline,
                        style = MaterialTheme.typography.bodySmall,
                        color = onGradient.copy(alpha = 0.95f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // What an account adds, in three short chips.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BenefitChip(
                    icon = Icons.Filled.CloudDone,
                    text = if (isPersian) "نسخهٔ ابری" else "Cloud copy",
                    tint = onGradient,
                    modifier = Modifier.weight(1f)
                )
                BenefitChip(
                    icon = Icons.Filled.PhoneAndroid,
                    text = if (isPersian) "چند دستگاه" else "Any device",
                    tint = onGradient,
                    modifier = Modifier.weight(1f)
                )
                BenefitChip(
                    icon = Icons.Filled.Shield,
                    text = if (isPersian) "امن و رمزنگاری‌شده" else "Encrypted",
                    tint = onGradient,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // The action: a light pill on the gradient so it stands out as the thing to tap.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(onGradient)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isPersian) "ساخت حساب کاربری و انتقال دادهٔ من" else "Create my account & keep my data",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { translationX = nudge * density * (if (isRtl) -1f else 1f) }
                )
            }
        }
    }
}

@Composable
private fun BenefitChip(
    icon: ImageVector,
    text: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}