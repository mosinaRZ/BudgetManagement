# Cidna — Personal Finance Management

Cidna is a personal finance management application built for Android, with a Go backend designed to provide secure authentication, synchronization, and multi-device support.

The project consists of two main parts:

* Android client built with Kotlin and Jetpack Compose
* Go backend providing authentication, OTP, synchronization, and secure account services

---

## Android Application

Cidna is a modern personal finance management application focused on helping users track, analyze, and manage their personal finances.

### Main Features

* Income and expense transaction management
* Categories
* Budget limits
* Saving goals
* Automatic detection of bank transactions from SMS
* Balance widget and notifications
* Speech-to-text transaction input
* Excel (XLSX) and PDF export
* Persian and English language support
* Light and dark themes
* Secure local data storage
* Biometric authentication

### Android Architecture

The Android application is built using:

* Kotlin
* Jetpack Compose
* Clean Architecture
* Hilt
* Room
* WorkManager
* SQLCipher
* Android Security APIs

### Project Structure

```text
app/
├── data/          # Room, Preferences, Repositories and security
├── ui/            # Compose screens, components and theme
├── di/            # Hilt and dependency injection
├── utils/         # SMS parser, export manager, biometric utilities
├── worker/        # WorkManager workers
└── security/      # Database key provider and biometric managers
```

### Security

The Android application includes several security mechanisms:

* SQLCipher encrypted database
* Secure storage for sensitive application data
* Password hashing
* Biometric authentication
* End-to-end encryption support for synchronized financial data

### Data Export

Users can export financial information in:

* Excel (XLSX)
* PDF

Exports can include information such as:

* Balance
* Income
* Expenses
* Daily averages
* Financial statistics

---

# Go Backend

The backend provides the server-side foundation for authentication, account management, OTP delivery, and synchronization between multiple Android devices.

The backend is implemented in Go with a production-oriented architecture.

## Backend Features

* Argon2id password hashing
* Short-lived JWT access tokens
* Rotating refresh tokens with reuse detection
* Phone and email identifiers stored as keyed hashes rather than plaintext
* OTP request and verification
* OTP expiry and attempt limits
* Rate limiting
* Email delivery through SMTP
* SMS delivery through a configurable HTTP webhook
* Mandatory phone OTP during registration
* Optional email OTP when an email is provided
* Password reset using OTP and E2EE recovery key
* Multi-device registration and device validation
* Incremental cursor-based synchronization
* Server revisions
* Idempotent request replay
* Conflict reporting
* Soft-delete / tombstone synchronization records
* MongoDB unique indexes
* TTL cleanup for OTP, refresh-token and synchronization records
* Request body limits
* Unknown JSON-field rejection
* Panic recovery
* Graceful shutdown

---

## E2EE Recovery Contract

The Android client generates a random Data Encryption Key (DEK).

Two envelopes are maintained:

1. `password_key_envelope`

   The DEK encrypted or wrapped using a key derived from the user's password.

2. `recovery_key_envelope`

   The DEK encrypted or wrapped using a randomly generated recovery key.

The backend stores only these opaque envelopes.

The raw DEK is never sent to the server.

The recovery key is never stored by the backend.

During password reset, the client must prove possession of the recovery key and submit a newly generated `password_key_envelope` for the same DEK.

Without the recovery key, an OTP-based password reset cannot decrypt historical encrypted financial data.

This is an intentional property of end-to-end encryption.

The Android application should display or export the recovery key during account creation and clearly warn the user that losing it may make encrypted historical financial data unrecoverable.

---

## API

### Public Endpoints

```text
GET  /health

POST /auth/otp/request
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/password/reset
POST /auth/logout
```

### Authenticated Endpoints

```text
POST /api/v1/sync
```

---

## Synchronization

The backend provides incremental synchronization between multiple devices.

The synchronization system supports:

* Cursor-based synchronization
* Server-side revisions
* Idempotent requests
* Conflict detection and reporting
* Soft deletes / tombstones
* Device validation
* Multi-device registration

### Android Compatibility

All synchronized entities should use a string UUID as their logical `entityId`.

In particular, `BudgetLimit` should not use a local auto-increment `Long` ID if it participates in the shared synchronization protocol.

Money values should be represented as integer minor/base units (`Long`) rather than `Double` to avoid floating-point currency errors.

---

## Production Configuration

For production environments:

```env
ENV=prod
DEV_LOG_OTP=false
```

Configure:

* MongoDB
* JWT secret
* SMTP provider
* SMS webhook
* TLS / reverse proxy
* Backups
* Monitoring
* Provider credentials
* Operational security controls

MongoDB should run as a replica set because synchronization uses MongoDB transactions.

The generic SMS webhook expects:

```json
{
  "to": "+989...",
  "code": "123456",
  "message": "Cidna verification code: 123456"
}
```

An optional authorization header can also be provided:

```text
Authorization: Bearer <token>
```

---

## Development

Copy the environment template:

```bash
cp .env.example .env
```

Configure MongoDB and an OTP delivery method.

Then run:

```bash
go mod download
go test ./...
go run ./cmd/api
```

For local MongoDB replica-set testing:

```bash
docker compose -f deployments/docker-compose.yml up --build
```

MongoDB integration tests require `MONGO_TEST_URI` and a MongoDB replica set.

Without a configured MongoDB test environment, those tests intentionally skip instead of falsely reporting success.

---

## Verification

The current backend includes fixes for the remaining unit-test contract mismatches from the previous phase:

* Registration tests inject a deterministic OTP service instead of bypassing the mandatory registration OTP flow.
* Login tests use the current `identifier` contract.
* Authentication handlers avoid exposing internal error details.
* Refresh-token rotation treats concurrent revoke conflicts as refresh-token reuse and invalidates all sessions for the user.

The Go source code is formatted with `gofmt`.

---

## Android Requirements

* Android Studio Ladybug or newer
* JDK 11+
* minSdk 26

---

## Current Project Status

The Android application and Go backend are under active development.

The backend currently provides the foundation required for:

* Secure authentication
* OTP verification
* Account recovery
* Multi-device synchronization
* Secure financial data synchronization

The system should not be considered production-ready without proper deployment configuration, secrets management, TLS, backups, monitoring, provider configuration, and operational security controls.

---

## Roadmap

* [ ] Complete Go backend integration with Android
* [ ] Complete end-to-end synchronization
* [ ] Expand unit and UI test coverage
* [ ] CI/CD with GitHub Actions
* [ ] Production infrastructure
* [ ] Google Play release
* [ ] Additional financial analysis features

---

## Contributing

1. Fork the repository.
2. Create a new branch.
3. Make your changes.
4. Run the tests.
5. Create a Pull Request.

---

## License

See the `LICENSE` file for licensing information.
See the `LICENSE` file for licensing information.
