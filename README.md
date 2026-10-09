<div align="center">

# Cidna · سیدنا

### Personal Finance, Budgeting & Savings — Android + Go
### مدیریت مالی شخصی، بودجه‌بندی و پس‌انداز — اندروید و بک‌اند Go

**A local-first personal finance application designed to help people understand, organize, and plan their money.**

**اپلیکیشنی محلی‌محور برای ثبت، دسته‌بندی، تحلیل و برنامه‌ریزی امور مالی شخصی.**

[English](#english) · [فارسی](#فارسی) · [Architecture](#architecture) · [Setup](#getting-started) · [API](#api-overview) · [License](#license)

</div>

---

<a id="english"></a>
## English

## 1. Project overview

**Cidna** (also referred to in the Android project as **Budget Management**) is an Android personal-finance project built with Kotlin and Jetpack Compose, accompanied by a Go backend for account authentication, profile operations, and synchronization across devices.

The Android app is designed around a **local-first** workflow: day-to-day financial records are managed in a local database, while account and synchronization capabilities connect the app to the backend. This separation is intended to keep routine record-keeping responsive and to support users who may not always have a reliable connection. Exact behavior depends on the configured backend and the state of the synchronization queue.

This repository contains the Android client and the Go service, along with Docker deployment configuration. It is a development project; review the production-readiness checklist before deploying it with real user data.

## 2. Product capabilities

### Transactions and day-to-day money management
- Record income and expenses.
- Organize transactions with categories and review transaction history.
- Use pending-transaction flows where applicable in the app.
- Review financial activity through summaries and analytics screens.
- Use supported transaction-entry helpers, including speech input and bank-SMS parsing, where the device, permissions, and app configuration allow them.

### Categories
- Organize records by category.
- Use the app's predefined category set and category suggestion helpers.
- Keep category-related data available to the transaction and analytics features.

### Budgets and spending limits
- Configure spending limits associated with categories.
- Track activity against configured limits and review the relevant screens.
- Treat alerts and limit calculations as decision-support features; users should verify important financial decisions against their own records.

### Savings goals and piggy banks
- Create and track savings goals (piggy banks).
- Record goal operations and review progress.
- The project includes background-work infrastructure for scheduled goal operations. Confirm the configured schedule, available-balance behavior, and notification delivery in your target build before relying on automated deposits in production.

### Debts and receivables
- Track money owed to others and money expected from others.
- Organize debt/credit records and associated payment information.
- Review payment and due-date information where available in the UI.

### Analytics and date presentation
- Review financial summaries and charts.
- The UI includes Persian/English localization paths and Jalali date utilities for Persian-oriented date presentation.
- The exact chart set and date filters are defined by the current Android implementation.

### Export and sharing
- Export supported financial information to **PDF** and **Excel-compatible XLSX** files.
- Use transaction-sharing helpers to format/share supported transaction details.
- Exported files may contain sensitive financial information. Store and share them carefully.

### Convenience and accessibility
- Light/dark theme support.
- Notifications and reminders supported by the app's notification components.
- A home-screen balance widget.
- Optional biometric authentication using Android's biometric APIs.
- Optional haptic feedback and speech-related entry helpers.
- SMS-related functionality is permission-dependent and should be enabled only when the user understands and wants that behavior.

### Accounts, profile, and synchronization
- Account flows for OTP requests, registration, login, token refresh, recovery preparation, password reset, and logout are represented in the client/backend API.
- Profile API support is present in the Android networking layer.
- The backend exposes device-management and synchronization endpoints.
- The synchronization model includes transactions, categories, budget limits, debt/credit records, savings goals, savings-goal operations, and debt payments.
- Synchronization correctness depends on server configuration, valid credentials, device identity, encryption/key setup, and successful network requests. Do not assume that a feature is production-ready solely because an endpoint or model exists.

## 3. Technology stack

| Area | Technology / implementation |
|---|---|
| Android language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Android architecture | UI screens, repositories, local/network data sources, use cases, ViewModels and a manual `AppContainer` |
| Local persistence | Room |
| Database protection | SQLCipher integration; inspect the active key-management path in the current source before deployment |
| Background jobs | AndroidX WorkManager |
| Secure preferences | AndroidX Security Crypto and Android Keystore-related components where used |
| Authentication / networking | Kotlin HTTP client wrappers and JSON serialization |
| Backend | Go |
| Backend data store | MongoDB |
| Distributed rate-limit dependency | Redis configuration is included in the deployment setup |
| Deployment | Docker Compose |
| Export | PDF and XLSX |
| Localization / dates | Persian and English resources/utilities; Jalali date helpers |

The Android project currently uses a **manual `AppContainer`** for dependency wiring; it does not use Hilt. Versions and build configuration are defined in `gradle/libs.versions.toml`, the root Gradle files, and `app/build.gradle.kts`.

## 4. Repository layout

```text
BudgetManagement/
├── app/
│   ├── src/main/java/ir/hamedan/budgetmanagement/
│   │   ├── data/
│   │   │   ├── export/        # PDF/XLSX export
│   │   │   ├── local/         # Room database, entities, DAOs
│   │   │   ├── network/       # Auth, profile, sync and device APIs
│   │   │   ├── preferences/   # App preferences
│   │   │   ├── repository/    # Data access/repository implementations
│   │   │   ├── security/      # Session, device identity and key helpers
│   │   │   ├── sync/          # Synchronization engine and entity types
│   │   │   └── time/          # Time utilities
│   │   ├── di/                # AppContainer and ViewModel factory
│   │   ├── domain/usecase/    # Domain use cases
│   │   ├── platform/          # Android platform integrations
│   │   ├── ui/                # Compose screens, components and theme
│   │   ├── utils/             # Reusable utilities and parsers
│   │   └── worker/            # Background workers
│   ├── src/test/              # Local unit tests
│   └── build.gradle.kts
├── backend/
│   ├── cmd/                   # API and admin-seeding entry points
│   ├── internal/              # Domain, use cases, infrastructure and HTTP
│   ├── deployments/           # Dockerfiles and Compose configurations
│   ├── .env.example           # Configuration template (no production secrets)
│   ├── go.mod
│   └── go.sum
├── gradle/
├── LICENSE
└── README.md
```

## 5. Getting started

### Prerequisites

- Android Studio compatible with the Gradle/Android Gradle Plugin versions in this repository.
- A compatible JDK as required by the Gradle wrapper and project configuration.
- Go version compatible with `backend/go.mod` if running the service outside Docker.
- Docker Engine and Docker Compose v2 for containerized backend development.
- An Android emulator or physical device for client testing.

### Configure the backend

1. Open a terminal in `backend/`.
2. Create a local environment file from the template. Compose reads the environment file from the directory where the Compose project is invoked, so follow the file location and variable requirements of the selected Compose command.
3. Replace all example credentials and secrets with local development values. Never commit real secrets.
4. For Compose, ensure the MongoDB connection points to the Compose service name (`mongo`) and Redis, when enabled, points to the Compose service name (`redis`) rather than `localhost` from inside the API container.
5. Start the services from the deployment directory:

```bash
cd backend/deployments
# Create deployments/.env from ../.env.example and configure it first.
docker compose up --build -d

docker compose ps
docker compose logs -f api
```

The API container includes a health check for `GET /health`. To check it from the host, use `http://localhost:8080/health` in a local development environment. A response from the health endpoint confirms basic process health, not that every authentication, email/SMS, or synchronization flow is configured correctly.

To stop the stack:

```bash
docker compose down
```

To remove the database volume as well, use `docker compose down -v` **only if you intentionally want to delete local MongoDB data**.

### Configure Android

The debug build currently defines a development backend URL in `app/build.gradle.kts`. If testing on a physical phone, the URL must be reachable from that phone; `localhost` on the phone refers to the phone itself, not the development computer. Use the computer's reachable LAN/hotspot address and ensure the firewall and network isolation settings permit the connection. Do not ship a private LAN URL in a release build.

Build and test from the repository root:

```bash
# Windows PowerShell
.\gradlew.bat assembleDebug
.\gradlew.bat test

# macOS / Linux
./gradlew assembleDebug
./gradlew test
```

Release builds require a real HTTPS backend URL. Configure it with a Gradle property or environment variable, for example:

```bash
./gradlew assembleRelease -PbackendBaseUrl=https://your-api.example.com/
```

or set `BACKEND_BASE_URL` in the build environment. Configure release signing separately; never store signing passwords, JWT secrets, database credentials, SMTP credentials, or SMS-provider tokens in source control.

### Backend development without Docker

```bash
cd backend
go mod download
go test ./...
go run ./cmd/api
```

A local MongoDB replica-set configuration and any required Redis service must be running and configured before starting the API. Consult `backend/.env.example` and the deployment files for required variables.

## 6. Configuration and operational security

Important environment variables include:

| Variable | Purpose |
|---|---|
| `ENV` | Runtime environment; production mode has stricter requirements |
| `HTTP_PORT` | HTTP listener port |
| `MONGO_URI` | MongoDB connection string |
| `MONGO_DB_NAME` | Database name |
| `JWT_SECRET` | Signing secret; use a strong, randomly generated value |
| `JWT_ACCESS_TTL_MINUTES` | Access-token lifetime |
| `JWT_REFRESH_TTL_DAYS` | Refresh-token lifetime |
| `OTP_EXPIRY_MINUTES` | OTP validity period |
| `REDIS_URL` | Redis endpoint for distributed rate limiting |
| `SMS_WEBHOOK_URL`, `SMS_AUTH_TOKEN` | SMS provider integration settings |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `EMAIL_FROM` | Email delivery settings |
| `CORS_ALLOWED_ORIGINS` | Explicit browser-origin allow-list, where applicable |
| `TRUSTED_PROXY_CIDRS` | Proxy networks trusted for forwarded client information |
| `TLS_CERT_FILE`, `TLS_KEY_FILE` | TLS certificate/key paths for deployments configured to terminate TLS in the service |
| `DEV_LOG_OTP` | Development-only OTP logging; must be disabled in production |

**Before exposing the service to the internet:**
- Use TLS end-to-end or a correctly configured trusted TLS-terminating proxy.
- Disable development OTP logging and configure real SMS/email providers as required by the enabled flows.
- Generate strong unique secrets and use a secret manager or mounted secret files where supported.
- Restrict database/network access; do not expose MongoDB or Redis publicly.
- Verify rate limiting, CORS, proxy trust, authentication, recovery, refresh-token rotation and logout behavior.
- Configure database backups, restore drills, monitoring and alerting.
- Review SMS permissions, notification content, exported files, logs and personal-data retention.
- Run dependency, static-analysis and security tests before each release.

Security-related libraries and code paths are not a substitute for an independent security audit. Review the actual implementation and deployment topology before making claims about end-to-end encryption, recovery guarantees, or regulatory compliance.

## 7. API overview

The backend routes include the following API groups (availability and authorization are determined by server configuration):

### Health and authentication

```http
GET  /health
POST /auth/otp/request
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/recovery/prepare
POST /auth/password/reset
POST /auth/logout
```

### Authenticated device and synchronization operations

```http
POST   /api/v1/sync
GET    /api/v1/devices
DELETE /api/v1/devices/{deviceID}
```

### Administrative operation

```http
PUT /api/v1/admin/users/{userID}/role
```

Use the server's request/response contracts and authorization middleware as the source of truth. Do not call administrative endpoints from an untrusted client or expose credentials in logs.

## 8. Data and synchronization model

The sync contract defines entity families including:

- `TRANSACTION`
- `CATEGORY`
- `BUDGET_LIMIT`
- `DEBT_CREDIT`
- `SAVING_GOAL`
- `SAVING_GOAL_OPERATION`
- `DEBT_PAYMENT`

The codebase includes sync state/metadata, entity identifiers, revision/cursor concepts, and background synchronization infrastructure. Multi-device behavior should be validated with realistic scenarios: offline edits, account switching, repeated requests, concurrent updates, deleted records, stale devices, and interrupted syncs.

Financial amounts should use the project's established integer monetary representation and unit contract. Avoid introducing floating-point arithmetic for exact currency amounts. Before changing sync models, update the Android entities/DAOs, serialization contract, backend validation and persistence, and corresponding tests together.

## 9. Testing and quality checks

Android unit tests are under `app/src/test`; instrumentation tests, if present, are under `app/src/androidTest`. The Go service has package-level tests where provided.

Suggested local checks:

```bash
# Android
./gradlew test
./gradlew assembleDebug

# Backend
cd backend
go test ./...
go vet ./...
```

For a release candidate, additionally test registration and login end-to-end, OTP delivery, recovery, refresh and logout; Android-to-Go connectivity on a real device; sync across two devices; account switching; database migration; permission denial; SMS parsing with representative messages; export correctness; scheduled work after reboot; and failure/retry behavior when services are unavailable.

A successful compile alone does not prove data integrity, security, or production readiness.

## 10. Known deployment considerations

- The debug API URL is a development setting. Update it for your network and never use it as a production endpoint.
- Release configuration intentionally requires a non-placeholder HTTPS backend URL.
- SMS, email, TLS, Redis, and production secrets require environment-specific configuration.
- WorkManager timing is subject to Android scheduling constraints and is not a precise real-time scheduler.
- Exported reports, notifications, SMS access and recovery material can expose sensitive data; evaluate the threat model for your deployment.
- Feature behavior can vary by Android version, device manufacturer, granted permissions and backend configuration.

## 11. Contributing

Contributions are welcome under the terms of the project license. Please:

1. Describe the problem and expected behavior in an issue or pull request.
2. Keep changes focused and preserve existing UI/architecture conventions.
3. Add or update tests for bug fixes and behavior changes.
4. Avoid committing secrets, personal data, generated exports, local environment files or build artifacts.
5. Document configuration changes and API contract changes in the same change set.
6. Clearly identify behavior that is incomplete or requires deployment-specific setup.

## 12. License

This project is licensed under the **GNU Affero General Public License v3.0 or (at your option) any later version** (`AGPL-3.0-or-later`). See [`LICENSE`](LICENSE) for the complete license text.

The AGPL includes specific obligations when modified versions are conveyed and when users interact with a modified version over a network. Read the license carefully before distributing binaries or operating a modified public service. Third-party dependencies, fonts, icons, and other bundled assets may have separate license terms; those terms remain applicable to their respective components.

---

<a id="فارسی"></a>
## فارسی

## ۱. معرفی پروژه

**سیدنا (Cidna)** ــ که در بخشی از ساختار اندروید با نام **Budget Management** نیز شناخته می‌شود ــ پروژه‌ای برای مدیریت امور مالی شخصی است. کلاینت اندروید با Kotlin و Jetpack Compose توسعه داده شده و یک سرویس بک‌اند Go برای احراز هویت حساب، عملیات مرتبط با پروفایل و همگام‌سازی چنددستگاهی در کنار آن قرار دارد.

معماری برنامه **محلی‌محور (Local-first)** است؛ یعنی اطلاعات روزمره مالی در پایگاه داده محلی برنامه مدیریت می‌شوند و قابلیت‌های حساب کاربری و همگام‌سازی از طریق بک‌اند در دسترس قرار می‌گیرند. هدف این رویکرد، پاسخ‌گویی مناسب در کارهای روزمره و کاهش وابستگی ثبت اطلاعات به اتصال دائمی اینترنت است. رفتار دقیق هر قابلیت به تنظیمات بک‌اند و وضعیت صف همگام‌سازی وابسته است.

این مخزن شامل کلاینت اندروید، سرویس Go و فایل‌های استقرار Docker است. پیش از استفاده در محیط عملیاتی و با اطلاعات مالی واقعی، چک‌لیست انتشار و امنیت را کامل بررسی کنید.

## ۲. قابلیت‌های محصول

### ثبت و مدیریت تراکنش‌ها
- ثبت درآمد و هزینه.
- دسته‌بندی تراکنش‌ها و مرور سوابق مالی.
- پشتیبانی از جریان‌های مرتبط با تراکنش‌های در انتظار، در بخش‌هایی که در برنامه پیاده‌سازی شده‌اند.
- مشاهده خلاصه‌ها و تحلیل‌های مالی از طریق صفحه‌های مربوطه.
- وجود ابزارهای کمکی برای ورود اطلاعات، از جمله ورودی صوتی و پردازش پیامک بانکی؛ استفاده از این قابلیت‌ها به نسخه برنامه، مجوزهای دستگاه و تنظیمات وابسته است.

### دسته‌بندی‌ها
- سازمان‌دهی اطلاعات مالی بر اساس دسته‌بندی.
- پشتیبانی از مجموعه دسته‌بندی‌های آماده و ابزارهای پیشنهاد دسته‌بندی.
- استفاده از دسته‌بندی‌ها در ثبت تراکنش و بخش‌های تحلیلی.

### بودجه‌بندی و محدودیت‌های مالی
- تعریف سقف هزینه برای دسته‌بندی‌های موردنظر.
- بررسی وضعیت هزینه‌ها نسبت به محدودیت‌های ثبت‌شده.
- تحلیل‌ها و هشدارها ابزار کمکی تصمیم‌گیری هستند؛ برای تصمیم‌های مهم مالی، اطلاعات ثبت‌شده را نیز بررسی کنید.

### اهداف پس‌انداز و قلک‌ها
- ایجاد و پیگیری اهداف پس‌انداز یا قلک‌ها.
- ثبت عملیات مرتبط با هدف و مشاهده روند پیشرفت.
- وجود زیرساخت کارهای پس‌زمینه برای عملیات زمان‌بندی‌شده قلک‌ها. پیش از اتکا به واریز خودکار در محیط عملیاتی، زمان‌بندی، رفتار هنگام کمبود موجودی و ارسال اعلان را در نسخه نهایی آزمایش کنید.

### بدهی‌ها و طلب‌ها
- ثبت مبالغی که باید پرداخت شوند یا از دیگران دریافت شوند.
- نگهداری اطلاعات بدهی/طلب و پرداخت‌های مرتبط.
- بررسی تاریخ سررسید و اطلاعات پرداخت در بخش‌هایی که رابط کاربری ارائه می‌کند.

### تحلیل مالی و تاریخ‌ها
- مشاهده خلاصه‌ها و نمودارهای مالی.
- پشتیبانی‌های زبانی فارسی و انگلیسی و ابزارهای تاریخ جلالی در بخش‌های مرتبط با نمایش تاریخ.
- مجموعه نمودارها و فیلترهای نهایی مطابق پیاده‌سازی فعلی اندروید است.

### خروجی و اشتراک‌گذاری
- خروجی گرفتن از اطلاعات پشتیبانی‌شده در قالب **PDF** و **XLSX سازگار با Excel**.
- وجود ابزارهای قالب‌بندی و اشتراک‌گذاری جزئیات تراکنش.
- فایل‌های خروجی ممکن است اطلاعات مالی حساس داشته باشند؛ در نگهداری و ارسال آن‌ها دقت کنید.

### امکانات تکمیلی
- پشتیبانی از تم روشن و تیره.
- اعلان‌ها و یادآورها از طریق اجزای اعلان برنامه.
- ویجت موجودی در صفحه اصلی اندروید.
- احراز هویت زیست‌سنجی اختیاری با APIهای اندروید.
- بازخورد لمسی و ابزارهای کمکی ورود صوتی.
- قابلیت‌های پیامکی به مجوزهای مربوط وابسته‌اند و باید با اطلاع و انتخاب کاربر فعال شوند.

### حساب کاربری، پروفایل و همگام‌سازی
- مسیرهای API مربوط به درخواست OTP، ثبت‌نام، ورود، تمدید توکن، آماده‌سازی بازیابی، بازنشانی گذرواژه و خروج در کلاینت/بک‌اند تعریف شده‌اند.
- لایه شبکه اندروید دارای پشتیبانی API پروفایل است.
- بک‌اند مسیرهای مدیریت دستگاه و همگام‌سازی را ارائه می‌کند.
- مدل همگام‌سازی شامل تراکنش، دسته‌بندی، محدودیت مالی، بدهی/طلب، هدف پس‌انداز، عملیات هدف و پرداخت بدهی است.
- صحت همگام‌سازی به تنظیمات سرور، اعتبارنامه‌ها، شناسه دستگاه، تنظیمات کلید/رمزنگاری و موفقیت درخواست‌های شبکه وابسته است. وجود یک مدل یا endpoint به‌تنهایی به معنای آماده‌بودن کامل قابلیت برای محیط عملیاتی نیست.

## ۳. فناوری‌های استفاده‌شده

| بخش | فناوری / پیاده‌سازی |
|---|---|
| زبان اندروید | Kotlin |
| رابط کاربری | Jetpack Compose و Material 3 |
| معماری اندروید | صفحه‌ها، Repositoryها، منابع داده محلی/شبکه، Use Caseها، ViewModelها و `AppContainer` دستی |
| ذخیره‌سازی محلی | Room |
| محافظت پایگاه داده | وابستگی SQLCipher؛ مسیر فعال مدیریت کلید باید در سورس نسخه مورد استفاده بررسی شود |
| کارهای پس‌زمینه | AndroidX WorkManager |
| تنظیمات امن | اجزای AndroidX Security Crypto و Android Keystore در بخش‌های مربوط |
| شبکه و احراز هویت | کلاینت‌های HTTP و JSON serialization |
| بک‌اند | Go |
| پایگاه داده بک‌اند | MongoDB |
| محدودسازی توزیع‌شده درخواست‌ها | تنظیمات Redis در استقرار موجود است |
| استقرار | Docker Compose |
| خروجی | PDF و XLSX |
| زبان و تاریخ | ابزارهای فارسی/انگلیسی و تاریخ جلالی |

در وضعیت فعلی، تزریق وابستگی‌ها با **`AppContainer` دستی** انجام می‌شود و Hilt در پروژه استفاده نشده است. نسخه‌ها در `gradle/libs.versions.toml` و فایل‌های Gradle تعریف شده‌اند.

## ۴. ساختار مخزن

```text
BudgetManagement/
├── app/
│   ├── src/main/java/ir/hamedan/budgetmanagement/
│   │   ├── data/
│   │   │   ├── export/        # خروجی PDF/XLSX
│   │   │   ├── local/         # Room، Entityها و DAOها
│   │   │   ├── network/       # APIهای حساب، پروفایل، Sync و دستگاه
│   │   │   ├── preferences/   # تنظیمات برنامه
│   │   │   ├── repository/    # پیاده‌سازی Repositoryها
│   │   │   ├── security/      # نشست، شناسه دستگاه و ابزارهای کلید
│   │   │   ├── sync/          # موتور و انواع همگام‌سازی
│   │   │   └── time/          # ابزارهای زمان
│   │   ├── di/                # AppContainer و ViewModel factory
│   │   ├── domain/usecase/    # منطق‌های کاربردی دامنه
│   │   ├── platform/          # اتصال به قابلیت‌های اندروید
│   │   ├── ui/                # صفحه‌ها، اجزا و تم Compose
│   │   ├── utils/             # ابزارها و parserها
│   │   └── worker/            # کارهای پس‌زمینه
│   ├── src/test/              # تست‌های واحد
│   └── build.gradle.kts
├── backend/
│   ├── cmd/                   # ورودی اجرای API و ابزار seed ادمین
│   ├── internal/              # دامنه، use case، زیرساخت و HTTP
│   ├── deployments/           # Dockerfile و Compose
│   ├── .env.example           # نمونه تنظیمات، بدون secret واقعی
│   ├── go.mod
│   └── go.sum
├── gradle/
├── LICENSE
└── README.md
```

## ۵. راه‌اندازی پروژه

### پیش‌نیازها

- نسخه‌ای از Android Studio که با نسخه‌های Gradle و Android Gradle Plugin پروژه سازگار باشد.
- JDK سازگار با Gradle Wrapper و پیکربندی پروژه.
- نسخه Go مطابق `backend/go.mod` برای اجرای مستقیم بک‌اند.
- Docker Engine و Docker Compose v2 برای اجرای بک‌اند در کانتینر.
- شبیه‌ساز اندروید یا دستگاه واقعی برای تست کلاینت.

### راه‌اندازی بک‌اند با Docker

۱. وارد پوشه `backend/` شوید.
۲. فایل محیطی محلی را از روی نمونه بسازید. دقت کنید Compose فایل محیطی را بر اساس محل اجرای پروژه Compose می‌خواند؛ محل فایل و متغیرهای اجباری را با فرمان انتخابی تطبیق دهید.
۳. مقادیر نمونه را با اطلاعات توسعه محلی جایگزین کنید. هیچ secret واقعی را در Git ثبت نکنید.
۴. در Docker Compose، آدرس MongoDB باید به سرویس `mongo` اشاره کند و آدرس Redis در صورت استفاده باید `redis` باشد، نه `localhost` داخل کانتینر API.
۵. سرویس‌ها را از پوشه استقرار اجرا کنید:

```bash
cd backend/deployments
# ابتدا deployments/.env را از ../.env.example بسازید و تنظیم کنید.
docker compose up --build -d

docker compose ps
docker compose logs -f api
```

برای بررسی سلامت پایه API از میزبان محلی می‌توانید `http://localhost:8080/health` را باز کنید. پاسخ موفق این endpoint فقط سلامت پایه فرایند را نشان می‌دهد و اثبات نمی‌کند که OTP، ایمیل/پیامک یا همه جریان‌های همگام‌سازی به‌درستی تنظیم شده‌اند.

توقف سرویس‌ها:

```bash
docker compose down
```

برای حذف volume پایگاه داده نیز می‌توان از `docker compose down -v` استفاده کرد؛ **این فرمان را فقط زمانی اجرا کنید که عمداً قصد حذف داده‌های محلی MongoDB را دارید.**

### راه‌اندازی اندروید

آدرس API در نسخه debug در `app/build.gradle.kts` تعریف شده است. برای تست روی گوشی واقعی، آدرس باید از همان گوشی قابل دسترسی باشد؛ `localhost` روی گوشی به خود گوشی اشاره می‌کند، نه رایانه توسعه. از آدرس قابل دسترس رایانه در شبکه یا هات‌اسپات استفاده کنید و فایروال و تنظیمات جداسازی شبکه را بررسی کنید. آدرس خصوصی شبکه را در نسخه release قرار ندهید.

از ریشه مخزن:

```bash
# Windows PowerShell
.\gradlew.bat assembleDebug
.\gradlew.bat test

# macOS / Linux
./gradlew assembleDebug
./gradlew test
```

ساخت release به آدرس واقعی HTTPS نیاز دارد. برای نمونه:

```bash
./gradlew assembleRelease -PbackendBaseUrl=https://your-api.example.com/
```

یا متغیر محیطی `BACKEND_BASE_URL` را در محیط build تنظیم کنید. امضای release را جداگانه پیکربندی کنید و رمز امضا، JWT secret، اعتبارنامه پایگاه داده، SMTP یا توکن سرویس پیامک را در مخزن نگه ندارید.

### اجرای بک‌اند بدون Docker

```bash
cd backend
go mod download
go test ./...
go run ./cmd/api
```

پیش از اجرای API باید MongoDB با پیکربندی replica set و هر سرویس Redis موردنیاز در حال اجرا و تنظیم شده باشند. فایل `backend/.env.example` و فایل‌های پوشه استقرار را بررسی کنید.

## ۶. تنظیمات و امنیت عملیاتی

متغیرهای مهم محیطی:

| متغیر | کاربرد |
|---|---|
| `ENV` | محیط اجرا؛ حالت production محدودیت‌های بیشتری دارد |
| `HTTP_PORT` | پورت سرویس HTTP |
| `MONGO_URI` | رشته اتصال MongoDB |
| `MONGO_DB_NAME` | نام پایگاه داده |
| `JWT_SECRET` | کلید امضای توکن؛ باید تصادفی و قوی باشد |
| `JWT_ACCESS_TTL_MINUTES` | طول عمر access token |
| `JWT_REFRESH_TTL_DAYS` | طول عمر refresh token |
| `OTP_EXPIRY_MINUTES` | اعتبار زمانی OTP |
| `REDIS_URL` | آدرس Redis برای محدودسازی توزیع‌شده درخواست‌ها |
| `SMS_WEBHOOK_URL`, `SMS_AUTH_TOKEN` | تنظیمات اتصال سرویس پیامک |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `EMAIL_FROM` | تنظیمات ارسال ایمیل |
| `CORS_ALLOWED_ORIGINS` | فهرست مجاز originهای مرورگر در صورت نیاز |
| `TRUSTED_PROXY_CIDRS` | شبکه‌های پراکسی مورد اعتماد |
| `TLS_CERT_FILE`, `TLS_KEY_FILE` | مسیر گواهی و کلید TLS در استقرارهای مربوط |
| `DEV_LOG_OTP` | ثبت OTP در لاگ توسعه؛ در production باید غیرفعال باشد |

**پیش از انتشار عمومی سرویس:**
- TLS را در سرویس یا پراکسی مورد اعتماد به‌درستی پیکربندی کنید.
- ثبت OTP در لاگ را غیرفعال و سرویس واقعی پیامک/ایمیل موردنیاز را تنظیم کنید.
- secretهای یکتا و قوی تولید کنید و در صورت امکان از secret manager یا فایل‌های secret استفاده کنید.
- دسترسی شبکه و پایگاه داده را محدود کنید؛ MongoDB و Redis را مستقیماً در اینترنت منتشر نکنید.
- محدودسازی نرخ، CORS، اعتماد به پراکسی، احراز هویت، بازیابی حساب، چرخش refresh token و خروج را آزمایش کنید.
- از پایگاه داده نسخه پشتیبان تهیه کنید و فرایند بازیابی را عملاً آزمایش کنید.
- مجوزهای SMS، محتوای اعلان‌ها، فایل‌های خروجی، لاگ‌ها و نگهداری اطلاعات شخصی را بازبینی کنید.
- پیش از هر انتشار، تحلیل وابستگی‌ها، بررسی ایستا و تست‌های امنیتی را اجرا کنید.

وجود کتابخانه‌ها یا کدهای امنیتی جایگزین ممیزی مستقل امنیت نیست. پیش از ادعا درباره رمزنگاری سرتاسری، تضمین بازیابی یا انطباق قانونی، پیاده‌سازی واقعی و معماری استقرار را بررسی کنید.

## ۷. نمای کلی API

مسیرهای بک‌اند شامل گروه‌های زیر است؛ دسترسی و نیاز به احراز هویت تابع پیکربندی سرور است.

### سلامت و احراز هویت

```http
GET  /health
POST /auth/otp/request
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/recovery/prepare
POST /auth/password/reset
POST /auth/logout
```

### عملیات احراز هویت‌شده دستگاه و همگام‌سازی

```http
POST   /api/v1/sync
GET    /api/v1/devices
DELETE /api/v1/devices/{deviceID}
```

### عملیات مدیریتی

```http
PUT /api/v1/admin/users/{userID}/role
```

قرارداد request/response و middlewareهای سرور مرجع نهایی هستند. endpoint مدیریتی را از کلاینت غیرقابل‌اعتماد فراخوانی نکنید و اطلاعات ورود را در لاگ ثبت نکنید.

## ۸. مدل داده و همگام‌سازی

قرارداد Sync خانواده‌های زیر را تعریف می‌کند:

- `TRANSACTION` — تراکنش
- `CATEGORY` — دسته‌بندی
- `BUDGET_LIMIT` — محدودیت مالی
- `DEBT_CREDIT` — بدهی/طلب
- `SAVING_GOAL` — هدف پس‌انداز
- `SAVING_GOAL_OPERATION` — عملیات هدف پس‌انداز
- `DEBT_PAYMENT` — پرداخت بدهی

در کد، اجزای وضعیت/متادیتای Sync، شناسه‌های موجودیت و مفاهیم revision/cursor و زیرساخت همگام‌سازی پس‌زمینه وجود دارند. رفتار چنددستگاهی را با سناریوهای واقعی آزمایش کنید: ویرایش آفلاین، تعویض حساب، درخواست تکراری، ویرایش هم‌زمان، حذف رکورد، دستگاه قدیمی و قطع ارتباط در میانه همگام‌سازی.

مبالغ مالی باید از نمایش عدد صحیح و قرارداد واحد پولی تعریف‌شده در پروژه پیروی کنند. برای مبالغ دقیق پولی، محاسبات ممیز شناور را وارد نکنید. هنگام تغییر مدل Sync، Entity/DAOهای اندروید، قرارداد serialization، اعتبارسنجی و ذخیره‌سازی بک‌اند و تست‌های مربوط را هماهنگ به‌روزرسانی کنید.

## ۹. تست و کنترل کیفیت

تست‌های واحد اندروید در `app/src/test` قرار دارند و تست‌های instrumentation در صورت وجود در `app/src/androidTest` هستند. بک‌اند Go نیز در پکیج‌هایی که تست دارند، تست‌های مرتبط را نگه می‌دارد.

```bash
# Android
./gradlew test
./gradlew assembleDebug

# Backend
cd backend
go test ./...
go vet ./...
```

برای نسخه کاندید انتشار، ثبت‌نام و ورود، تحویل OTP، بازیابی، تمدید و خروج؛ اتصال اندروید به Go روی دستگاه واقعی؛ همگام‌سازی دو دستگاه؛ تعویض حساب؛ migration پایگاه داده؛ رد مجوزها؛ پردازش پیامک‌های نمونه؛ صحت خروجی‌ها؛ اجرای کارهای زمان‌بندی‌شده پس از reboot؛ و رفتار retry هنگام قطع سرویس را نیز آزمایش کنید.

کامپایل موفق به‌تنهایی صحت داده، امنیت یا آمادگی برای production را اثبات نمی‌کند.

## ۱۰. نکات مهم استقرار

- آدرس API نسخه debug صرفاً برای توسعه است؛ برای شبکه خود تنظیمش کنید و از آن در production استفاده نکنید.
- پیکربندی release عمداً به آدرس HTTPS واقعی و غیرنمونه نیاز دارد.
- SMS، ایمیل، TLS، Redis و secretهای عملیاتی نیاز به تنظیمات اختصاصی محیط دارند.
- زمان‌بندی WorkManager تحت محدودیت‌های زمان‌بندی اندروید است و تضمین اجرای دقیق در لحظه مشخص را نمی‌دهد.
- خروجی‌ها، اعلان‌ها، دسترسی SMS و اطلاعات بازیابی ممکن است داده حساس افشا کنند؛ مدل تهدید محیط خود را بررسی کنید.
- رفتار قابلیت‌ها می‌تواند با نسخه اندروید، سازنده دستگاه، مجوزهای اعطاشده و تنظیمات بک‌اند متفاوت باشد.

## ۱۱. مشارکت در توسعه

مشارکت‌ها مطابق شرایط مجوز پروژه پذیرفته می‌شوند. لطفاً:

۱. مشکل و رفتار مورد انتظار را در Issue یا Pull Request توضیح دهید.
۲. تغییرات را محدود و با قراردادهای معماری و رابط کاربری فعلی هماهنگ نگه دارید.
۳. برای رفع باگ یا تغییر رفتار، تست اضافه یا به‌روزرسانی کنید.
۴. secretها، اطلاعات شخصی، خروجی‌های تولیدشده، فایل محیطی محلی و فایل‌های build را commit نکنید.
۵. تغییرات تنظیمات و قرارداد API را در همان تغییر مستند کنید.
۶. رفتار ناقص یا وابسته به تنظیمات محیط را صریح مشخص کنید.

## ۱۲. مجوز

این پروژه تحت **مجوز عمومی همگانی آفرو گنو نسخه ۳ یا (به انتخاب شما) هر نسخه جدیدتر** (`AGPL-3.0-or-later`) منتشر می‌شود. متن کامل در فایل [`LICENSE`](LICENSE) قرار دارد.

مجوز AGPL برای توزیع نسخه‌های تغییریافته و نیز تعامل کاربران با نسخه تغییریافته از طریق شبکه، تعهدات مشخصی دارد. پیش از انتشار فایل اجرایی یا ارائه سرویس عمومی مبتنی بر نسخه تغییریافته، متن مجوز را کامل بخوانید. وابستگی‌های شخص ثالث، فونت‌ها، آیکون‌ها و دارایی‌های همراه ممکن است مجوزهای مستقل داشته باشند و شرایط همان اجزا همچنان برقرار است.

---

<div align="center">

**Cidna / سیدنا — Personal Finance Management**

Licensed under GNU AGPL v3.0 or later · `AGPL-3.0-or-later`

</div>
