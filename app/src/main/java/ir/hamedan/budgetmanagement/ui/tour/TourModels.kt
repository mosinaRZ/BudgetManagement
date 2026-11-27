package ir.hamedan.budgetmanagement.ui.tour

/** صفحه‌ای که هر مرحله‌ی تور روی آن اتفاق می‌افتد. */
enum class TourScreen { HOME, TRANSACTIONS, ANALYTICS, SETTINGS, ADD }

/**
 * بخش‌هایی از رابط که تور می‌تواند روی آن‌ها اسپات‌لایت بیندازد.
 * هر کدام با `Modifier.tourTarget(...)` روی خودِ کامپوننت علامت‌گذاری می‌شوند.
 */
enum class TourTarget {
    HOME_NOTIFICATIONS,
    HOME_BALANCE,
    HOME_GOALS,
    HOME_LIMITS,
    HOME_DEBTS,

    TX_TIME_CHIPS,
    TX_FILTER,
    TX_ROW,
    TX_ACTIONS,

    AN_FILTER_BAR,
    AN_INDICATORS,
    AN_BARS,

    SET_PROFILE,
    SET_EXPORT,

    ADD_TRANSACTION,
    ADD_GOALS,
    ADD_CATEGORIES,
    ADD_LIMITS,
    ADD_DEBTS
}

/** حرکت نمایشی روی هدف: «TAP» فقط یک موج لمس ظریف روی هدف نشان می‌دهد. */
enum class TourGesture { NONE, TAP }

/**
 * یک مرحله‌ی تور.
 *
 * @param target null یعنی مرحله‌ی تمام‌صفحه (خوش‌آمدگویی/پایان) بدون اسپات‌لایت.
 * @param whereFa،whereEn نشانیِ دقیق این امکان در برنامه (کدام صفحه و کدام قسمت)؛ در کارت تور کنار نقشه‌ی نوار پایین نشان داده می‌شود.
 * @param handX،handY محل موج لمس داخل کادر هدف (۰ تا ۱).
 */
data class TourStep(
    val id: String,
    val screen: TourScreen,
    val target: TourTarget?,
    val sectionFa: String,
    val sectionEn: String,
    val titleFa: String,
    val titleEn: String,
    val whereFa: String,
    val whereEn: String,
    val bodyFa: String,
    val bodyEn: String,
    val gesture: TourGesture = TourGesture.NONE,
    val handX: Float = 0.5f,
    val handY: Float = 0.5f
) {
    fun title(isPersian: Boolean) = if (isPersian) titleFa else titleEn
    fun body(isPersian: Boolean) = if (isPersian) bodyFa else bodyEn
    fun section(isPersian: Boolean) = if (isPersian) sectionFa else sectionEn
    fun location(isPersian: Boolean) = if (isPersian) whereFa else whereEn
}

/** شناسه‌ی مرحله‌هایی که صفحه‌ها برای نمایش خودکارِ «لمس» به آن‌ها واکنش نشان می‌دهند. */
object TourStepIds {
    const val WELCOME = "welcome"
    const val TX_ROW = "tx_row"
    const val TX_ACTIONS = "tx_actions"
    const val AN_BARS = "an_bars"
    const val FINALE = "finale"
}

/**
 * متن مرحله‌ها: لحن مؤدبانه و محترمانه، بدون ایموجی؛ هر مرحله یک بخش را معرفی می‌کند، همان بخش را
 * اسپات‌لایت می‌کند و نشانی دقیق آن را در برنامه می‌گوید.
 */
object TourSteps {
    val all: List<TourStep> = listOf(
        TourStep(
            id = TourStepIds.WELCOME, screen = TourScreen.HOME, target = null,
            sectionFa = "شروع", sectionEn = "Start",
            titleFa = "به سیدنا خوش آمدید", titleEn = "Welcome to Cidna",
            whereFa = "نقشه‌ی برنامه: چهار صفحه و دکمه‌ی افزودن", whereEn = "App map: four tabs and the Add button",
            bodyFa = "در کمتر از دو دقیقه، بخش‌های اصلی برنامه و جای هر امکان را به شما نشان می‌دهیم. تمام داده‌هایی که می‌بینید نمونه است و به اطلاعات واقعی شما دست نمی‌زنیم.",
            bodyEn = "In under two minutes we will show you the main areas of the app and where each feature lives. Everything you see is sample data; your real information is never touched."
        ),
        TourStep(
            id = "home_notifications", screen = TourScreen.HOME, target = TourTarget.HOME_NOTIFICATIONS,
            sectionFa = "خانه", sectionEn = "Home",
            titleFa = "اعلان‌ها", titleEn = "Notifications",
            whereFa = "صفحه‌ی خانه، گوشه‌ی بالای صفحه", whereEn = "Home tab, top corner",
            bodyFa = "با لمس این زنگ، هشدارهای سقف هزینه، سررسید بدهی‌ها، واریز به قلک‌ها و رویدادهای امنیتی حساب را می‌بینید. عدد روی زنگ، تعداد اعلان‌های خوانده‌نشده است.",
            bodyEn = "Tap this bell to see budget alerts, debt due dates, savings deposits and account security events. The badge shows how many notifications are unread.",
            gesture = TourGesture.TAP, handX = 0.55f, handY = 0.6f
        ),
        TourStep(
            id = "home_balance", screen = TourScreen.HOME, target = TourTarget.HOME_BALANCE,
            sectionFa = "خانه", sectionEn = "Home",
            titleFa = "تراز کلی", titleEn = "Total balance",
            whereFa = "صفحه‌ی خانه، نخستین کارت", whereEn = "Home tab, first card",
            bodyFa = "اختلاف مجموع درآمد و مجموع هزینه‌های شما. با یک نگاه می‌دانید وضعیت مالی‌تان مثبت است یا منفی. با آیکون چشم می‌توانید مبلغ را پنهان کنید.",
            bodyEn = "The difference between your total income and total spending. One glance tells you whether you are in the green. The eye icon hides the amount."
        ),
        TourStep(
            id = "home_goals", screen = TourScreen.HOME, target = TourTarget.HOME_GOALS,
            sectionFa = "خانه", sectionEn = "Home",
            titleFa = "اهداف پس‌انداز", titleEn = "Savings goals",
            whereFa = "صفحه‌ی خانه، زیر تراز کلی", whereEn = "Home tab, below the balance",
            bodyFa = "برای هر هدف، مثل سفر یا خرید لپ‌تاپ، یک قلک بسازید و پیشرفت آن را روی نوار پیشرفت دنبال کنید.",
            bodyEn = "Create a goal for anything you are saving for, such as a trip or a laptop, and follow its progress on the progress bar."
        ),
        TourStep(
            id = "home_limits", screen = TourScreen.HOME, target = TourTarget.HOME_LIMITS,
            sectionFa = "خانه", sectionEn = "Home",
            titleFa = "محدودیت‌های خرج‌کرد", titleEn = "Spending limits",
            whereFa = "صفحه‌ی خانه، پس از اهداف پس‌انداز", whereEn = "Home tab, after savings goals",
            bodyFa = "برای هر دسته سقفی تعیین کنید؛ مثلاً خوراک ماهانه سه میلیون. هنگام رسیدن به ۵۰، ۸۰ و ۱۰۰ درصد سقف به شما اطلاع می‌دهیم.",
            bodyEn = "Set a ceiling for each category, for example Food at 3M per month. We notify you at 50, 80 and 100 percent of the limit."
        ),
        TourStep(
            id = "home_debts", screen = TourScreen.HOME, target = TourTarget.HOME_DEBTS,
            sectionFa = "خانه", sectionEn = "Home",
            titleFa = "بدهی و طلب", titleEn = "Debts & credits",
            whereFa = "صفحه‌ی خانه، پس از محدودیت‌ها", whereEn = "Home tab, after spending limits",
            bodyFa = "بدهی‌ها و طلب‌های خود را همراه با درصد پرداخت و شمارش معکوس سررسید در اینجا پیگیری کنید تا هیچ موعدی از قلم نیفتد.",
            bodyEn = "Track what you owe and what is owed to you, with payment progress and a due-date countdown, so no deadline is missed."
        ),
        TourStep(
            id = "tx_chips", screen = TourScreen.TRANSACTIONS, target = TourTarget.TX_TIME_CHIPS,
            sectionFa = "تراکنش‌ها", sectionEn = "Transactions",
            titleFa = "چیپ‌های زمانی", titleEn = "Time chips",
            whereFa = "تب تراکنش‌ها، نوار بالای فهرست", whereEn = "Transactions tab, bar above the list",
            bodyFa = "با یک لمس میان روزانه، هفتگی، ماهانه و کل جابه‌جا شوید؛ فهرست بلافاصله بر همان اساس فیلتر می‌شود.",
            bodyEn = "Switch between Daily, Weekly, Monthly and All with one tap; the list filters instantly.",
            gesture = TourGesture.TAP, handX = 0.5f, handY = 0.6f
        ),
        TourStep(
            id = "tx_filter", screen = TourScreen.TRANSACTIONS, target = TourTarget.TX_FILTER,
            sectionFa = "تراکنش‌ها", sectionEn = "Transactions",
            titleFa = "فیلتر پیشرفته", titleEn = "Advanced filter",
            whereFa = "تب تراکنش‌ها، کنار چیپ‌های زمانی", whereEn = "Transactions tab, beside the time chips",
            bodyFa = "برای جست‌وجوی دقیق‌تر: نمایش فقط درآمد یا فقط هزینه، مرتب‌سازی بر اساس مبلغ و انتخاب بازه‌ی تاریخی دلخواه.",
            bodyEn = "For precise searching: show only income or only expenses, sort by amount, and choose any date range.",
            gesture = TourGesture.TAP, handX = 0.55f, handY = 0.6f
        ),
        TourStep(
            id = TourStepIds.TX_ROW, screen = TourScreen.TRANSACTIONS, target = TourTarget.TX_ROW,
            sectionFa = "تراکنش‌ها", sectionEn = "Transactions",
            titleFa = "لمس یک تراکنش", titleEn = "Tapping a transaction",
            whereFa = "تب تراکنش‌ها، فهرست تراکنش‌ها", whereEn = "Transactions tab, transaction list",
            bodyFa = "هر ردیف یک تراکنش است. با لمس آن، منوی میان‌بر باز می‌شود؛ اکنون یک نمونه را برای شما باز می‌کنیم.",
            bodyEn = "Each row is a transaction. Tapping it opens its quick menu; we will now open one for you.",
            gesture = TourGesture.TAP, handX = 0.45f, handY = 0.5f
        ),
        TourStep(
            id = TourStepIds.TX_ACTIONS, screen = TourScreen.TRANSACTIONS, target = TourTarget.TX_ACTIONS,
            sectionFa = "تراکنش‌ها", sectionEn = "Transactions",
            titleFa = "ویرایش، حذف و اشتراک‌گذاری", titleEn = "Edit, delete and share",
            whereFa = "تب تراکنش‌ها، پس از لمس هر ردیف", whereEn = "Transactions tab, after tapping a row",
            bodyFa = "با دکمه‌ی ویرایش، اطلاعات تراکنش را اصلاح می‌کنید؛ با حذف، آن را پاک می‌کنید؛ و با اشتراک‌گذاری، تصویری مرتب از تراکنش برای ارسال به دیگران می‌سازید.",
            bodyEn = "Edit corrects the transaction, Delete removes it, and Share creates a neat image of it that you can send to others."
        ),
        TourStep(
            id = "an_filters", screen = TourScreen.ANALYTICS, target = TourTarget.AN_FILTER_BAR,
            sectionFa = "آمار", sectionEn = "Analytics",
            titleFa = "چیپ‌ها و فیلتر در صفحه‌ی آمار", titleEn = "Chips and filter in Analytics",
            whereFa = "تب آمار، نوار بالای صفحه", whereEn = "Analytics tab, bar at the top",
            bodyFa = "همین چیپ‌های زمانی و فیلتر پیشرفته در این صفحه هم در دسترس است. با تغییر بازه، همه‌ی نمودارها و شاخص‌ها هم‌زمان به‌روز می‌شوند.",
            bodyEn = "The same time chips and advanced filter are available here. When you change the range, every chart and indicator updates together."
        ),
        TourStep(
            id = "an_indicators", screen = TourScreen.ANALYTICS, target = TourTarget.AN_INDICATORS,
            sectionFa = "آمار", sectionEn = "Analytics",
            titleFa = "شاخص‌های مالی", titleEn = "Financial indicators",
            whereFa = "تب آمار، زیر نوار فیلتر", whereEn = "Analytics tab, below the filter bar",
            bodyFa = "نرخ پس‌انداز، سرعت هزینه‌ی روزانه، مقایسه با دوره‌ی قبل، پایبندی به سقف‌ها و پیشرفت اهداف در این بخش قرار دارد. با لمس عنوان آن، کارت‌های جزئیات باز می‌شوند و رنگ‌ها وضعیت مطلوب، هشدار یا بحرانی را نشان می‌دهند.",
            bodyEn = "Savings rate, daily burn, comparison with the last period, budget adherence and goal progress live here. Tap its title to open the detail cards; colors show healthy, warning or critical status."
        ),
        TourStep(
            id = TourStepIds.AN_BARS, screen = TourScreen.ANALYTICS, target = TourTarget.AN_BARS,
            sectionFa = "آمار", sectionEn = "Analytics",
            titleFa = "نمودار ستونی", titleEn = "Bar chart",
            whereFa = "تب آمار، نمودار ستونی", whereEn = "Analytics tab, bar chart",
            bodyFa = "هر ستون یک دوره است. با لمس هر ستون، کل صفحه فقط برای همان دوره محاسبه می‌شود؛ اکنون یک ستون برای شما انتخاب شد.",
            bodyEn = "Each bar is one period. Tapping a bar recalculates the whole page for just that period; a bar has just been selected for you.",
            gesture = TourGesture.TAP, handX = 0.4f, handY = 0.75f
        ),
        TourStep(
            id = "set_profile", screen = TourScreen.SETTINGS, target = TourTarget.SET_PROFILE,
            sectionFa = "تنظیمات", sectionEn = "Settings",
            titleFa = "پروفایل", titleEn = "Profile",
            whereFa = "تب تنظیمات، ابتدای صفحه", whereEn = "Settings tab, top of the page",
            bodyFa = "نام، اطلاعات و امنیت حساب شما اینجاست. نوار درصد تکمیل نشان می‌دهد چه مواردی هنوز باقی مانده است.",
            bodyEn = "Your name, details and account security are here. The completion bar shows what is still missing."
        ),
        TourStep(
            id = "set_export", screen = TourScreen.SETTINGS, target = TourTarget.SET_EXPORT,
            sectionFa = "تنظیمات", sectionEn = "Settings",
            titleFa = "گزارش PDF و Excel", titleEn = "PDF and Excel reports",
            whereFa = "تب تنظیمات، بخش داده‌ها و گزارش‌ها", whereEn = "Settings tab, Data and reports section",
            bodyFa = "از تراکنش‌های خود در بازه‌ی دلخواه، خروجی PDF یا XLSX بگیرید؛ مناسب ارائه به حسابدار یا تحلیل بیشتر در اکسل.",
            bodyEn = "Export your transactions for any period as PDF or XLSX, suitable for your accountant or further analysis in Excel."
        ),
        TourStep(
            id = "add_transaction", screen = TourScreen.ADD, target = TourTarget.ADD_TRANSACTION,
            sectionFa = "افزودن", sectionEn = "Add",
            titleFa = "ثبت تراکنش", titleEn = "Add a transaction",
            whereFa = "دکمه‌ی + در نوار پایین، صفحه‌ی افزودن", whereEn = "The + button in the bottom bar, Add page",
            bodyFa = "دکمه‌ی + در نوار پایین همیشه شما را به این صفحه می‌آورد. هزینه یا درآمد را انتخاب کنید و در چند ثانیه ثبت کنید؛ ثبت صوتی هم پشتیبانی می‌شود.",
            bodyEn = "The + button in the bottom bar always brings you to this page. Pick Expense or Income and log it in seconds; voice entry is supported too."
        ),
        TourStep(
            id = "add_goals", screen = TourScreen.ADD, target = TourTarget.ADD_GOALS,
            sectionFa = "افزودن", sectionEn = "Add",
            titleFa = "مدیریت اهداف پس‌انداز", titleEn = "Manage savings goals",
            whereFa = "صفحه‌ی افزودن، ردیف نخست مدیریت", whereEn = "Add page, first management row",
            bodyFa = "اهداف جدید بسازید یا اهداف موجود را ویرایش کنید.",
            bodyEn = "Create new goals or edit existing ones."
        ),
        TourStep(
            id = "add_categories", screen = TourScreen.ADD, target = TourTarget.ADD_CATEGORIES,
            sectionFa = "افزودن", sectionEn = "Add",
            titleFa = "مدیریت دسته‌بندی‌های مالی", titleEn = "Manage financial categories",
            whereFa = "صفحه‌ی افزودن، ردیف دوم مدیریت", whereEn = "Add page, second management row",
            bodyFa = "دسته‌های هزینه و درآمد را با آیکون دلخواه خود بسازید و ویرایش کنید.",
            bodyEn = "Create and edit expense and income categories with any icon you like."
        ),
        TourStep(
            id = "add_limits", screen = TourScreen.ADD, target = TourTarget.ADD_LIMITS,
            sectionFa = "افزودن", sectionEn = "Add",
            titleFa = "مدیریت محدودیت‌های خرج‌کرد", titleEn = "Manage spending limits",
            whereFa = "صفحه‌ی افزودن، ردیف سوم مدیریت", whereEn = "Add page, third management row",
            bodyFa = "دسته، سقف مبلغ و بازه‌ی زمانی هر محدودیت را از اینجا تنظیم کنید.",
            bodyEn = "Set the category, amount ceiling and time range of each limit here."
        ),
        TourStep(
            id = "add_debts", screen = TourScreen.ADD, target = TourTarget.ADD_DEBTS,
            sectionFa = "افزودن", sectionEn = "Add",
            titleFa = "مدیریت بدهی و طلب", titleEn = "Manage debts and credits",
            whereFa = "صفحه‌ی افزودن، ردیف چهارم مدیریت", whereEn = "Add page, fourth management row",
            bodyFa = "طرف حساب، مبلغ و تاریخ سررسید (یا قسط ماهانه) را ثبت کنید؛ یادآوری به‌صورت خودکار ارسال می‌شود.",
            bodyEn = "Record the person, amount and due date (or monthly installment); reminders are sent automatically."
        ),
        TourStep(
            id = TourStepIds.FINALE, screen = TourScreen.HOME, target = null,
            sectionFa = "پایان", sectionEn = "Done",
            titleFa = "تور به پایان رسید", titleEn = "The tour is complete",
            whereFa = "نقشه‌ی برنامه: چهار صفحه و دکمه‌ی افزودن", whereEn = "App map: four tabs and the Add button",
            bodyFa = "اکنون می‌توانید اولین تراکنش خود را ثبت کنید. هر زمان نیاز داشتید، همه‌ی این امکانات در همان صفحه‌هایی که دیدید در دسترس است.",
            bodyEn = "You can now record your first transaction. Whenever you need them, all of these features are available on the pages you just saw."
        )
    )
}