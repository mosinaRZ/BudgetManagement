# -*- coding: utf-8 -*-
"""
تنها فایلی که برای انتشار و به‌روزرسانی روزمره باید ویرایش کنید.
Single place for everything that changes: domain, store links, socials, versions, legal dates.
"""

# ── ۱) دامنه ─────────────────────────────────────────────────────────────
DOMAIN = "https://cidna.ir"          # ← دامنهٔ واقعی (بدون / انتهایی). canonical، hreflang، sitemap و nginx خودکار ساخته می‌شوند.
TODAY  = "2026-10-24"                 # ← تاریخ آخرین تغییر محتوا (برای sitemap و schema)

# ── ۲) لینک‌های دریافت ───────────────────────────────────────────────────
# تا وقتی url=None است، دکمه به شکل «به‌زودی» نمایش داده می‌شود. لینک را که گذاشتید، همان لحظه فعال می‌شود.
STORES = [
    dict(id="myket",  fa="مایکت",       en="Myket",       url=None),
    dict(id="bazaar", fa="کافه‌بازار",   en="Cafe Bazaar", url=None),
    dict(id="direct", fa="دانلود مستقیم", en="Direct download", url=None,   # مثلاً "/downloads/cidna-1.1.2.apk"
         sha256=None,                    # ← پس از ساخت APK، هش SHA-256 را اینجا بگذارید؛ در صفحهٔ دریافت نمایش داده می‌شود
         size=None),                     # ← مثلاً "18 MB"
]
APP_VERSION  = "1.1.2"                   # از app/build.gradle.kts (versionName)
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
