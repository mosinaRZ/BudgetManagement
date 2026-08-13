package ir.hamedan.budgetmanagement.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.preferences.NotificationCategory
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.StatusBarAuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale

@Composable
fun NotificationCalibrationScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()

    LaunchedEffect(Unit) {
        NotificationPreferences.ensureDefaultsInitialized(context)
    }

    var typeStates by remember {
        mutableStateOf(NotificationPreferences.getAllTypeStates(context))
    }

    fun refreshStates() {
        typeStates = NotificationPreferences.getAllTypeStates(context)
    }

    fun setTypeEnabled(type: NotificationType, enabled: Boolean) {
        NotificationPreferences.setTypeEnabled(context, type, enabled)
        refreshStates()
    }

    fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        NotificationPreferences.setCategoryEnabled(context, category, enabled)
        refreshStates()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (isPersian)
                        "هر نوع اعلان را جداگانه فعال یا غیرفعال کنید. تغییرات بلافاصله اعمال می‌شوند."
                    else
                        "Enable or disable each notification type individually. Changes apply immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            NotificationCategory.entries.forEach { category ->
                val typesInCategory = NotificationType.entries.filter { it.category == category }
                val allEnabled = typesInCategory.all { typeStates[it] == true }

                item(key = "header_$category") {
                    NotificationCategoryHeader(
                        category = category,
                        isPersian = isPersian,
                        allEnabled = allEnabled,
                        onToggleAll = { enabled -> setCategoryEnabled(category, enabled) }
                    )
                }

                items(
                    count = typesInCategory.size,
                    key = { index -> typesInCategory[index].name }
                ) { index ->
                    val type = typesInCategory[index]
                    NotificationTypeToggleRow(
                        type = type,
                        isPersian = isPersian,
                        checked = typeStates[type] == true,
                        onCheckedChange = { enabled -> setTypeEnabled(type, enabled) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isPersian) "بازگشت" else "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) "کالیبراسیون اعلان‌ها" else "Notification Calibration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) "مدیریت دقیق انواع اعلان" else "Fine-tune notification types",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCategoryHeader(
    category: NotificationCategory,
    isPersian: Boolean,
    allEnabled: Boolean,
    onToggleAll: (Boolean) -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), cardShape)
            .clip(cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = if (isPersian) category.titleFa else category.titleEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPersian) "همه" else "All",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 6.dp)
            )
            Switch(
                checked = allEnabled,
                onCheckedChange = onToggleAll,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun NotificationTypeToggleRow(
    type: NotificationType,
    isPersian: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), cardShape)
            .clip(cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = if (isPersian) type.titleFa else type.titleEn,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isPersian) type.descriptionFa else type.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

private fun categoryIcon(category: NotificationCategory): ImageVector = when (category) {
    NotificationCategory.TRANSACTIONS -> Icons.Default.ReceiptLong
    NotificationCategory.SMS -> Icons.Default.Sms
    NotificationCategory.CATEGORIES -> Icons.Default.Category
    NotificationCategory.BUDGET -> Icons.Default.Speed
    NotificationCategory.GOALS -> Icons.Default.Savings
    NotificationCategory.DEBT_CREDIT -> Icons.Default.AccountBalance
    NotificationCategory.REMINDERS -> Icons.Default.Alarm
    NotificationCategory.SYSTEM -> Icons.Default.Settings
}
