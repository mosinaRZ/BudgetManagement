package handler

import (
	"html/template"
	"net/http"
	"strings"
)

// LegalHandler serves the public pages that app stores require: a privacy policy and a
// web-accessible account-deletion page (Google Play asks for a URL in the Data safety form).
//
// The texts describe what this backend and app actually do. They are a starting point: have them
// reviewed against your own legal situation before publishing.
type LegalHandler struct {
	supportEmail string
}

func NewLegalHandler(supportEmail string) *LegalHandler {
	return &LegalHandler{supportEmail: strings.TrimSpace(supportEmail)}
}

type legalPage struct {
	Title        string
	SupportEmail string
	Kind         string
}

var legalTemplate = template.Must(template.New("legal").Parse(`<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{.Title}}</title>
<style>
body{font-family:system-ui,Tahoma,sans-serif;max-width:760px;margin:0 auto;padding:24px;line-height:1.9;color:#1f2933}
h1{font-size:1.5rem}h2{font-size:1.15rem;margin-top:2rem}
.en{direction:ltr;text-align:left;border-top:1px solid #d0d7de;margin-top:2.5rem;padding-top:1rem}
code{background:#f1f3f5;padding:2px 6px;border-radius:4px}
</style>
</head>
<body>
{{if eq .Kind "privacy"}}
<h1>سیاست حریم خصوصی — مدیریت بودجه</h1>
<h2>اطلاعاتی که جمع‌آوری می‌کنیم</h2>
<p>شماره موبایل (برای ثبت‌نام و ورود)، ایمیل و نام و جنسیت و تاریخ تولد (اختیاری)، و اطلاعات نمایشی دستگاه شامل نام و مدل، نسخه اندروید و برنامه، آدرس IP (به‌صورت ماسک‌شده نمایش داده می‌شود) و یک اثر انگشت یک‌طرفه (hash) از نصب برنامه برای شناسایی نصب مجدد.</p>
<h2>اطلاعات مالی شما</h2>
<p>تراکنش‌ها، دسته‌بندی‌ها، بودجه‌ها، بدهی‌ها و اهداف پس‌انداز <strong>قبل از ارسال روی دستگاه شما رمزنگاری می‌شوند</strong> و سرور فقط داده رمزشده را نگه می‌دارد؛ ما نمی‌توانیم محتوای آن را بخوانیم. کلید رمزنگاری از گذرواژه و کلید بازیابی شما ساخته می‌شود؛ اگر هر دو را گم کنید، بازیابی داده ممکن نیست.</p>
<h2>پیامک و میکروفون</h2>
<p>در نسخه‌هایی که تشخیص پیامک بانکی دارند، متن پیامک فقط روی همان دستگاه پردازش می‌شود و به سرور ارسال نمی‌شود. میکروفون فقط وقتی شما ورود صوتی را انتخاب کنید و از سرویس تشخیص گفتار دستگاه استفاده می‌شود.</p>
<h2>اشتراک‌گذاری</h2>
<p>اطلاعات شما فروخته یا برای تبلیغات استفاده نمی‌شود. برای ارسال کد تأیید، شماره یا ایمیل شما به سرویس ارسال پیامک/ایمیل داده می‌شود.</p>
<h2>نگهداری و حذف</h2>
<p>می‌توانید حساب را از مسیر «تنظیمات ← امنیت ← حذف حساب کاربری» حذف کنید (جزئیات: <a href="/account-deletion">/account-deletion</a>). اطلاعات حساب حذف‌شده ۳۰ روز برای امکان بازیابی نگه داشته می‌شود و سپس برای همیشه پاک می‌شود.</p>
<h2>امنیت</h2>
<p>ارتباط با سرور با HTTPS است، گذرواژه‌ها با Argon2 هش می‌شوند و نشست‌ها قابل ابطال هستند.</p>
<h2>تماس</h2>
<p>{{if .SupportEmail}}<a href="mailto:{{.SupportEmail}}">{{.SupportEmail}}</a>{{else}}آدرس پشتیبانی در تنظیمات سرور (<code>SUPPORT_EMAIL</code>) مشخص نشده است.{{end}}</p>
<div class="en">
<h1>Privacy Policy — Budget Management</h1>
<p><strong>Data we collect:</strong> phone number (sign-up/sign-in); optional email, name, gender, birth date; display-only device info (name, model, OS/app version, masked IP, one-way installation hash).</p>
<p><strong>Financial data</strong> is encrypted on your device before upload; the server stores ciphertext only and cannot read it. The key derives from your password and recovery key — if you lose both, data cannot be recovered.</p>
<p><strong>SMS &amp; microphone:</strong> where bank-SMS detection exists, SMS text is processed on-device only. The microphone is used only when you choose voice input.</p>
<p><strong>Sharing:</strong> we do not sell data or use it for ads. Your phone/email is passed to our SMS/email provider to deliver verification codes.</p>
<p><strong>Retention &amp; deletion:</strong> delete your account in Settings → Security → Delete Account (see <a href="/account-deletion">/account-deletion</a>). Deleted accounts are kept 30 days for restoration, then permanently erased.</p>
<p><strong>Contact:</strong> {{if .SupportEmail}}<a href="mailto:{{.SupportEmail}}">{{.SupportEmail}}</a>{{else}}not configured{{end}}</p>
</div>
{{else}}
<h1>حذف حساب کاربری — مدیریت بودجه</h1>
<h2>از داخل برنامه</h2>
<p>برنامه را باز کنید و به <code>تنظیمات ← امنیت ← حذف حساب کاربری</code> بروید، گذرواژه را وارد و تأیید کنید.</p>
<h2>چه اتفاقی می‌افتد؟</h2>
<p>از همه دستگاه‌ها خارج می‌شوید و اطلاعات روی دستگاه شما پاک می‌شود. اطلاعات رمزشده روی سرور ۳۰ روز نگه داشته می‌شود؛ در این مدت با ورود دوباره و انتخاب «بازیابی حساب» حساب برمی‌گردد. پس از ۳۰ روز، حساب، داده‌های همگام‌شده و دستگاه‌ها برای همیشه پاک می‌شوند.</p>
<h2>بدون دسترسی به برنامه</h2>
<p>{{if .SupportEmail}}از ایمیل <a href="mailto:{{.SupportEmail}}">{{.SupportEmail}}</a> با موضوع «حذف حساب» و شماره موبایل ثبت‌شده برایمان بنویسید.{{else}}با پشتیبانی تماس بگیرید.{{end}}</p>
<div class="en">
<h1>Delete your account — Budget Management</h1>
<p><strong>In the app:</strong> Settings → Security → Delete Account, then confirm with your password.</p>
<p><strong>What happens:</strong> you are signed out everywhere and local data is erased. Encrypted server data is kept 30 days so you can restore by signing in and choosing “Restore account”; after 30 days the account, synced data and devices are permanently erased.</p>
<p><strong>Without the app:</strong> {{if .SupportEmail}}email <a href="mailto:{{.SupportEmail}}">{{.SupportEmail}}</a> with the subject “Delete account” and your registered phone number.{{else}}contact support.{{end}}</p>
</div>
{{end}}
</body>
</html>`))

func (h *LegalHandler) PrivacyPolicy(w http.ResponseWriter, r *http.Request) {
	h.render(w, legalPage{Title: "سیاست حریم خصوصی", SupportEmail: h.supportEmail, Kind: "privacy"})
}

func (h *LegalHandler) AccountDeletion(w http.ResponseWriter, r *http.Request) {
	h.render(w, legalPage{Title: "حذف حساب کاربری", SupportEmail: h.supportEmail, Kind: "deletion"})
}

func (h *LegalHandler) render(w http.ResponseWriter, p legalPage) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "public, max-age=3600")
	if err := legalTemplate.Execute(w, p); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
	}
}
