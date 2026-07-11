package ir.hamedan.budgetmanagement.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : BottomNavItem("home", Icons.Default.Home, "خانه")
    data object Transactions : BottomNavItem("transactions", Icons.Default.CompareArrows, "تراکنش‌ها")
    data object Analytics : BottomNavItem("analytics", Icons.Default.BarChart, "آنالیز")
    data object Settings : BottomNavItem("settings", Icons.Default.Settings, "تنظیمات")
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Transactions,
    BottomNavItem.Analytics,
    BottomNavItem.Settings
)

@Composable
fun CapsuleBottomNavigation(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit
) {
    val isPersian = isPersianLocale()
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .shadow(8.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItemCustom(
                    item = item,
                    isSelected = isSelected,
                    isPersian = isPersian,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun NavigationBarItemCustom(
    item: BottomNavItem,
    isSelected: Boolean,
    isPersian: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "itemBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        label = "itemColor"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Text(
                text = if (isPersian) item.label else getEnglishLabel(item),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun getEnglishLabel(item: BottomNavItem): String = when (item) {
    BottomNavItem.Home -> "Home"
    BottomNavItem.Transactions -> "Transactions"
    BottomNavItem.Analytics -> "Analytics"
    BottomNavItem.Settings -> "Settings"
}