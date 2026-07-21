package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.animateColorAsState
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
    val shape = RoundedCornerShape(24.dp) // کاهش انحنا برای فیت شدن بهتر با آیکون‌های عمودی

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 💡 نکته مهم: پدینگ‌های بیرونی و ناوبارپدینگ را برداشتم چون حالا در MainActivity داخل یک Row مدیریت می‌شوند
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), shape) // افزایش اندک آلفا برای خوانایی بهتر متن‌های زیر آن موقع اسکرول
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround, // تقسیم مساوی فضا بین آیتم‌ها
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
    // انیمیشن رنگ‌ها هنگام سوئیچ
    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary // رنگ سبز جدید شما برای آیتم فعال
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // خاکستری برای غیرفعال
        label = "itemColor"
    )

    // تغییر چیدمان به حالت عمودی (آیکون بالا، متن پایین)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp) // فاصله ظریف بین آیکون و متن
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        // متن همیشه در زیر آیکون قرار دارد بدون تکان خوردن انیمیشن افقی
        Text(
            text = if (isPersian) item.label else getEnglishLabel(item),
            style = MaterialTheme.typography.labelSmall, // فونت ریز استاندارد
            color = contentColor,
            maxLines = 1
        )
    }
}

private fun getEnglishLabel(item: BottomNavItem): String = when (item) {
    BottomNavItem.Home -> "Home"
    BottomNavItem.Transactions -> "Transactions"
    BottomNavItem.Analytics -> "Analytics"
    BottomNavItem.Settings -> "Settings"
}