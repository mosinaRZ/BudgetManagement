package ir.hamedan.budgetmanagement.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.ui.navigation.MainTabRoute

data class BottomNavItem(
    val route: MainTabRoute,
    val icon: ImageVector,
    @StringRes val labelRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = MainTabRoute.Home,
        icon = Icons.Default.Home,
        labelRes = R.string.nav_home
    ),
    BottomNavItem(
        route = MainTabRoute.Transactions,
        icon = Icons.Default.CompareArrows,
        labelRes = R.string.nav_transactions
    ),
    BottomNavItem(
        route = MainTabRoute.Analytics,
        icon = Icons.Default.BarChart,
        labelRes = R.string.nav_analytics
    ),
    BottomNavItem(
        route = MainTabRoute.Settings,
        icon = Icons.Default.Settings,
        labelRes = R.string.nav_settings
    )
)

@Composable
fun CapsuleBottomNavigation(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute?.contains(item.route::class.simpleName ?: "") == true

                NavigationBarItemCustom(
                    item = item,
                    isSelected = isSelected,
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
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        label = "itemColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = stringResource(item.labelRes),
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}