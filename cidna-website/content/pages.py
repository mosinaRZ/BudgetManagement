# -*- coding: utf-8 -*-
import config as C
from engine.ui import *
from engine.doc import doc, simple_page
from engine.layout import page, social_links
import content.faq as FAQ

def card(icon, h, p): return f"<article class='card'>{ico(icon)}<h3>{h}</h3><p>{p}</p></article>"

# ── Features ───────────────────────────────────────────────────────────
def features(L):
    t = T(L)
    groups = [
     (t("شروع و حساب کاربری","Start & account"), [
        ("user", t("حالت مهمان","Guest mode"), t("بدون حساب و شماره موبایل شروع کنید، با رمز یا اثر انگشت. بعداً اطلاعات را به حساب جدید منتقل کنید یا از صفر شروع کنید.","Start with no account or phone number, protected by a passcode or fingerprint. Later move data into a new account or start fresh.")),
        ("phone", t("حساب با شماره موبایل","Phone-number account"), t("ثبت‌نام با کد پیامکی یک‌بارمصرف، بازیابی گذرواژه با کلید بازیابی و حذف نرم حساب با مهلت ۳۰ روزه.","Sign up with a one-time SMS code, recover your password with a recovery key, and delete softly with a 30-day restore window.")),
        ("device", t("چنددستگاهی","Multi-device"), t("با یک حساب روی چند گوشی همگام شوید؛ فهرست دستگاه‌ها، خروج از راه دور و اعلان ورود از دستگاه جدید.","Sync across phones with one account; device list, remote sign-out and new-device alerts.")),
     ]),
     (t("ثبت و دسته‌بندی","Record & categorise"), [
        ("wallet", t("تراکنش‌ها","Transactions"), t("ثبت درآمد و هزینه، مرور سوابق، بررسی تراکنش‌های در انتظار و میان‌بر ثبت سریع.","Record income and expenses, browse history, review pending transactions and use the quick-add shortcut.")),
        ("mic", t("ورود صوتی","Voice entry"), t("عنوان تراکنش را بگویید و پیش از ذخیره بررسی کنید.","Say the transaction title and check it before saving.")),
        ("tag", t("دسته‌بندی‌ها","Categories"), t("دسته‌های آماده، دسته‌های سفارشی و پیشنهاد دسته‌بندی.","Predefined and custom categories, plus category suggestions.")),
        ("sms", t("پیامک بانکی (نسخهٔ مستقیم)","Bank SMS (direct build)"), t("پیامک‌های بانکی تشخیص داده و به‌صورت تراکنش در انتظار پیشنهاد می‌شوند؛ پردازش فقط روی گوشی. فقط در نسخهٔ direct.","Bank SMS are detected and offered as pending transactions, processed on the phone only. Direct build only.")),
     ]),
     (t("برنامه‌ریزی","Plan"), [
        ("pie", t("بودجه و سقف هزینه","Budgets & limits"), t("سقف هزینه برای هر دسته، با نوار پیشرفت و هشدار.","A spending limit per category, with progress bars and alerts.")),
        ("target", t("قلک‌های پس‌انداز","Savings piggy banks"), t("قلک برای هر هدف، واریز و برداشت و واریز ماهانهٔ زمان‌بندی‌شده.","A piggy bank per goal, deposits and withdrawals, and scheduled monthly deposits.")),
        ("swap", t("بدهی و طلب","Debts & receivables"), t("مبالغ بدهکار و طلبکار، پرداخت‌های جزئی، سررسید و یادآوری.","Money owed and expected, partial payments, due dates and reminders.")),
     ]),
     (t("بینش و خروجی","Insight & output"), [
        ("chart", t("تحلیل مالی","Analytics"), t("خلاصه‌ها و نمودارها با تاریخ جلالی یا میلادی.","Summaries and charts with Jalali or Gregorian dates.")),
        ("file", t("خروجی PDF و Excel","PDF & Excel export"), t("از سوابق بازه‌ای دلخواه گزارش بگیرید.","Export your records for any period.")),
        ("share", t("کارت تصویری تراکنش","Transaction image cards"), t("اشتراک‌گذاری تراکنش به‌صورت تصویر، هماهنگ با زبان، جهت متن و واحد پول برنامه.","Share a transaction as an image that follows the app’s language, direction and currency.")),
     ]),
     (t("حریم خصوصی و امنیت","Privacy & security"), [
        ("lock", t("رمزنگاری سرتاسری","End-to-end encryption"), t("اطلاعات مالی پیش از همگام‌سازی روی گوشی با AES-256-GCM رمز می‌شوند؛ سرور فقط داده‌ی رمزشده نگه می‌دارد.","Financial records are encrypted on the phone with AES-256-GCM before sync; the server stores ciphertext only.")),
        ("finger", t("قفل برنامه و ورود زیستی","App lock & biometrics"), t("رمز ورود، اثر انگشت اختیاری و پایگاه‌دادهٔ رمزنگاری‌شده روی گوشی.","A passcode, optional fingerprint and an encrypted on-device database.")),
        ("offline", t("آفلاین پس از ورود","Offline after sign-in"), t("بعد از اولین ورود موفق، بدون اینترنت هم کار می‌کند و با اتصال بعدی همگام می‌شود.","After the first successful sign-in it works with no internet and syncs on the next connection.")),
        ("gift", t("رایگان و بدون تبلیغ","Free, no ads"), t("بدون اشتراک، خرید درون‌برنامه‌ای، تبلیغ یا ردیاب شخص ثالث.","No subscription, in-app purchases, ads or third-party trackers.")),
     ]),
     (t("راحتی روزمره","Everyday comfort"), [
        ("lang", t("فارسی و انگلیسی","Persian & English"), t("هر دو زبان کامل، با پشتیبانی درست راست‌به‌چپ و چپ‌به‌راست.","Both languages in full, with correct right-to-left and left-to-right layout.")),
        ("cal", t("جلالی، میلادی، تومان، ریال","Jalali, Gregorian, Toman, Rial"), t("تاریخ و مبلغ همان‌طور که در ذهن دارید.","Dates and amounts the way you think of them.")),
        ("eye-off", t("مخفی‌کردن مبالغ","Hide amounts"), t("با یک لمس مبالغ صفحهٔ اصلی را پنهان کنید.","Hide the home-screen amounts with one tap.")),
        ("sun", t("تم روشن و تیره","Light & dark theme"), t("هر دو تم؛ همان رنگ‌هایی که در این وب‌سایت می‌بینید.","Both themes — the same palette you see on this website.")),
        ("bell", t("اعلان و یادآور","Notifications & reminders"), t("یادآور سررسید بدهی و قلک و اعلان‌های امنیتی.","Reminders for debt due dates and goals, plus security alerts.")),
        ("home", t("ویجت موجودی","Balance widget"), t("موجودی را بی‌آنکه برنامه را باز کنید، روی صفحهٔ اصلی گوشی ببینید.","See your balance on the home screen without opening the app.")),
     ]),
    ]
    inner = "<section class='sec'><div class='wrap'>" + "".join(f"<h2 class='group-title'>{g}</h2><div class='cards'>{''.join(card(*i) for i in items)}</div>" for g, items in groups) + "</div></section>"
    inner += section(f"<div class='band glass'><div><h2>{t('آماده‌اید امتحان کنید؟','Ready to try it?')}</h2><p>{t('رایگان است و حتی بدون حساب هم شروع می‌شود.','It’s free and starts even without an account.')}</p></div>{store_buttons(L)}</div>")
    return simple_page(L, "features", t("امکانات سیدنا","Cidna features"),
        t("امکانات سیدنا: ثبت هزینه و درآمد، بودجه، قلک پس‌انداز، بدهی و طلب، خروجی PDF/Excel، رمزنگاری سرتاسری، تقویم جلالی و کار آفلاین.","Cidna features: income and expense tracking, budgets, savings piggy banks, debts, PDF/Excel export, end-to-end encryption, Jalali calendar and offline use."),
        t("هرچه برای مدیریت پول شخصی لازم است، در یک اپ سبک و خصوصی.","Everything personal money management needs, in one light and private app."), inner,
        keywords=t("امکانات برنامه مدیریت مالی, ثبت درآمد و هزینه, قلک پس انداز, مدیریت بدهی, خروجی اکسل","finance app features, income expense tracker, savings goals, debt tracker, export to Excel"))

# ── Download ───────────────────────────────────────────────────────────
def download(L):
    t = T(L); d = next(s for s in C.STORES if s["id"] == "direct")
    rows = [(t("قیمت","Price"), t("رایگان","Free")), (t("نسخه","Version"), fad(C.APP_VERSION) if L == "fa" else C.APP_VERSION),
            (t("اندروید","Android"), t(f"{fad(C.MIN_ANDROID)} یا بالاتر", f"{C.MIN_ANDROID} or later")), (t("زبان","Languages"), t("فارسی، انگلیسی","Persian, English")),
            (t("مجوز","License"), "AGPL-3.0-or-later")]
    if d.get("size"): rows.insert(3, (t("حجم فایل مستقیم","Direct file size"), f"<span class='ltr'>{d['size']}</span>"))
    kv = "<ul class='kv'>" + "".join(f"<li><b>{k}:</b> <span>{v}</span></li>" for k, v in rows) + "</ul>"
    sha = ""
    if d.get("sha256"):
        sha = f"<h2 id='verify'>{t('بررسی سلامت فایل مستقیم','Verify the direct file')}</h2><p>{t('هش SHA-256 فایل را با مقدار زیر مقایسه کنید:','Compare the file’s SHA-256 hash with this value:')}</p><p class='hash'>{d['sha256']}</p><p><button class='btn btn-ghost btn-sm' type='button' data-copy='{d['sha256']}' data-ok='{t('کپی شد','Copied')}'>{t('کپی هش','Copy hash')}</button></p>"
    secs = f"""<article class='prose'>
<h2 id='get'>{t('دریافت','Get the app')}</h2>{store_buttons(L)}<p class='mt'>{t('برای خبر نسخه‌های تازه، ما را در <a href="'+C.X_URL+'" rel="noopener">ایکس</a> یا <a href="'+C.INSTAGRAM+'" rel="noopener">اینستاگرام</a> دنبال کنید.','For news about new versions, follow us on <a href="'+C.X_URL+'" rel="noopener">X</a> or <a href="'+C.INSTAGRAM+'" rel="noopener">Instagram</a>.')}</p>
<h2 id='info'>{t('مشخصات','Details')}</h2>{kv}
<h2 id='safe'>{t('فقط از لینک‌های رسمی دریافت کنید','Only download from official links')}</h2>
<p>{t('سیدنا متن‌باز است و هر کسی می‌تواند نسخهٔ خودش را بسازد؛ اما نسخه‌های غیررسمی ممکن است داده‌های شما را ببینند. اگر فایلی را از جای دیگری گرفته‌اید، دوباره از همین صفحه دریافت کنید. تغییردهندگان باید نام و نشان دیگری انتخاب کنند.','Cidna is open source, so anyone can build their own copy — but unofficial builds could see your data. If you got a file from somewhere else, download it again from this page. Forks must use a different name and icon.')}</p>
<h2 id='direct'>{t('نصب فایل مستقیم (APK)','Installing the direct file (APK)')}</h2>
<ol><li>{t('فایل را دریافت کنید.','Download the file.')}</li><li>{t('هنگام باز کردن، اندروید برای مرورگر یا مدیر فایل اجازهٔ «نصب برنامه‌های ناشناس» را می‌خواهد؛ فقط برای همین یک‌بار اجازه دهید.','When you open it, Android asks to allow “install unknown apps” for your browser or file manager — allow it just for this install.')}</li><li>{t('در صورت تمایل، هش فایل را با مقدار پایین همین صفحه مقایسه کنید.','Optionally compare the file’s hash with the value below on this page.')}</li><li>{t('نصب کنید و برنامه را باز کنید.','Install and open the app.')}</li></ol>
<div class='callout'><p>{t('قابلیت تشخیص پیامک بانکی فقط در نسخهٔ مستقیم (direct) وجود دارد و فقط پس از اجازهٔ شما فعال می‌شود.','Bank-SMS detection exists only in the direct build, and only after you grant the permission.')}</p></div>
{sha}</article>"""
    body = f"""<section class='page-hero'><div class='wrap'>{{{{CRUMBS}}}}<h1>{t('دریافت سیدنا','Download Cidna')}</h1><p class='lead'>{t('رایگان، بدون تبلیغ، برای اندروید. از هر راهی که برایتان راحت‌تر است.','Free, ad-free, for Android — choose whichever route suits you.')}</p></div></section>
<div class='wrap doc'><aside class='toc glass'><b>{t('در این صفحه','On this page')}</b><a href='#get'>{t('دریافت','Get the app')}</a><a href='#info'>{t('مشخصات','Details')}</a><a href='#safe'>{t('امنیت دریافت','Safe download')}</a><a href='#direct'>{t('نصب مستقیم','Direct install')}</a></aside>{secs}</div>"""
    cr = [(t("خانه","Home"), url(L,"")), (t("دریافت برنامه","Download"), url(L,"download"))]
    return page(L, "download", t("دریافت سیدنا برای اندروید","Download Cidna for Android"),
        t(f"دریافت رایگان سیدنا از {store_names('fa')}؛ برنامهٔ مدیریت مالی شخصی با رمزنگاری سرتاسری برای اندروید ۸ و بالاتر.",f"Download Cidna free from {store_names('en')} — an end-to-end encrypted personal finance app for Android 8+."),
        body, crumbs=cr, keywords=t("دانلود سیدنا, دانلود برنامه مدیریت مالی, سیدنا کافه بازار, دانلود مستقیم سیدنا","download Cidna, Cidna APK, Cidna Cafe Bazaar, Cidna direct download"))

# ── FAQ page ───────────────────────────────────────────────────────────
def faq(L):
    t = T(L); items = FAQ.items(L)
    inner = section(faq_html(items))
    return simple_page(L, "faq", t("پرسش‌های متداول","Frequently asked questions"),
        t("پاسخ پرسش‌های متداول دربارهٔ سیدنا: رایگان بودن، رمزنگاری سرتاسری، استفاده آفلاین، حذف حساب، پیامک بانکی و پشتیبانی.","Answers about Cidna: being free, end-to-end encryption, offline use, account deletion, bank SMS and support."),
        t("پاسخ‌های روشن و صادقانه؛ اگر چیزی جا مانده بود، بنویسید.","Clear, honest answers — and if something’s missing, write to us."), inner,
        schema=[faq_schema(items)])

# ── Security ───────────────────────────────────────────────────────────
def security(L):
    t = T(L); P = lambda s: f"<p>{s}</p>"; ul = lambda *a: "<ul>" + "".join(f"<li>{x}</li>" for x in a) + "</ul>"
    note = f"<div class='callout'><p><b>{t('خلاصه:','In short:')}</b> {t('اطلاعات مالی شما پیش از همگام‌سازی روی گوشی رمزنگاری سرتاسری می‌شود؛ گذرواژه و کلید بازیابی هرگز از گوشی خارج نمی‌شوند؛ ما فقط چیزی را ادعا می‌کنیم که بتوانیم نشان بدهیم.','Your financial records are end-to-end encrypted on the phone before sync; your password and recovery key never leave it; and we only claim what we can show.')}</p></div>"
    S = [
     ("device", t("روی گوشی شما","On your device"), ul(
        t("پایگاه‌دادهٔ Room با SQLCipher رمزگذاری می‌شود. گذرواژهٔ آن تصادفی است و در EncryptedSharedPreferences با پشتوانهٔ Android Keystore نگه‌داری می‌شود.","The Room database is encrypted with SQLCipher. Its passphrase is random and kept in EncryptedSharedPreferences backed by the Android Keystore."),
        t("توکن‌ها و کلیدها در حافظهٔ رمزگذاری‌شده ذخیره می‌شوند.","Tokens and keys are stored in encrypted preferences."),
        t("ورود زیستی اختیاری از کلیدی در Keystore استفاده می‌کند که زیست‌سنجی قوی می‌خواهد.","Optional biometric sign-in uses a Keystore key that requires strong biometrics."))),
     ("e2e", t("پیش از ارسال: رمزنگاری سرتاسری","Before upload: end-to-end encryption"),
        P(t("هر رکورد همگام‌شده با یک کلید دادهٔ تصادفی ۲۵۶ بیتی و AES-256-GCM رمزگذاری می‌شود. کلید داده روی سرور فقط به‌شکل «بسته‌بندی‌شده» نگه‌داری می‌شود: یک‌بار زیر کلیدی مشتق‌شده از گذرواژهٔ شما و یک‌بار زیر کلیدی مشتق‌شده از کلید بازیابی.","Every synced record is encrypted with a random 256-bit data key using AES-256-GCM. On the server the data key exists only in wrapped form: once under a key derived from your password and once under a key derived from your recovery key."))
        +P(t("<b>پروتکل نسخهٔ ۲:</b> گوشی با PBKDF2-HMAC-SHA256 (گذرواژهٔ نرمال‌سازی‌شده با NFKC، ۶۰۰٬۰۰۰ تکرار، نمکِ مخصوص هر حساب) یک کلید اصلی می‌سازد و آن را با HKDF به دو مقدار مستقل تقسیم می‌کند: یک <i>کلید بسته‌بندی</i> که هرگز از گوشی خارج نمی‌شود و یک <i>راز احراز هویت</i> که تنها چیزی است که به سرور می‌رود. بازیابی هم با یک کلید بازیابی تصادفی ۲۵۶ بیتی به همین شکل کار می‌کند (فقط یک «اثبات» مشتق‌شده فرستاده می‌شود، نه خود کلید). پاکت‌ها AES-256-GCM هستند و با AAD به هدفشان بسته شده‌اند. پس سرور نه گذرواژه دارد، نه کلید بازیابی، و نه کلیدی که بتواند کلید داده را باز کند.",
              "<b>Protocol v2:</b> the device derives a master key with PBKDF2-HMAC-SHA256 (NFKC-normalised password, 600,000 iterations, per-account salt) and splits it with HKDF into two independent values — a <i>wrapping key</i> that never leaves the device and an <i>authentication secret</i> that is the only thing sent to the server. Recovery works the same way with a random 256-bit recovery key (a derived proof is sent, never the key). Envelopes are AES-256-GCM, bound to their purpose through AAD. The server therefore holds neither the password, nor the recovery key, nor any key able to unwrap the data key."))),
     ("auth", t("احراز هویت","Authentication"), ul(
        t("راز احراز هویت مشتق‌شده از گذرواژه، یک‌بار دیگر با Argon2id هش می‌شود؛ نشت پایگاه‌داده چیزی مستقیماً قابل‌استفاده نمی‌دهد.","The password-derived authentication secret is hashed again with Argon2id, so a database leak yields nothing directly usable."),
        t("برنامه پارامترهای KDF کمتر از حداقل پشتیبانی‌شده را نمی‌پذیرد.","Clients refuse KDF parameters below the supported minimum."),
        t("توکن دسترسی کوتاه‌عمر (JWT، HS256، بررسی issuer، نسخه‌دار بر اساس نشست) و توکن نوسازی چرخشی با تشخیص استفادهٔ مجدد.","Short-lived JWT access tokens (HS256, issuer-checked, session-versioned) and rotating refresh tokens with reuse detection."),
        t("قفل حساب پس از تلاش‌های ناموفق مکرر، محدودیت تعداد تلاش برای کد یک‌بارمصرف و محدودیت نرخ برای هر IP و کاربر.","Account lockout after repeated failures, OTP attempt limits, and per-IP and per-user rate limiting."),
        t("هشدار ورود دستگاه جدید و خروج از راه دور.","New-device alerts and remote sign-out."))),
     ("transport", t("انتقال و استقرار","Transport & deployment"), ul(
        t("فقط HTTPS (ارتباط رمزنشده فقط در نسخه‌های دیباگ مجاز است).","HTTPS only (cleartext is permitted solely in debug builds)."),
        t("پایگاه‌داده و API فقط روی شبکهٔ داخلی Docker در دسترس‌اند و راه‌اندازی در حالت تولید، رمزهای ضعیف یا خالی را نمی‌پذیرد.","The database and API are reachable only on the internal Docker network, and production start-up refuses weak or missing secrets."))),
     ("screen", t("حریم تصویر و پشتیبان‌گیری","Screen privacy & backups"), P(t("پنجرهٔ برنامه به‌طور پیش‌فرض FLAG_SECURE دارد (بدون اسکرین‌شات و با نمای خالی در برنامه‌های اخیر) و در تنظیمات قابل تغییر است. پشتیبان‌گیری و انتقال دستگاه اندروید کاملاً کنار گذاشته شده است.","The app window is FLAG_SECURE by default (no screenshots, blank Recents), user-switchable in Settings. Android backup and device transfer are fully excluded."))),
     ("limits", t("دامنه و محدودیت‌ها (لطفاً بخوانید)","Scope & limits (please read)"),
        "<div class='callout warn'>"+ul(
        t("اطلاعات مالی سرتاسری رمزنگاری می‌شوند: نه اپراتور و نه مهاجمی که سرور را در اختیار گرفته، بدون حدس‌زدن گذرواژهٔ کاربر نمی‌تواند آن‌ها را بخواند. خطر باقی‌مانده، حدس‌زدن آفلاینِ گذرواژه‌های <b>ضعیف</b> توسط کسی است که راز احراز هویت را ضبط کرده (همان مدل مدیران رمز رایج)؛ برای همین گذرواژهٔ بلند و منحصربه‌فرد توصیه می‌کنیم و KDF عمداً سنگین است.","Financial records are end-to-end encrypted: neither the operator nor an attacker who controls the server can decrypt them without guessing the user’s password. The residual risk is offline guessing of <b>weak</b> passwords by someone who captures the authentication secret (the same model as mainstream password managers), which is why long unique passwords are encouraged and the KDF is expensive."),
        t("خارج از پوشش رمزنگاری: فراداده‌ای که سرویس برای کار کردن لازم دارد (شماره موبایل، مشخصات نمایه، فهرست دستگاه‌ها، زمان و حجم ترافیک همگام‌سازی). به همین دلیل سیدنا را «دانش صفر» (zero-knowledge) معرفی نمی‌کنیم.","Not covered by the encryption: account metadata the service needs to operate (phone number, profile fields, device list, timing and size of sync traffic). That is why we do not call Cidna “zero-knowledge”."),
        t("اگر هم گذرواژه و هم کلید بازیابی را گم کنید، داده‌ی همگام‌شده قابل بازیابی نیست.","If you lose <b>both</b> the password and the recovery key, synced data cannot be recovered."),
        t("کتابخانه‌های امنیتی جایگزین ممیزی مستقل نیستند.","Security libraries are not a substitute for an independent audit."))+"</div>"),
     ("report", t("گزارش آسیب‌پذیری","Reporting a vulnerability"), P(t(f"لطفاً مشکل امنیتی را به‌صورت خصوصی و از طریق ایمیل <a href='mailto:{C.EMAIL}?subject=Security'><span class='ltr'>{C.EMAIL}</span></a> گزارش دهید و آن را به‌صورت عمومی منتشر نکنید. <a href='/.well-known/security.txt'>security.txt</a>",
        f"Please report security issues privately by email to <a href='mailto:{C.EMAIL}?subject=Security'><span class='ltr'>{C.EMAIL}</span></a> and do not open a public issue. <a href='/.well-known/security.txt'>security.txt</a>"))),
    ]
    return doc(L, "security", t("امنیت و رمزنگاری","Security & encryption"),
        t("مدل امنیتی سیدنا: رمزنگاری سرتاسری اطلاعات مالی، SQLCipher، Argon2id، پروتکل نسخهٔ ۲ و محدودیت‌ها به‌صورت صادقانه.","Cidna’s security model: end-to-end encrypted financial data, SQLCipher, Argon2id, protocol v2 — and its honest limits."),
        t("چه چیزی را چگونه محافظت می‌کنیم، و دقیقاً چه چیزی را ادعا نمی‌کنیم.","How we protect what we protect — and exactly what we don’t claim."), S, [(t("امنیت","Security"),"security")],
        note=note, meta=t(f"آخرین بازبینی: {fad(C.TODAY)}", f"Last reviewed: {C.TODAY}"),
        keywords=t("رمزنگاری سرتاسری, امنیت اپ مالی, SQLCipher, Argon2id, حریم خصوصی اطلاعات مالی","end-to-end encryption, finance app security, SQLCipher, Argon2id, financial privacy"))

# ── Contact (همهٔ لینک‌های «درباره ما و پشتیبانی» اپ) ─────────────────────
def contact_links(L):
    t = T(L)
    ch = [("mail", t("ارسال ایمیل به پشتیبانی","Email support"), C.EMAIL, "mailto:" + C.EMAIL),
          ("whatsapp", t("واتس‌اپ توسعه‌دهنده","WhatsApp contact"), t("گفتگوی مستقیم","Chat directly"), C.WHATSAPP),
          ("x", t("ایکس (توییتر)","X (Twitter)"), "@CidnaApp", C.X_URL),
          ("instagram", t("اینستاگرام","Instagram"), "@cidnaapp", C.INSTAGRAM)]
    lg = [("scale", t("قوانین و مقررات","Terms of service"), href(L,"terms")), ("shield", t("حریم خصوصی","Privacy policy"), href(L,"privacy")),
          ("code", t("مجوزهای متن‌باز","Open-source licenses"), href(L,"licenses")), ("trash", t("حذف حساب","Delete account"), href(L,"account-deletion"))]
    a = "".join(f"<li><a href='{h}'{' rel=\"noopener\"' if h.startswith('http') else ''}>{svg(i,22)}<span>{n}<small class='ltr'>{s}</small></span></a></li>" for i, n, s, h in ch)
    b = "".join(f"<li><a href='{h}'>{svg(i,22)}<span>{n}</span></a></li>" for i, n, h in lg)
    return a, b

def contact(L):
    t = T(L); a, b = contact_links(L)
    inner = f"""<section class='sec'><div class='wrap'><div class='cards'>
<div><h2 class='group-title'>{t('با ما در تماس باشید','Get in touch')}</h2><ul class='linklist'>{a}</ul></div>
<div><h2 class='group-title'>{t('حقوقی و حساب','Legal & account')}</h2><ul class='linklist'>{b}</ul></div></div>
<p class='note mt2'>{t('پشتیبانی داخل برنامه هم در تنظیمات ← «درباره ما و پشتیبانی» هست. گزارش مشکل امنیتی را فقط خصوصی و با ایمیل بفرستید.','In-app support is also under Settings → “About Us & Support”. Please send security reports privately, by email only.')}</p></div></section>"""
    return simple_page(L, "contact", t("تماس و پشتیبانی","Contact & support"),
        t("راه‌های ارتباط با سیدنا: ایمیل پشتیبانی، واتس‌اپ، ایکس و اینستاگرام؛ و پیوند صفحه‌های حقوقی.","Ways to reach Cidna: support email, WhatsApp, X and Instagram — plus links to the legal pages."),
        t("هر راهی که برایتان راحت‌تر است.","Whichever way is easiest for you."), inner)

# ── About ──────────────────────────────────────────────────────────────
def about(L):
    t = T(L); a, b = contact_links(L)
    src = f"<li><a href='{e(C.SOURCE_URL)}' rel='noopener'>{svg('code',22)}<span>{t('کد منبع','Source code')}</span></a></li>" if C.SOURCE_URL else ""
    S = [
     ("why", t("چرا سیدنا؟","Why Cidna?"), "<p>"+t("بیشتر اپ‌های مالی برای کاربر انگلیسی‌زبان، تقویم میلادی و ذهنیت دلاری ساخته شده‌اند و پیش از اینکه ارزشی بدهند، اطلاعاتتان را می‌خواهند. سیدنا این رابطه را برعکس می‌کند: در چند ثانیه به‌عنوان مهمان شروع کنید، به فارسی یا انگلیسی ثبت کنید، تاریخ جلالی و تومان را بومی ببینید و اطلاعاتتان را روی گوشی خودتان نگه دارید. فقط وقتی همگام‌سازی چنددستگاهی خواستید، حساب بسازید.",
        "Most finance apps are built for English, Gregorian-calendar, dollar-thinking users and ask for your data before they give you value. Cidna flips that: start as a guest in seconds, record in Persian or English, see Jalali dates and Toman natively, and keep your records on your own phone. Create an account only when you want multi-device sync.")+"</p>"),
     ("pillars", t("سه ستون","Three pillars"), "<ol><li>"+t("<b>محلی‌محور.</b> سوابق در پایگاه‌دادهٔ رمزنگاری‌شدهٔ روی گوشی است؛ استفاده سریع است و پس از ورود آفلاین هم کار می‌کند.","<b>Local-first.</b> Records live in an encrypted on-device database; use is fast and keeps working offline after sign-in.")+"</li><li>"+t("<b>حریم خصوصی از ابتدا.</b> اطلاعات مالی پیش از ارسال روی گوشی سرتاسری رمزنگاری می‌شود.","<b>Private by design.</b> Financial records are end-to-end encrypted on the phone before upload.")+"</li><li>"+t("<b>ساخته‌شده برای فارسی‌زبان و انگلیسی‌زبان.</b> راست‌به‌چپ و چپ‌به‌راست، تقویم جلالی، تومان/ریال.","<b>Made for Persian and English speakers.</b> RTL and LTR, Jalali calendar, Toman/Rial.")+"</li></ol>"),
     ("free", t("رایگان و متن‌باز","Free and open source"), "<p>"+t("سیدنا کاملاً رایگان است و بدون تبلیغ و ردیاب. کد آن تحت مجوز GNU AGPL نسخهٔ ۳ یا بالاتر منتشر شده است؛ نام و نشان «سیدنا / Cidna» تحت این مجوز نیست و فورک‌ها باید نام و آیکن دیگری بگذارند.","Cidna is completely free, with no ads or trackers. Its code is released under the GNU AGPL v3 or later; the “Cidna / سیدنا” name and logo are not licensed with it, so forks must use a different name and icon.")+f" <a href='{href(L,'licenses')}'>{t('مجوزها','Licenses')}</a></p>"),
     ("naming", t("دربارهٔ نام","About the name"), "<p>"+t("نام محصول در فارسی «سیدنا» و در انگلیسی «Cidna» است. «مدیریت بودجه» فقط توصیف کارکرد برنامه است.","The product is “سیدنا” in Persian and “Cidna” in English. “Budget Management” merely describes what the app does.")+"</p>"),
     ("provider", t("ارائه‌دهنده","Provider"), "<p>"+t(f"سیدنا توسط {C.PROVIDER_FA} (شخص حقیقی) ارائه می‌شود. تماس: ",f"Cidna is provided by {C.PROVIDER_EN} (an individual). Contact: ")+f"<a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a></p>"),
     ("links", t("راه‌های ارتباطی و صفحه‌های حقوقی","Contact channels & legal pages"), f"<ul class='linklist'>{a}{src}</ul><ul class='linklist'>{b}</ul>"),
    ]
    return doc(L, "about", t("درباره سیدنا","About Cidna"),
        t("سیدنا اپلیکیشن رایگان و متن‌باز مدیریت مالی شخصی برای فارسی‌زبانان و انگلیسی‌زبانان است؛ محلی‌محور، خصوصی و بدون تبلیغ.","Cidna is a free, open-source personal finance app for Persian and English speakers — local-first, private and ad-free."),
        t("پول‌تان، به زبان و تقویم خودتان، روی گوشی خودتان.","Your money, in your language and calendar, on your own phone."), S, [(t("درباره","About"),"about")])

# ── 404 ────────────────────────────────────────────────────────────────
def not_found():
    body = """<section class='page-hero'><div class='wrap'><h1>۴۰۴ · 404</h1><p class='lead'>صفحه‌ای که دنبالش بودید پیدا نشد. · The page you’re looking for isn’t here.</p>
<p><a class='btn btn-primary' href='/'>صفحهٔ اصلی</a> <a class='btn btn-ghost' href='/en/'>English home</a></p></div></section>"""
    return page("fa", "404", "صفحه پیدا نشد", "صفحهٔ درخواستی پیدا نشد.", body, noindex=True)
