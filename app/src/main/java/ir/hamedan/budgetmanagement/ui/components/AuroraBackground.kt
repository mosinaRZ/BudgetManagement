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
fun AuroraBackground() {
    // گرفتن رنگ‌های اصلی و کمکی از تمی که با هم ساختیم
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)       // هاله سبز نعنایی ملایم
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) // هاله مریم‌گلی
    val backgroundColor = MaterialTheme.colorScheme.background                   // مشکی دودی پس‌زمینه شما

    Canvas(modifier = Modifier.fillMaxSize()) {
        // ۱. ابتدا کل صفحه را با رنگ پس‌زمینه تیره شما پر می‌کنیم
        drawRect(color = backgroundColor)

        // ۲. هاله رنگی اول (سبز نعنایی - متمایل به مرکز و بالا)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor, Color.Transparent),
                center = Offset(x = size.width * 0.4f, y = size.height * 0.35f),
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = Offset(x = size.width * 0.4f, y = size.height * 0.35f)
        )

        // ۳. هاله رنگی دوم (سبز مریم‌گلی - متمایل به مرکز و پایین برای مخلوط شدن رنگ‌ها)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor, Color.Transparent),
                center = Offset(x = size.width * 0.6f, y = size.height * 0.5f),
                radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(x = size.width * 0.6f, y = size.height * 0.5f)
        )
    }
}