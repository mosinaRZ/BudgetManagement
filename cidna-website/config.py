# -*- coding: utf-8 -*-
"""
تنها فایلی که برای انتشار و به‌روزرسانی روزمره باید ویرایش کنید.
Single place for everything that changes: domain, store links, socials, versions, legal dates.
"""

# ── ۱) دامنه ─────────────────────────────────────────────────────────────
DOMAIN = "https://cidna.ir"          # ← دامنهٔ واقعی (بدون / انتهایی). canonical، hreflang، sitemap و nginx خودکار ساخته می‌شوند.
TODAY  = "2026-10-29"                 # ← تاریخ آخرین تغییر محتوا (برای sitemap و schema)

# ── ۲) لینک‌های دریافت ───────────────────────────────────────────────────
# فروشگاهی که url=None دارد در سایت اصلاً نمایش داده نمی‌شود (نه دکمه، نه متن «به‌زودی»).
STORES = [
    dict(id="bazaar", fa="کافه‌بازار",   en="Cafe Bazaar",
         url="https://cafebazaar.ir/app/?id=ir.hamedan.budgetmanagement&ref=share"),
    dict(id="direct", fa="دانلود مستقیم", en="Direct download",
         url="/downloads/budget-app.apk",   # فایل روی سرور: /var/www/cidna/downloads/budget-app.apk
         sha256="acee32d1e36c15d32dba733353acb738b20235773dbfa02b75dceb6f68c11462",                    # ← خروجی `sha256sum budget-app.apk` را اینجا بگذارید؛ در صفحهٔ دریافت نمایش داده می‌شود
         size="17 MB"),
    dict(id="myket",  fa="مایکت",       en="Myket",       url=None),       # ← لینک مایکت را که گذاشتید، دکمه خودکار ظاهر می‌شود
]
APP_VERSION  = "1.1.4"                   # از app/build.gradle.kts (versionName)
MIN_ANDROID  = "8.0"                     # minSdk 26
SOURCE_URL   = None                      # ← لینک کد منبع (AGPL-3.0 §13 اجبار می‌کند برای نسخهٔ اصلاح‌شدهٔ سرویس شبکه‌ای، سورس در دسترس باشد)

# ── ۳) راه‌های ارتباطی؛ عیناً همان‌هایی که در «درباره ما و پشتیبانی» اپ هست ──
EMAIL      = "cidna.app@gmail.com"
WHATSAPP   = "https://wa.me/989180500841"
X_URL      = "https://twitter.com/CidnaApp"
INSTAGRAM  = "https://www.instagram.com/cidnaapp"

# ── ۴) متن‌های حقوقی (باید با legal_handler.go بک‌اند یکی باشد) ───────────
LEGAL_VERSION     = "1.0"
LEGAL_UPDATED_FA  = "۲۶ مهر ۱۴۰۵"
LEGAL_UPDATED_EN  = "18 October 2026"
LEGAL_UPDATED_ISO = "2026-10-18"

PROVIDER_FA = "محمدسینا رمضانی عهدیه"
PROVIDER_EN = "Muhammed Cenna Ramezani Ahdieh"