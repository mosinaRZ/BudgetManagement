package handler

import (
	"html/template"
	"net/http"
)

// LegalHandler serves the public pages that app stores require: a privacy policy, terms of
// service, an open-source notices page and a web-accessible account-deletion page.
//
// The texts describe what this backend and app actually do. They are a starting point: have them
// reviewed by a lawyer before publishing, and update them whenever the app's data handling changes.
type LegalHandler struct{}

const (
	// legalVersion and the dates below must be bumped together whenever a legal text changes.
	legalVersion   = "1.0"
	legalUpdatedFa = "۲۶ مهر ۱۴۰۵"
	legalUpdatedEn = "18 October 2026"
)

func NewLegalHandler(_ string) *LegalHandler { return &LegalHandler{} }

type legalPage struct {
	Title     string
	Kind      string
	Version   string
	UpdatedFa string
	UpdatedEn string
}

var legalTemplate = template.Must(template.New("legal").Parse(`<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{.Title}} — سیدنا | Cidna</title>
<style>
body{font-family:system-ui,Tahoma,sans-serif;max-width:760px;margin:0 auto;padding:24px;line-height:1.9;color:#1f2933}
h1{font-size:1.5rem}h2{font-size:1.15rem;margin-top:2rem}
.en{direction:ltr;text-align:left;border-top:1px solid #d0d7de;margin-top:2.5rem;padding-top:1rem}
.meta{color:#52606d;font-size:.9rem}
nav a{margin-inline-end:12px}
code{background:#f1f3f5;padding:2px 6px;border-radius:4px}
table{border-collapse:collapse;width:100%;direction:ltr;text-align:left;font-size:.95rem}
td,th{border:1px solid #d0d7de;padding:6px 10px;vertical-align:top}
</style>
</head>
<body>
<nav><a href="/terms">قوانین و مقررات</a><a href="/privacy-policy">حریم خصوصی</a><a href="/account-deletion">حذف حساب</a><a href="/licenses">مجوزهای متن‌باز</a></nav>
{{if eq .Kind "privacy"}}
<h1>سیاست حریم خصوصی — سیدنا (Cidna)</h1>
<p class="meta">نسخه {{.Version}} — آخرین به‌روزرسانی: {{.UpdatedFa}}</p>
<h2>مسئول داده‌ها</h2>
<p>برنامه سیدنا (Cidna) توسط محمدسینا رمضانی عهدیه (شخص حقیقی) ارائه می‌شود. مسئول نگه‌داری داده‌های سمت سرور همین شخص است. راه تماس: از بخش پشتیبانی داخل برنامه استفاده کنید</p>
<h2>اطلاعاتی که جمع‌آوری می‌کنیم</h2>
<p>شماره موبایل (برای ثبت‌نام و ورود)، نام و جنسیت و تاریخ تولد (اختیاری)، و اطلاعات نمایشی دستگاه شامل نام و مدل، نسخه اندروید و برنامه، آدرس IP (به‌صورت ماسک‌شده نمایش داده می‌شود) و یک اثر انگشت یک‌طرفه (hash) از نصب برنامه برای شناسایی نصب مجدد. خود گذرواژه شما هرگز به سرور ارسال نمی‌شود: دستگاه شما از آن یک مقدار ورود جداگانه مشتق می‌کند و فقط هشِ Argon2id همان مقدار نزد ما نگه داشته می‌شود.</p>
<h2>اطلاعات مالی شما</h2>
<p>تراکنش‌ها، دسته‌بندی‌ها، بودجه‌ها، بدهی‌ها و اهداف پس‌انداز <strong>به‌صورت سرتاسری (End-to-End) رمزنگاری می‌شوند</strong>: کلید رمزنگاری روی دستگاه شما ساخته می‌شود و فقط به‌شکل رمزشده از آن خارج می‌شود، و سرور فقط داده رمزشده‌ای را نگه می‌دارد که ما نمی‌توانیم بخوانیم. گذرواژه و کلید بازیابی شما هرگز به سرور ارسال نمی‌شوند (دستگاه فقط مقادیری مشتق‌شده می‌فرستد که قفل داده را باز نمی‌کنند)؛ بنابراین ما و هر مهاجمی که به سرور نفوذ کند، بدون حدس زدن گذرواژه‌ی شما نمی‌توانیم داده مالی را رمزگشایی کنیم. به همین دلیل از گذرواژه‌ی بلند و منحصربه‌فرد استفاده کنید. اطلاعات لازم برای کارکرد سرویس (شماره موبایل، اطلاعات نمایه و فهرست دستگاه‌ها) زیر پوشش این رمزنگاری نیستند. اگر گذرواژه و کلید بازیابی را هر دو گم کنید، بازیابی داده ممکن نیست. پایگاه داده روی خود دستگاه نیز رمزنگاری شده است.</p>
<h2>پیامک و میکروفون</h2>
<p>در نسخه‌هایی که تشخیص پیامک بانکی دارند، متن پیامک فقط روی همان دستگاه پردازش می‌شود و به سرور ارسال نمی‌شود. این قابلیت فقط پس از اجازه شما در پنجره مجوز اندروید کار می‌کند و هر زمان می‌توانید مجوز را از تنظیمات گوشی بگیرید. میکروفون فقط وقتی شما ورود صوتی را انتخاب کنید استفاده می‌شود. تشخیص گفتار توسط سرویس گفتار خود گوشی انجام می‌شود؛ بسته به گوشی و تنظیمات آن، ممکن است صدا برای پردازش به ارائه‌دهنده آن سرویس (مثلاً گوگل) ارسال شود. سیدنا صدای شما را ضبط یا ذخیره نمی‌کند.</p>
<h2>اشتراک‌گذاری و اشخاص ثالث</h2>
<p>اطلاعات شما فروخته یا برای تبلیغات استفاده نمی‌شود. برنامه ابزار تبلیغاتی، رهگیری یا تحلیل رفتار شخص ثالث ندارد. برای ارسال کد تأیید، شماره موبایل شما به سرویس ارسال پیامک داده می‌شود. فایل‌های خروجی (PDF و Excel) و کارت اشتراک‌گذاری تراکنش فقط وقتی شما درخواست کنید ساخته می‌شوند و هر جا که خودتان ارسال کنید می‌روند.</p>
<h2>کوکی</h2>
<p>این صفحه‌ها و سرویس سیدنا از کوکی یا ابزار ردیابی استفاده نمی‌کنند.</p>
<h2>مدت نگه‌داری</h2>
<p>اطلاعات حساب تا زمانی که حساب فعال است نگه داشته می‌شود. سوابق فعالیت دستگاه‌ها (برای نمایش دستگاه‌های ورودی) حداکثر ۳۰ روز، و عملیات همگام‌سازی موقت حداکثر ۷ روز نگه داشته می‌شوند و سپس خودکار پاک می‌شوند.</p>
<h2>حذف حساب</h2>
<p>می‌توانید حساب را از مسیر «تنظیمات ← امنیت ← حذف حساب کاربری» حذف کنید (جزئیات: <a href="/account-deletion">/account-deletion</a>). اطلاعات حساب حذف‌شده ۳۰ روز برای امکان بازیابی نگه داشته می‌شود و سپس برای همیشه پاک می‌شود.</p>
<h2>امنیت</h2>
<p>ارتباط با سرور با HTTPS است، داده مالی سرتاسری رمزنگاری می‌شود، مقدار ورودِ مشتق‌شده از گذرواژه با Argon2id هش می‌شود و نشست‌ها قابل ابطال هستند. هیچ سامانه‌ای کاملاً بدون خطر نیست و امنیت مطلق را نمی‌توانیم تضمین کنیم.</p>
<h2>کاربران زیر ۱۸ سال</h2>
<p>استفاده از برنامه برای افراد زیر ۱۸ سال باید با اطلاع و نظارت ولی یا سرپرست قانونی باشد.</p>
<h2>حقوق شما</h2>
<p>می‌توانید درباره اطلاعات خود پرسش، درخواست اصلاح یا درخواست حذف داشته باشید. از اطلاعات مالی خود نیز می‌توانید داخل برنامه خروجی PDF یا Excel بگیرید.</p>
<h2>تغییر این سیاست</h2>
<p>در صورت تغییر، نسخه و تاریخ بالای صفحه به‌روز می‌شود و تغییرات مهم از راه برنامه اطلاع داده می‌شود.</p>
<h2>تماس</h2>
<p>پشتیبانی داخل برنامه / in-app support</p>
<div class="en">
<h1>Privacy Policy — Cidna</h1>
<p class="meta">Version {{.Version}} — last updated: {{.UpdatedEn}}</p>
<p><strong>Controller:</strong> Cidna is provided by Mohammad Sina Ramezani Ahdieh (an individual), who is responsible for the data kept on the server. Contact: use in-app support</p>
<p><strong>Data we collect:</strong> phone number (sign-up/sign-in); name, gender, birth date; display-only device info (name, model, OS/app version, masked IP, one-way installation hash). Your password itself never reaches our servers: your device derives a separate sign-in value from it, and we store only an Argon2id hash of that value.</p>
<p><strong>Financial data is end-to-end encrypted.</strong> It is encrypted on your device with a key that is generated on your device and leaves it only in wrapped (encrypted) form; our servers store ciphertext that we cannot read. Your password and your recovery key are never sent to us (your device sends only values derived from them that cannot unlock your data), so neither we nor an attacker who compromises our servers can decrypt your financial data without guessing your password. Because of that last point, please use a long, unique password. Account details the service needs to operate (phone number, profile fields, device list) are not covered by this encryption. If you lose both your password and recovery key, data cannot be recovered. The on-device database is encrypted too.</p>
<p><strong>SMS &amp; microphone:</strong> where bank-SMS detection exists, SMS text is processed on-device only, and only after you grant the Android permission (you can revoke it any time). The microphone is used only when you choose voice input. Speech recognition is performed by your phone's speech service; depending on the phone and its settings, audio may be sent to that service's provider (for example Google). Cidna does not record or store your voice.</p>
<p><strong>Sharing:</strong> we do not sell data or use it for ads. The app contains no third-party advertising, tracking or analytics. Your phone number is passed to our SMS provider to deliver verification codes. PDF/Excel exports and transaction share cards are created only when you ask and go wherever you send them.</p>
<p><strong>Cookies:</strong> these pages and the Cidna service use no cookies or tracking tools.</p>
<p><strong>Retention:</strong> account data is kept while your account is active. Device activity records are kept at most 30 days and temporary sync operations at most 7 days, then erased automatically.</p>
<p><strong>Deletion:</strong> delete your account in Settings → Security → Delete Account (see <a href="/account-deletion">/account-deletion</a>). Deleted accounts are kept 30 days for restoration, then permanently erased.</p>
<p><strong>Security:</strong> HTTPS transport, end-to-end encryption of financial data, password-derived sign-in values hashed with Argon2id, revocable sessions. No system is perfectly secure.</p>
<p><strong>Under 18:</strong> use by people under 18 should be with the knowledge and supervision of a parent or legal guardian.</p>
<p><strong>Your rights:</strong> you may ask questions, request correction or deletion of your data, and export your financial data as PDF or Excel inside the app.</p>
<p><strong>Changes:</strong> the version and date above are updated on every change; material changes are announced in the app.</p>
<p><strong>Contact:</strong> پشتیبانی داخل برنامه / in-app support</p>
</div>
{{else if eq .Kind "terms"}}
<h1>قوانین و مقررات استفاده — سیدنا (Cidna)</h1>
<p class="meta">نسخه {{.Version}} — آخرین به‌روزرسانی: {{.UpdatedFa}}</p>
<h2>۱. ارائه‌دهنده و پذیرش</h2>
<p>سیدنا (Cidna) برنامه مدیریت مالی شخصی است که توسط محمدسینا رمضانی عهدیه (شخص حقیقی) ارائه می‌شود. با ساخت حساب یا استفاده از برنامه، این قوانین و <a href="/privacy-policy">سیاست حریم خصوصی</a> را می‌پذیرید. اگر نمی‌پذیرید، از برنامه استفاده نکنید.</p>
<h2>۲. ماهیت خدمت</h2>
<p>سیدنا ابزاری برای ثبت و دسته‌بندی تراکنش‌ها، بودجه، بدهی و طلب و اهداف پس‌انداز است. <strong>سیدنا مشاوره مالی، حسابداری، حقوقی یا مالیاتی نیست.</strong> تحلیل‌ها، نمودارها، هشدارها و پیشنهادها فقط کمک تصمیم‌گیری‌اند و مسئولیت تصمیم‌های مالی با خود شماست.</p>
<h2>۳. دقت اطلاعات</h2>
<p>ورودی‌های شما، تشخیص خودکار پیامک بانکی و ورود صوتی ممکن است ناقص یا اشتباه باشند. پیش از اتکا به هر عدد یا تراکنش ثبت‌شده آن را با سوابق بانکی خود تطبیق دهید. سیدنا به حساب بانکی شما متصل نیست و جابه‌جایی پول انجام نمی‌دهد.</p>
<h2>۴. حساب کاربری، گذرواژه و کلید بازیابی</h2>
<p>حفظ گذرواژه و کلید بازیابی با شماست. داده‌های مالی شما پیش از ارسال روی دستگاه به‌صورت سرتاسری رمزنگاری می‌شود و گذرواژه و کلید بازیابی شما هرگز به سرور ارسال نمی‌شوند. بنابراین <strong>در صورت گم‌شدن هم‌زمان گذرواژه و کلید بازیابی، بازگرداندن داده ممکن نیست</strong>. اطلاعات واردشده باید درست و متعلق به خودتان باشد.</p>
<h2>۵. استفاده مجاز</h2>
<p>مجاز نیست: استفاده خلاف قانون، تلاش برای نفوذ یا اختلال در سرویس، دور زدن محدودیت‌ها، ارسال حجم غیرعادی درخواست، یا استفاده از حساب دیگران بدون اجازه.</p>
<h2>۶. دسترسی به سرویس و همگام‌سازی</h2>
<p>برنامه به‌صورت محلی‌محور کار می‌کند ولی همگام‌سازی و ورود به اینترنت و سرور وابسته است. دسترسی بی‌وقفه و بدون خطا را تضمین نمی‌کنیم و ممکن است سرویس برای نگه‌داری یا به‌روزرسانی موقتاً قطع شود. توصیه می‌کنیم از اطلاعات مهم خود خروجی بگیرید.</p>
<h2>۷. محدودیت مسئولیت</h2>
<p>برنامه به‌صورت «همان‌گونه که هست» ارائه می‌شود. تا حدی که قانون اجازه می‌دهد، ارائه‌دهنده مسئول زیان‌های غیرمستقیم، از دست رفتن داده یا سود، یا تصمیم‌های مالی مبتنی بر اطلاعات برنامه نیست. این بند حقوق غیرقابل‌سلب شما طبق قوانین جمهوری اسلامی ایران را محدود نمی‌کند.</p>
<h2>۸. حذف حساب و پایان استفاده</h2>
<p>هر زمان می‌توانید حساب را حذف کنید (<a href="/account-deletion">راهنمای حذف حساب</a>). در صورت نقض این قوانین یا سوءاستفاده از سرویس، ممکن است دسترسی حساب محدود یا قطع شود.</p>
<h2>۹. مالکیت</h2>
<p>نام و نشان «سیدنا / Cidna» متعلق به ارائه‌دهنده است. کد برنامه تحت مجوز GNU AGPL نسخه ۳ یا بالاتر منتشر شده است و فهرست اجزای متن‌باز در <a href="/licenses">صفحه مجوزها</a> آمده است. داده‌های شما متعلق به خودتان است.</p>
<h2>۱۰. تغییر قوانین</h2>
<p>ممکن است این قوانین به‌روز شوند. نسخه و تاریخ بالای صفحه تغییر می‌کند و ادامه استفاده پس از اطلاع‌رسانی به معنای پذیرش نسخه جدید است. تغییرات مهم از راه برنامه اطلاع داده می‌شود.</p>
<h2>۱۱. قانون حاکم</h2>
<p>این قوانین تابع قوانین جمهوری اسلامی ایران است و اختلافات در مراجع صالح قضایی ایران بررسی می‌شود.</p>
<h2>۱۲. تماس</h2>
<p>پشتیبانی داخل برنامه / in-app support</p>
<div class="en">
<h1>Terms of Service — Cidna</h1>
<p class="meta">Version {{.Version}} — last updated: {{.UpdatedEn}}</p>
<p><strong>1. Provider and acceptance.</strong> Cidna is a personal-finance app provided by Mohammad Sina Ramezani Ahdieh (an individual). By creating an account or using the app you accept these Terms and the <a href="/privacy-policy">Privacy Policy</a>. If you do not accept them, do not use the app.</p>
<p><strong>2. Nature of the service.</strong> Cidna helps you record and categorise transactions, budgets, debts/receivables and savings goals. <strong>It is not financial, accounting, legal or tax advice.</strong> Analytics, charts, alerts and suggestions only support your decisions, which remain your responsibility.</p>
<p><strong>3. Accuracy.</strong> Your entries, automatic bank-SMS detection and voice input can be incomplete or wrong. Check figures against your bank records before relying on them. Cidna is not connected to your bank account and does not move money.</p>
<p><strong>4. Account, password and recovery key.</strong> You are responsible for your password and recovery key. Financial data is end-to-end encrypted on your device before upload, and your password and recovery key are never sent to our servers. <strong>If you lose both your password and recovery key, your data cannot be recovered.</strong> Information you enter must be accurate and belong to you.</p>
<p><strong>5. Acceptable use.</strong> You must not use the service unlawfully, try to break into or disrupt it, bypass limits, send abnormal request volumes, or use someone else's account without permission.</p>
<p><strong>6. Availability and sync.</strong> The app is local-first, but sync and sign-in depend on the internet and the server. We do not guarantee uninterrupted or error-free service; it may be paused for maintenance. We recommend exporting important data.</p>
<p><strong>7. Limitation of liability.</strong> The app is provided “as is”. To the extent permitted by law, the provider is not liable for indirect damages, loss of data or profit, or financial decisions based on information in the app. This does not limit rights you cannot waive under the laws of the Islamic Republic of Iran.</p>
<p><strong>8. Deletion and termination.</strong> You can delete your account at any time (<a href="/account-deletion">how to delete</a>). Access may be restricted or ended for breach of these Terms or abuse of the service.</p>
<p><strong>9. Ownership.</strong> The name and mark “Cidna / سیدنا” belong to the provider. The app's code is released under the GNU AGPL v3 or later; open-source components are listed on the <a href="/licenses">licenses page</a>. Your data belongs to you.</p>
<p><strong>10. Changes.</strong> We may update these Terms. The version and date above change, and continued use after notice means you accept the new version. Material changes are announced in the app or in the app.</p>
<p><strong>11. Governing law.</strong> These Terms are governed by the laws of the Islamic Republic of Iran; disputes go to the competent Iranian courts.</p>
<p><strong>12. Contact:</strong> پشتیبانی داخل برنامه / in-app support</p>
</div>
{{else if eq .Kind "licenses"}}
<h1>مجوزهای متن‌باز — سیدنا (Cidna)</h1>
<p class="meta">نسخه {{.Version}} — آخرین به‌روزرسانی: {{.UpdatedFa}}</p>
<p>سیدنا تحت مجوز <strong>GNU Affero General Public License v3.0 یا بالاتر</strong> (AGPL-3.0-or-later) منتشر شده است. اجزای زیر مجوز مستقل خود را دارند. نام مجوز طبق پروژه بالادستی هر جزء ذکر شده است.</p>
<table>
<tr><th>Component</th><th>License</th></tr>
<tr><td>AndroidX (Compose, Material 3, Navigation, Lifecycle, Activity, AppCompat, Core, Room, WorkManager, Glance, Biometric, Security Crypto)</td><td>Apache-2.0</td></tr>
<tr><td>Kotlin, kotlinx.serialization, kotlinx.coroutines</td><td>Apache-2.0</td></tr>
<tr><td>Lottie for Android (Airbnb)</td><td>Apache-2.0</td></tr>
<tr><td>fastexcel (Dhatim)</td><td>MIT</td></tr>
<tr><td>SQLCipher for Android (Zetetic)</td><td>BSD-style (SQLCipher Community Edition)</td></tr>
<tr><td>Vazirmatn font — Copyright The Vazirmatn Project Authors (github.com/rastikerdar/vazirmatn)</td><td>SIL Open Font License 1.1</td></tr>
<tr><td>Inter font — Copyright The Inter Project Authors (github.com/rsms/inter)</td><td>SIL Open Font License 1.1</td></tr>
<tr><td>Backend (Go): go-chi/chi, go-playground/validator, golang-jwt/jwt, joho/godotenv, mongo-go-driver, golang.org/x/*</td><td>MIT / Apache-2.0 / BSD-3-Clause (see each module)</td></tr>
</table>
<p>متن کامل هر مجوز در مخزن بالادستی همان جزء در دسترس است. برای درخواست متن کامل یا گزارش اشتباه در این فهرست: پشتیبانی داخل برنامه / in-app support</p>
<div class="en">
<h1>Open-source notices — Cidna</h1>
<p>Cidna is released under the <strong>GNU AGPL v3.0 or later</strong>. The components listed above carry their own licenses, named as stated by each upstream project. Full license texts are available from each upstream repository. To request a full text or report an error in this list: پشتیبانی داخل برنامه / in-app support</p>
</div>
{{else}}
<h1>حذف حساب کاربری — سیدنا (Cidna)</h1>
<h2>از داخل برنامه</h2>
<p>برنامه را باز کنید و به <code>تنظیمات ← امنیت ← حذف حساب کاربری</code> بروید، گذرواژه را وارد و تأیید کنید.</p>
<h2>چه اتفاقی می‌افتد؟</h2>
<p>از همه دستگاه‌ها خارج می‌شوید و اطلاعات روی دستگاه شما پاک می‌شود. اطلاعات رمزشده روی سرور ۳۰ روز نگه داشته می‌شود؛ در این مدت با ورود دوباره و انتخاب «بازیابی حساب» حساب برمی‌گردد. پس از ۳۰ روز، حساب، داده‌های همگام‌شده و دستگاه‌ها برای همیشه پاک می‌شوند.</p>
<h2>بدون دسترسی به برنامه</h2>
<p>برای حذف حساب، از بخش پشتیبانی داخل برنامه اقدام کنید.</p>
<div class="en">
<h1>Delete your account — Cidna</h1>
<p><strong>In the app:</strong> Settings → Security → Delete Account, then confirm with your password.</p>
<p><strong>What happens:</strong> you are signed out everywhere and local data is erased. Encrypted server data is kept 30 days so you can restore by signing in and choosing “Restore account”; after 30 days the account, synced data and devices are permanently erased.</p>
<p><strong>Without the app:</strong> use in-app support to request account deletion.</p>
</div>
{{end}}
</body>
</html>`))

func (h *LegalHandler) page(title, kind string) legalPage {
	return legalPage{
		Title: title, Kind: kind,
		Version: legalVersion, UpdatedFa: legalUpdatedFa, UpdatedEn: legalUpdatedEn,
	}
}

func (h *LegalHandler) PrivacyPolicy(w http.ResponseWriter, r *http.Request) {
	h.render(w, h.page("سیاست حریم خصوصی", "privacy"))
}

func (h *LegalHandler) Terms(w http.ResponseWriter, r *http.Request) {
	h.render(w, h.page("قوانین و مقررات", "terms"))
}

func (h *LegalHandler) Licenses(w http.ResponseWriter, r *http.Request) {
	h.render(w, h.page("مجوزهای متن‌باز", "licenses"))
}

func (h *LegalHandler) AccountDeletion(w http.ResponseWriter, r *http.Request) {
	h.render(w, h.page("حذف حساب کاربری", "deletion"))
}

func (h *LegalHandler) render(w http.ResponseWriter, p legalPage) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "public, max-age=3600")
	if err := legalTemplate.Execute(w, p); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
	}
}
