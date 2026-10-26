# -*- coding: utf-8 -*-
"""متن‌های حقوقی — منبع حقیقت: backend/internal/interface/http/handler/legal_handler.go
هر بار آن فایل تغییر کرد، همین‌جا و تاریخ/نسخه را در config.py هماهنگ کنید."""
import config as C
from engine.ui import *
from engine.doc import doc

def _meta(L):
    t = T(L)
    return t(f"نسخهٔ {fad(C.LEGAL_VERSION)} — آخرین به‌روزرسانی: {C.LEGAL_UPDATED_FA}", f"Version {C.LEGAL_VERSION} — last updated: {C.LEGAL_UPDATED_EN}")

def _contact(L):
    t = T(L)
    return t(f"از بخش پشتیبانی داخل برنامه یا ایمیل <a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a> استفاده کنید.",
             f"Use in-app support or email <a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a>.")

def _legal_doc(L, slug, title, desc, intro, sections):
    t = T(L)
    return doc(L, slug, title, desc, intro, sections, [(title, slug)], meta=_meta(L), nav_slug="")

def privacy(L):
    t = T(L); P = lambda s: f"<p>{s}</p>"
    S = [
     ("controller", t("مسئول داده‌ها","Data controller"), P(t(f"برنامهٔ سیدنا (Cidna) توسط {C.PROVIDER_FA} (شخص حقیقی) ارائه می‌شود. مسئول نگه‌داری داده‌های سمت سرور همین شخص است. راه تماس: ","Cidna is provided by "+C.PROVIDER_EN+" (an individual), who is responsible for the data kept on the server. Contact: ")+_contact(L))),
     ("collect", t("اطلاعاتی که جمع‌آوری می‌کنیم","Data we collect"), P(t("شماره موبایل (برای ثبت‌نام و ورود)، نام و جنسیت و تاریخ تولد (اختیاری)، و اطلاعات نمایشی دستگاه شامل نام و مدل، نسخهٔ اندروید و برنامه، آدرس IP (به‌صورت ماسک‌شده نمایش داده می‌شود) و یک اثر انگشت یک‌طرفه (hash) از نصب برنامه برای شناسایی نصب مجدد. خود گذرواژهٔ شما هرگز به سرور ارسال نمی‌شود: دستگاه شما از آن یک مقدار ورود جداگانه مشتق می‌کند و فقط هشِ Argon2id همان مقدار نزد ما نگه داشته می‌شود.",
        "Phone number (sign-up and sign-in); name, gender and birth date (optional); display-only device info (name, model, OS and app version, masked IP, a one-way installation hash used to recognise reinstalls). Your password itself never reaches our servers: your device derives a separate sign-in value from it, and we store only an Argon2id hash of that value."))),
     ("financial", t("اطلاعات مالی شما","Your financial data"),
        P(t("تراکنش‌ها، دسته‌بندی‌ها، بودجه‌ها، بدهی‌ها و اهداف پس‌انداز <strong>به‌صورت سرتاسری (End-to-End) رمزنگاری می‌شوند</strong>: کلید رمزنگاری روی دستگاه شما ساخته می‌شود و فقط به‌شکل رمزشده از آن خارج می‌شود، و سرور فقط داده‌ی رمزشده‌ای را نگه می‌دارد که ما نمی‌توانیم بخوانیم. گذرواژه و کلید بازیابی شما هرگز به سرور ارسال نمی‌شوند (دستگاه فقط مقادیری مشتق‌شده می‌فرستد که قفل داده را باز نمی‌کنند)؛ بنابراین ما و هر مهاجمی که به سرور نفوذ کند، بدون حدس زدن گذرواژه‌ی شما نمی‌توانیم داده‌ی مالی را رمزگشایی کنیم. به همین دلیل از گذرواژه‌ی بلند و منحصربه‌فرد استفاده کنید.",
          "Transactions, categories, budgets, debts and savings goals are <strong>end-to-end encrypted</strong>: the encryption key is generated on your device and leaves it only in wrapped (encrypted) form, and our servers store ciphertext that we cannot read. Your password and recovery key are never sent to us (your device sends only values derived from them that cannot unlock your data), so neither we nor an attacker who compromises our servers can decrypt your financial data without guessing your password. Because of that, please use a long, unique password."))
        + P(t("اطلاعات لازم برای کارکرد سرویس (شماره موبایل، اطلاعات نمایه و فهرست دستگاه‌ها) زیر پوشش این رمزنگاری نیستند. اگر گذرواژه و کلید بازیابی را هر دو گم کنید، بازیابی داده ممکن نیست. پایگاه داده روی خود دستگاه نیز رمزنگاری شده است.",
              "Account details the service needs to operate (phone number, profile fields, device list) are not covered by this encryption. If you lose both your password and recovery key, data cannot be recovered. The on-device database is encrypted too."))),
     ("sms", t("پیامک و میکروفون","SMS & microphone"), P(t("در نسخه‌هایی که تشخیص پیامک بانکی دارند، متن پیامک فقط روی همان دستگاه پردازش می‌شود و به سرور ارسال نمی‌شود. این قابلیت فقط پس از اجازهٔ شما در پنجرهٔ مجوز اندروید کار می‌کند و هر زمان می‌توانید مجوز را از تنظیمات گوشی بگیرید. میکروفون فقط وقتی شما ورود صوتی را انتخاب کنید استفاده می‌شود. تشخیص گفتار توسط سرویس گفتار خود گوشی انجام می‌شود؛ بسته به گوشی و تنظیمات آن، ممکن است صدا برای پردازش به ارائه‌دهندهٔ آن سرویس (مثلاً گوگل) ارسال شود. سیدنا صدای شما را ضبط یا ذخیره نمی‌کند.",
        "Where bank-SMS detection exists, SMS text is processed on the device only, and only after you grant the Android permission (you can revoke it any time). The microphone is used only when you choose voice input. Speech recognition is performed by your phone’s speech service; depending on the phone and its settings, audio may be sent to that service’s provider (for example Google). Cidna does not record or store your voice."))),
     ("sharing", t("اشتراک‌گذاری و اشخاص ثالث","Sharing & third parties"), P(t("اطلاعات شما فروخته یا برای تبلیغات استفاده نمی‌شود. برنامه ابزار تبلیغاتی، رهگیری یا تحلیل رفتار شخص ثالث ندارد. برای ارسال کد تأیید، شماره موبایل شما به سرویس ارسال پیامک داده می‌شود. فایل‌های خروجی (PDF و Excel) و کارت اشتراک‌گذاری تراکنش فقط وقتی شما درخواست کنید ساخته می‌شوند و هر جا که خودتان ارسال کنید می‌روند.",
        "We do not sell data or use it for ads. The app contains no third-party advertising, tracking or analytics. Your phone number is passed to our SMS provider to deliver verification codes. PDF/Excel exports and transaction share cards are created only when you ask and go wherever you send them."))),
     ("cookies", t("کوکی و این وب‌سایت","Cookies & this website"), P(t("این صفحه‌ها و سرویس سیدنا از کوکی یا ابزار ردیابی استفاده نمی‌کنند. این وب‌سایت هیچ منبع شخص ثالثی (فونت، تحلیل رفتار، تبلیغ) بارگذاری نمی‌کند و تنها انتخاب تم روشن/تاریک شما را در حافظهٔ مرورگر خودتان نگه می‌دارد. سرور وب ممکن است برای امنیت و عیب‌یابی، گزارش‌های فنی استاندارد (مانند آدرس IP و زمان درخواست) را برای مدتی محدود نگه دارد.",
        "These pages and the Cidna service use no cookies or tracking tools. This website loads no third-party resources (fonts, analytics, ads) and only remembers your light/dark theme choice in your own browser. The web server may keep standard technical logs (such as IP address and request time) for a limited period for security and troubleshooting."))),
     ("retention", t("مدت نگه‌داری","Retention"), P(t("اطلاعات حساب تا زمانی که حساب فعال است نگه داشته می‌شود. سوابق فعالیت دستگاه‌ها (برای نمایش دستگاه‌های ورودی) حداکثر ۳۰ روز، و عملیات همگام‌سازی موقت حداکثر ۷ روز نگه داشته می‌شوند و سپس خودکار پاک می‌شوند.",
        "Account data is kept while your account is active. Device activity records are kept at most 30 days and temporary sync operations at most 7 days, then erased automatically."))),
     ("deletion", t("حذف حساب","Deleting your account"), P(t(f"می‌توانید حساب را از مسیر «تنظیمات ← امنیت ← حذف حساب کاربری» حذف کنید (جزئیات: <a href='{href(L,'account-deletion')}'>حذف حساب</a>). اطلاعات حساب حذف‌شده ۳۰ روز برای امکان بازیابی نگه داشته می‌شود و سپس برای همیشه پاک می‌شود.",
        f"Delete your account in Settings → Security → Delete Account (see <a href='{href(L,'account-deletion')}'>account deletion</a>). Deleted accounts are kept 30 days for restoration, then permanently erased."))),
     ("security", t("امنیت","Security"), P(t("ارتباط با سرور با HTTPS است، داده مالی سرتاسری رمزنگاری می‌شود، مقدار ورودِ مشتق‌شده از گذرواژه با Argon2id هش می‌شود و نشست‌ها قابل ابطال هستند. هیچ سامانه‌ای کاملاً بدون خطر نیست و امنیت مطلق را نمی‌توانیم تضمین کنیم.",
        "HTTPS transport, end-to-end encryption of financial data, password-derived sign-in values hashed with Argon2id, revocable sessions. No system is perfectly secure, and we cannot guarantee absolute security."))),
     ("minors", t("کاربران زیر ۱۸ سال","Under 18"), P(t("استفاده از برنامه برای افراد زیر ۱۸ سال باید با اطلاع و نظارت ولی یا سرپرست قانونی باشد.","Use by people under 18 should be with the knowledge and supervision of a parent or legal guardian."))),
     ("rights", t("حقوق شما","Your rights"), P(t("می‌توانید درباره اطلاعات خود پرسش، درخواست اصلاح یا درخواست حذف داشته باشید. از اطلاعات مالی خود نیز می‌توانید داخل برنامه خروجی PDF یا Excel بگیرید.","You may ask questions, request correction or deletion of your data, and export your financial data as PDF or Excel inside the app."))),
     ("changes", t("تغییر این سیاست","Changes to this policy"), P(t("در صورت تغییر، نسخه و تاریخ بالای صفحه به‌روز می‌شود و تغییرات مهم از راه برنامه اطلاع داده می‌شود.","The version and date above are updated on every change; material changes are announced in the app."))),
     ("contact", t("تماس","Contact"), P(_contact(L))),
    ]
    return _legal_doc(L, "privacy", t("سیاست حریم خصوصی","Privacy policy"),
        t("سیاست حریم خصوصی سیدنا: چه اطلاعاتی جمع می‌کنیم، رمزنگاری سرتاسری اطلاعات مالی، مدت نگه‌داری و حقوق شما.","Cidna’s privacy policy: what we collect, end-to-end encryption of financial data, retention and your rights."),
        t("آنچه جمع می‌کنیم، آنچه رمز می‌کنیم و آنچه هرگز نمی‌کنیم — به زبان روشن.","What we collect, what we encrypt and what we never do — in plain language."), S)

def terms(L):
    t = T(L); P = lambda s: f"<p>{s}</p>"
    S = [
     ("provider", t("۱. ارائه‌دهنده و پذیرش","1. Provider and acceptance"), P(t(f"سیدنا (Cidna) برنامهٔ مدیریت مالی شخصی است که توسط {C.PROVIDER_FA} (شخص حقیقی) ارائه می‌شود. با ساخت حساب یا استفاده از برنامه، این قوانین و <a href='{href(L,'privacy')}'>سیاست حریم خصوصی</a> را می‌پذیرید. اگر نمی‌پذیرید، از برنامه استفاده نکنید.",
        f"Cidna is a personal-finance app provided by {C.PROVIDER_EN} (an individual). By creating an account or using the app you accept these Terms and the <a href='{href(L,'privacy')}'>Privacy Policy</a>. If you do not accept them, do not use the app."))),
     ("nature", t("۲. ماهیت خدمت","2. Nature of the service"), P(t("سیدنا ابزاری برای ثبت و دسته‌بندی تراکنش‌ها، بودجه، بدهی و طلب و اهداف پس‌انداز است. <strong>سیدنا مشاورهٔ مالی، حسابداری، حقوقی یا مالیاتی نیست.</strong> تحلیل‌ها، نمودارها، هشدارها و پیشنهادها فقط کمک تصمیم‌گیری‌اند و مسئولیت تصمیم‌های مالی با خود شماست.",
        "Cidna helps you record and categorise transactions, budgets, debts/receivables and savings goals. <strong>It is not financial, accounting, legal or tax advice.</strong> Analytics, charts, alerts and suggestions only support your decisions, which remain your responsibility."))),
     ("accuracy", t("۳. دقت اطلاعات","3. Accuracy"), P(t("ورودی‌های شما، تشخیص خودکار پیامک بانکی و ورود صوتی ممکن است ناقص یا اشتباه باشند. پیش از اتکا به هر عدد یا تراکنش ثبت‌شده آن را با سوابق بانکی خود تطبیق دهید. سیدنا به حساب بانکی شما متصل نیست و جابه‌جایی پول انجام نمی‌دهد.",
        "Your entries, automatic bank-SMS detection and voice input can be incomplete or wrong. Check figures against your bank records before relying on them. Cidna is not connected to your bank account and does not move money."))),
     ("account", t("۴. حساب کاربری، گذرواژه و کلید بازیابی","4. Account, password and recovery key"), P(t("حفظ گذرواژه و کلید بازیابی با شماست. داده‌های مالی شما پیش از ارسال روی دستگاه به‌صورت سرتاسری رمزنگاری می‌شود و گذرواژه و کلید بازیابی شما هرگز به سرور ارسال نمی‌شوند. بنابراین <strong>در صورت گم‌شدن هم‌زمان گذرواژه و کلید بازیابی، بازگرداندن داده ممکن نیست</strong>. اطلاعات واردشده باید درست و متعلق به خودتان باشد.",
        "You are responsible for your password and recovery key. Financial data is end-to-end encrypted on your device before upload, and your password and recovery key are never sent to our servers. <strong>If you lose both your password and recovery key, your data cannot be recovered.</strong> Information you enter must be accurate and belong to you."))),
     ("use", t("۵. استفادهٔ مجاز","5. Acceptable use"), P(t("مجاز نیست: استفادهٔ خلاف قانون، تلاش برای نفوذ یا اختلال در سرویس، دور زدن محدودیت‌ها، ارسال حجم غیرعادی درخواست، یا استفاده از حساب دیگران بدون اجازه.",
        "You must not use the service unlawfully, try to break into or disrupt it, bypass limits, send abnormal request volumes, or use someone else’s account without permission."))),
     ("availability", t("۶. دسترسی به سرویس و همگام‌سازی","6. Availability and sync"), P(t("برنامه به‌صورت محلی‌محور کار می‌کند ولی همگام‌سازی و ورود به اینترنت و سرور وابسته است. دسترسی بی‌وقفه و بدون خطا را تضمین نمی‌کنیم و ممکن است سرویس برای نگه‌داری یا به‌روزرسانی موقتاً قطع شود. توصیه می‌کنیم از اطلاعات مهم خود خروجی بگیرید.",
        "The app is local-first, but sync and sign-in depend on the internet and the server. We do not guarantee uninterrupted or error-free service; it may be paused for maintenance. We recommend exporting important data."))),
     ("liability", t("۷. محدودیت مسئولیت","7. Limitation of liability"), P(t("برنامه به‌صورت «همان‌گونه که هست» ارائه می‌شود. تا حدی که قانون اجازه می‌دهد، ارائه‌دهنده مسئول زیان‌های غیرمستقیم، از دست رفتن داده یا سود، یا تصمیم‌های مالی مبتنی بر اطلاعات برنامه نیست. این بند حقوق غیرقابل‌سلب شما طبق قوانین جمهوری اسلامی ایران را محدود نمی‌کند.",
        "The app is provided “as is”. To the extent permitted by law, the provider is not liable for indirect damages, loss of data or profit, or financial decisions based on information in the app. This does not limit rights you cannot waive under the laws of the Islamic Republic of Iran."))),
     ("termination", t("۸. حذف حساب و پایان استفاده","8. Deletion and termination"), P(t(f"هر زمان می‌توانید حساب را حذف کنید (<a href='{href(L,'account-deletion')}'>راهنمای حذف حساب</a>). در صورت نقض این قوانین یا سوءاستفاده از سرویس، ممکن است دسترسی حساب محدود یا قطع شود.",
        f"You can delete your account at any time (<a href='{href(L,'account-deletion')}'>how to delete</a>). Access may be restricted or ended for breach of these Terms or abuse of the service."))),
     ("ownership", t("۹. مالکیت","9. Ownership"), P(t(f"نام و نشان «سیدنا / Cidna» متعلق به ارائه‌دهنده است. کد برنامه تحت مجوز GNU AGPL نسخهٔ ۳ یا بالاتر منتشر شده است و فهرست اجزای متن‌باز در <a href='{href(L,'licenses')}'>صفحهٔ مجوزها</a> آمده است. داده‌های شما متعلق به خودتان است.",
        f"The name and mark “Cidna / سیدنا” belong to the provider. The app’s code is released under the GNU AGPL v3 or later; open-source components are listed on the <a href='{href(L,'licenses')}'>licenses page</a>. Your data belongs to you."))),
     ("changes", t("۱۰. تغییر قوانین","10. Changes"), P(t("ممکن است این قوانین به‌روز شوند. نسخه و تاریخ بالای صفحه تغییر می‌کند و ادامهٔ استفاده پس از اطلاع‌رسانی به معنای پذیرش نسخهٔ جدید است. تغییرات مهم از راه برنامه اطلاع داده می‌شود.",
        "We may update these Terms. The version and date above change, and continued use after notice means you accept the new version. Material changes are announced in the app."))),
     ("law", t("۱۱. قانون حاکم","11. Governing law"), P(t("این قوانین تابع قوانین جمهوری اسلامی ایران است و اختلافات در مراجع صالح قضایی ایران بررسی می‌شود.","These Terms are governed by the laws of the Islamic Republic of Iran; disputes go to the competent Iranian courts."))),
     ("contact", t("۱۲. تماس","12. Contact"), P(_contact(L))),
    ]
    return _legal_doc(L, "terms", t("قوانین و مقررات استفاده","Terms of service"),
        t("قوانین و مقررات استفاده از اپلیکیشن سیدنا: ماهیت خدمت، مسئولیت‌ها، حساب کاربری، حذف حساب و قانون حاکم.","Terms of service for the Cidna app: nature of the service, responsibilities, accounts, deletion and governing law."),
        t("شرایط استفاده از سیدنا؛ کوتاه، روشن و هماهنگ با رفتار واقعی برنامه.","The terms for using Cidna — short, clear and matched to what the app really does."), S)

def licenses(L):
    t = T(L)
    rows = [("AndroidX (Compose, Material 3, Navigation, Lifecycle, Activity, AppCompat, Core, Room, WorkManager, Glance, Biometric, Security Crypto)","Apache-2.0"),
            ("Kotlin, kotlinx.serialization, kotlinx.coroutines","Apache-2.0"),("Lottie for Android (Airbnb)","Apache-2.0"),("fastexcel (Dhatim)","MIT"),
            ("SQLCipher for Android (Zetetic)","BSD-style (SQLCipher Community Edition)"),
            ("Vazirmatn font — Copyright The Vazirmatn Project Authors (github.com/rastikerdar/vazirmatn)","SIL Open Font License 1.1"),
            ("Inter font — Copyright The Inter Project Authors (github.com/rsms/inter)","SIL Open Font License 1.1"),
            ("Backend (Go): go-chi/chi, go-playground/validator, golang-jwt/jwt, joho/godotenv, mongo-go-driver, golang.org/x/*","MIT / Apache-2.0 / BSD-3-Clause (see each module)")]
    tbl = "<div class='tbl'><table><thead><tr><th>Component</th><th>License</th></tr></thead><tbody>" + "".join(f"<tr><td>{c}</td><td>{l}</td></tr>" for c, l in rows) + "</tbody></table></div>"
    src = f"<p><a href='{e(C.SOURCE_URL)}' rel='noopener'>{t('کد منبع سیدنا','Cidna source code')}</a></p>" if C.SOURCE_URL else ""
    S = [
     ("own", t("مجوز خود سیدنا","Cidna’s own license"), "<p>"+t("سیدنا تحت مجوز <strong>GNU Affero General Public License v3.0 یا بالاتر</strong> (AGPL-3.0-or-later) منتشر شده است. استفادهٔ شخصی و تجاری آزاد است؛ بازتوزیع نسخهٔ اصلی یا اصلاح‌شده مستلزم حفظ مجوز و اعلان‌های حق نشر و ارائهٔ کد منبع متناظر با همان مجوز است. این مجوز هیچ حقی بر نام و نشان «سیدنا / Cidna» نمی‌دهد.",
        "Cidna is released under the <strong>GNU Affero General Public License v3.0 or later</strong> (AGPL-3.0-or-later). Personal and commercial use is allowed; redistributing the original or a modified version requires keeping the license and copyright notices and offering the corresponding source under the same license. The license grants no rights to the “Cidna / سیدنا” name or logo.")+"</p>"+src),
     ("third", t("اجزای شخص ثالث","Third-party components"), "<p>"+t("اجزای زیر مجوز مستقل خود را دارند. نام مجوز طبق پروژهٔ بالادستی هر جزء ذکر شده است.","The components below carry their own licenses, named as stated by each upstream project.")+"</p>"+tbl+
        "<p>"+t("متن کامل هر مجوز در مخزن بالادستی همان جزء در دسترس است. برای درخواست متن کامل یا گزارش اشتباه در این فهرست: ","Full license texts are available from each upstream repository. To request a full text or report an error in this list: ")+_contact(L)+"</p>"),
    ]
    return _legal_doc(L, "licenses", t("مجوزهای متن‌باز","Open-source licenses"),
        t("مجوز AGPL-3.0 سیدنا و فهرست مجوزهای اجزای متن‌باز و فونت‌های استفاده‌شده در برنامه.","Cidna’s AGPL-3.0 license and the licenses of the open-source components and fonts used in the app."),
        t("سیدنا بر دوش پروژه‌های متن‌باز ساخته شده؛ این‌ها فهرست و مجوزشان است.","Cidna stands on open-source work. Here is the list and their licenses."), S)

def account_deletion(L):
    t = T(L); P = lambda s: f"<p>{s}</p>"
    S = [
     ("inapp", t("از داخل برنامه","In the app"), P(t("برنامه را باز کنید و به <code>تنظیمات ← امنیت ← حذف حساب کاربری</code> بروید، گذرواژه را وارد و تأیید کنید.","Open the app, go to <code>Settings → Security → Delete Account</code>, enter your password and confirm."))),
     ("what", t("چه اتفاقی می‌افتد؟","What happens?"), P(t("از همهٔ دستگاه‌ها خارج می‌شوید و اطلاعات روی دستگاه شما پاک می‌شود. اطلاعات رمزشده روی سرور ۳۰ روز نگه داشته می‌شود؛ در این مدت با ورود دوباره و انتخاب «بازیابی حساب» حساب برمی‌گردد. پس از ۳۰ روز، حساب، داده‌های همگام‌شده و دستگاه‌ها برای همیشه پاک می‌شوند.",
        "You are signed out everywhere and local data is erased. Encrypted server data is kept for 30 days so you can restore by signing in and choosing “Restore account”; after 30 days the account, synced data and devices are permanently erased."))),
     ("guest", t("حالت مهمان","Guest mode"), P(t("اگر حساب نساخته‌اید و فقط در حالت مهمان هستید، اطلاعاتتان فقط روی گوشی است؛ می‌توانید فضای کاری مهمان را از تنظیمات حذف کنید یا برنامه را پاک کنید.","If you never created an account, your data lives only on the phone; you can delete the guest workspace in Settings or simply uninstall the app."))),
     ("noapp", t("بدون دسترسی به برنامه","Without access to the app"), P(t(f"از بخش پشتیبانی داخل برنامه یا ایمیل <a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a> درخواست حذف حساب بدهید.",
        f"Use in-app support or email <a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a> to request account deletion."))),
    ]
    return doc(L, "account-deletion", t("حذف حساب کاربری","Delete your account"),
        t("راهنمای حذف حساب سیدنا: مسیر داخل برنامه، مهلت ۳۰ روزهٔ بازیابی و حذف دائمی داده‌ها.","How to delete your Cidna account: in-app steps, the 30-day restore window and permanent erasure."),
        t("حذف حساب ساده است و ۳۰ روز فرصت بازگشت دارید.","Deleting is simple, and you have 30 days to change your mind."), S, [(t("حذف حساب","Delete account"), "account-deletion")],
        meta=_meta(L), nav_slug="")
