# -*- coding: utf-8 -*-
import config as C
from engine.ui import *

def items(L):
    """(question, answer_html, show_on_home)"""
    t = T(L)
    src = (f" <a href='{e(C.SOURCE_URL)}' rel='noopener'>{t('کد منبع','Source code')}</a>." if C.SOURCE_URL else "")
    return [
     (t("سیدنا واقعاً رایگان است؟","Is Cidna really free?"),
      "<p>"+t("بله، کاملاً. سیدنا اشتراک، خرید درون‌برنامه‌ای و نسخهٔ پولی ندارد و هیچ تبلیغی هم نشان نمی‌دهد. کد برنامه نیز با مجوز AGPL-3.0-or-later متن‌باز است.",
              "Yes, completely. Cidna has no subscription, no in-app purchases and no paid tier, and it shows no ads. The code is open source under AGPL-3.0-or-later.")+src+"</p>", True),
     (t("بعد از ورود می‌توانم بدون اینترنت از برنامه استفاده کنم؟","Can I use the app offline after signing in?"),
      "<p>"+t("بله. بعد از ساخت حساب و اولین ورود موفق، سوابق شما روی خود گوشی است و ثبت تراکنش، مرور سوابق، بودجه، قلک، بدهی و آمار بدون اینترنت کار می‌کند. هر وقت دوباره به اینترنت وصل شدید، تغییرات همگام می‌شوند.",
              "Yes. After you create an account and sign in successfully once, your records live on the phone, so recording transactions, browsing history, budgets, piggy banks, debts and analytics all work without internet. Changes sync the next time you’re online.")+"</p>"
      "<p>"+t("توجه: ساخت حساب، ورود اولیه و همگام‌سازی به اینترنت نیاز دارند. اگر دستگاهی را از راه دور از حسابتان خارج کنید، آن دستگاه همان لحظه‌ای که آنلاین شود متوجه می‌شود.",
              "Note: creating an account, the first sign-in and syncing need internet. If you remove a device from your account remotely, that device finds out the moment it goes online.")+"</p>", True),
     (t("آیا اطلاعات من سرتاسری (End-to-End) رمزنگاری می‌شود؟","Is my data end-to-end encrypted?"),
      "<p>"+t("بله، برای اطلاعات مالی. تراکنش‌ها، دسته‌بندی‌ها، بودجه‌ها، بدهی‌ها و اهداف پس‌انداز پیش از ارسال روی خود گوشی با AES-256-GCM رمز می‌شوند. کلید این رمزنگاری روی گوشی ساخته می‌شود و فقط به‌شکل «بسته‌بندی‌شده» از آن خارج می‌شود. گذرواژه و کلید بازیابی شما هرگز به سرور فرستاده نمی‌شوند؛ پس نه ما و نه مهاجمی که به سرور نفوذ کند، بدون حدس‌زدن گذرواژهٔ شما نمی‌تواند داده‌ها را باز کند.",
              "Yes — for your financial records. Transactions, categories, budgets, debts and savings goals are encrypted on the phone with AES-256-GCM before upload. The key is generated on your device and leaves it only in wrapped form. Your password and recovery key are never sent to the server, so neither we nor an attacker who compromises the server can open your data without guessing your password.")+"</p>"
      "<p>"+t("دقت کنید: اطلاعات لازم برای کارکرد سرویس (مثل شماره موبایل و فهرست دستگاه‌ها) زیر پوشش این رمزنگاری نیست. جزئیات کامل در <a href='"+href(L,'security')+"'>صفحهٔ امنیت</a> آمده است.",
              "Scope matters: the details the service needs to run (such as your phone number and device list) are not covered by this encryption. Full details are on the <a href='"+href(L,'security')+"'>Security page</a>.")+"</p>", True),
     (t("برای شروع حتماً باید ثبت‌نام کنم؟","Do I have to sign up to start?"),
      "<p>"+t("نه. حالت مهمان بدون حساب و بدون شماره موبایل کار می‌کند و با رمز یا اثر انگشت قفل می‌شود. هر وقت خواستید حساب بسازید و تصمیم بگیرید اطلاعات مهمان به حساب جدید منتقل شود یا از صفر شروع کنید.",
              "No. Guest mode works with no account and no phone number, protected by a passcode or fingerprint. Create an account whenever you like and choose whether to move your guest data into it or start fresh.")+"</p>", True),
     (t("اگر گذرواژه یا کلید بازیابی را گم کنم چه می‌شود؟","What if I lose my password or recovery key?"),
      "<p>"+t("اگر فقط گذرواژه را فراموش کنید، با کلید بازیابی برمی‌گردید؛ این کلید فقط یک‌بار در پایان ثبت‌نام نشان داده می‌شود، پس جای امنی ذخیره‌اش کنید. اگر هر دو را گم کنید، به‌خاطر همان رمزنگاری سرتاسری، بازیابی دادهٔ همگام‌شده ممکن نیست.",
              "If you only forget the password, the recovery key gets you back in. It is shown just once at the end of sign-up, so store it somewhere safe. If you lose both, synced data can’t be recovered — that is the flip side of end-to-end encryption.")+"</p>", True),
     (t("سیدنا به حساب بانکی من وصل می‌شود؟","Does Cidna connect to my bank account?"),
      "<p>"+t("خیر. سیدنا به هیچ حساب بانکی یا سامانهٔ پرداختی وصل نیست و پولی جابه‌جا نمی‌کند؛ ابزار ثبت و مرور است.",
              "No. Cidna isn’t connected to any bank account or payment system and never moves money. It’s a record-keeping tool.")+"</p>", True),
     (t("تشخیص تراکنش از پیامک بانکی چطور کار می‌کند؟","How does bank-SMS detection work?"),
      "<p>"+t("این قابلیت فقط در نسخهٔ «مستقیم» (direct) است و فقط پس از اجازهٔ شما فعال می‌شود. متن پیامک فقط روی همان گوشی پردازش می‌شود، به سرور ارسال نمی‌شود و به‌صورت «تراکنش در انتظار» برای تأیید خودتان پیشنهاد می‌شود. نسخه‌های دیگر هیچ مجوز پیامکی ندارند.",
              "This exists only in the “direct” build and only after you grant the permission. SMS text is processed on the phone, never sent to a server, and offered as a pending transaction for you to confirm. Other builds have no SMS permissions.")+"</p>", False),
     (t("سیدنا تبلیغ یا ردیاب دارد؟","Does Cidna have ads or trackers?"),
      "<p>"+t("خیر. هیچ تبلیغ، ابزار تحلیل رفتار یا ردیاب شخص ثالثی در برنامه نیست. این وب‌سایت هم کوکی ندارد و فقط انتخاب تم شما را در مرورگر خودتان نگه می‌دارد.",
              "No. The app has no advertising, analytics or third-party trackers. This website sets no cookies either; it only remembers your theme choice in your own browser.")+"</p>", False),
     (t("حذف حساب چطور کار می‌کند؟","How does account deletion work?"),
      "<p>"+t("حذف ابتدا «نرم» است: نشست‌ها فوراً لغو می‌شوند و تا ۳۰ روز می‌توانید با ورود دوباره حساب را بازیابی کنید. بعد از آن، حساب و داده‌های همگام‌شده برای همیشه پاک می‌شوند. مراحل در <a href='"+href(L,'account-deletion')+"'>صفحهٔ حذف حساب</a> آمده است.",
              "Deletion is “soft” first: sessions end immediately and you can restore the account by signing in within 30 days. After that, the account and synced data are erased permanently. Steps are on the <a href='"+href(L,'account-deletion')+"'>account deletion page</a>.")+"</p>", False),
     (t("از چه تقویم و واحد پولی پشتیبانی می‌کند؟","Which calendars and currencies are supported?"),
      "<p>"+t("تقویم جلالی و میلادی، نمایش مبلغ به تومان یا ریال، و رابط کاملاً فارسی و انگلیسی با پشتیبانی درست از راست‌به‌چپ و چپ‌به‌راست.",
              "Jalali and Gregorian calendars, amounts in Toman or Rial, and a fully Persian and English interface with proper right-to-left and left-to-right support.")+"</p>", False),
     (t("روی چه دستگاه‌هایی کار می‌کند؟","Which devices does it run on?"),
      "<p>"+t(f"سیدنا یک برنامهٔ اندروید است (اندروید {fad(C.MIN_ANDROID)} یا بالاتر). نسخهٔ iOS یا وب ندارد.",
              f"Cidna is an Android app (Android {C.MIN_ANDROID} or later). There’s no iOS or web version.")+"</p>", False),
     (t("می‌توانم از چند گوشی استفاده کنم؟","Can I use it on more than one phone?"),
      "<p>"+t("بله. با همان حساب روی گوشی دیگر وارد شوید تا اطلاعات همگام شود. فهرست دستگاه‌ها را می‌بینید، می‌توانید از راه دور دستگاهی را خارج کنید و با هر ورود از دستگاه جدید اعلان می‌گیرید.",
              "Yes. Sign in with the same account on another phone and your data syncs. You can see your device list, remove a device remotely, and get an alert whenever a new device signs in.")+"</p>", False),
     (t("می‌توانم از اطلاعاتم خروجی بگیرم؟","Can I export my data?"),
      "<p>"+t("بله، خروجی PDF و Excel (XLSX) از تراکنش‌ها در تنظیمات برنامه، و کارت تصویری برای اشتراک‌گذاری یک تراکنش.",
              "Yes — PDF and Excel (XLSX) exports of transactions from the app’s settings, plus shareable image cards for individual transactions.")+"</p>", False),
     (t("استفاده برای زیر ۱۸ سال مجاز است؟","Can people under 18 use it?"),
      "<p>"+t("استفاده برای افراد زیر ۱۸ سال باید با اطلاع و نظارت ولی یا سرپرست قانونی باشد.",
              "Use by people under 18 should be with the knowledge and supervision of a parent or legal guardian.")+"</p>", False),
     (t("برای پشتیبانی چطور تماس بگیرم؟","How do I contact support?"),
      "<p>"+t(f"از ایمیل <a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a>، واتس‌اپ یا شبکه‌های اجتماعی؛ همهٔ راه‌ها در <a href='{href(L,'contact')}'>صفحهٔ تماس</a> هست.",
              f"Email <a href='mailto:{C.EMAIL}'><span class='ltr'>{C.EMAIL}</span></a>, WhatsApp or social media — every channel is on the <a href='{href(L,'contact')}'>Contact page</a>.")+"</p>", False),
    ]
