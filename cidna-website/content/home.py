# -*- coding: utf-8 -*-
import config as C
from engine.ui import *
from engine.layout import page, HAS_LIGHT_SHOT
import content.faq as FAQ

def tile(icon, h, p, cls=""):
    return f"<article class='tile {cls}'>{ico(icon)}<h3>{h}</h3><p>{p}</p></article>"

def hero_phone(L):
    t = T(L)
    alt = t("صفحهٔ اصلی اپلیکیشن سیدنا: تراز کل حساب، درآمد و هزینه، قلک‌های پس‌انداز و محدودیت‌های خرج‌کرد",
            "Cidna home screen: total balance, income and expenses, savings piggy banks and spending limits")
    light = f"<img class='shot-light' src='/assets/img/app-home-light.webp' width='628' height='1280' alt='{e(alt)}' decoding='async'>" if HAS_LIGHT_SHOT else ""
    return f"""<div class='stage'>
<div class='phone'><picture><source media='(max-width:520px)' srcset='/assets/img/app-home-sm.webp'><img src='/assets/img/app-home.webp' width='628' height='1280' alt='{e(alt)}' fetchpriority='high' decoding='async'></picture>{light}
<span class='tag a'>{svg('wallet')}{t('ترازتان، یک نگاه','Your balance at a glance')}</span>
<span class='tag b'>{svg('target')}{t('یک قلک برای هر هدف','A piggy bank per goal')}</span>
<span class='tag c'>{svg('pie')}{t('سقف خرج برای هر دسته','A limit for every category')}</span></div>
<p class='sample-note'>{t('تصویر واقعی از نسخهٔ تیرهٔ برنامه (داده‌ها نمونه‌اند)','Real screenshot of the dark theme (sample data)')}</p></div>"""

def flow(L):
    t = T(L)
    return f"""<div class='flow' role='img' aria-label='{e(t("داده‌ها روی گوشی خوانا هستند، با AES-256-GCM قفل می‌شوند و روی سرور فقط به‌صورت رمزشده و نامفهوم ذخیره می‌شوند","Your data is readable on your phone, locked with AES-256-GCM, and stored on the server only as unreadable ciphertext"))}'>
<div class='node plain'><b>{t('روی گوشی شما','On your phone')}</b><small>{t('تراکنش‌ها، بودجه‌ها، قلک‌ها','Transactions, budgets, piggy banks')}</small><small>{t('خوانا — فقط برای شما','Readable — only to you')}</small></div>
<div class='lock'>{svg('lock')}<code>AES-256-GCM</code></div>
<div class='node'><b>{t('روی سرور','On the server')}</b><small>{t('فقط داده‌ی رمزشده؛ برای ما هم نامفهوم','Ciphertext only — unreadable even to us')}</small><div class='cipher' aria-hidden='true' data-final='q7Zx9fA3…Lm0WpK2='>q7Zx9fA3…Lm0WpK2=</div></div></div>"""

def home(L):
    t = T(L); n = (lambda s: fad(s) if L == "fa" else s)
    faqs = [x for x in FAQ.items(L) if x[2]]
    chips = [("gift", t("رایگان و بدون تبلیغ","Free, no ads")), ("lock", t("رمزنگاری سرتاسری","End-to-end encrypted")), ("offline", t("آفلاین پس از ورود","Offline after sign-in"))]
    chips_html = "".join(f"<li>{svg(i,18)}{x}</li>" for i, x in chips)

    hero = f"""<section class='hero'><div class='wrap hero-grid'>
<div>
<h1>{t('پولتان، بالاخره آرام و روشن.','Your money, finally calm.')}</h1>
<p class='lead'>{t('درآمد، هزینه، بودجه، قلک و بدهی‌هایتان در یک اپ اندروید؛ به فارسی، با تقویم جلالی و تومان. کاملاً رایگان، با رمزنگاری سرتاسری، و پس از اولین ورود بدون اینترنت هم کار می‌کند.',
 'Income, spending, budgets, piggy banks and debts in one Android app — in Persian or English, with the Jalali calendar and Toman. Completely free, end-to-end encrypted, and it keeps working offline once you’ve signed in.')}</p>
<ul class='chips'>{chips_html}</ul>
{store_buttons(L)}
</div>
{hero_phone(L)}
</div></section>"""

    pledge = section(f"""{head(t('سه قولی که می‌توانیم نشانتان بدهیم','Three promises we can show you'),
  t('نه شعار؛ هر کدام در کد برنامه و در متن حریم خصوصی آمده است.','Not slogans — each one is in the app’s code and written into the privacy policy.'))}
<div class='pledge'>
<article class='panel big'>{ico('lock')}<h3>{t('رمزنگاری سرتاسری','End-to-end encryption')}</h3>
<p>{t('اطلاعات مالی شما پیش از ارسال، روی خود گوشی قفل می‌شود. کلید این قفل روی گوشی ساخته می‌شود و هرگز به‌صورت باز از آن خارج نمی‌شود؛ سرور فقط داده‌ی رمزشده نگه می‌دارد که نه ما می‌توانیم بخوانیم و نه کسی که به سرور نفوذ کند.',
 'Your financial records are locked on the phone before anything is uploaded. The key is created on your device and never leaves it in the clear; the server keeps only ciphertext that neither we nor anyone who breaks into the server can read.')}</p>
{flow(L)}
<p class='note'>{t('گذرواژه و کلید بازیابی هرگز به سرور فرستاده نمی‌شوند. چون امنیت به گذرواژه‌ی شما تکیه دارد، گذرواژه‌ی بلند و منحصربه‌فرد انتخاب کنید. ','Your password and recovery key are never sent to the server. Because the lock depends on your password, choose a long, unique one. ')}<a href='{href(L,'security')}'>{t('دامنه‌ی دقیق و محدودیت‌ها','Exact scope and limits')}</a></p></article>
<article class='panel'>{ico('gift')}<h3>{t('کاملاً رایگان','Completely free')}</h3>
<p>{t('بدون اشتراک، بدون خرید درون‌برنامه‌ای و بدون تبلیغ. حتی لازم نیست حساب بسازید؛ حالت مهمان از همان لحظه‌ی اول کار می‌کند. کد برنامه هم متن‌باز است.','No subscription, no in-app purchases, no ads. You don’t even need an account — guest mode works from the first second. The code is open source.')}</p></article>
<article class='panel'>{ico('offline')}<h3>{t('پس از ورود، آفلاین هم کار می‌کند','Works offline once you’re in')}</h3>
<p>{t('یک‌بار حساب بسازید و وارد شوید؛ از آن به بعد ثبت تراکنش، بودجه، قلک، بدهی و آمار روی خود گوشی کار می‌کند، حتی بدون اینترنت. با اولین اتصال، همه‌چیز همگام می‌شود.','Create an account and sign in once; from then on, recording, budgets, piggy banks, debts and analytics run on the phone itself, even with no connection. Everything syncs when you’re back online.')}</p></article>
</div>""", "")

    feats = section(f"""{head(t('هرچه برای مدیریت پولتان لازم است','Everything you need to manage your money'),
  t('از ثبت یک خرج ساده تا هدف‌گذاری برای پس‌انداز؛ سریع، فارسی و خصوصی.','From logging a coffee to saving for a goal — fast, Persian-first and private.'))}
<div class='bento'>
{tile('user',t('شروع بدون حساب','Start without an account'),t('حالت مهمان با رمز یا اثر انگشت. بعداً اطلاعات را به حساب جدید منتقل کنید یا از صفر شروع کنید.','Guest mode with a passcode or fingerprint. Later, move your data into a new account or start fresh.'),'w3 accent')}
{tile('mic',t('ثبت سریع','Fast entry'),t('میان‌بر ثبت سریع، ورود صوتی و مرور تراکنش‌های در انتظار؛ هزینه‌ی روزانه شود کار ۳۰ ثانیه‌ای.','Quick-add shortcut, voice entry and a pending-transaction review — logging takes seconds.'),'w3')}
{tile('target',t('قلک‌های پس‌انداز','Savings piggy banks'),t('برای هر هدف یک قلک بسازید، واریز و برداشت کنید و واریز ماهانه زمان‌بندی کنید.','One piggy bank per goal, with deposits, withdrawals and scheduled monthly deposits.'))}
{tile('pie',t('سقف هزینه','Spending limits'),t('برای هر دسته سقف تعیین کنید و با نوار پیشرفت و هشدار، از آن عبور نکنید.','Set a limit per category and stay under it with progress bars and alerts.'))}
{tile('swap',t('بدهی و طلب','Debts & receivables'),t('طرف حساب، پرداخت‌های جزئی، سررسید و یادآور؛ دیگر لای چت‌ها گم نمی‌شوند.','Counterparties, partial payments, due dates and reminders — no more debts lost in chats.'))}
{tile('cal',t('جلالی، تومان، راست‌به‌چپ','Jalali, Toman, true RTL'),t('تاریخ جلالی یا میلادی، مبلغ به تومان یا ریال، و رابط فارسی و انگلیسی؛ بومی، نه ترجمه‌شده.','Jalali or Gregorian dates, Toman or Rial amounts, Persian and English UI — native, not an afterthought.'))}
{tile('file',t('خروجی و اشتراک','Export & share'),t('خروجی PDF و Excel و کارت تصویری تراکنش که با زبان و واحد پول برنامه هماهنگ است.','PDF and Excel export, plus shareable transaction cards that follow the app’s language and currency.'))}
{tile('finger',t('قفل و دستگاه‌ها','Lock & devices'),t('قفل برنامه، ورود زیستی، فهرست دستگاه‌ها با خروج از راه دور و اعلان ورود جدید.','App lock, biometric sign-in, a device list with remote sign-out and new-device alerts.'))}
</div><p class='more'><a class='btn btn-ghost' href='{href(L,'features')}'>{t('همه‌ی امکانات','All features')}</a></p>""", "")

    journey = section(f"""{head(t('از اولین لمس تا چند گوشی','From first tap to several phones'))}
<ol class='journey'>
<li><b>{t('امتحان کنید','Try it')}</b><span>{t('برنامه را به‌عنوان مهمان باز کنید؛ چیزی برای ثبت‌نام نیست.','Open the app as a guest — nothing to sign up for.')}</span></li>
<li><b>{t('حساب بسازید','Create an account')}</b><span>{t('با شماره موبایل و کد پیامکی. بعد از اولین ورود موفق، آفلاین هم کار می‌کند.','With your phone number and an SMS code. After the first successful sign-in it works offline too.')}</span></li>
<li><b>{t('تصمیم بگیرید','Decide')}</b><span>{t('اگر اطلاعات مهمان دارید، بپرسیم: به حساب جدید منتقل شود یا از صفر شروع کنید؟','If you have guest data, we ask once: move it to the new account, or start fresh?')}</span></li>
<li><b>{t('کلید بازیابی را نگه دارید','Keep your recovery key')}</b><span>{t('فقط یک‌بار نشان داده می‌شود و تنها راه بازگشت، اگر گذرواژه را فراموش کنید.','Shown once — your only way back if you forget the password.')}</span></li>
<li><b>{t('چنددستگاهی شوید','Go multi-device')}</b><span>{t('روی گوشی دیگر وارد شوید و با هر ورود از دستگاه جدید اعلان بگیرید.','Sign in on another phone and get an alert for every new device.')}</span></li>
</ol>""", "")

    scope = section(f"""{head(t('صادقانه: رمزنگاری ما چه چیزهایی را پوشش می‌دهد؟','Honestly: what does our encryption cover?'),
  t('ما فقط چیزی را ادعا می‌کنیم که بتوانیم نشان بدهیم؛ به همین دلیل از «دانش صفر» حرف نمی‌زنیم.','We only claim what we can show — which is why we don’t say “zero-knowledge”.'))}
<div class='scope'>
<article class='panel yes'><h3>{t('رمزنگاری سرتاسری می‌شود','End-to-end encrypted')}</h3><ul>
<li>{svg('check')}<span>{t('تراکنش‌ها و دسته‌بندی‌ها','Transactions and categories')}</span></li>
<li>{svg('check')}<span>{t('بودجه‌ها و سقف‌های هزینه','Budgets and spending limits')}</span></li>
<li>{svg('check')}<span>{t('بدهی‌ها، طلب‌ها و اهداف پس‌انداز','Debts, receivables and savings goals')}</span></li>
<li>{svg('check')}<span>{t('پایگاه‌داده‌ی روی گوشی هم رمزگذاری‌شده است (SQLCipher)','The on-device database is encrypted too (SQLCipher)')}</span></li></ul></article>
<article class='panel no'><h3>{t('خارج از این پوشش است','Not covered by it')}</h3><ul>
<li>{svg('info')}<span>{t('شماره موبایل و مشخصات نمایه — برای کار کردن سرویس لازم‌اند','Phone number and profile fields — the service needs them to work')}</span></li>
<li>{svg('info')}<span>{t('فهرست دستگاه‌ها، زمان و حجم ترافیک همگام‌سازی','Device list, timing and size of sync traffic')}</span></li>
<li>{svg('info')}<span>{t('اگر هم گذرواژه و هم کلید بازیابی را گم کنید، داده‌ی همگام‌شده قابل بازیابی نیست','If you lose both password and recovery key, synced data can’t be recovered')}</span></li></ul></article>
</div><p class='more'><a href='{href(L,'security')}'>{t('مدل امنیتی کامل را بخوانید','Read the full security model')}</a></p>""", "")

    dl = section(f"""<div class='band glass'><div><h2>{t('سیدنا را همین حالا بگیرید','Get Cidna now')}</h2>
<p>{t('برای اندروید ۸ و بالاتر. لینک‌های دریافت به‌محض انتشار در همین‌جا فعال می‌شوند.','For Android 8 and up. Download links go live here the moment they’re published.')}</p></div>{store_buttons(L)}</div>""", "")

    faq = section(f"""{head(t('پرسش‌های پرتکرار','Frequently asked questions'))}{faq_html(faqs)}
<p class='more'><a href='{href(L,'faq')}'>{t('همه‌ی پرسش‌ها','All questions')}</a></p>""", "")

    body = hero + pledge + feats + journey + scope + dl + faq

    app = {"@type":["SoftwareApplication","MobileApplication"],"@id":C.DOMAIN+"/#app","name":"Cidna","alternateName":"سیدنا","applicationCategory":"FinanceApplication",
      "operatingSystem":f"Android {C.MIN_ANDROID}+","softwareVersion":C.APP_VERSION,"inLanguage":["fa","en"],"url":C.DOMAIN+"/","publisher":{"@id":C.DOMAIN+"/#org"},
      "isAccessibleForFree":True,"offers":{"@type":"Offer","price":"0","priceCurrency":"IRR"},
      "license":"https://www.gnu.org/licenses/agpl-3.0.html","screenshot":C.DOMAIN+"/assets/img/app-home.webp","image":C.DOMAIN+"/assets/img/og-cover-"+L+".png",
      "description":t("اپلیکیشن رایگان مدیریت مالی شخصی برای اندروید با رمزنگاری سرتاسری، تقویم جلالی، تومان/ریال و امکان استفاده آفلاین پس از ورود.","Free personal finance app for Android with end-to-end encryption, Jalali calendar, Toman/Rial and offline use after sign-in."),
      "featureList":["Income and expense tracking","Budgets per category","Savings goals","Debts and receivables","Jalali and Gregorian calendars","Toman and Rial","PDF and XLSX export","Guest mode","End-to-end encrypted sync","Offline use after sign-in","Light and dark theme"]}
    dl_urls = [s["url"] for s in C.STORES if s["url"]]
    if dl_urls: app["downloadUrl"] = dl_urls[0]
    return page(L, "", t("سیدنا؛ اپ مدیریت مالی شخصی رایگان با رمزنگاری سرتاسری","Cidna — free personal finance app with end-to-end encryption"),
        t("سیدنا اپلیکیشن رایگان مدیریت مالی شخصی برای اندروید: ثبت هزینه، بودجه، قلک و بدهی با تقویم جلالی و تومان؛ رمزنگاری سرتاسری و کار آفلاین پس از ورود.",
          "Cidna is a free Android personal finance app: track spending, budgets, piggy banks and debts with Jalali dates and Toman — end-to-end encrypted and usable offline after sign-in."),
        body, schema=[app, faq_schema(faqs)],
        keywords=t("مدیریت مالی شخصی, اپلیکیشن مدیریت هزینه, ثبت هزینه روزانه, بودجه بندی, قلک پس انداز, برنامه حسابداری شخصی اندروید, رمزنگاری سرتاسری",
                   "personal finance app, expense tracker Android, budget app, savings goals, Persian finance app, Jalali calendar, end-to-end encrypted"))
