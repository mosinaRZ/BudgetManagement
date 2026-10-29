# -*- coding: utf-8 -*-
import config as C
from engine.ui import *
from engine.doc import doc, simple_page
from engine.layout import page

# slug, fa title, en title, fa desc, en desc, icon, category, published
GUIDES = [
 ("getting-started","شروع کار با سیدنا: از نصب تا اولین ورود","Getting started with Cidna: from install to first sign-in","مهمان یا حساب؟ چطور شروع کنید، کلید بازیابی را کجا نگه دارید و قفل برنامه را روشن کنید.","Guest or account? How to start, where to keep your recovery key and how to turn on the app lock.","rocket","app","2026-10-24"),
 ("add-transaction","ثبت تراکنش در سیدنا: دستی، صوتی و سریع","Recording a transaction in Cidna: manual, voice and quick","هزینه یا درآمد را در چند ثانیه ثبت کنید؛ با دسته‌بندی، یادداشت و کشیدن برای ذخیره.","Log an expense or income in seconds — with a category, a note and swipe-to-save.","mic","app","2026-10-24"),
 ("limits-and-piggy-banks","سقف هزینه و قلک پس‌انداز در سیدنا","Spending limits and savings piggy banks in Cidna","از صفحهٔ اصلی برای هر دسته سقف بگذارید و برای هر هدف یک قلک بسازید.","Set a limit per category and create a piggy bank per goal, straight from the home screen.","target","app","2026-10-24"),
 ("offline-and-sync","استفادهٔ آفلاین و همگام‌سازی در سیدنا","Using Cidna offline, and how sync works","بعد از اولین ورود، بدون اینترنت هم کار می‌کند. بدانید چه چیزی آفلاین است و کی همگام می‌شود.","After the first sign-in it works with no internet. What works offline and when things sync.","offline","app","2026-10-24"),
 ("export-and-share","خروجی PDF و Excel و اشتراک‌گذاری تراکنش","Exporting to PDF and Excel, and sharing a transaction","از سوابق‌تان گزارش بگیرید و کارت تصویری یک تراکنش را بفرستید.","Create reports from your records and send a transaction as an image card.","file","app","2026-10-24"),
 ("secure-your-account","امن‌سازی حساب: قفل، اثر انگشت و دستگاه‌ها","Securing your account: lock, fingerprint and devices","قفل برنامه، ورود زیستی، دستگاه‌های ورودی و کلید بازیابی را درست تنظیم کنید.","Set up the app lock, biometric sign-in, signed-in devices and your recovery key properly.","shield","app","2026-10-24"),
 ("how-to-budget","چطور بودجه‌بندی کنیم؟ راهنمای عملی","How to build a budget: a practical guide","بودجه‌بندی را از صفر یاد بگیرید: درآمد، دسته‌ها، سقف هزینه و بازبینی ماهانه.","Learn budgeting from scratch: income, categories, spending limits and monthly reviews.","pie","money","2026-10-21"),
 ("track-expenses","ثبت هزینه‌های روزانه بدون خسته شدن","Track daily expenses without burning out","چند عادت ساده که ثبت هزینه را از یک کار سنگین به یک عادت ۳۰ ثانیه‌ای تبدیل می‌کند.","A few simple habits that turn expense tracking from a chore into a 30-second routine.","wallet","money","2026-10-21"),
 ("savings-goals","پس‌انداز هدف‌دار: از قلک تا واریز ماهانه","Goal-based saving: from piggy banks to monthly deposits","هدف‌های مالی را مشخص کنید و با واریز زمان‌بندی‌شده به آن‌ها برسید.","Define financial goals and reach them with scheduled deposits.","target","money","2026-10-21"),
 ("manage-debts","مدیریت بدهی و طلب بین دوستان و خانواده","Managing debts and receivables with friends and family","چطور بدهی‌ها و طلب‌های شخصی را بدون دلخوری و فراموشی دنبال کنیم.","How to track personal debts and receivables without awkwardness or forgetting.","swap","money","2026-10-21"),
]

def body(L, slug):
    t = T(L); P = lambda s: f"<p>{s}</p>"
    ol = lambda *a: "<ol>" + "".join(f"<li>{x}</li>" for x in a) + "</ol>"
    ul = lambda *a: "<ul>" + "".join(f"<li>{x}</li>" for x in a) + "</ul>"
    lnk = lambda s, fa, en: f"<a href='{href(L,s)}'>{t(fa,en)}</a>"
    if slug == "getting-started":
        return [
         ("install", t("نصب","Install"), P(t(f"سیدنا برای اندروید {fad(C.MIN_ANDROID)} و بالاتر است. از صفحهٔ {chr(171)}دریافت{chr(187)} یکی از راه‌ها ({store_names('fa')}) را انتخاب کنید و فقط از لینک‌های رسمی دانلود کنید.",
            f"Cidna needs Android {C.MIN_ANDROID} or later. On the Download page pick {store_names('en')} — and only use the official links.")) + P(lnk("download","به صفحهٔ دریافت بروید","Go to the Download page"))),
         ("start", t("مهمان یا حساب؟","Guest or account?"), ul(t("<b>مهمان:</b> بدون شماره موبایل و بدون حساب؛ همه‌چیز فقط روی گوشی شما می‌ماند.","<b>Guest:</b> no phone number, no account — everything stays on your phone."),
            t("<b>حساب:</b> با شماره موبایل و کد پیامکی؛ برای همگام‌سازی چنددستگاهی و پشتیبان رمزشده.","<b>Account:</b> phone number and SMS code — for multi-device sync and an encrypted backup."))
            + P(t("می‌توانید به‌عنوان مهمان شروع کنید و بعداً حساب بسازید. اگر اطلاعات مهمان دارید، هنگام ثبت‌نام می‌پرسیم: به حساب جدید منتقل شود یا از صفر شروع کنید.","You can start as a guest and create an account later. If you already have guest data, sign-up asks once whether to move it into the account or start fresh."))),
         ("recovery", t("کلید بازیابی را همان لحظه ذخیره کنید","Save the recovery key right away"), P(t("پس از ساخت حساب، یک کلید بازیابی فقط یک‌بار نشان داده می‌شود. آن را در جای امنی (مثلاً یک مدیر رمز یا کاغذ) نگه دارید. چون اطلاعات شما سرتاسری رمزنگاری می‌شود، اگر هم گذرواژه و هم این کلید را گم کنید، هیچ‌کس نمی‌تواند داده‌ی همگام‌شده را بازیابی کند.","After you create an account, a recovery key is shown exactly once. Store it somewhere safe (a password manager or paper). Because your data is end-to-end encrypted, if you lose both the password and this key, nobody can recover the synced data."))),
         ("lock", t("قفل برنامه","App lock"), P(t("در تنظیمات، بخش «امنیت برنامه»، رمز ورود و ورود زیستی (اثر انگشت) را تنظیم کنید. راهنمای کامل: ","In Settings, under “App Security”, set a passcode and biometric sign-in. Full guide: ")+lnk("guides/secure-your-account","امن‌سازی حساب","Securing your account"))),
        ]
    if slug == "add-transaction":
        return [
         ("open", t("باز کردن فرم","Open the form"), P(t("در نوار پایین برنامه دکمهٔ «+» را بزنید و ثبت تراکنش جدید را انتخاب کنید.","Tap the “+” button in the app’s bottom bar and choose a new transaction."))),
         ("fill", t("پر کردن فرم","Fill it in"), ol(t("نوع را انتخاب کنید: <b>هزینه</b> یا <b>درآمد</b>.","Pick the type: <b>Expense</b> or <b>Income</b>."),
            t("<b>عنوان</b> را بنویسید یا با آیکن میکروفون بگویید.","Type a <b>title</b>, or speak it with the microphone icon."),
            t("<b>مبلغ</b> را وارد کنید (بزرگ‌تر از صفر).","Enter the <b>amount</b> (greater than zero)."),
            t("یک <b>دسته‌بندی</b> انتخاب کنید یا از «مدیریت دسته‌بندی‌ها» دستهٔ جدید بسازید.","Choose a <b>category</b>, or create one from “Manage Categories”."),
            t("اگر خواستید، <b>یادداشت</b> اختیاری بنویسید.","Add an optional <b>note</b>."),
            t("برای ذخیره، دکمه را <b>بکشید</b>؛ این کار جلوی ثبت اتفاقی را می‌گیرد.","<b>Swipe</b> to save — it prevents accidental entries."))),
         ("voice", t("ورود صوتی","Voice entry"), P(t("تشخیص گفتار را سرویس گفتار خود گوشی انجام می‌دهد و ممکن است برای پردازش، صدا را به ارائه‌دهندهٔ همان سرویس بفرستد؛ سیدنا صدای شما را ضبط یا ذخیره نمی‌کند. نتیجه را پیش از ذخیره بخوانید.","Speech recognition is done by your phone’s speech service, which may send audio to its provider; Cidna does not record or store your voice. Check the result before saving."))),
         ("pending", t("تراکنش‌های در انتظار","Pending transactions"), P(t("تراکنش‌های پیشنهادی (مثلاً از پیامک بانکی در نسخهٔ مستقیم) ابتدا «در انتظار» می‌مانند تا شما تأیید یا ردشان کنید. متن پیامک فقط روی گوشی پردازش می‌شود.","Suggested transactions (for example from bank SMS in the direct build) wait as “pending” until you confirm or reject them. SMS text is processed on the phone only."))),
        ]
    if slug == "limits-and-piggy-banks":
        return [
         ("limits", t("سقف هزینه برای هر دسته","A limit for every category"), P(t("در صفحهٔ اصلی، کارت «محدودیت‌های خرج‌کرد» را پیدا کنید و «تنظیم محدودیت جدید» را بزنید. دسته‌ای مثل «خوراکی و رستوران» را انتخاب و سقف ماهانه را مشخص کنید؛ نوار پیشرفت و هشدارها نشان می‌دهند چقدر از سقف مصرف شده است.",
            "On the home screen find the spending-limits card and tap “set a new limit”. Pick a category such as food and dining and set a monthly ceiling; the progress bar and alerts show how much is used."))),
         ("piggy", t("قلک برای هر هدف","A piggy bank per goal"), P(t("کارت «قلک‌های پس‌انداز» را پیدا کنید و «ساخت یک قلک جدید» را بزنید. مبلغ هدف را بنویسید؛ هر قلک پیشرفت خودش را با درصد نشان می‌دهد (مثلاً یک کیس گیمینگ ۲۵٪).",
            "Find the savings piggy banks card and tap “create a new piggy bank”. Enter the target amount; each piggy bank shows its own progress as a percentage.")) + ul(t("واریز و برداشت دستی","Manual deposits and withdrawals"), t("واریز ماهانهٔ زمان‌بندی‌شده","Scheduled monthly deposits"))),
         ("tips", t("چند توصیه","A few tips"), ul(t("سقف‌ها را با میانگین سه ماه اخیر شروع کنید، نه با آرزو.","Start limits from your last three months’ average, not from wishful thinking."), t("هر هدف یک قلک؛ ترکیب نکنید تا پیشرفت هر یک دیده شود.","One piggy bank per goal; don’t mix them so each progress stays visible.")) + P(lnk("guides/how-to-budget","راهنمای بودجه‌بندی","Budgeting guide")))
        ]
    if slug == "offline-and-sync":
        return [
         ("local", t("سوابق روی گوشی شماست","Your records live on your phone"), P(t("سیدنا «محلی‌محور» است: پایگاه‌دادهٔ شما روی خود گوشی و رمزگذاری‌شده (SQLCipher) نگه‌داری می‌شود. به همین خاطر استفادهٔ روزمره سریع است و به اینترنت وابسته نیست.","Cidna is local-first: your database sits on the phone, encrypted with SQLCipher. Daily use is fast and doesn’t depend on a connection."))),
         ("off", t("بعد از اولین ورود چه چیزی آفلاین کار می‌کند؟","What works offline after the first sign-in?"), ul(t("ثبت و ویرایش تراکنش‌ها","Recording and editing transactions"), t("بودجه‌ها، قلک‌ها، بدهی و طلب","Budgets, piggy banks, debts and receivables"), t("آمار و نمودارها","Analytics and charts"), t("خروجی PDF و Excel","PDF and Excel export"))
            + P(t("برای اولین بار، ساخت حساب و ورود به اینترنت نیاز دارد.","The very first time, creating an account and signing in need internet."))),
         ("sync", t("همگام‌سازی چه زمانی انجام می‌شود؟","When does sync happen?"), P(t("هر وقت اینترنت وصل شود، سیدنا تغییرات را همگام می‌کند و هم‌زمان اعتبار نشست را بررسی می‌کند. هر رکورد پیش از ارسال روی گوشی رمزنگاری می‌شود. اگر دستگاهی در حالت آفلاین از حساب خارج شده باشد، با اولین اتصال متوجه می‌شود.","Whenever a connection appears, Cidna syncs changes and checks the session. Every record is encrypted on the phone before upload. A device removed from the account while offline finds out on its next connection."))),
         ("lost", t("اگر گوشی عوض شد","If you change phones"), P(t("روی گوشی جدید با همان حساب وارد شوید. اگر گذرواژه را فراموش کرده‌اید، کلید بازیابی لازم است.","Sign in with the same account on the new phone. If you’ve forgotten the password, you’ll need the recovery key."))),
        ]
    if slug == "export-and-share":
        return [
         ("export", t("خروجی PDF و Excel","PDF and Excel export"), P(t("در تنظیمات، بخش «دریافت اطلاعات و گزارش‌ها»، قالب Excel یا PDF را انتخاب و بازهٔ زمانی را مشخص کنید. فایل فقط وقتی شما بخواهید ساخته می‌شود.","In Settings, under “Export Data & Reports”, choose Excel or PDF and set the period. The file is created only when you ask."))),
         ("share", t("کارت تصویری تراکنش","Transaction image cards"), P(t("هر تراکنش را می‌توانید به‌صورت یک کارت تصویری بفرستید؛ کارت با زبان، جهت متن و واحد پول برنامه هماهنگ است و هر جا که خودتان بفرستید می‌رود.","Any transaction can be sent as an image card that follows the app’s language, direction and currency — and goes only where you send it."))),
         ("why", t("چرا خروجی بگیریم؟","Why export?"), P(t("خروجی، نسخهٔ پشتیبان خواناست و برای حسابداری یا اشتراک با خانواده کاربرد دارد. توصیه می‌کنیم از اطلاعات مهم گاهی خروجی بگیرید.","An export is a readable backup, handy for accounting or sharing with family. We recommend exporting important data now and then."))),
        ]
    if slug == "secure-your-account":
        return [
         ("lock", t("قفل برنامه و اثر انگشت","App lock and fingerprint"), P(t("در «امنیت برنامه» رمز ورود بگذارید و در صورت تمایل ورود زیستی را روشن کنید. ورود زیستی از کلیدی در Keystore اندروید استفاده می‌کند که زیست‌سنجی قوی می‌خواهد.","Under “App Security” set a passcode and optionally turn on biometric sign-in, which uses an Android Keystore key that requires strong biometrics."))),
         ("pass", t("گذرواژهٔ خوب","A good password"), P(t("چون اطلاعات مالی شما با کلیدی محافظت می‌شود که از گذرواژه‌تان مشتق می‌شود، گذرواژهٔ بلند و منحصربه‌فرد مهم‌ترین کار شماست.","Because your financial data is protected by a key derived from your password, a long, unique password is the most important thing you can do."))),
         ("devices", t("دستگاه‌ها","Devices"), P(t("فهرست دستگاه‌های واردشده را ببینید، دستگاه ناشناس را از راه دور خارج کنید و با هر ورود از دستگاه جدید اعلان بگیرید.","See the signed-in devices, remove an unknown one remotely and get an alert for every new device."))),
         ("privacy", t("حریم تصویر","Screen privacy"), P(t("پیش‌فرض، پنجرهٔ برنامه از اسکرین‌شات و نمایش در لیست برنامه‌های اخیر محافظت می‌شود و می‌توانید این را در تنظیمات تغییر دهید. در صفحهٔ اصلی با آیکن چشم هم می‌توانید مبالغ را پنهان کنید.","By default the app window is protected from screenshots and from showing in Recents; you can change this in Settings. On the home screen the eye icon hides amounts."))),
         ("more", t("بیشتر بدانید","Learn more"), P(lnk("security","مدل امنیتی کامل","Full security model")+" · "+lnk("account-deletion","حذف حساب","Delete account"))),
        ]
    if slug == "how-to-budget":
        return [
         ("why", t("چرا بودجه؟","Why budget?"), P(t("بودجه یعنی تصمیم گرفتن دربارهٔ پول پیش از خرج‌کردن آن. هدف محدودکردن زندگی نیست؛ هدف این است که بدانید پولتان کجا می‌رود و آگاهانه انتخاب کنید.","A budget means deciding about money before you spend it. The goal isn’t to restrict your life — it’s to know where money goes and choose on purpose."))),
         ("steps", t("پنج قدم عملی","Five practical steps"), ol(t("<b>درآمد ماهانه را بنویسید</b> — فقط مبلغی که واقعاً به دستتان می‌رسد.","<b>Write down monthly income</b> — only what actually reaches you."),
            t("<b>هزینه‌های ثابت</b> — اجاره، قسط، قبض‌ها.","<b>Fixed costs</b> — rent, installments, bills."),
            t("<b>هزینه‌های متغیر</b> — خوراک، رفت‌وآمد، تفریح؛ برای هر دسته سقف بگذارید.","<b>Variable costs</b> — groceries, transport, leisure; set a limit per category."),
            t("<b>پس‌انداز را اول کنار بگذارید</b> — پیش از هزینه‌های متغیر جدا کنید.","<b>Pay yourself first</b> — set savings aside before variable spending."),
            t("<b>هر ماه بازبینی کنید</b> — سقف‌ها را با واقعیت تطبیق دهید.","<b>Review monthly</b> — adjust limits to reality."))),
         ("rule", t("قاعدهٔ ۵۰/۳۰/۲۰ فقط یک نقطهٔ شروع است","The 50/30/20 rule is only a starting point"), P(t("الگوی رایج: ۵۰٪ نیازها، ۳۰٪ خواسته‌ها، ۲۰٪ پس‌انداز. در شرایط اقتصادی متغیر، درصدها را با وضعیت خودتان تنظیم کنید؛ مهم‌تر از درصد، ثبات عادت است.","A common pattern: 50% needs, 30% wants, 20% savings. In changing economic conditions, tune the percentages to your situation — the habit matters more than the numbers."))),
         ("app", t("در سیدنا","In Cidna"), P(t("برای هر دسته سقف تعیین کنید و با نوار پیشرفت و هشدارها مصرف را دنبال کنید. ","Set a limit per category and follow it with progress bars and alerts. ")+lnk("guides/limits-and-piggy-banks","راهنمای قدم‌به‌قدم","Step-by-step guide"))),
        ]
    if slug == "track-expenses":
        return [
         ("habit", t("عادت ۳۰ ثانیه‌ای","The 30-second habit"), P(t("ثبت هزینه وقتی ماندگار می‌شود که سریع باشد. هزینه را همان لحظه یا آخر روز، در یک زمان ثابت ثبت کنید.","Tracking sticks when it’s fast. Log an expense right away, or at a fixed time at the end of each day."))),
         ("tips", t("پنج ترفند","Five tricks"), ul(t("از میان‌بر افزودن سریع استفاده کنید.","Use the quick-add shortcut."), t("هزینه را با صدا ثبت کنید و پیش از تأیید بررسی کنید.","Speak the expense and review before confirming."), t("دسته‌ها را کم و ساده نگه دارید.","Keep categories few and simple."), t("تراکنش‌های در انتظار را هفتگی مرور کنید.","Review pending transactions weekly."), t("هر هفته پنج دقیقه نمودار را ببینید.","Spend five minutes a week on the charts."))),
         ("sms", t("پیامک بانکی","Bank SMS"), P(t("در نسخهٔ مستقیم (direct)، پیامک‌های بانکی تشخیص داده و به‌صورت تراکنش در انتظار پیشنهاد می‌شوند؛ پردازش فقط روی دستگاه است.","In the direct build, bank SMS are detected and offered as pending transactions; processing happens on the device only."))),
        ]
    if slug == "savings-goals":
        return [
         ("define", t("هدف را مشخص کنید","Define the goal"), P(t("هدف خوب مبلغ و موعد دارد: «۵۰ میلیون تومان تا شش ماه دیگر برای سفر».","A good goal has an amount and a deadline: “save for a trip within six months”."))),
         ("auto", t("واریز زمان‌بندی‌شده","Scheduled deposits"), P(t("هدف را به واریز ماهانه تقسیم کنید و در سیدنا زمان‌بندی کنید تا به اراده وابسته نباشد.","Split the goal into monthly deposits and schedule them in Cidna so it doesn’t depend on willpower."))),
         ("piggy", t("قلک‌های جدا","Separate piggy banks"), P(t("برای هر هدف یک قلک بسازید تا پیشرفت هر کدام جدا دیده شود. ","Create one piggy bank per goal so each one’s progress is visible. ")+lnk("guides/limits-and-piggy-banks","ساخت قلک","Creating a piggy bank"))),
        ]
    return [
     ("why", t("چرا ثبت؟","Why record it?"), P(t("بدهی و طلب‌های شخصی اغلب در یادداشت یا چت گم می‌شوند. ثبت شفاف، هم از فراموشی جلوگیری می‌کند و هم از دلخوری.","Personal debts often get lost in notes or chats. A clear record prevents both forgetting and awkwardness."))),
     ("how", t("چه چیزی ثبت کنیم؟","What to record"), ul(t("طرف حساب و مبلغ","The other person and the amount"), t("تاریخ سررسید","Due date"), t("پرداخت‌های بخشی","Partial payments"))),
     ("remind", t("یادآور","Reminders"), P(t("در سیدنا برای سررسیدها یادآور تنظیم کنید تا نیازی به پیگیری مکرر نباشد.","Set due-date reminders in Cidna so you don’t need to chase repeatedly."))),
    ]

EXTRA_ICONS = {"rocket":"<path d='M5 15c-1.500 1.500-2 5-2 5s3.500-.5 5-2'/><path d='M14 4c3-1.500 6-1.500 6-1.500S20 6 18.500 9L13 14.500 9.500 11z'/><path d='M9.500 11L5 12l-2-1 4-3 3 .5M13 14.500L12 19l1 2 3-4-.5-3'/>"}
from engine import ui as _ui
_ui.ICONS.update(EXTRA_ICONS)

def cards(L, cat=None, exclude=None, limit=None):
    t = T(L)
    gs = [g for g in GUIDES if (cat is None or g[6] == cat) and g[0] != exclude][:limit]
    return "".join(f"<a class='card' href='{href(L,'guides/'+g[0])}'>{ico(g[5])}<h3>{t(g[1],g[2])}</h3><p>{t(g[3],g[4])}</p><span class='go'>{t('بخوانید','Read')}</span></a>" for g in gs)

def hub(L):
    t = T(L)
    inner = f"""<section class='sec'><div class='wrap'>
<h2 class='group-title'>{t('کار با برنامه','Using the app')}</h2><div class='cards'>{cards(L,'app')}</div>
<h2 class='group-title'>{t('مدیریت مالی شخصی','Personal finance basics')}</h2><div class='cards'>{cards(L,'money')}</div></div></section>"""
    sch = [{"@type":"CollectionPage","url":url(L,"guides"),"name":t("آموزش‌ها","Guides"),"hasPart":[{"@type":"Article","headline":t(g[1],g[2]),"url":url(L,"guides/"+g[0])} for g in GUIDES]}]
    return simple_page(L, "guides", t("آموزش‌های سیدنا","Cidna guides"),
        t("آموزش قدم‌به‌قدم کار با سیدنا و اصول مدیریت مالی شخصی: ثبت تراکنش، سقف هزینه، قلک، آفلاین و همگام‌سازی، خروجی و امنیت.","Step-by-step Cidna tutorials and personal-finance basics: recording, spending limits, piggy banks, offline and sync, export and security."),
        t("هم یاد بگیرید برنامه چطور کار می‌کند، هم چطور با پولتان بهتر کنار بیایید.","Learn how the app works — and how to get along better with your money."),
        inner, crumbs=[(t("آموزش‌ها","Guides"),"guides")], schema=sch,
        keywords=t("آموزش مدیریت مالی, آموزش ثبت هزینه, بودجه بندی شخصی, قلک پس انداز, ثبت تراکنش","personal finance guide, expense tracking tutorial, budgeting, savings goals"))

def article(L, g):
    t = T(L); slug, tf, te, df, de, ic, cat, pub = g
    title = t(tf, te)
    rel = f"<section class='sec'><div class='wrap'><h2 class='group-title'>{t('بیشتر بخوانید','Keep reading')}</h2><div class='cards'>{cards(L, cat, exclude=slug, limit=3)}</div></div></section>"
    art = {"@type":"Article","headline":title,"description":t(df,de),"inLanguage":L,"datePublished":pub,"dateModified":C.TODAY,"mainEntityOfPage":url(L,"guides/"+slug),
           "author":{"@id":C.DOMAIN+"/#org"},"publisher":{"@id":C.DOMAIN+"/#org"},"image":f"{C.DOMAIN}/assets/img/og-cover-{L}.png"}
    dsc = t(df + " — راهنمای سیدنا، اپ مدیریت مالی شخصی.", de + " — a Cidna guide.")
    return doc(L, "guides/"+slug, title, dsc, t(df,de), body(L, slug), [(t("آموزش‌ها","Guides"),"guides"),(title,"guides/"+slug)],
               schema=[art], meta=t(f"آخرین بازبینی: {fad(C.TODAY)}", f"Last updated: {C.TODAY}"), og_type="article", published=pub, after=rel, nav_slug="guides")
