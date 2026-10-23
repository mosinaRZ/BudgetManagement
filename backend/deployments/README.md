# استقرار کامل بک‌اند روی یک VPS

یک VPS (Ubuntu 22.04/24.04، حداقل ۲ گیگ RAM) + یک دامنه کافی است. همه چیز با Docker Compose بالا می‌آید و **Redis لازم نیست**:

```
اینترنت ──443──▶ Caddy (HTTPS خودکار) ──▶ API (Go) ──▶ MongoDB (replica set تک‌نودی)
                                                         └──▶ بکاپ روزانه (mongodump)
```

فقط پورت‌های ۸۰ و ۴۴۳ (Caddy) از بیرون باز هستند؛ API و MongoDB فقط داخل شبکه Docker در دسترس‌اند.

## راه‌اندازی

1. رکورد DNS دامنه (مثلاً `api.example.com`) را روی IP سرور بگذارید.
2. Docker را نصب کنید و فایروال را ببندید:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ufw allow OpenSSH && ufw allow 80,443/tcp && ufw allow 443/udp && ufw enable
   ```
3. پروژه را روی سرور بگذارید و تنظیمات را بسازید:
   ```bash
   cd cidna/backend/deployments
   cp .env.production.example .env
   nano .env        # همه مقادیر را عوض کنید (secretها: openssl rand -hex 32 / 48)
   ```
4. اجرا:
   ```bash
   docker compose up -d --build
   docker compose ps
   curl https://api.example.com/health
   ```
5. ساخت ادمین اولیه (فقط وقتی هنوز هیچ ادمینی وجود ندارد؛ رمز را در تاریخچه shell ذخیره نکنید و پس از اجرا امن نگه دارید):
   ```bash
   docker compose exec api /app/seed-admin --identifier you@example.com --password 'یک-رمز-حداقل-۱۲-کاراکتری'
   ```
6. آدرس `https://api.example.com` را در تنظیمات release اپ اندروید قرار دهید.

## نکات مهم

- در `ENV=prod` ارسال OTP واقعی (`SMS_WEBHOOK_URL`) اجباری است و `JWT_SECRET` باید حداقل ۶۴ کاراکتر باشد.
- رمز MongoDB داخل URI قرار می‌گیرد؛ از رمز hex (`openssl rand -hex 32`) استفاده کنید.
- محدودسازی نرخ درخواست‌ها در حافظه فرایند API نگه‌داری می‌شود (یک نمونه API)؛ با ری‌استارت ریست می‌شود.
- keyfile مربوط به replica set در اولین اجرا داخل volume ساخته می‌شود؛ نیازی به ساخت یا کپی دستی نیست.

## بکاپ و بازیابی

بکاپ‌ها هر ۲۴ ساعت در `deployments/backups/` ذخیره می‌شوند (نگهداری ۱۴ روز). چون روی همان سرور هستند، حتماً یک کپی بیرون از سرور هم بگیرید (مثلاً `rsync`/`rclone` در cron).

```bash
# بازیابی
docker compose stop api
docker compose exec -T mongo sh -c 'mongorestore --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --archive --gzip --drop' < backups/cidna-YYYYMMDDTHHMMSSZ.archive.gz
docker compose start api
```

## به‌روزرسانی

```bash
git pull && docker compose up -d --build
```

## توسعه محلی

```bash
cp ../.env.example .env
docker compose -f docker-compose.dev.yml up --build -d   # API روی http://localhost:8080
```

## حذف حساب و پاکسازی نهایی

حذف حساب «نرم» است: کاربر از برنامه (`تنظیمات ← امنیت ← حذف حساب کاربری`) حساب را حذف می‌کند و `isDeleted=true` می‌شود؛ همه نشست‌ها فوراً باطل می‌شوند. تا ۳۰ روز با ورود و انتخاب «بازیابی حساب» برمی‌گردد. بعد از آن باید داده‌ها برای همیشه پاک شوند (الزام گوگل‌پلی). یک cron روزانه بگذارید:

```bash
# crontab -e   (هر شب ساعت ۳)
0 3 * * * cd /path/to/cidna/backend/deployments && docker compose exec -T api /app/purge-deleted --older-than-days 30
# پیش‌نمایش بدون حذف:
docker compose exec api /app/purge-deleted --older-than-days 30 --dry-run
```

تا قبل از پاکسازی، ثبت‌نام دوباره با همان شماره خطای Conflict می‌دهد (کاربر باید حساب را بازیابی کند).

## صفحه‌های لازم برای گوگل‌پلی

بک‌اند صفحه‌های حقوقی عمومی را ارائه می‌کند:

- `https://api.example.com/privacy-policy` ← حریم خصوصی (فیلد Privacy policy در Play Console)
- `https://api.example.com/account-deletion` ← حذف حساب (فیلد Delete account URL در بخش Data safety)
- `https://api.example.com/terms` ← قوانین و مقررات (در اپ هم از فرم ثبت‌نام و تنظیمات لینک شده است)
- `https://api.example.com/licenses` ← مجوزهای متن‌باز

با هر تغییر در متن‌ها، ثابت‌های `legalVersion` و تاریخ‌ها را در `legal_handler.go` به‌روز کنید.

متن‌ها پیش‌نویس‌اند؛ قبل از انتشار با شرایط حقوقی خودتان تطبیق دهید.
