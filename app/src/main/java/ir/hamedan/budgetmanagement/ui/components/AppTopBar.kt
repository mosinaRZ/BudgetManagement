package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.theme.AppDimens
import ir.hamedan.budgetmanagement.ui.theme.AppShapes
import ir.hamedan.budgetmanagement.ui.theme.AppSpacing

/**
 * ظاهر مشترک همه‌ی «پیل»های شناور بالای صفحه (تاپ‌بار، دکمه‌ی بازگشت، اکشن‌ها).
 * هر سه حالت دقیقاً یک پس‌زمینه، بوردر و گوشه دارند تا صفحات یک‌دست دیده شوند.
 */
@Composable
fun Modifier.appBarPill(shape: RoundedCornerShape = AppShapes.TopBar): Modifier {
    val colors = MaterialTheme.colorScheme
    return this
        .background(colors.surfaceVariant.copy(alpha = 0.75f), shape)
        .border(1.dp, colors.outline.copy(alpha = 0.15f), shape)
        .clip(shape)
}

/**
 * تاپ‌بار صفحه‌های تب‌دار (فقط عنوان). خودش فاصله‌ی status bar، پدینگ کناری استاندارد
 * و فاصله‌ی عمودی را اعمال می‌کند؛ پس صفحه‌ها نباید دوباره آن‌ها را اضافه کنند.
 */
@Composable
fun AppTitleTopBar(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = AppDimens.ScreenHorizontal,
                vertical = AppDimens.TopBarVerticalPadding
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.TopBarHeight)
                .appBarPill()
                .padding(horizontal = AppSpacing.Md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * تاپ‌بار زیرصفحه‌ها: دکمه‌ی بازگشت + عنوان (+ اکشن اختیاری انتهایی).
 * در RTL/LTR خودکار جهت می‌گیرد چون از Row و آیکن AutoMirrored استفاده می‌کند.
 */
@Composable
fun AppBackTopBar(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = AppDimens.ScreenHorizontal,
                vertical = AppDimens.TopBarVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.TopBarSlotGap)
    ) {
        AppBarIconSlot(onClick = onBack, contentDescription = backContentDescription) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backContentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(AppDimens.TopBarHeight)
                .appBarPill()
                .padding(horizontal = AppSpacing.Md),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (trailing != null) trailing()
    }
}

/** اسلات آیکنیِ ۵۶×۵۶ با ظاهر پیل (بازگشت، رفرش، …). */
@Composable
fun AppBarIconSlot(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(AppDimens.TopBarHeight)
            .appBarPill(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            content()
        }
    }
}

/** فاصله‌ی افقی کوچک بین اسلات‌ها در صورت نیاز به چیدمان دستی. */
@Composable
fun AppBarGap() {
    androidx.compose.foundation.layout.Spacer(Modifier.width(AppDimens.TopBarSlotGap))
}