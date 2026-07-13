package ir.hamedan.budgetmanagement.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Color Scheme
val LightColorScheme = lightColorScheme(
    // 🟢 رنگ‌های اصلی (Primary) - بر پایه رنگ‌های شما برای حالت روشن
    primary = Color(0xFF408A71),            // رنگ جدید ۲: سبز مریم‌گلی پخته برای دکمه‌ها و المان‌های فعال اصلی
    onPrimary = Color(0xFFFFFFFF),          // متن سفید روی رنگ اصلی برای کنتراست بالا
    primaryContainer = Color(0xFFB0E4CC),   // رنگ جدید ۱: سبز نعنایی روشن برای پس‌زمینه کارت‌ها و کپسول فعال
    onPrimaryContainer = Color(0xFF002114), // متن بسیار تیره روی کانتینر اصلی

    // ⚪ رنگ‌های دوم (Secondary) - رنگ‌های کمکی هماهنگ
    secondary = Color(0xFF4F6358),          // سبز خاکستری متوسط برای المان‌های درجه دو
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8DA), // پس‌زمینه فرعی روشن
    onSecondaryContainer = Color(0xFF0D1F16),

    // 🟠 رنگ‌های سوم (Tertiary) - رنگ مکمل (بسیار مناسب برای مبالغ منفی یا هشدارها در مدیریت مالی)
    tertiary = Color(0xFFA33500),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF380D00),

    // 🔴 رنگ‌های خطا (Error)
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // 📱 پس‌زمینه‌ها (Background & Surface) - روشن، تمیز و متمایل به نعنایی بسیار ملایم
    background = Color(0xFFF4FBF7),         // پس‌زمینه کل صفحات (سفیدِ نعنایی بسیار لایت که چشم را نمی‌زند)
    onBackground = Color(0xFF091413),       // رنگ جدید ۴: متن‌های اصلی کاملاً تیره و خوانا

    surface = Color(0xFFF4FBF7),            // سطح اصلی اجزا
    onSurface = Color(0xFF091413),          // رنگ جدید ۴: متن‌های روی سطوح
    surfaceVariant = Color(0xFFDBE5E0),     // رنگ سطوح فرعی (مثلاً پس‌زمینه کپسول باتم‌بار در حالت لایت)
    onSurfaceVariant = Color(0xFF3F4944),   // متن و آیکون‌های غیرفعال روی سطوح فرعی

    // 🔲 خطوط محیطی و مرزها (Outline)
    outline = Color(0xFF707974),            // برای بوردرها و خطوط جداکننده نازک
    outlineVariant = Color(0xFFBFC9C3),

    inverseSurface = Color(0xFF2D312E),
    inverseOnSurface = Color(0xFFEFF2EE),
    inversePrimary = Color(0xFFB0E4CC),

    // 🌫️ سایه‌ها و لایه‌های مختلف Surface (مخصوص Material 3)
    surfaceDim = Color(0xFFD5DBD7),
    surfaceBright = Color(0xFFF4FBF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5F1),
    surfaceContainer = Color(0xFFE9EFEB),
    surfaceContainerHigh = Color(0xFFE3E9E5),
    surfaceContainerHighest = Color(0xFFDDE4DF),
)
// Dark Color Scheme (پیشنهادی بر اساس توضیحات طراحی)
val DarkColorScheme = darkColorScheme(
    // 🟢 رنگ‌های اصلی (Primary) - بر پایه رنگ‌های جدید شما
    primary = Color(0xFFB0E4CC),            // رنگ جدید ۱: روشن‌ترین سبز برای المان‌های فعال
    onPrimary = Color(0xFF003825),          // متن و آیکون روی رنگ اصلی (سبز بسیار تیره برای خوانایی)
    primaryContainer = Color(0xFF285A48),   // رنگ جدید ۳: یشمی تیره برای باکس‌ها و کارت‌های اصلی
    onPrimaryContainer = Color(0xFFCEF1E1), // متن روی کانتینر اصلی

    // ⚪ رنگ‌های دوم (Secondary) - تعدیل شده برای هماهنگی با تم سبز شما
    secondary = Color(0xFFB4CCBF),          // سبز خاکستری ملایم برای المان‌های درجه دو
    onSecondary = Color(0xFF20352C),
    secondaryContainer = Color(0xFF408A71),  // رنگ جدید ۲: استفاده به عنوان کانتینر فرعی یا هایلایت‌ها
    onSecondaryContainer = Color(0xFFE2E2E2),

    // 🟠 رنگ‌های سوم (Tertiary) - رنگ مکمل (بسیار مناسب برای مبالغ منفی یا هشدارها در مدیریت مالی)
    tertiary = Color(0xFFFFB59B),
    onTertiary = Color(0xFF380D00),
    tertiaryContainer = Color(0xFF812800),
    onTertiaryContainer = Color(0xFFFFDBCF),

    // 🔴 رنگ‌های خطا (Error)
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // 📱 پس‌زمینه‌ها (Background & Surface) - با تم سبز دودی فوق‌العاده تاریک شما
    background = Color(0xFF091413),         // رنگ جدید ۴: پس‌زمینه کل صفحات (امولد با هاله سبز بسیار شیک)
    onBackground = Color(0xFFE1E3E0),       // متن‌های روی پس‌زمینه اصلی

    surface = Color(0xFF091413),            // رنگ جدید ۴: سطح اصلی اجزا
    onSurface = Color(0xFFE1E3E0),          // متن‌های روی سطوح
    surfaceVariant = Color(0xFF3F4944),     // رنگ سطوح فرعی مثل پس‌زمینه کپسول باتم‌بار شما
    onSurfaceVariant = Color(0xFFBFC9C3),   // متن و آیکون‌های غیرفعال روی سطوح فرعی

    // 🔲 خطوط محیطی و مرزها (Outline)
    outline = Color(0xFF89938E),            // برای بوردرها و خطوط جداکننده نازک
    outlineVariant = Color(0xFF3F4944),

    inverseSurface = Color(0xFFE1E3E0),
    inverseOnSurface = Color(0xFF2C312E),
    inversePrimary = Color(0xFF006D4B),
)