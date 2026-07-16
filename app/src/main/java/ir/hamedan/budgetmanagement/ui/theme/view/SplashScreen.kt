package ir.hamedan.budgetmanagement.ui.theme.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_anim))

    // گرفتن ورژن برنامه و تبدیل ارقام آن در صورت فارسی بودن زبان برنامه
    val versionName = remember(context, isPersian) {
        val rawVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        if (isPersian) {
            // تبدیل اعداد انگلیسی به فارسی برای هماهنگی کامل بصری
            rawVersion
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹')
        } else {
            rawVersion
        }
    }

    // ۱. انیمیشن به صورت بی‌نهایت پخش شود تا زمانی که تایمر ما تمام شود
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // ۲. مدیریت زمان نمایش با استفاده از delay (به میلی‌ثانیه)
    LaunchedEffect(Unit) {
        delay(3000) // تغییر این مقدار زمان نمایش را کم و زیاد می‌کند (۳۰۰۰ یعنی ۳ ثانیه)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )

        // نمایش ورژن دوزبانه هماهنگ با زبان برنامه
        Text(
            text = if (isPersian) "نسخه $versionName" else "Version $versionName",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        )
    }
}