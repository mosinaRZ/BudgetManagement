package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun StatusBarAuroraBackground() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val backgroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = Modifier.fillMaxSize()) {
        // ۱. پر کردن کل صفحه با رنگ پس‌زمینه
        drawRect(color = backgroundColor)

        // محاسبه ارتفاع دقیق یک‌سوم صفحه
        val oneThirdHeight = size.height / 3f

        // ۲. رسم مستطیل شفق اصلی در یک‌سوم بالای صفحه
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.85f), // نور قوی در بالاترین قسمت
                    primaryColor.copy(alpha = 0.35f),
                    Color.Transparent                 // محو شدن کامل در انتهای یک‌سوم
                ),
                startY = 0f,
                endY = oneThirdHeight
            ),
            topLeft = Offset.Zero,
            size = Size(width = size.width, height = oneThirdHeight)
        )

        // ۳. لایه شفق مکمل برای عمق‌بخشی بیشتر و ترکیب رنگ نرم‌تر
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    secondaryColor,
                    secondaryColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = oneThirdHeight
            ),
            topLeft = Offset.Zero,
            size = Size(width = size.width, height = oneThirdHeight)
        )
    }
}