package ir.hamedan.budgetmanagement.data.preferences

enum class NotificationCategory(
    val titleFa: String,
    val titleEn: String
) {
    TRANSACTIONS("تراکنش‌ها", "Transactions"),
    SMS("پیامک بانکی", "Bank SMS"),
    CATEGORIES("دسته‌بندی‌ها", "Categories"),
    BUDGET("محدودیت بودجه", "Budget Limits"),
    GOALS("اهداف پس‌انداز", "Saving Goals"),
    DEBT_CREDIT("بدهی و طلب", "Debt & Credit"),
    REMINDERS("یادآوری‌ها", "Reminders"),
    SYSTEM("سیستم", "System")
}

enum class NotificationType(
    val category: NotificationCategory,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val defaultEnabled: Boolean
) {
    TRANSACTION_ADD(
        NotificationCategory.TRANSACTIONS,
        "ثبت تراکنش جدید",
        "New Transaction Added",
        "وقتی تراکنش جدیدی ثبت می‌کنید.",
        "When you add a new transaction.",
        defaultEnabled = false
    ),
    TRANSACTION_EDIT(
        NotificationCategory.TRANSACTIONS,
        "ویرایش تراکنش",
        "Transaction Updated",
        "وقتی تراکنشی را ویرایش می‌کنید.",
        "When you edit a transaction.",
        defaultEnabled = false
    ),
    TRANSACTION_DELETE(
        NotificationCategory.TRANSACTIONS,
        "حذف تراکنش",
        "Transaction Deleted",
        "وقتی تراکنشی را حذف می‌کنید.",
        "When you delete a transaction.",
        defaultEnabled = false
    ),
    TRANSACTION_CATEGORY_CHANGE(
        NotificationCategory.TRANSACTIONS,
        "تغییر دسته‌بندی تراکنش‌ها",
        "Transactions Category Changed",
        "وقتی حذف دسته‌بندی، تراکنش‌ها را جابه‌جا می‌کند.",
        "When deleting a category moves its transactions.",
        defaultEnabled = false
    ),

    SMS_PARSED(
        NotificationCategory.SMS,
        "تراکنش پیامکی جدید",
        "New SMS Transaction",
        "وقتی پیامک بانکی دریافت و پردازش می‌شود.",
        "When a bank SMS is received and parsed.",
        defaultEnabled = true
    ),
    SMS_PENDING(
        NotificationCategory.SMS,
        "تراکنش پیامکی معلق",
        "Pending SMS Transaction",
        "وقتی پیامک بانکی منتظر تأیید شماست.",
        "When a bank SMS is waiting for your confirmation.",
        defaultEnabled = true
    ),
    SMS_CONFIRMED(
        NotificationCategory.SMS,
        "تأیید تراکنش پیامکی",
        "SMS Transaction Confirmed",
        "وقتی تراکنش پیامکی را تأیید و ثبت می‌کنید.",
        "When you confirm and save an SMS transaction.",
        defaultEnabled = false
    ),

    CATEGORY_ADD(
        NotificationCategory.CATEGORIES,
        "افزودن دسته‌بندی",
        "Category Added",
        "وقتی دسته‌بندی جدیدی ایجاد می‌کنید.",
        "When you create a new category.",
        defaultEnabled = false
    ),
    CATEGORY_UPDATE(
        NotificationCategory.CATEGORIES,
        "ویرایش دسته‌بندی",
        "Category Updated",
        "وقتی دسته‌بندی را ویرایش می‌کنید.",
        "When you edit a category.",
        defaultEnabled = false
    ),
    CATEGORY_DELETE(
        NotificationCategory.CATEGORIES,
        "حذف دسته‌بندی",
        "Category Deleted",
        "وقتی دسته‌بندی را حذف می‌کنید.",
        "When you delete a category.",
        defaultEnabled = false
    ),

    BUDGET_THRESHOLD(
        NotificationCategory.BUDGET,
        "هشدار سقف بودجه",
        "Budget Limit Alert",
        "وقتی مصرف یک دسته به ۵۰٪، ۸۰٪ یا ۱۰۰٪ سقف می‌رسد.",
        "When a category reaches 50%, 80%, or 100% of its budget.",
        defaultEnabled = true
    ),
    BUDGET_ADD(
        NotificationCategory.BUDGET,
        "ثبت محدودیت بودجه",
        "Budget Limit Added",
        "وقتی محدودیت بودجه جدیدی تعریف می‌کنید.",
        "When you set a new budget limit.",
        defaultEnabled = false
    ),
    BUDGET_STATUS_CHANGE(
        NotificationCategory.BUDGET,
        "تغییر وضعیت محدودیت",
        "Budget Limit Status Changed",
        "وقتی محدودیت بودجه را فعال یا غیرفعال می‌کنید.",
        "When you enable or disable a budget limit.",
        defaultEnabled = false
    ),
    BUDGET_DELETE(
        NotificationCategory.BUDGET,
        "حذف محدودیت بودجه",
        "Budget Limit Deleted",
        "وقتی محدودیت بودجه را حذف می‌کنید.",
        "When you delete a budget limit.",
        defaultEnabled = false
    ),

    GOAL_PROGRESS(
        NotificationCategory.GOALS,
        "پیشرفت هدف پس‌انداز",
        "Saving Goal Progress",
        "وقتی به ۵۰٪، ۸۰٪ یا ۱۰۰٪ هدف پس‌انداز می‌رسید.",
        "When you reach 50%, 80%, or 100% of a saving goal.",
        defaultEnabled = true
    ),
    GOAL_ADD(
        NotificationCategory.GOALS,
        "ثبت هدف پس‌انداز",
        "Saving Goal Added",
        "وقتی هدف پس‌انداز جدیدی ایجاد می‌کنید.",
        "When you create a new saving goal.",
        defaultEnabled = false
    ),
    GOAL_UPDATE(
        NotificationCategory.GOALS,
        "ویرایش هدف پس‌انداز",
        "Saving Goal Updated",
        "وقتی هدف پس‌انداز را ویرایش می‌کنید.",
        "When you edit a saving goal.",
        defaultEnabled = false
    ),
    GOAL_DELETE(
        NotificationCategory.GOALS,
        "حذف هدف پس‌انداز",
        "Saving Goal Deleted",
        "وقتی هدف پس‌انداز را حذف می‌کنید.",
        "When you delete a saving goal.",
        defaultEnabled = false
    ),
    GOAL_DEPOSIT(
        NotificationCategory.GOALS,
        "واریز به هدف",
        "Deposit to Goal",
        "وقتی به هدف پس‌انداز واریز می‌کنید.",
        "When you deposit to a saving goal.",
        defaultEnabled = false
    ),
    GOAL_WITHDRAW(
        NotificationCategory.GOALS,
        "برداشت از هدف",
        "Withdraw from Goal",
        "وقتی از هدف پس‌انداز برداشت می‌کنید.",
        "When you withdraw from a saving goal.",
        defaultEnabled = false
    ),
    GOAL_AUTO_DEPOSIT(
        NotificationCategory.GOALS,
        "واریز خودکار ماهانه",
        "Auto Monthly Deposit",
        "وقتی واریز خودکار ماهانه به قلک انجام یا ناموفق می‌شود.",
        "When monthly auto-deposit to a goal succeeds or fails.",
        defaultEnabled = true
    ),

    DEBT_ADD(
        NotificationCategory.DEBT_CREDIT,
        "ثبت بدهی/طلب",
        "Debt/Credit Record Added",
        "وقتی بدهی یا طلب جدیدی ثبت می‌کنید.",
        "When you add a new debt or credit record.",
        defaultEnabled = false
    ),
    DEBT_PAYMENT(
        NotificationCategory.DEBT_CREDIT,
        "پرداخت/دریافت بدهی/طلب",
        "Debt/Credit Payment",
        "وقتی بدهی پرداخت یا طلب دریافت می‌شود.",
        "When you pay a debt or receive a credit.",
        defaultEnabled = false
    ),
    DEBT_ADJUSTMENT(
        NotificationCategory.DEBT_CREDIT,
        "اصلاح واریزی",
        "Payment Adjustment",
        "وقتی مبلغ واریزی بدهی/طلب اصلاح می‌شود.",
        "When a debt/credit payment amount is adjusted.",
        defaultEnabled = false
    ),
    DEBT_DELETE(
        NotificationCategory.DEBT_CREDIT,
        "حذف بدهی/طلب",
        "Debt/Credit Deleted",
        "وقتی رکورد بدهی/طلب حذف می‌شود.",
        "When a debt/credit record is deleted.",
        defaultEnabled = false
    ),
    DEBT_SETTLE(
        NotificationCategory.DEBT_CREDIT,
        "تغییر وضعیت تسویه",
        "Settlement Status Changed",
        "وقتی وضعیت تسویه بدهی/طلب تغییر می‌کند.",
        "When settlement status of a debt/credit changes.",
        defaultEnabled = false
    ),
    DEBT_DUE_REMINDER(
        NotificationCategory.DEBT_CREDIT,
        "یادآوری سررسید",
        "Due Date Reminder",
        "۱، ۳ یا ۷ روز قبل از سررسید بدهی/طلب.",
        "1, 3, or 7 days before a debt/credit due date.",
        defaultEnabled = true
    ),
    DEBT_DUE_DATE_CHANGE(
        NotificationCategory.DEBT_CREDIT,
        "تغییر تاریخ سررسید",
        "Due Date Changed",
        "وقتی تاریخ سررسید بدهی/طلب تغییر می‌کند.",
        "When a debt/credit due date is changed.",
        defaultEnabled = false
    ),
    DEBT_DUE_SETTLE(
        NotificationCategory.DEBT_CREDIT,
        "تسویه از یادآوری",
        "Settle from Reminder",
        "وقتی از دیالوگ یادآوری، بدهی/طلب را تسویه می‌کنید.",
        "When you settle from the due-date reminder dialog.",
        defaultEnabled = false
    ),

    INACTIVITY_REMINDER(
        NotificationCategory.REMINDERS,
        "یادآوری عدم فعالیت",
        "Inactivity Reminder",
        "وقتی مدتی از برنامه استفاده نکرده‌اید.",
        "When you haven't used the app for a while.",
        defaultEnabled = true
    ),

    SETTINGS_CHANGED(
        NotificationCategory.SYSTEM,
        "تغییر تنظیمات",
        "Settings Changed",
        "وقتی تنظیمات برنامه (واحد پول، امنیت و...) تغییر می‌کند.",
        "When app settings (currency, security, etc.) are changed.",
        defaultEnabled = false
    );

    val prefKey: String get() = "notif_type_$name"
}