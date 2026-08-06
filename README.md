# Budget Management (مدیریت بودجه)

اپلیکیشن اندرویدی مدیریت بودجه شخصی با Jetpack Compose + Clean Architecture.

## ویژگی‌های اصلی
- ثبت تراکنش درآمد/هزینه
- دسته‌بندی‌ها، اهداف پس‌انداز، محدودیت بودجه
- **دریافت خودکار تراکنش از پیامک بانکی** (پارسر SMS + SmsReceiver)
- ویجت موجودی + نوتیفیکیشن
- گفتار به متن (ویس به متن)
- خروجی Excel (Xlsx) + PDF
- پشتیبانی کامل فارسی + انگلیسی + تم روشن/تاریک

## وضعیت پروژه
- معماری در حال بازنویسی و بهبود است (AppContainer + Hilt + Clean Architecture)
- بخش لاگین فعلاً نمایشی است و در آینده با بک‌اند Go جایگزین می‌شود
- نسخه فعلی: 1.0.1 (در حال توسعه)

## پیش‌نیازها
- Android Studio Ladybug یا جدیدتر
- JDK 11+
- minSdk 26

## نحوه اجرا
1. کلون کنید
2. Gradle Sync بزنید
3. روی دستگاه/امولاتور اجرا کنید

## ساختار پروژه
app/
├── data/          # Room + Preferences + Repository + security
├── ui/            # Compose Screens + Components + Theme
├── di/            # Hilt + AppContainer
├── utils/         # SmsParser, ExportManager, Biometric...
├── worker/        # WorkManager
└── security/      # DatabaseKeyProvider, BiometricPromptManager


## امنیت
- دیتابیس کاملاً رمزنگاری شده با SQLCipher
- رمزنگاری SharedPreferences با androidx.security:security-crypto
- هاشینگ پسورد + بیومتریک (در حال تکمیل)

## صادرات داده
- Excel + PDF با آمار دقیق (موجودی، درآمد، هزینه، میانگین روزانه)

## نقشه راه
- [ ] بک‌اند کامل با Go + API
- [ ] تست‌های واحد + UI
- [ ] CI/CD با GitHub Actions
- [ ] انتشار در Google Play

## مشارکت
- Fork بزنید
- Branch جدید ایجاد کنید
- Pull Request بدهید