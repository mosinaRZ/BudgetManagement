<div align="center">

# Cidna · سیدنا

### The New Generation of Financial Managers!
### نسل جدید مدیر مالی!

**Personal finance, budgeting & savings for Persian and English speakers — private by design, on Android.**

**مدیریت مالی شخصی، بودجه‌بندی و پس‌انداز برای کاربران فارسی‌زبان و انگلیسی‌زبان — با حریم خصوصی از پایه، روی اندروید.**

[English](#english) · [فارسی](#فارسی) · [Legal](#14-legal-information) · [حقوقی](#۱۴-اطلاعات-حقوقی)

`AGPL-3.0-or-later` · Kotlin · Jetpack Compose · Go · MongoDB

</div>

---

<a id="english"></a>
## English

## 1. Executive summary

**Cidna** (Persian: **سیدنا**) turns scattered receipts, bank texts and mental arithmetic into one clear picture of where your money goes — in your language, in your calendar, in your currency, on the phone you already carry.

**The 30-second pitch.** Most finance apps are built for English, Gregorian-calendar, dollar-thinking users and ask you to hand over your data before they give you any value. Cidna flips that: start in seconds as a guest, record income and expenses in Persian or English, see Jalali dates and Toman or Rial amounts natively, and keep your records in an encrypted database on your own device. Create an account only when you want multi-device sync.

| Proof point | What it means for the customer |
|---|---|
| **2 languages, full RTL/LTR** | Persian and English are both first-class, not a translation afterthought |
| **Jalali + Gregorian calendars** | Dates look the way people actually read them |
| **Toman / Rial display** | Amounts shown in the currency people think in |
| **Guest mode** | Try the real product with no account and no phone number |
| **Encrypted on device** | Records live in an encrypted local database |
| **0 ads, 0 analytics, 0 third-party trackers** | The app earns trust instead of monetising attention |
| **30-day restore window** | Deleted an account by mistake? Sign in within 30 days to restore it |

## 2. The problem — and our answer

| What people tell us | How Cidna answers |
|---|---|
| "I never know where my money went." | Fast entry: quick-add shortcut, voice entry, pending-transaction review, and (direct build only) bank-SMS suggestions |
| "Finance apps don't speak my language or use my calendar." | Full Persian/English, RTL/LTR, Jalali dates, Toman/Rial |
| "I don't want to hand my financial data to a stranger." | Local-first storage, and records are encrypted on the device before any sync upload |
| "I have to sign up before I can even try it." | Guest mode — start now, decide on an account later |
| "Goals and debts live in my notes and group chats." | Savings goals, debts and receivables with due dates and reminders |
| "I can't show anyone what I spent." | Shareable transaction cards that follow the app's language, direction and currency |

## 3. Positioning: three pillars

1. **Local-first.** Your records live in an encrypted on-device database. Day-to-day use is fast and works offline; the backend is only needed for accounts and sync.
2. **Private by design.** Financial records are **end-to-end encrypted** on the device *before* upload, and your password never leaves the phone; the sync server stores only ciphertext it cannot read (exact scope and limits are stated in [Security model](#9-security-model)).
3. **Made for Persian and English speakers.** Full RTL/LTR, Jalali calendar, Toman/Rial display and Persian bank-SMS recognition.

> **Naming.** The product name is **Cidna** in English and **سیدنا** in Persian. "Budget Management" is only a descriptive phrase for what the app does, and the Kotlin package (`ir.hamedan.budgetmanagement`) is a historical identifier that is kept unchanged because it is the Android application ID.

## 4. Product tour

| Area | What you get |
|---|---|
| **Guest mode** | Start with no account, protected by a passcode or fingerprint. Later, move the data into a new account or start fresh |
| **Transactions** | Record income and expenses, browse history, pending-transaction review, quick-add shortcut, voice entry |
| **Categories** | Predefined categories, custom categories, category suggestions |
| **Budgets** | Per-category spending limits with progress tracking and alerts |
| **Savings goals** | Piggy banks, deposits and withdrawals, scheduled monthly deposits |
| **Debts & receivables** | Money owed and money expected, partial payments, due dates and reminders |
| **Analytics** | Summaries and charts, Jalali/Gregorian date presentation |
| **Export & sharing** | PDF and XLSX export; shareable transaction cards (image) that follow the app's language, direction and currency |
| **Security** | App lock, optional biometric sign-in, encrypted local database, device list with remote sign-out, new-device alerts |
| **Accounts** | Phone-based registration with OTP, password recovery with a recovery key, soft account deletion with a 30-day restore window |
| **Convenience** | Light/dark theme, home-screen balance widget, amount-hiding mode, notifications and reminders |
| **Bank-SMS import** | Detects bank SMS messages and offers them as pending transactions. **Only in the `direct` distribution**; the Google Play build contains no SMS permissions |

Amounts are stored as integers (Toman) and converted at the UI boundary, so floating-point rounding never reaches the database or the API.

## 5. The customer journey

1. **Try** — open the app as a guest; nothing to sign up for.
2. **Commit** — create an account with a phone number and one-time SMS code, choose a password, and accept the Terms of Service and Privacy Policy.
3. **Decide** — if guest data exists on the device, the sign-up screen asks once: move it into the new account, or start fresh. The choice is applied after the first sign-in, so an abandoned registration never loses data.
4. **Protect** — save the recovery key shown once at the end of registration. It is the only way back in if the password is forgotten.
5. **Go multi-device** — sign in on another device, sync, and get an alert whenever a new device signs in to your account.

## 6. Our trust promise (read with section 8)

We say what we can prove: financial records are end-to-end encrypted before upload; your password and recovery key are never sent to our servers (we store only an Argon2id hash of a password-derived sign-in value); there is no advertising, analytics or third-party tracking; and account deletion is soft for 30 days, then permanent. We claim end-to-end encryption of financial records only, not "zero-knowledge" of everything: account details such as phone number are visible to the service — [section 8](#8-security-model) explains the exact scope.

**Call to action.** Install Cidna from Google Play (or the direct build), try it as a guest today, and create an account when you want your records on more than one device. Questions, partnerships or press: **cidna.app@gmail.com**.

## 7. Architecture

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

## 8. Security model

What the project does:

- **On device:** Room database encrypted with SQLCipher; the passphrase is random and kept in `EncryptedSharedPreferences` (Android Keystore-backed). Tokens and keys are stored in encrypted preferences. Optional biometric credential uses a Keystore key that requires strong biometrics.
- **Before upload:** every synced record is encrypted with a random 256-bit data key using AES-256-GCM. The data key is stored on the server only in *wrapped* form: once under a key derived from the user's password and once under a key derived from the recovery key. **Protocol v2:** the device derives a master key with PBKDF2-HMAC-SHA256 (NFKC-normalised password, 600,000 iterations, per-account salt) and splits it with HKDF into two independent values — a *wrapping key* that never leaves the device and an *authentication secret* that is the only thing sent to the server. Recovery works the same way with a random 256-bit recovery key (a derived *proof* is sent, the key never is). Envelopes are AES-256-GCM, bound to their purpose through AAD. The server therefore holds neither the password, nor the recovery key, nor any key able to unwrap the data key.
- **Authentication:** the password-derived authentication secret is hashed again with Argon2id (a database leak yields nothing directly usable); clients refuse KDF parameters below the supported minimum; short-lived JWT access tokens (HS256, issuer-checked, session-versioned); rotating refresh tokens with reuse detection; account lockout after repeated failures; OTP with attempt limits; per-IP and per-user rate limiting.
- **Transport and deployment:** HTTPS only (cleartext is permitted solely in debug builds); MongoDB and the API are reachable only on the internal Docker network; production start-up refuses weak or missing secrets.

Scope and limits (please read):

- Financial records are end-to-end encrypted: neither the operator nor an attacker who controls the server can decrypt them without guessing the user's password. The residual risk is offline guessing of **weak** passwords by someone who captures the authentication secret (the same model as mainstream password managers), which is why long unique passwords are encouraged and the KDF is expensive.
- Not covered by the encryption: account metadata the service needs to operate (phone number, profile fields, device list, timing and size of sync traffic). Do not describe the project as "zero-knowledge".
- Screen privacy: the app window is `FLAG_SECURE` by default (no screenshots, blank Recents), user-switchable in Settings. Android backup and device transfer are fully excluded.
- If a user loses **both** the password and the recovery key, synced data cannot be recovered.
- Security libraries are not a substitute for an independent audit. Review the implementation and your deployment before making compliance claims.

To report a vulnerability, use a private security report. Please do not open a public issue for security problems.

## 9. Technology stack

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

## 10. Repository layout

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

## 11. Getting started

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

Follow `backend/deployments/README.md` (Persian). In short: point a DNS name at the server, copy `.env.production.example` to `.env`, fill **every** value, then run `docker compose up -d --build`. Production mode requires real SMS delivery settings and a JWT secret of at least 64 characters.

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

## 12. Configuration and API

### Key environment variables

| Variable | Purpose |
|---|---|
| `ENV` | `dev`, `test` or `prod` (production enforces stricter checks) |
| `JWT_SECRET` | Token signing secret (≥ 32 chars; ≥ 64 in production) |
| `MONGO_URI`, `MONGO_DB_NAME` | Database connection |
| `TLS_TERMINATED_BY_PROXY`, `TRUSTED_PROXY_CIDRS` | Reverse-proxy HTTPS and trusted client-IP headers |
| `SMS_WEBHOOK_URL`, `SMS_AUTH_TOKEN` | SMS OTP delivery (required in production) |
| `DEV_LOG_OTP` | Prints OTPs to the log; refused outside `dev`/`test` |
| `API_DOMAIN`, `BACKUP_RETENTION_DAYS` | Used by the single-VPS Compose stack |

Never commit secrets, keystores or `.env` files.

### HTTP API

```http
GET  /health

POST /auth/otp/request       POST /auth/register       POST /auth/login/params
POST /auth/login             POST /auth/refresh        POST /auth/logout
POST /auth/recovery/prepare  POST /auth/password/reset
# auth_secret / new_auth_secret / current_auth_secret / recovery_proof are values
# derived on the device - never the password itself.

# Authenticated (Bearer token)
POST   /api/v1/auth/password/change
GET    /api/v1/account/profile      PUT /api/v1/account/profile
POST /api/v1/account/delete   # soft delete
POST   /api/v1/sync
GET    /api/v1/devices              GET /api/v1/devices/events
DELETE /api/v1/devices/{deviceID}
PUT    /api/v1/admin/users/{userID}/role                          # admin only

# Public legal pages (also required by app stores)
GET /privacy-policy   GET /terms   GET /licenses   GET /account-deletion
```

The server's handlers and DTOs are the source of truth for request and response contracts.

## 13. Legal information

> This section describes the project's licensing and legal posture to the best of the maintainer's knowledge. It is **not legal advice**; consult a qualified lawyer before relying on it, especially for commercial or cross-border use.

### 13.1 Software license

Copyright © 2026 Muhammed Cenna Ramezani Ahdieh.

Cidna is free software released under the **GNU Affero General Public License, version 3 or (at your option) any later version** (`AGPL-3.0-or-later`). The full text is in [`LICENSE`](LICENSE).

In practical terms:

- **Using the app** for personal or commercial purposes is allowed.
- **Redistributing** the app or a modified version (APK, AAB, or source) requires keeping the license, the copyright notices, and offering the corresponding source code under the same license.
- **Running a modified backend as a network service** triggers AGPL section 13: you must offer the complete corresponding source of your modified version to the users who interact with it over the network.
- The license grants **no rights to the name or logo** "Cidna / سیدنا" (see 14.4).

### 13.2 Contributions

Contributions are accepted under the project license (AGPL-3.0-or-later). By submitting a contribution you confirm that you have the right to do so and that it may be distributed under that license. Do not submit code, fonts, images or animations you do not have the right to license.

### 13.3 Third-party components

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

### 13.4 Name, logos and third-party marks

"Cidna" and "سیدنا" identify this project and its provider. Forks must use a different name and icon. WhatsApp, Instagram, X and Gmail are trademarks of their respective owners; their icons appear only to identify share destinations and imply no endorsement.

### 13.5 User-facing legal documents

The backend serves the Privacy Policy, Terms of Service, open-source notices and account-deletion instructions in Persian and English. Their addresses are what you enter in Google Play Console (Privacy policy URL and the account-deletion URL in the Data safety form). Bump `legalVersion` and the dates in `backend/internal/interface/http/handler/legal_handler.go` whenever the texts or the app's data handling change. The texts are a starting point and should be reviewed by a lawyer.

### 13.6 Personal data and retention

| Data | Purpose | Retention |
|---|---|---|
| Phone number (name, gender and birth date optional) | Account, sign-in, OTP | While the account exists |
| Password-derived sign-in value (the password itself is never sent) | Authentication | Stored only as an Argon2id hash |
| Encrypted financial records and wrapped keys | Multi-device sync | While the account exists |
| Device name/model, OS and app version, IP (shown masked), installation hash | Device list and new-device alerts | Device activity ≤ 30 days |
| Temporary sync operations | Sync reliability | ≤ 7 days |

Account deletion is a **soft delete**: sessions are revoked immediately, the account can be restored by signing in within **30 days**, and after that the `purge-deleted` job erases it permanently (run it daily from cron). The app does not include advertising, analytics or third-party trackers. SMS text is processed on the device only, and only in the `direct` distribution.

### 13.7 Disclaimers

- Cidna is a record-keeping tool. It is **not** financial, accounting, legal or tax advice, and it is not connected to any bank account or payment system.
- Automatic SMS detection, voice input and calculations may be wrong. Verify important figures against your bank records.
- The software is provided **"as is", without warranty** of any kind, as stated in the AGPL (sections 15 and 16).
- The Terms of Service are governed by the laws of the Islamic Republic of Iran.

### 13.8 Before publishing a release

- [ ] Confirm that you hold, or may redistribute, every non-code asset: Lottie animations, share-card backgrounds, banners, icons and fonts.
- [ ] Keep the privacy policy, the Play Data safety form and the actual behavior of the app consistent with each other.
- [ ] Review the legal texts with a lawyer.
- [ ] Keep `LICENSE` and the copyright notice in every distributed copy, and publish the corresponding source.

## 14. Contact

Project and legal contact: **cidna.app@gmail.com**

---

<a id="فارسی"></a>
<div dir="rtl">

## فارسی

## ۱. خلاصه مدیریتی

**سیدنا (Cidna)** رسیدهای پراکنده، پیامک‌های بانکی و حساب‌وکتاب‌های ذهنی را به یک تصویر روشن از مسیر پولتان تبدیل می‌کند — به زبان خودتان، با تقویم خودتان، به واحد پول خودتان و روی همان گوشی‌ای که همیشه همراه دارید.

**معرفی ۳۰ ثانیه‌ای.** بیشتر اپلیکیشن‌های مالی برای کاربر انگلیسی‌زبان، تقویم میلادی و ذهنیت دلاری ساخته شده‌اند و پیش از آنکه ارزشی بدهند، اطلاعاتتان را می‌خواهند. سیدنا این رابطه را برعکس می‌کند: در چند ثانیه به‌عنوان مهمان شروع کنید، درآمد و هزینه را فارسی یا انگلیسی ثبت کنید، تاریخ جلالی و مبلغ تومان یا ریال را بومی ببینید و سوابقتان را در پایگاه داده رمزنگاری‌شده روی گوشی خودتان نگه دارید. فقط وقتی همگام‌سازی چنددستگاهی خواستید، حساب بسازید.

| نقطه اتکا | برای مشتری یعنی چه |
|---|---|
| **۲ زبان با پشتیبانی کامل RTL/LTR** | فارسی و انگلیسی هر دو شهروند درجه یک‌اند، نه ترجمه‌ای ثانویه |
| **تقویم جلالی + میلادی** | تاریخ‌ها همان‌طور نمایش داده می‌شوند که مردم می‌خوانند |
| **نمایش تومان / ریال** | مبالغ به واحدی که مردم در ذهن دارند |
| **حالت مهمان** | استفاده از محصول واقعی بدون حساب و بدون شماره موبایل |
| **رمزنگاری روی دستگاه** | سوابق در پایگاه داده رمزنگاری‌شده محلی نگهداری می‌شود |
| **۰ تبلیغ، ۰ تحلیل رفتار، ۰ ردیاب شخص ثالث** | برنامه اعتماد می‌سازد، نه اینکه از توجه شما پول دربیاورد |
| **مهلت ۳۰ روزه بازیابی** | حساب را اشتباهی حذف کردید؟ تا ۳۰ روز با ورود دوباره برمی‌گردد |

## ۲. مسئله و پاسخ ما

| مشتری چه می‌گوید | سیدنا چه پاسخی دارد |
|---|---|
| «هیچ‌وقت نمی‌دانم پولم کجا رفت.» | ثبت سریع: میان‌بر ثبت سریع، ورود صوتی، بررسی تراکنش‌های در انتظار و (فقط نسخه direct) پیشنهاد از پیامک بانکی |
| «اپ‌های مالی نه زبان مرا می‌فهمند نه تقویم مرا.» | فارسی و انگلیسی کامل، RTL/LTR، تاریخ جلالی، تومان/ریال |
| «نمی‌خواهم اطلاعات مالی‌ام را به یک غریبه بدهم.» | ذخیره‌سازی محلی‌محور و رمزنگاری سوابق روی دستگاه پیش از هر ارسال برای همگام‌سازی |
| «حتی برای امتحان کردن هم باید ثبت‌نام کنم.» | حالت مهمان — همین حالا شروع کنید، درباره حساب بعداً تصمیم بگیرید |
| «هدف‌ها و بدهی‌هایم لای یادداشت‌ها و چت‌ها گم شده.» | اهداف پس‌انداز، بدهی و طلب با سررسید و یادآوری |
| «نمی‌توانم خرجم را به کسی نشان بدهم.» | کارت تصویری تراکنش که با زبان، جهت متن و واحد پول برنامه هماهنگ است |

## ۳. جایگاه‌یابی: سه ستون

1. **محلی‌محور (Local-first).** سوابق شما در پایگاه داده رمزنگاری‌شده روی خود گوشی است. استفاده روزمره سریع است و آفلاین هم کار می‌کند؛ بک‌اند فقط برای حساب کاربری و همگام‌سازی لازم است.
2. **حریم خصوصی از ابتدا.** اطلاعات مالی پیش از ارسال روی دستگاه **به‌صورت سرتاسری (End-to-End) رمزنگاری** می‌شوند و گذرواژه‌ی شما هرگز از گوشی خارج نمی‌شود؛ سرور فقط داده‌ی رمزشده‌ای را نگه می‌دارد که نمی‌تواند بخواند (دامنه دقیق و محدودیت‌ها در [مدل امنیتی](#۹-مدل-امنیتی) آمده است).
3. **ساخته‌شده برای فارسی‌زبان و انگلیسی‌زبان.** پشتیبانی کامل از راست‌به‌چپ و چپ‌به‌راست، تقویم جلالی، نمایش تومان/ریال و تشخیص پیامک‌های بانکی ایران.

> **درباره نام.** نام محصول در فارسی **سیدنا** و در انگلیسی **Cidna** است. عبارت «مدیریت بودجه» صرفاً توصیف کارکرد برنامه است و نام پکیج کاتلین (`ir.hamedan.budgetmanagement`) یک شناسه تاریخی است که چون همان Application ID اندروید است، عمداً تغییر نکرده است.

## ۴. تور محصول

| بخش | امکانات |
|---|---|
| **حالت مهمان** | شروع بدون حساب، با رمز یا اثر انگشت. بعداً می‌توانید اطلاعات را به حساب جدید منتقل کنید یا از صفر شروع کنید |
| **تراکنش‌ها** | ثبت درآمد و هزینه، مرور سوابق، بررسی تراکنش‌های در انتظار، میان‌بر ثبت سریع، ورود صوتی |
| **دسته‌بندی‌ها** | دسته‌بندی‌های آماده، دسته‌بندی سفارشی، پیشنهاد دسته‌بندی |
| **بودجه** | سقف هزینه برای هر دسته، پیگیری پیشرفت و هشدار |
| **اهداف پس‌انداز** | قلک‌ها، واریز و برداشت، واریز ماهانه زمان‌بندی‌شده |
| **بدهی و طلب** | مبالغ بدهکار و طلبکار، پرداخت‌های جزئی، سررسید و یادآوری |
| **تحلیل مالی** | خلاصه‌ها و نمودارها، نمایش تاریخ جلالی/میلادی |
| **خروجی و اشتراک‌گذاری** | خروجی PDF و XLSX؛ کارت تصویری تراکنش برای اشتراک‌گذاری، هماهنگ با زبان، جهت متن و واحد پول برنامه |
| **امنیت** | قفل برنامه، ورود زیست‌سنجی اختیاری، پایگاه داده رمزنگاری‌شده، فهرست دستگاه‌ها با امکان خروج از راه دور، اعلان ورود از دستگاه جدید |
| **حساب کاربری** | ثبت‌نام با شماره موبایل و OTP، بازیابی گذرواژه با کلید بازیابی، حذف نرم حساب با مهلت ۳۰ روزه بازیابی |
| **امکانات تکمیلی** | تم روشن و تیره، ویجت موجودی، حالت مخفی‌کردن مبالغ، اعلان‌ها و یادآورها |
| **دریافت از پیامک بانکی** | تشخیص پیامک بانکی و پیشنهاد آن به‌صورت تراکنش در انتظار. **فقط در نسخه `direct`**؛ نسخه گوگل‌پلی هیچ مجوز پیامکی ندارد |

مبالغ به‌صورت عدد صحیح (تومان) ذخیره و فقط در مرز رابط کاربری تبدیل می‌شوند؛ بنابراین خطای ممیز شناور هرگز به پایگاه داده یا API نمی‌رسد.

## ۵. سفر مشتری

1. **امتحان** — برنامه را به‌عنوان مهمان باز کنید؛ چیزی برای ثبت‌نام نیست.
2. **تعهد** — با شماره موبایل و کد پیامکی یک‌بارمصرف حساب بسازید، گذرواژه تعیین کنید و قوانین و مقررات و سیاست حریم خصوصی را بپذیرید.
3. **تصمیم** — اگر روی دستگاه اطلاعات مهمان وجود داشته باشد، صفحه ثبت‌نام یک‌بار می‌پرسد: به حساب جدید منتقل شود یا از صفر شروع کنید. انتخاب شما بعد از اولین ورود اعمال می‌شود، بنابراین ثبت‌نامِ نیمه‌کاره هرگز اطلاعاتتان را از بین نمی‌برد.
4. **محافظت** — کلید بازیابی را که یک‌بار در پایان ثبت‌نام نمایش داده می‌شود ذخیره کنید. اگر گذرواژه را فراموش کنید، تنها راه بازگشت همین است.
5. **چنددستگاهی شدن** — روی دستگاه دیگر وارد شوید، همگام‌سازی کنید و با هر ورود از دستگاه جدید اعلان بگیرید.

## ۶. تعهد ما در برابر اعتماد (همراه با بخش ۸ بخوانید)

آنچه می‌توانیم ثابت کنیم را می‌گوییم: سوابق مالی پیش از ارسال به‌صورت سرتاسری رمزنگاری می‌شوند؛ گذرواژه و کلید بازیابی هرگز به سرور ارسال نمی‌شوند (فقط هش Argon2id یک مقدار ورودِ مشتق‌شده از گذرواژه نگهداری می‌شود)؛ هیچ تبلیغ، تحلیل رفتار یا ردیاب شخص ثالثی وجود ندارد؛ و حذف حساب تا ۳۰ روز نرم است و پس از آن دائمی. رمزنگاری سرتاسری را فقط برای سوابق مالی ادعا می‌کنیم، نه «zero-knowledge» برای همه چیز: اطلاعات حساب مانند شماره موبایل برای سرویس قابل‌مشاهده است — دامنه دقیق در [بخش ۸](#۸-مدل-امنیتی) آمده است.

**دعوت به اقدام.** سیدنا را از گوگل‌پلی (یا نسخه direct) نصب کنید، همین امروز به‌عنوان مهمان امتحانش کنید و وقتی خواستید سوابقتان روی بیش از یک دستگاه باشد، حساب بسازید. سؤال، همکاری یا رسانه: **cidna.app@gmail.com**.

## ۷. معماری

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

## ۸. مدل امنیتی

آنچه پروژه انجام می‌دهد:

- **روی دستگاه:** پایگاه داده Room با SQLCipher رمزنگاری می‌شود؛ گذرواژه آن تصادفی است و در `EncryptedSharedPreferences` (متکی بر Android Keystore) نگهداری می‌شود. توکن‌ها و کلیدها در تنظیمات رمزنگاری‌شده ذخیره می‌شوند. اعتبارنامه زیست‌سنجی اختیاری از کلید Keystore استفاده می‌کند که زیست‌سنجی قوی را الزامی می‌کند.
- **پیش از ارسال:** هر رکورد همگام‌سازی‌شده با یک کلید داده تصادفی ۲۵۶ بیتی و AES-256-GCM رمزنگاری می‌شود. کلید داده روی سرور فقط به‌صورت *بسته‌بندی‌شده* نگه داشته می‌شود: یک‌بار با کلیدی مشتق‌شده از گذرواژه و یک‌بار با کلیدی مشتق‌شده از کلید بازیابی. **پروتکل نسخه ۲:** دستگاه با PBKDF2-HMAC-SHA256 (گذرواژه‌ی نرمال‌شده با NFKC، ۶۰۰٬۰۰۰ تکرار، salt اختصاصی هر حساب) یک کلید اصلی می‌سازد و با HKDF آن را به دو مقدار مستقل تقسیم می‌کند: *کلید بسته‌بندی* که هرگز از دستگاه خارج نمی‌شود و *رمز احراز هویت* که تنها چیزی است که به سرور فرستاده می‌شود. بازیابی هم همین‌طور است (کلید بازیابی ۲۵۶ بیتی تصادفی؛ فقط یک «proof» مشتق‌شده ارسال می‌شود). envelopeها AES-256-GCM هستند و با AAD به کاربردشان گره خورده‌اند. بنابراین سرور نه گذرواژه، نه کلید بازیابی و نه هیچ کلیدی که کلید داده را باز کند در اختیار ندارد.
- **احراز هویت:** رمز احراز هویتِ مشتق‌شده از گذرواژه دوباره با Argon2id هش می‌شود (نشت پایگاه داده چیز قابل‌استفاده‌ای نمی‌دهد)؛ کلاینت پارامترهای KDF ضعیف‌تر از حداقل را رد می‌کند؛ توکن دسترسی JWT کوتاه‌عمر (HS256، با بررسی issuer و نسخه نشست)؛ توکن نوسازی چرخشی با تشخیص استفاده مجدد؛ قفل حساب پس از تلاش‌های ناموفق مکرر؛ OTP با محدودیت تعداد تلاش؛ محدودسازی نرخ درخواست برای هر IP و هر کاربر.
- **ارتباط و استقرار:** فقط HTTPS (ترافیک بدون رمز فقط در نسخه debug مجاز است)؛ MongoDB و API فقط روی شبکه داخلی Docker در دسترس‌اند؛ در حالت production اجرای سرویس با secretهای ضعیف یا نامعتبر متوقف می‌شود.

دامنه و محدودیت‌ها (لطفاً بخوانید):

- سوابق مالی سرتاسری رمزنگاری می‌شوند: نه اپراتور و نه مهاجمی که سرور را در اختیار دارد بدون حدس زدن گذرواژه‌ی کاربر نمی‌تواند آن‌ها را رمزگشایی کند. ریسک باقی‌مانده، حدس آفلاینِ گذرواژه‌های **ضعیف** توسط کسی است که رمز احراز هویت را ضبط کرده باشد (همان مدل مدیرهای گذرواژه‌ی رایج)؛ به همین دلیل گذرواژه‌ی بلند و منحصربه‌فرد توصیه می‌شود و KDF سنگین است.
- زیر پوشش رمزنگاری نیست: فراداده‌ای که سرویس برای کار لازم دارد (شماره موبایل، نمایه، فهرست دستگاه‌ها، زمان و حجم همگام‌سازی). پروژه را «zero-knowledge» معرفی نکنید.
- حریم صفحه: پنجره‌ی برنامه به‌طور پیش‌فرض `FLAG_SECURE` است (بدون اسکرین‌شات و Recents خالی) و کاربر در تنظیمات می‌تواند آن را تغییر دهد. بکاپ و انتقال دستگاه کاملاً مستثنا شده‌اند.
- اگر کاربر **هم** گذرواژه **و هم** کلید بازیابی را گم کند، داده‌های همگام‌سازی‌شده قابل بازیابی نیست.
- کتابخانه‌های امنیتی جایگزین ممیزی مستقل نیستند. پیش از ادعای انطباق با الزامات قانونی، پیاده‌سازی و استقرار را بازبینی کنید.

برای گزارش آسیب‌پذیری، از یک کانال خصوصی و امن گزارش امنیتی استفاده کنید و لطفاً مسائل امنیتی را در Issue عمومی مطرح نکنید.

## ۹. فناوری‌ها

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

## ۱۰. ساختار مخزن

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

## ۱۱. راه‌اندازی

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

راهنمای `backend/deployments/README.md` را دنبال کنید. خلاصه: یک نام دامنه را روی IP سرور تنظیم کنید، `.env.production.example` را به `.env` کپی کنید، **همه** مقادیر را پر کنید و `docker compose up -d --build` را اجرا کنید. حالت production تنظیمات واقعی ارسال پیامک و JWT secret حداقل ۶۴ کاراکتری می‌خواهد.

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

## ۱۲. تنظیمات و API

### متغیرهای محیطی مهم

| متغیر | کاربرد |
|---|---|
| `ENV` | `dev`، `test` یا `prod` (حالت production بررسی‌های سخت‌گیرانه‌تری دارد) |
| `JWT_SECRET` | کلید امضای توکن (حداقل ۳۲ و در production حداقل ۶۴ کاراکتر) |
| `MONGO_URI`، `MONGO_DB_NAME` | اتصال به پایگاه داده |
| `TLS_TERMINATED_BY_PROXY`، `TRUSTED_PROXY_CIDRS` | HTTPS پشت reverse proxy و اعتماد به هدرهای IP کاربر |
| `SMS_WEBHOOK_URL`، `SMS_AUTH_TOKEN` | ارسال OTP پیامکی (در production الزامی) |
| `DEV_LOG_OTP` | چاپ OTP در لاگ؛ خارج از `dev`/`test` پذیرفته نمی‌شود |
| `API_DOMAIN`، `BACKUP_RETENTION_DAYS` | تنظیمات استقرار تک‌VPS |

هرگز secret، keystore یا فایل `.env` را commit نکنید.

### API

<div dir="ltr">

```http
GET  /health

POST /auth/otp/request       POST /auth/register       POST /auth/login/params
POST /auth/login             POST /auth/refresh        POST /auth/logout
POST /auth/recovery/prepare  POST /auth/password/reset
# auth_secret / new_auth_secret / current_auth_secret / recovery_proof are values
# derived on the device - never the password itself.

# Authenticated (Bearer token)
POST   /api/v1/auth/password/change
GET    /api/v1/account/profile      PUT /api/v1/account/profile
POST /api/v1/account/delete   # soft delete
POST   /api/v1/sync
GET    /api/v1/devices              GET /api/v1/devices/events
DELETE /api/v1/devices/{deviceID}
PUT    /api/v1/admin/users/{userID}/role                          # admin only

# Public legal pages (also required by app stores)
GET /privacy-policy   GET /terms   GET /licenses   GET /account-deletion
```

</div>

مرجع نهایی قرارداد درخواست و پاسخ، handlerها و DTOهای سرور هستند.

## ۱۳. اطلاعات حقوقی

> این بخش وضعیت مجوز و ملاحظات حقوقی پروژه را بر اساس بهترین دانش نگهدارنده شرح می‌دهد و **مشاوره حقوقی نیست**. پیش از اتکا به آن، به‌ویژه برای استفاده تجاری یا فرامرزی، با یک وکیل یا مشاور حقوقی مشورت کنید.

### ۱۳.۱ مجوز نرم‌افزار

حق نشر © ۲۰۲۶ محمدسینا رمضانی عهدیه.

سیدنا نرم‌افزار آزاد است و تحت **مجوز عمومی همگانی آفرو گنو (GNU AGPL) نسخه ۳ یا (به انتخاب شما) هر نسخه جدیدتر** (`AGPL-3.0-or-later`) منتشر می‌شود. متن کامل در فایل [`LICENSE`](LICENSE) قرار دارد.

در عمل:

- **استفاده از برنامه** برای مقاصد شخصی و تجاری آزاد است.
- **بازتوزیع** برنامه یا نسخه تغییریافته (APK، AAB یا سورس) مستلزم حفظ مجوز و اعلان‌های حق نشر و ارائه سورس متناظر با همان مجوز است.
- **اجرای نسخه تغییریافته بک‌اند به‌عنوان سرویس شبکه‌ای** بند ۱۳ AGPL را فعال می‌کند: باید سورس کامل متناظر نسخه تغییریافته را در اختیار کاربرانی که از طریق شبکه با آن کار می‌کنند قرار دهید.
- این مجوز **هیچ حقی نسبت به نام و نشان** «سیدنا / Cidna» نمی‌دهد (بند ۱۴.۴).

### ۱۳.۲ مشارکت در توسعه

مشارکت‌ها تحت مجوز پروژه (AGPL-3.0-or-later) پذیرفته می‌شوند. با ارسال مشارکت تأیید می‌کنید که حق ارائه آن را دارید و می‌تواند با همان مجوز توزیع شود. کد، فونت، تصویر یا انیمیشنی را که حق مجوزدهی آن را ندارید ارسال نکنید.

### ۱۳.۳ اجزای شخص ثالث

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

### ۱۳.۴ نام، نشان و علائم تجاری اشخاص ثالث

«سیدنا» و «Cidna» شناسه این پروژه و ارائه‌دهنده آن هستند؛ فورک‌ها باید نام و آیکون متفاوتی به کار ببرند. WhatsApp، Instagram، X و Gmail علائم تجاری صاحبان خود هستند و آیکون آن‌ها فقط برای نشان‌دادن مقصد اشتراک‌گذاری آمده و به معنای تأیید یا همکاری نیست.

### ۱۳.۵ متن‌های حقوقی رو به کاربر

بک‌اند سیاست حریم خصوصی، قوانین و مقررات، اعلان‌های متن‌باز و راهنمای حذف حساب را به دو زبان فارسی و انگلیسی ارائه می‌دهد. همین آدرس‌ها را در Google Play Console (نشانی Privacy policy و نشانی حذف حساب در فرم Data safety) وارد می‌کنید. با هر تغییر در متن‌ها یا در نحوه برخورد برنامه با داده، `legalVersion` و تاریخ‌ها را در `backend/internal/interface/http/handler/legal_handler.go` به‌روز کنید. این متن‌ها نقطه شروع‌اند و باید توسط وکیل بازبینی شوند.

### ۱۳.۶ داده‌های شخصی و مدت نگهداری

| داده | هدف | مدت نگهداری |
|---|---|---|
| شماره موبایل (نام، جنسیت و تاریخ تولد اختیاری) | حساب کاربری، ورود، OTP | تا زمانی که حساب وجود دارد |
| مقدار ورودِ مشتق‌شده از گذرواژه (خود گذرواژه ارسال نمی‌شود) | احراز هویت | فقط به‌صورت هش Argon2id |
| سوابق مالی رمزشده و کلیدهای بسته‌بندی‌شده | همگام‌سازی چنددستگاهی | تا زمانی که حساب وجود دارد |
| نام/مدل دستگاه، نسخه سیستم‌عامل و برنامه، IP (ماسک‌شده نمایش داده می‌شود)، هش نصب | فهرست دستگاه‌ها و اعلان دستگاه جدید | فعالیت دستگاه‌ها حداکثر ۳۰ روز |
| عملیات موقت همگام‌سازی | پایداری همگام‌سازی | حداکثر ۷ روز |

حذف حساب **حذف نرم** است: نشست‌ها فوراً باطل می‌شوند، حساب تا **۳۰ روز** با ورود دوباره قابل بازیابی است و پس از آن ابزار `purge-deleted` آن را برای همیشه پاک می‌کند (آن را روزانه با cron اجرا کنید). برنامه ابزار تبلیغاتی، تحلیل رفتار یا ردیاب شخص ثالث ندارد. متن پیامک فقط روی خود دستگاه و فقط در نسخه `direct` پردازش می‌شود.

### ۱۳.۷ سلب مسئولیت‌ها

- سیدنا ابزار ثبت و نگهداری سوابق است و **مشاوره مالی، حسابداری، حقوقی یا مالیاتی نیست**؛ همچنین به هیچ حساب بانکی یا سامانه پرداختی متصل نیست.
- تشخیص خودکار پیامک، ورود صوتی و محاسبات ممکن است نادرست باشند. ارقام مهم را با سوابق بانکی خود تطبیق دهید.
- نرم‌افزار مطابق بندهای ۱۵ و ۱۶ مجوز AGPL **«همان‌گونه که هست» و بدون هیچ‌گونه ضمانتی** ارائه می‌شود.
- قوانین و مقررات استفاده تابع قوانین جمهوری اسلامی ایران است.

### ۱۳.۸ پیش از انتشار هر نسخه

- [ ] مطمئن شوید که حق استفاده و بازتوزیع همه دارایی‌های غیرکد را دارید: انیمیشن‌های Lottie، پس‌زمینه کارت‌های اشتراک‌گذاری، بنرها، آیکون‌ها و فونت‌ها.
- [ ] سیاست حریم خصوصی، فرم Data safety گوگل‌پلی و رفتار واقعی برنامه را با هم هم‌خوان نگه دارید.
- [ ] متن‌های حقوقی را با وکیل بازبینی کنید.
- [ ] فایل `LICENSE` و اعلان حق نشر را در هر نسخه توزیع‌شده نگه دارید و سورس متناظر را منتشر کنید.

## ۱۴. تماس

تماس پروژه و امور حقوقی: **cidna.app@gmail.com**

</div>

---

<div align="center">

**Cidna · سیدنا — Personal Finance Management**

Licensed under GNU AGPL v3.0 or later · `AGPL-3.0-or-later`

</div>
