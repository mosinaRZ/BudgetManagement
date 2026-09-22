# Cidna — Personal Finance Management
# سیدنا — مدیریت امور مالی شخصی

> A privacy-focused personal finance application for Android, backed by a Go service for authentication and multi-device synchronization.
>
> یک اپلیکیشن مدیریت امور مالی شخصی برای اندروید با تمرکز بر حریم خصوصی، همراه با بک‌اند Go برای احراز هویت و همگام‌سازی چنددستگاهی.

## 🇬🇧 English

### Overview

Cidna is a local-first personal finance application. Financial records are primarily managed on-device and synchronized through an encrypted protocol with the Go backend.

### Technology

- Android: Kotlin, Jetpack Compose, Material 3
- Local data: Room + SQLCipher
- Background work: WorkManager
- Secure local storage: Android Keystore-backed encrypted preferences
- Backend: Go
- Database: MongoDB
- Auth: OTP, Argon2id, JWT access tokens, rotating refresh tokens
- Sync: cursor-based, revisioned, idempotent and conflict-aware
- Export: PDF and XLSX
- Localization: Persian and English
- Optional features: SMS parsing, notifications, widget, speech input and biometrics

### Android Structure

```text
app/src/main/java/ir/hamedan/budgetmanagement/
├── data/
│   ├── local/
│   ├── network/
│   ├── repository/
│   ├── security/
│   ├── sync/
│   └── time/
├── di/
├── ui/
│   ├── components/
│   ├── navigation/
│   ├── screens/
│   └── theme/
├── utils/
└── worker/
```

The current implementation uses a manual `AppContainer`; it does **not** currently use Hilt.

### Go Backend Structure

```text
backend/
├── cmd/
├── internal/
│   ├── domain/
│   ├── infrastructure/
│   ├── interface/http/
│   ├── pkg/
│   └── usecase/
└── deployments/
```

### Main Features

- Income and expense transactions
- Categories
- Budget limits
- Saving goals
- Debts and receivables
- Analytics
- Bank-SMS transaction parsing
- Notifications and widget
- Speech-to-text transaction input
- PDF/XLSX export
- Persian/Jalali and English date presentation
- Light/dark themes
- Biometric authentication
- Encrypted local database
- Encrypted multi-device synchronization

### Security

Backend protections include Argon2id, short-lived JWT access tokens, rotating refresh tokens, reuse detection, session-version invalidation, rate limiting, validation and security headers.

Android protections include SQLCipher, encrypted preferences, installation-scoped device identity, AES-GCM sync payload encryption and biometric authentication.

### E2EE / Recovery

The client maintains a data encryption key (DEK). The DEK can be wrapped using a password-derived key and a recovery key. The backend stores opaque envelopes and encrypted records.

Losing the recovery material may make historical encrypted data unrecoverable after password reset or device loss. Production UX should therefore provide a clear recovery-key backup/export flow.

### Synchronization

Synchronized entity types:

```text
TRANSACTION
CATEGORY
BUDGET_LIMIT
DEBT_CREDIT
SAVING_GOAL
SAVING_GOAL_OPERATION
DEBT_PAYMENT
```

The protocol uses UUID/string logical IDs, entity versions, server revisions, cursors, request IDs, request fingerprints, tombstones, conflict reporting and MongoDB transactions.

Money should use integer minor/base units (`Long`) rather than `Double`.

### API

Public:

```text
GET  /health
POST /auth/otp/request
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/recovery/prepare
POST /auth/password/reset
POST /auth/logout
```

Authenticated:

```text
POST   /api/v1/sync
GET    /api/v1/devices
DELETE /api/v1/devices/{deviceID}
```

Admin:

```text
PUT /api/v1/admin/users/{userID}/role
```

### Development

```bash
cd backend
go mod download
go test ./...
go run ./cmd/api
```

```bash
docker compose -f deployments/docker-compose.yml up --build
```

Android:

```bash
./gradlew assembleDebug
./gradlew test
```

### Production Checklist

- [ ] Remove all authentication bypasses.
- [ ] Verify register/OTP/login/recovery/reset end-to-end.
- [ ] Verify multi-device sync and conflicts.
- [ ] Verify offline behavior and account switching.
- [ ] Run unit/instrumentation/integration tests.
- [ ] Run dependency vulnerability scans.
- [ ] Configure release signing and secret management.
- [ ] Configure TLS, CORS and rate limiting.
- [ ] Configure backups and restore drills.
- [ ] Add monitoring/crash reporting.
- [ ] Security-review exported Android components and SMS parsing.

### License

See `LICENSE`.

---

## 🇮🇷 فارسی

### معرفی

Cidna یک اپلیکیشن مدیریت امور مالی شخصی با معماری local-first است. اطلاعات مالی روی دستگاه مدیریت می‌شوند و از طریق یک پروتکل رمزنگاری‌شده با بک‌اند Go همگام می‌شوند.

### فناوری‌ها

- Kotlin و Jetpack Compose
- Room و SQLCipher
- WorkManager
- Android Keystore و Encrypted Preferences
- Go و MongoDB
- Argon2id، JWT و Refresh Token چرخشی
- OTP و recovery
- همگام‌سازی cursor-based و revision-based
- PDF و XLSX
- فارسی و انگلیسی
- تقویم جلالی
- SMS parser، اعلان، widget، ورود صوتی و biometric

### معماری اندروید

```text
data/local        → Room، Entity و DAO
data/network      → HTTP و API
data/repository   → Repository
data/security     → Session، Device Identity و Sync Key
data/sync         → Sync Engine
di                → AppContainer
ui                → Compose UI
utils             → ابزارهای قابل استفاده مجدد
worker            → WorkManager
```

> در وضعیت فعلی پروژه Hilt استفاده نمی‌شود و Dependency Injection توسط `AppContainer` انجام می‌شود.

### معماری بک‌اند

```text
domain             → Entity و Repository Contract
usecase            → منطق کسب‌وکار
infrastructure     → MongoDB، Auth، JWT، Config و Logging
interface/http     → Handler، DTO، Middleware و Router
cmd                → Entry Point
deployments        → Docker
```

### قابلیت‌ها

- درآمد و هزینه
- دسته‌بندی
- محدودیت خرج
- اهداف و قلک
- بدهی و طلب
- تحلیل مالی
- تشخیص تراکنش از SMS بانکی
- اعلان و widget
- ورود صوتی
- خروجی PDF/Excel
- فارسی/انگلیسی
- تم روشن/تیره
- biometric
- دیتابیس رمزنگاری‌شده
- همگام‌سازی رمزنگاری‌شده و چنددستگاهی

### امنیت و E2EE

داده‌های مالی قبل از sync رمزنگاری می‌شوند. کلید داده با کلید مشتق‌شده از گذرواژه و recovery key محافظت می‌شود و backend نباید plaintext مالی را نیاز داشته باشد.

از دست دادن recovery material می‌تواند بازیابی داده‌های رمزنگاری‌شده را پس از reset رمز عبور یا تعویض دستگاه غیرممکن کند؛ بنابراین backup آن باید در UX به‌صورت واضح طراحی شود.

### قرارداد Sync

```text
TRANSACTION
CATEGORY
BUDGET_LIMIT
DEBT_CREDIT
SAVING_GOAL
SAVING_GOAL_OPERATION
DEBT_PAYMENT
```

شناسه entityهای sync باید UUID/string باشند و مقادیر مالی باید با `Long` و واحد پایه/خرد پولی ذخیره شوند.

### API

```text
GET  /health
POST /auth/otp/request
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/recovery/prepare
POST /auth/password/reset
POST /auth/logout

POST   /api/v1/sync
GET    /api/v1/devices
DELETE /api/v1/devices/{deviceID}

PUT /api/v1/admin/users/{userID}/role
```

### قبل از انتشار

- حذف کامل auth bypass
- تکمیل register/OTP/recovery/reset
- تست اتصال Kotlin ↔ Go
- تست multi-device
- تست conflict و offline
- تست logout و account switching
- تست SMS receiver
- تست release build
- مدیریت secrets
- TLS/CORS/rate limiting
- backup/restore
- monitoring/crash reporting

### License

فایل `LICENSE` شرایط استفاده از پروژه را مشخص می‌کند.
