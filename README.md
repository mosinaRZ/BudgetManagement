<div align="center">

# Cidna · سیدنا

### Personal finance, budgeting & savings — Android + Go
### مدیریت مالی شخصی، بودجه‌بندی و پس‌انداز — اندروید و بک‌اند Go

**A local-first, privacy-minded personal finance app built for Persian and English speakers.**

**اپلیکیشنی محلی‌محور و حریم‌خصوصی‌محور برای ثبت، دسته‌بندی، تحلیل و برنامه‌ریزی امور مالی شخصی؛ با پشتیبانی کامل فارسی و انگلیسی.**

[English](#english) · [فارسی](#فارسی) · [Legal](#9-legal-information) · [حقوقی](#۹-اطلاعات-حقوقی)

`AGPL-3.0-or-later` · Kotlin · Jetpack Compose · Go · MongoDB

</div>

---

<a id="english"></a>
## English

## 1. About Cidna

**Cidna** (Persian: **سیدنا**) is a personal-finance application for Android with an optional Go backend for account sign-in and multi-device synchronization.

It is built around three ideas:

- **Local-first.** Your records live in an encrypted on-device database. Day-to-day use stays fast and works without a connection; the backend is only needed for accounts and sync.
- **Private by design.** Financial records are encrypted on the device *before* they are uploaded, so the sync server stores ciphertext rather than readable data (see [Security model](#4-security-model) for the exact scope and limits of this claim).
- **Made for Persian and English users.** Full RTL/LTR support, Jalali (Persian) calendar, Toman/Rial display, and Persian bank-SMS recognition.

> **Naming.** The product name is **Cidna** in English and **سیدنا** in Persian. "Budget Management" is only a descriptive phrase for what the app does, and the Kotlin package (`ir.hamedan.budgetmanagement`) is a historical identifier that is kept unchanged because it is the Android application ID.

## 2. Features

| Area | What you get |
|---|---|
| **Transactions** | Record income and expenses, browse history, pending-transaction review, quick-add shortcut, voice entry |
| **Categories** | Predefined categories, custom categories, category suggestions |
| **Budgets** | Per-category spending limits with progress tracking and alerts |
| **Savings goals** | Piggy banks, deposits and withdrawals, scheduled monthly deposits |
| **Debts & receivables** | Money owed and money expected, partial payments, due dates and reminders |
| **Analytics** | Summaries and charts, Jalali/Gregorian date presentation |
| **Export & sharing** | PDF and XLSX export; shareable transaction cards (image) that follow the app's language, direction and currency |
| **Security** | App lock, optional biometric sign-in, encrypted local database, device list with remote sign-out, new-device alerts |
| **Accounts** | Phone-based registration with OTP, optional email, password recovery with a recovery key, soft account deletion with a 30-day restore window |
| **Convenience** | Light/dark theme, home-screen balance widget, amount-hiding mode, notifications and reminders |
| **Bank-SMS import** | Detects bank SMS messages and offers them as pending transactions. **Only in the `direct` distribution**; the Google Play build contains no SMS permissions |

Amounts are stored as integers (Toman) and converted at the UI boundary, so floating-point rounding never reaches the database or the API.

## 3. Architecture

```text
┌──────────────────────────── Android app ────────────────────────────┐
│ Jetpack Compose UI → ViewModels → Use cases → Repositories          │
│                                         │                           │
│          Room + SQLCipher (local DB) ◄──┤                           │
│          WorkManager (sync, reminders)  │                           │
│          Sync engine ── AES-256-GCM encrypt/decrypt ──┐             │
└────────────────────────────────────────────────────── │ ───────────┘
                                                         │ HTTPS
                       Internet ──► Caddy (automatic HTTPS)
                                         │
                                  Go API (chi, JWT)
                                         │
                       MongoDB (single-node replica set) + daily dumps
```

The whole backend (HTTPS, API, MongoDB, backups) runs on one VPS with Docker Compose. No Redis or other external cache is required.

## 4. Security model

What the project does:

- **On device:** Room database encrypted with SQLCipher; the passphrase is random and kept in `EncryptedSharedPreferences` (Android Keystore-backed). Tokens and keys are stored in encrypted preferences. Optional biometric credential uses a Keystore key that requires strong biometrics.
- **Before upload:** every synced record is encrypted with a random 256-bit data key using AES-256-GCM. The data key is stored on the server only in *wrapped* form: once under a key derived from the user's password and once under a key derived from the recovery key (PBKDF2-HMAC-SHA256).
- **Authentication:** passwords are hashed with Argon2id; short-lived JWT access tokens (HS256, issuer-checked, session-versioned); rotating refresh tokens with reuse detection; account lockout after repeated failures; OTP with attempt limits; per-IP and per-user rate limiting.
- **Transport and deployment:** HTTPS only (cleartext is permitted solely in debug builds); MongoDB and the API are reachable only on the internal Docker network; production start-up refuses weak or missing secrets.

Scope and limits (please read):

- The sync server cannot read your data **from a database dump or backup alone**. Because the same password is used both to sign in (it is sent to the server over HTTPS and verified there) and to derive the key that wraps the data key, this design does **not** protect against a malicious or fully compromised *running* server. Do not describe the project as "end-to-end encrypted" or "zero-knowledge" without this qualification.
- If a user loses **both** the password and the recovery key, synced data cannot be recovered.
- Security libraries are not a substitute for an independent audit. Review the implementation and your deployment before making compliance claims.

To report a vulnerability, email **cidna.app@gmail.com** with the subject "Security". Please do not open a public issue for security problems.

## 5. Technology stack

| Area | Technology |
|---|---|
| Android | Kotlin 2.2, Jetpack Compose (Material 3), Navigation, Glance (widget), WorkManager, Room, SQLCipher, AndroidX Security Crypto, Biometric, Lottie, fastexcel |
| Android SDK | `minSdk 26`, `targetSdk 36`, two product flavors: `play` and `direct` |
| Dependency wiring | Manual `AppContainer` (no Hilt) |
| Backend | Go 1.26, chi, golang-jwt, validator, MongoDB driver, `x/crypto` (Argon2) |
| Data store | MongoDB 7 (single-node replica set) |
| Rate limiting | In-process token bucket (single API instance) |
| Deployment | Docker Compose, Caddy, scheduled `mongodump` backups |
| Localization | Persian and English resources, Jalali date utilities |

## 6. Repository layout

```text
cidna/
├── app/
│   ├── src/main/        # Android client (Kotlin, Compose)
│   ├── src/play/        # Google Play flavor manifest (no SMS permissions)
│   ├── src/debug/       # Debug-only network configuration
│   ├── src/test/        # Local unit tests
│   ├── src/androidTest/ # Instrumentation tests
│   └── schemas/         # Exported Room schemas
├── backend/
│   ├── cmd/
│   │   ├── api/             # HTTP API entry point
│   │   └── purge-deleted/   # Permanent purge of expired soft-deleted accounts
│   ├── internal/            # domain · usecase · infrastructure · interface (HTTP)
│   └── deployments/         # Dockerfile, Compose files, Caddyfile, backup script
├── gradle/
├── LICENSE
└── README.md
```

## 7. Getting started

### Prerequisites

- Android Studio compatible with the AGP/Gradle versions in `gradle/libs.versions.toml`, and a matching JDK
- Docker Engine with Compose v2 (backend), or Go matching `backend/go.mod`
- An emulator or a physical Android device

### Run the backend (development)

```bash
cd backend/deployments
cp ../.env.example .env          # then replace every example value
docker compose -f docker-compose.dev.yml up --build -d
curl http://localhost:8080/health
```

Stop it with `docker compose -f docker-compose.dev.yml down`. Add `-v` only if you intentionally want to delete the local MongoDB data.

### Deploy the backend (production, single VPS)

Follow `backend/deployments/README.md` (Persian). In short: point a DNS name at the server, copy `.env.production.example` to `.env`, fill **every** value, then run `docker compose up -d --build`. Production mode requires real SMS and email delivery settings and a JWT secret of at least 64 characters.

### Build the Android app

```bash
# Debug (backend URL: -PdebugBackendBaseUrl=http://<LAN-IP>:8080/)
./gradlew assemblePlayDebug

# Unit tests
./gradlew testPlayDebugUnitTest

# Release (HTTPS backend URL is mandatory; bump versionCode for every upload)
./gradlew bundlePlayRelease -PbackendBaseUrl=https://api.example.com/ -PversionCode=2
./gradlew assembleDirectRelease -PbackendBaseUrl=https://api.example.com/
```

Release signing is read from `keystore.properties` (git-ignored) or `RELEASE_*` environment variables. A phone cannot reach `localhost` on your computer: use the computer's LAN address for debug builds on a physical device.

## 8. Configuration and API

### Key environment variables

| Variable | Purpose |
|---|---|
| `ENV` | `dev`, `test` or `prod` (production enforces stricter checks) |
| `JWT_SECRET` | Token signing secret (≥ 32 chars; ≥ 64 in production) |
| `MONGO_URI`, `MONGO_DB_NAME` | Database connection |
| `TLS_TERMINATED_BY_PROXY`, `TRUSTED_PROXY_CIDRS` | Reverse-proxy HTTPS and trusted client-IP headers |
| `SMS_WEBHOOK_URL`, `SMS_AUTH_TOKEN` | SMS OTP delivery (required in production) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `EMAIL_FROM` | Email OTP delivery (required in production) |
| `SUPPORT_EMAIL` | Contact address shown on the public legal pages |
| `DEV_LOG_OTP` | Prints OTPs to the log; refused outside `dev`/`test` |
| `API_DOMAIN`, `BACKUP_RETENTION_DAYS` | Used by the single-VPS Compose stack |

Never commit secrets, keystores or `.env` files.

### HTTP API

```http
GET  /health

POST /auth/otp/request       POST /auth/register       POST /auth/login
POST /auth/refresh           POST /auth/logout
POST /auth/recovery/prepare  POST /auth/password/reset

# Authenticated (Bearer token)
POST   /api/v1/auth/password/change
GET    /api/v1/account/profile      PUT /api/v1/account/profile
PUT    /api/v1/account/email        POST /api/v1/account/delete   # soft delete
POST   /api/v1/sync
GET    /api/v1/devices              GET /api/v1/devices/events
DELETE /api/v1/devices/{deviceID}
PUT    /api/v1/admin/users/{userID}/role                          # admin only

# Public legal pages (also required by app stores)
GET /privacy-policy   GET /terms   GET /licenses   GET /account-deletion
```

The server's handlers and DTOs are the source of truth for request and response contracts.

## 9. Legal information

> This section describes the project's licensing and legal posture to the best of the maintainer's knowledge. It is **not legal advice**; consult a qualified lawyer before relying on it, especially for commercial or cross-border use.

### 9.1 Software license

Copyright © 2026 Mohammad Sina Ramezani Ohdieh.

Cidna is free software released under the **GNU Affero General Public License, version 3 or (at your option) any later version** (`AGPL-3.0-or-later`). The full text is in [`LICENSE`](LICENSE).

In practical terms:

- **Using the app** for personal or commercial purposes is allowed.
- **Redistributing** the app or a modified version (APK, AAB, or source) requires keeping the license, the copyright notices, and offering the corresponding source code under the same license.
- **Running a modified backend as a network service** triggers AGPL section 13: you must offer the complete corresponding source of your modified version to the users who interact with it over the network.
- The license grants **no rights to the name or logo** "Cidna / سیدنا" (see 9.4).

### 9.2 Contributions

Contributions are accepted under the project license (AGPL-3.0-or-later). By submitting a contribution you confirm that you have the right to do so and that it may be distributed under that license. Do not submit code, fonts, images or animations you do not have the right to license.

### 9.3 Third-party components

Each component keeps its own license; those terms continue to apply.

| Component | License |
|---|---|
| AndroidX libraries (Compose, Material 3, Navigation, Lifecycle, Room, WorkManager, Glance, Biometric, Security Crypto, …) | Apache-2.0 |
| Kotlin, kotlinx.serialization, kotlinx.coroutines | Apache-2.0 |
| Lottie for Android (Airbnb) | Apache-2.0 |
| fastexcel (Dhatim) | MIT |
| SQLCipher for Android (Zetetic), Community Edition | BSD-style |
| Vazirmatn font (The Vazirmatn Project Authors) | SIL Open Font License 1.1 |
| Inter font (The Inter Project Authors) | SIL Open Font License 1.1 |
| Go modules: chi, validator, golang-jwt, godotenv, mongo-go-driver, `golang.org/x/*` | MIT / BSD-3-Clause / Apache-2.0 (see each module) |

The running backend also lists these notices at `/licenses`. Distributing the app means passing on the required copyright and license notices of these components (for example the SQLCipher and OFL font notices).

### 9.4 Name, logos and third-party marks

"Cidna" and "سیدنا" identify this project and its provider. Forks must use a different name and icon. WhatsApp, Instagram, X and Gmail are trademarks of their respective owners; their icons appear only to identify share destinations and imply no endorsement.

### 9.5 User-facing legal documents

The backend serves the Privacy Policy, Terms of Service, open-source notices and account-deletion instructions in Persian and English. Their addresses are what you enter in Google Play Console (Privacy policy URL and the account-deletion URL in the Data safety form). Bump `legalVersion` and the dates in `backend/internal/interface/http/handler/legal_handler.go` whenever the texts or the app's data handling change. The texts are a starting point and should be reviewed by a lawyer.

### 9.6 Personal data and retention

| Data | Purpose | Retention |
|---|---|---|
| Phone number (email, name, gender and birth date optional) | Account, sign-in, OTP | While the account exists |
| Password | Authentication | Stored only as an Argon2id hash |
| Encrypted financial records and wrapped keys | Multi-device sync | While the account exists |
| Device name/model, OS and app version, IP (shown masked), installation hash | Device list and new-device alerts | Device activity ≤ 30 days |
| Temporary sync operations | Sync reliability | ≤ 7 days |

Account deletion is a **soft delete**: sessions are revoked immediately, the account can be restored by signing in within **30 days**, and after that the `purge-deleted` job erases it permanently (run it daily from cron). The app does not include advertising, analytics or third-party trackers. SMS text is processed on the device only, and only in the `direct` distribution.

### 9.7 Disclaimers

- Cidna is a record-keeping tool. It is **not** financial, accounting, legal or tax advice, and it is not connected to any bank account or payment system.
- Automatic SMS detection, voice input and calculations may be wrong. Verify important figures against your bank records.
- The software is provided **"as is", without warranty** of any kind, as stated in the AGPL (sections 15 and 16).
- The Terms of Service are governed by the laws of the Islamic Republic of Iran.

### 9.8 Before publishing a release

- [ ] Confirm that you hold, or may redistribute, every non-code asset: Lottie animations, share-card backgrounds, banners, icons and fonts.
- [ ] Keep the privacy policy, the Play Data safety form and the actual behavior of the app consistent with each other.
- [ ] Use a real `SUPPORT_EMAIL` and review the legal texts with a lawyer.
- [ ] Keep `LICENSE` and the copyright notice in every distributed copy, and publish the corresponding source.

## 10. Contact

Project and legal contact: **cidna.app@gmail.com**

---

<a id="فارسی"></a>
<div dir="rtl">

## فارسی

## ۱. معرفی سیدنا

**سیدنا (Cidna)** یک اپلیکیشن اندروید برای مدیریت مالی شخصی است که یک بک‌اند اختیاری مبتنی بر Go برای ورود به حساب و همگام‌سازی چنددستگاهی در کنار آن قرار دارد.

سه ایده اصلی پروژه:

- **محلی‌محور (Local-first).** اطلاعات شما در یک پایگاه داده رمزنگاری‌شده روی خود گوشی نگه داشته می‌شود. استفاده روزمره سریع است و بدون اینترنت هم کار می‌کند؛ بک‌اند فقط برای حساب کاربری و همگام‌سازی لازم است.
- **حریم خصوصی از ابتدا.** اطلاعات مالی پیش از ارسال روی دستگاه رمزنگاری می‌شوند و سرور داده رمزشده را نگه می‌دارد، نه داده خوانا (دامنه دقیق و محدودیت‌های این ادعا در بخش [مدل امنیتی](#۴-مدل-امنیتی) آمده است).
- **ساخته‌شده برای کاربر فارسی‌زبان و انگلیسی‌زبان.** پشتیبانی کامل از راست‌به‌چپ و چپ‌به‌راست، تقویم جلالی، نمایش تومان/ریال و تشخیص پیامک‌های بانکی ایران.

> **درباره نام.** نام محصول در فارسی **سیدنا** و در انگلیسی **Cidna** است. عبارت «مدیریت بودجه» صرفاً توصیف کارکرد برنامه است و نام پکیج کاتلین (`ir.hamedan.budgetmanagement`) یک شناسه تاریخی است که چون همان Application ID اندروید است، عمداً تغییر نکرده است.

## ۲. قابلیت‌ها

| بخش | امکانات |
|---|---|
| **تراکنش‌ها** | ثبت درآمد و هزینه، مرور سوابق، بررسی تراکنش‌های در انتظار، میان‌بر ثبت سریع، ورود صوتی |
| **دسته‌بندی‌ها** | دسته‌بندی‌های آماده، دسته‌بندی سفارشی، پیشنهاد دسته‌بندی |
| **بودجه** | سقف هزینه برای هر دسته، پیگیری پیشرفت و هشدار |
| **اهداف پس‌انداز** | قلک‌ها، واریز و برداشت، واریز ماهانه زمان‌بندی‌شده |
| **بدهی و طلب** | مبالغ بدهکار و طلبکار، پرداخت‌های جزئی، سررسید و یادآوری |
| **تحلیل مالی** | خلاصه‌ها و نمودارها، نمایش تاریخ جلالی/میلادی |
| **خروجی و اشتراک‌گذاری** | خروجی PDF و XLSX؛ کارت تصویری تراکنش برای اشتراک‌گذاری، هماهنگ با زبان، جهت متن و واحد پول برنامه |
| **امنیت** | قفل برنامه، ورود زیست‌سنجی اختیاری، پایگاه داده رمزنگاری‌شده، فهرست دستگاه‌ها با امکان خروج از راه دور، اعلان ورود از دستگاه جدید |
| **حساب کاربری** | ثبت‌نام با شماره موبایل و OTP، ایمیل اختیاری، بازیابی گذرواژه با کلید بازیابی، حذف نرم حساب با مهلت ۳۰ روزه بازیابی |
| **امکانات تکمیلی** | تم روشن و تیره، ویجت موجودی، حالت مخفی‌کردن مبالغ، اعلان‌ها و یادآورها |
| **دریافت از پیامک بانکی** | تشخیص پیامک بانکی و پیشنهاد آن به‌صورت تراکنش در انتظار. **فقط در نسخه `direct`**؛ نسخه گوگل‌پلی هیچ مجوز پیامکی ندارد |

مبالغ به‌صورت عدد صحیح (تومان) ذخیره و فقط در مرز رابط کاربری تبدیل می‌شوند؛ بنابراین خطای ممیز شناور هرگز به پایگاه داده یا API نمی‌رسد.

## ۳. معماری

<div dir="ltr">

```text
┌──────────────────────────── Android app ────────────────────────────┐
│ Jetpack Compose UI → ViewModels → Use cases → Repositories          │
│                                         │                           │
│          Room + SQLCipher (local DB) ◄──┤                           │
│          WorkManager (sync, reminders)  │                           │
│          Sync engine ── AES-256-GCM encrypt/decrypt ──┐             │
└────────────────────────────────────────────────────── │ ───────────┘
                                                         │ HTTPS
                       Internet ──► Caddy (automatic HTTPS)
                                         │
                                  Go API (chi, JWT)
                                         │
                       MongoDB (single-node replica set) + daily dumps
```

</div>

کل بک‌اند (HTTPS، API، MongoDB و بکاپ) با Docker Compose روی یک VPS اجرا می‌شود. Redis یا هیچ کش خارجی دیگری لازم نیست.

## ۴. مدل امنیتی

آنچه پروژه انجام می‌دهد:

- **روی دستگاه:** پایگاه داده Room با SQLCipher رمزنگاری می‌شود؛ گذرواژه آن تصادفی است و در `EncryptedSharedPreferences` (متکی بر Android Keystore) نگهداری می‌شود. توکن‌ها و کلیدها در تنظیمات رمزنگاری‌شده ذخیره می‌شوند. اعتبارنامه زیست‌سنجی اختیاری از کلید Keystore استفاده می‌کند که زیست‌سنجی قوی را الزامی می‌کند.
- **پیش از ارسال:** هر رکورد همگام‌سازی‌شده با یک کلید داده تصادفی ۲۵۶ بیتی و AES-256-GCM رمزنگاری می‌شود. کلید داده روی سرور فقط به‌صورت *بسته‌بندی‌شده* نگه داشته می‌شود: یک‌بار با کلیدی مشتق‌شده از گذرواژه و یک‌بار با کلیدی مشتق‌شده از کلید بازیابی (PBKDF2-HMAC-SHA256).
- **احراز هویت:** گذرواژه‌ها با Argon2id هش می‌شوند؛ توکن دسترسی JWT کوتاه‌عمر (HS256، با بررسی issuer و نسخه نشست)؛ توکن نوسازی چرخشی با تشخیص استفاده مجدد؛ قفل حساب پس از تلاش‌های ناموفق مکرر؛ OTP با محدودیت تعداد تلاش؛ محدودسازی نرخ درخواست برای هر IP و هر کاربر.
- **ارتباط و استقرار:** فقط HTTPS (ترافیک بدون رمز فقط در نسخه debug مجاز است)؛ MongoDB و API فقط روی شبکه داخلی Docker در دسترس‌اند؛ در حالت production اجرای سرویس با secretهای ضعیف یا نامعتبر متوقف می‌شود.

دامنه و محدودیت‌ها (لطفاً بخوانید):

- سرور **فقط از روی یک نسخه پشتیبان یا dump پایگاه داده** نمی‌تواند داده شما را بخواند. چون یک گذرواژه هم برای ورود (به سرور ارسال و همان‌جا بررسی می‌شود) و هم برای ساخت کلیدِ بسته‌بندی کلید داده استفاده می‌شود، این طراحی در برابر **سرور مخرب یا کاملاً نفوذشده‌ای که در حال اجراست** محافظت نمی‌کند. بدون این توضیح، پروژه را «رمزنگاری سرتاسری» یا «zero-knowledge» معرفی نکنید.
- اگر کاربر **هم** گذرواژه **و هم** کلید بازیابی را گم کند، داده‌های همگام‌سازی‌شده قابل بازیابی نیست.
- کتابخانه‌های امنیتی جایگزین ممیزی مستقل نیستند. پیش از ادعای انطباق با الزامات قانونی، پیاده‌سازی و استقرار را بازبینی کنید.

برای گزارش آسیب‌پذیری، ایمیل **cidna.app@gmail.com** را با موضوع «Security» بنویسید و لطفاً مسائل امنیتی را در Issue عمومی مطرح نکنید.

## ۵. فناوری‌ها

| بخش | فناوری |
|---|---|
| اندروید | Kotlin 2.2، Jetpack Compose (Material 3)، Navigation، Glance (ویجت)، WorkManager، Room، SQLCipher، AndroidX Security Crypto، Biometric، Lottie، fastexcel |
| SDK اندروید | `minSdk 26`، `targetSdk 36`، دو flavor: `play` و `direct` |
| تزریق وابستگی | `AppContainer` دستی (بدون Hilt) |
| بک‌اند | Go 1.26، chi، golang-jwt، validator، درایور MongoDB، `x/crypto` (Argon2) |
| پایگاه داده | MongoDB 7 (replica set تک‌نودی) |
| محدودسازی نرخ | token bucket درون‌حافظه‌ای (یک نمونه API) |
| استقرار | Docker Compose، Caddy، بکاپ زمان‌بندی‌شده `mongodump` |
| زبان و تاریخ | منابع فارسی و انگلیسی، ابزارهای تاریخ جلالی |

## ۶. ساختار مخزن

<div dir="ltr">

```text
cidna/
├── app/
│   ├── src/main/        # Android client (Kotlin, Compose)
│   ├── src/play/        # مانیفست flavor گوگل‌پلی (بدون مجوز پیامک)
│   ├── src/debug/       # تنظیمات شبکه مخصوص debug
│   ├── src/test/        # تست‌های واحد
│   ├── src/androidTest/ # تست‌های instrumentation
│   └── schemas/         # اسکیماهای صادرشده Room
├── backend/
│   ├── cmd/
│   │   ├── api/             # ورودی API
│   │   └── purge-deleted/   # پاکسازی دائمی حساب‌های حذف‌شده منقضی
│   ├── internal/            # domain · usecase · infrastructure · interface (HTTP)
│   └── deployments/         # Dockerfile، فایل‌های Compose، Caddyfile، اسکریپت بکاپ
├── gradle/
├── LICENSE
└── README.md
```

</div>

## ۷. راه‌اندازی

### پیش‌نیازها

- Android Studio سازگار با نسخه‌های AGP/Gradle در `gradle/libs.versions.toml` و JDK مناسب
- Docker Engine همراه با Compose v2 (بک‌اند)، یا Go مطابق `backend/go.mod`
- شبیه‌ساز یا دستگاه واقعی اندروید

### اجرای بک‌اند (توسعه)

<div dir="ltr">

```bash
cd backend/deployments
cp ../.env.example .env          # سپس همه مقادیر نمونه را عوض کنید
docker compose -f docker-compose.dev.yml up --build -d
curl http://localhost:8080/health
```

</div>

توقف سرویس‌ها با `docker compose -f docker-compose.dev.yml down` انجام می‌شود. گزینه `-v` را فقط وقتی بزنید که عمداً می‌خواهید داده‌های محلی MongoDB پاک شود.

### استقرار بک‌اند (production روی یک VPS)

راهنمای `backend/deployments/README.md` را دنبال کنید. خلاصه: یک نام دامنه را روی IP سرور تنظیم کنید، `.env.production.example` را به `.env` کپی کنید، **همه** مقادیر را پر کنید و `docker compose up -d --build` را اجرا کنید. حالت production تنظیمات واقعی ارسال پیامک و ایمیل و JWT secret حداقل ۶۴ کاراکتری می‌خواهد.

### ساخت برنامه اندروید

<div dir="ltr">

```bash
# Debug (آدرس بک‌اند: -PdebugBackendBaseUrl=http://<LAN-IP>:8080/)
./gradlew assemblePlayDebug

# تست‌های واحد
./gradlew testPlayDebugUnitTest

# Release (آدرس HTTPS اجباری است؛ برای هر آپلود versionCode را افزایش دهید)
./gradlew bundlePlayRelease -PbackendBaseUrl=https://api.example.com/ -PversionCode=2
./gradlew assembleDirectRelease -PbackendBaseUrl=https://api.example.com/
```

</div>

امضای release از `keystore.properties` (خارج از Git) یا متغیرهای محیطی `RELEASE_*` خوانده می‌شود. گوشی به `localhost` رایانه شما دسترسی ندارد؛ برای تست روی گوشی واقعی از آدرس رایانه در شبکه محلی استفاده کنید.

## ۸. تنظیمات و API

### متغیرهای محیطی مهم

| متغیر | کاربرد |
|---|---|
| `ENV` | `dev`، `test` یا `prod` (حالت production بررسی‌های سخت‌گیرانه‌تری دارد) |
| `JWT_SECRET` | کلید امضای توکن (حداقل ۳۲ و در production حداقل ۶۴ کاراکتر) |
| `MONGO_URI`، `MONGO_DB_NAME` | اتصال به پایگاه داده |
| `TLS_TERMINATED_BY_PROXY`، `TRUSTED_PROXY_CIDRS` | HTTPS پشت reverse proxy و اعتماد به هدرهای IP کاربر |
| `SMS_WEBHOOK_URL`، `SMS_AUTH_TOKEN` | ارسال OTP پیامکی (در production الزامی) |
| `SMTP_HOST`، `SMTP_PORT`، `SMTP_USER`، `SMTP_PASSWORD`، `EMAIL_FROM` | ارسال OTP ایمیلی (در production الزامی) |
| `SUPPORT_EMAIL` | نشانی تماس در صفحه‌های حقوقی عمومی |
| `DEV_LOG_OTP` | چاپ OTP در لاگ؛ خارج از `dev`/`test` پذیرفته نمی‌شود |
| `API_DOMAIN`، `BACKUP_RETENTION_DAYS` | تنظیمات استقرار تک‌VPS |

هرگز secret، keystore یا فایل `.env` را commit نکنید.

### API

<div dir="ltr">

```http
GET  /health

POST /auth/otp/request       POST /auth/register       POST /auth/login
POST /auth/refresh           POST /auth/logout
POST /auth/recovery/prepare  POST /auth/password/reset

# Authenticated (Bearer token)
POST   /api/v1/auth/password/change
GET    /api/v1/account/profile      PUT /api/v1/account/profile
PUT    /api/v1/account/email        POST /api/v1/account/delete   # soft delete
POST   /api/v1/sync
GET    /api/v1/devices              GET /api/v1/devices/events
DELETE /api/v1/devices/{deviceID}
PUT    /api/v1/admin/users/{userID}/role                          # admin only

# Public legal pages (also required by app stores)
GET /privacy-policy   GET /terms   GET /licenses   GET /account-deletion
```

</div>

مرجع نهایی قرارداد درخواست و پاسخ، handlerها و DTOهای سرور هستند.

## ۹. اطلاعات حقوقی

> این بخش وضعیت مجوز و ملاحظات حقوقی پروژه را بر اساس بهترین دانش نگهدارنده شرح می‌دهد و **مشاوره حقوقی نیست**. پیش از اتکا به آن، به‌ویژه برای استفاده تجاری یا فرامرزی، با یک وکیل یا مشاور حقوقی مشورت کنید.

### ۹.۱ مجوز نرم‌افزار

حق نشر © ۲۰۲۶ محمدسینا رمضانی عهدیه (Mohammad Sina Ramezani Ohdieh).

سیدنا نرم‌افزار آزاد است و تحت **مجوز عمومی همگانی آفرو گنو (GNU AGPL) نسخه ۳ یا (به انتخاب شما) هر نسخه جدیدتر** (`AGPL-3.0-or-later`) منتشر می‌شود. متن کامل در فایل [`LICENSE`](LICENSE) قرار دارد.

در عمل:

- **استفاده از برنامه** برای مقاصد شخصی و تجاری آزاد است.
- **بازتوزیع** برنامه یا نسخه تغییریافته (APK، AAB یا سورس) مستلزم حفظ مجوز و اعلان‌های حق نشر و ارائه سورس متناظر با همان مجوز است.
- **اجرای نسخه تغییریافته بک‌اند به‌عنوان سرویس شبکه‌ای** بند ۱۳ AGPL را فعال می‌کند: باید سورس کامل متناظر نسخه تغییریافته را در اختیار کاربرانی که از طریق شبکه با آن کار می‌کنند قرار دهید.
- این مجوز **هیچ حقی نسبت به نام و نشان** «سیدنا / Cidna» نمی‌دهد (بند ۹.۴).

### ۹.۲ مشارکت در توسعه

مشارکت‌ها تحت مجوز پروژه (AGPL-3.0-or-later) پذیرفته می‌شوند. با ارسال مشارکت تأیید می‌کنید که حق ارائه آن را دارید و می‌تواند با همان مجوز توزیع شود. کد، فونت، تصویر یا انیمیشنی را که حق مجوزدهی آن را ندارید ارسال نکنید.

### ۹.۳ اجزای شخص ثالث

هر جزء مجوز مستقل خود را دارد و شرایط آن همچنان برقرار است.

<div dir="ltr">

| Component | License |
|---|---|
| AndroidX libraries (Compose, Material 3, Navigation, Lifecycle, Room, WorkManager, Glance, Biometric, Security Crypto, …) | Apache-2.0 |
| Kotlin, kotlinx.serialization, kotlinx.coroutines | Apache-2.0 |
| Lottie for Android (Airbnb) | Apache-2.0 |
| fastexcel (Dhatim) | MIT |
| SQLCipher for Android (Zetetic), Community Edition | BSD-style |
| Vazirmatn font (The Vazirmatn Project Authors) | SIL Open Font License 1.1 |
| Inter font (The Inter Project Authors) | SIL Open Font License 1.1 |
| Go modules: chi, validator, golang-jwt, godotenv, mongo-go-driver, `golang.org/x/*` | MIT / BSD-3-Clause / Apache-2.0 |

</div>

بک‌اند در حال اجرا همین اعلان‌ها را در آدرس `/licenses` نیز نمایش می‌دهد. توزیع برنامه یعنی انتقال اعلان‌های حق نشر و مجوز لازم این اجزا (مثلاً اعلان SQLCipher و فونت‌های OFL).

### ۹.۴ نام، نشان و علائم تجاری اشخاص ثالث

«سیدنا» و «Cidna» شناسه این پروژه و ارائه‌دهنده آن هستند؛ فورک‌ها باید نام و آیکون متفاوتی به کار ببرند. WhatsApp، Instagram، X و Gmail علائم تجاری صاحبان خود هستند و آیکون آن‌ها فقط برای نشان‌دادن مقصد اشتراک‌گذاری آمده و به معنای تأیید یا همکاری نیست.

### ۹.۵ متن‌های حقوقی رو به کاربر

بک‌اند سیاست حریم خصوصی، قوانین و مقررات، اعلان‌های متن‌باز و راهنمای حذف حساب را به دو زبان فارسی و انگلیسی ارائه می‌دهد. همین آدرس‌ها را در Google Play Console (نشانی Privacy policy و نشانی حذف حساب در فرم Data safety) وارد می‌کنید. با هر تغییر در متن‌ها یا در نحوه برخورد برنامه با داده، `legalVersion` و تاریخ‌ها را در `backend/internal/interface/http/handler/legal_handler.go` به‌روز کنید. این متن‌ها نقطه شروع‌اند و باید توسط وکیل بازبینی شوند.

### ۹.۶ داده‌های شخصی و مدت نگهداری

| داده | هدف | مدت نگهداری |
|---|---|---|
| شماره موبایل (ایمیل، نام، جنسیت و تاریخ تولد اختیاری) | حساب کاربری، ورود، OTP | تا زمانی که حساب وجود دارد |
| گذرواژه | احراز هویت | فقط به‌صورت هش Argon2id |
| سوابق مالی رمزشده و کلیدهای بسته‌بندی‌شده | همگام‌سازی چنددستگاهی | تا زمانی که حساب وجود دارد |
| نام/مدل دستگاه، نسخه سیستم‌عامل و برنامه، IP (ماسک‌شده نمایش داده می‌شود)، هش نصب | فهرست دستگاه‌ها و اعلان دستگاه جدید | فعالیت دستگاه‌ها حداکثر ۳۰ روز |
| عملیات موقت همگام‌سازی | پایداری همگام‌سازی | حداکثر ۷ روز |

حذف حساب **حذف نرم** است: نشست‌ها فوراً باطل می‌شوند، حساب تا **۳۰ روز** با ورود دوباره قابل بازیابی است و پس از آن ابزار `purge-deleted` آن را برای همیشه پاک می‌کند (آن را روزانه با cron اجرا کنید). برنامه ابزار تبلیغاتی، تحلیل رفتار یا ردیاب شخص ثالث ندارد. متن پیامک فقط روی خود دستگاه و فقط در نسخه `direct` پردازش می‌شود.

### ۹.۷ سلب مسئولیت‌ها

- سیدنا ابزار ثبت و نگهداری سوابق است و **مشاوره مالی، حسابداری، حقوقی یا مالیاتی نیست**؛ همچنین به هیچ حساب بانکی یا سامانه پرداختی متصل نیست.
- تشخیص خودکار پیامک، ورود صوتی و محاسبات ممکن است نادرست باشند. ارقام مهم را با سوابق بانکی خود تطبیق دهید.
- نرم‌افزار مطابق بندهای ۱۵ و ۱۶ مجوز AGPL **«همان‌گونه که هست» و بدون هیچ‌گونه ضمانتی** ارائه می‌شود.
- قوانین و مقررات استفاده تابع قوانین جمهوری اسلامی ایران است.

### ۹.۸ پیش از انتشار هر نسخه

- [ ] مطمئن شوید که حق استفاده و بازتوزیع همه دارایی‌های غیرکد را دارید: انیمیشن‌های Lottie، پس‌زمینه کارت‌های اشتراک‌گذاری، بنرها، آیکون‌ها و فونت‌ها.
- [ ] سیاست حریم خصوصی، فرم Data safety گوگل‌پلی و رفتار واقعی برنامه را با هم هم‌خوان نگه دارید.
- [ ] از `SUPPORT_EMAIL` واقعی استفاده کنید و متن‌های حقوقی را با وکیل بازبینی کنید.
- [ ] فایل `LICENSE` و اعلان حق نشر را در هر نسخه توزیع‌شده نگه دارید و سورس متناظر را منتشر کنید.

## ۱۰. تماس

تماس پروژه و امور حقوقی: **cidna.app@gmail.com**

</div>

---

<div align="center">

**Cidna · سیدنا — Personal Finance Management**

Licensed under GNU AGPL v3.0 or later · `AGPL-3.0-or-later`

</div>
