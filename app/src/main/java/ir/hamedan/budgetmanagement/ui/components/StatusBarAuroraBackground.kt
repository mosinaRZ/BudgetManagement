package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun StatusBarAuroraBackground() {
    // رنگ‌ها با آلفای (شفافیت) بالاتر برای نوردهی خیلی بیشتر
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val backgroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = Modifier.fillMaxSize()) {
        // ۱. پر کردن کل صفحه با رنگ پس‌زمینه تیره
        drawRect(color = backgroundColor)

        // ۲. هاله اصلی شفق با پهنا و شعاع بیشتر (افزایش از 0.95 به 1.4)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor,
                    primaryColor.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(x = size.width * 0.5f, y = 0f),
                radius = size.width * 1.4f
            ),
            radius = size.width * 1.4f,
            center = Offset(x = size.width * 0.5f, y = 0f)
        )

        // ۳. هاله مکمل برای پهنای بیشتر در بالای صفحه (افزایش از 0.8 به 1.2)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor,
                    secondaryColor.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(x = size.width * 0.5f, y = size.height * 0.03f),
                radius = size.width * 1.2f
            ),
            radius = size.width * 1.2f,
            center = Offset(x = size.width * 0.5f, y = size.height * 0.03f)
        )
    }
}