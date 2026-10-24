# -*- coding: utf-8 -*-
import json, os
import config as C
from engine.ui import *

NAV = [  # slug, fa, en, icon
    ("", "خانه", "Home", "home"),
    ("features", "امکانات", "Features", "pie"),
    ("guides", "آموزش", "Guides", "book"),
    ("security", "امنیت", "Security", "shield"),
    ("faq", "سوالات متداول", "FAQ", "info"),
    ("about", "درباره", "About", "user"),
]
DOCK = ["", "features", "guides", "security"]    # تب‌های نوار پایین موبایل (مثل bottom bar اپ)

HAS_LIGHT_SHOT = os.path.exists(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "static/img/app-home-light.webp"))

def asset(path):
    """cache-busting با تاریخ آخرین تغییر"""
    return f"/assets/{path}?v={C.TODAY.replace('-', '')}"

def social_links(L):
    t = T(L)
    return [("mail", t("ایمیل پشتیبانی","Email support"), "mailto:" + C.EMAIL),
            ("whatsapp", t("واتس‌اپ توسعه‌دهنده","WhatsApp"), C.WHATSAPP),
            ("x", t("ایکس (توییتر)","X (Twitter)"), C.X_URL),
            ("instagram", t("اینستاگرام","Instagram"), C.INSTAGRAM)]

def page(L, slug, title, desc, body, schema=None, crumbs=None, og_type="website", noindex=False,
         published=None, modified=None, keywords=None, nav_slug=None):
    t = T(L)
    other = "en" if L == "fa" else "fa"
    canon = url(L, slug)
    brand = t("سیدنا", "Cidna")
    full_title = title if slug == "" else f"{title} | {brand}"
    cur = nav_slug if nav_slug is not None else slug

    nav = "".join(f"<a href='{href(L,s)}'{' aria-current=\"page\"' if s==cur else ''}>{t(fa,en)}</a>" for s, fa, en, _ in NAV)
    dock = "".join(
        f"<a class='tab' href='{href(L,s)}'{' aria-current=\"page\"' if s==cur else ''}>{svg(ic,22)}<span>{t(fa,en)}</span></a>"
        for s, fa, en, ic in NAV if s in DOCK)

    graph = [
      {"@type":"Organization","@id":C.DOMAIN+"/#org","name":"Cidna","alternateName":"سیدنا","url":C.DOMAIN+"/",
       "logo":C.DOMAIN+"/assets/img/cidna-mark.png","email":C.EMAIL,
       "sameAs":[C.X_URL, C.INSTAGRAM]},
      {"@type":"WebSite","@id":C.DOMAIN+"/#site","url":C.DOMAIN+"/","name":"Cidna | سیدنا","inLanguage":["fa","en"],"publisher":{"@id":C.DOMAIN+"/#org"}},
    ]
    if crumbs:
        graph.append({"@type":"BreadcrumbList","itemListElement":[{"@type":"ListItem","position":i+1,"name":n,"item":u_} for i,(n,u_) in enumerate(crumbs)]})
    if schema: graph += schema
    ld = json.dumps({"@context":"https://schema.org","@graph":graph}, ensure_ascii=False, separators=(",",":"))

    bc = ""
    if crumbs:
        bc = "<nav aria-label='" + t("مسیر","Breadcrumb") + "'><ol class='crumbs'>" + "".join(
            (f"<li><a href='{u_.replace(C.DOMAIN,'')}'>{e(n)}</a></li>" if i < len(crumbs)-1 else f"<li aria-current='page'>{e(n)}</li>")
            for i,(n,u_) in enumerate(crumbs)) + "</ol></nav>"
    body = body.replace("{{CRUMBS}}", bc)

    og_img = f"{C.DOMAIN}/assets/img/og-cover-{L}.png"
    robots = "noindex,follow" if noindex else "index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1"
    art = ""
    if published: art += f"<meta property='article:published_time' content='{published}'>"
    if modified or published: art += f"<meta property='article:modified_time' content='{modified or C.TODAY}'>"
    kw = f"<meta name='keywords' content='{e(keywords)}'>" if keywords else ""
    alt = "" if noindex else (f"<link rel='alternate' hreflang='fa' href='{url('fa',slug)}'><link rel='alternate' hreflang='en' href='{url('en',slug)}'>"
                              f"<link rel='alternate' hreflang='x-default' href='{url('fa',slug)}'>")
    canon_tag = "" if noindex else f"<link rel='canonical' href='{canon}'>"
    lang_href = ("/en/" if slug == "404" else href(other, slug))
    font_pre = "vazirmatn-400" if L == "fa" else "inter-400"

    return f"""<!doctype html>
<html lang="{L}" dir="{'rtl' if L=='fa' else 'ltr'}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>{e(full_title)}</title>
<meta name="description" content="{e(desc)}">{kw}
<meta name="robots" content="{robots}">
{canon_tag}
{alt}
<meta name="theme-color" content="#F4FBF7" media="(prefers-color-scheme: light)"><meta name="theme-color" content="#091413" media="(prefers-color-scheme: dark)">
<meta name="color-scheme" content="light dark">
<meta name="application-name" content="Cidna"><meta name="apple-mobile-web-app-title" content="Cidna">
<link rel="icon" href="/assets/img/cidna-mark.png" type="image/png"><link rel="apple-touch-icon" href="/assets/img/apple-touch-icon.png"><link rel="manifest" href="/manifest.webmanifest">
<meta property="og:type" content="{og_type}"><meta property="og:site_name" content="Cidna | سیدنا"><meta property="og:title" content="{e(full_title)}"><meta property="og:description" content="{e(desc)}">
<meta property="og:url" content="{canon}"><meta property="og:image" content="{og_img}"><meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:image:alt" content="{e(t('صفحهٔ اصلی اپلیکیشن سیدنا با تراز حساب، قلک‌های پس‌انداز و محدودیت‌های خرج','Cidna app home screen with balance, savings piggy banks and spending limits'))}">
<meta property="og:locale" content="{'fa_IR' if L=='fa' else 'en_US'}"><meta property="og:locale:alternate" content="{'en_US' if L=='fa' else 'fa_IR'}">{art}
<meta name="twitter:card" content="summary_large_image"><meta name="twitter:title" content="{e(full_title)}"><meta name="twitter:description" content="{e(desc)}"><meta name="twitter:image" content="{og_img}">
<link rel="preload" href="/assets/fonts/{font_pre}.woff2" as="font" type="font/woff2" crossorigin>
<script src="{asset('js/theme.js')}"></script>
<link rel="stylesheet" href="{asset('css/site.css')}">
<script type="application/ld+json">{ld}</script>
</head>
<body class="lang-{L}">
<div class="progress" aria-hidden="true"></div>
<a class="skip" href="#main">{t('رفتن به محتوا','Skip to content')}</a>
<header class="topbar"><div class="wrap bar">
  <a class="brand" href="{href(L,'')}" aria-label="{t('سیدنا — صفحهٔ اصلی','Cidna — home')}">{LOGO}<span>{brand}</span></a>
  <nav class="primary" aria-label="{t('منوی اصلی','Main')}">{nav}</nav>
  <div class="tools">
    <a class="pill" href="{lang_href}" hreflang="{other}" lang="{other}" aria-label="{t('English version','نسخهٔ فارسی')}">{'EN' if L=='fa' else 'فا'}</a>
    <button class="pill icon-btn" type="button" data-theme-toggle aria-label="{t('تغییر تم روشن/تاریک','Toggle light/dark theme')}"><span class="when-dark">{svg('sun',20)}</span><span class="when-light">{svg('moon',20)}</span></button>
    <a class="btn btn-primary btn-sm cta-top" href="{href(L,'download')}">{t('دریافت','Get the app')}</a>
  </div>
</div></header>
<main id="main">{body}</main>
<footer class="footer"><div class="wrap">
  <div class="foot-grid">
    <div class="foot-brand"><a class="brand" href="{href(L,'')}">{LOGO2}<span>{brand}</span></a>
      <p>{t('مدیریت مالی شخصی برای اندروید؛ رایگان، با رمزنگاری سرتاسری و آفلاین پس از ورود.','Personal finance for Android — free, end-to-end encrypted, and usable offline after sign-in.')}</p></div>
    <div><h3>{t('محصول','Product')}</h3><a href="{href(L,'features')}">{t('امکانات','Features')}</a><a href="{href(L,'download')}">{t('دریافت برنامه','Download')}</a><a href="{href(L,'guides')}">{t('آموزش‌ها','Guides')}</a><a href="{href(L,'faq')}">{t('سوالات متداول','FAQ')}</a></div>
    <div><h3>{t('اعتماد و حقوقی','Trust & legal')}</h3><a href="{href(L,'security')}">{t('امنیت','Security')}</a><a href="{href(L,'privacy')}">{t('حریم خصوصی','Privacy policy')}</a><a href="{href(L,'terms')}">{t('قوانین و مقررات','Terms of service')}</a><a href="{href(L,'licenses')}">{t('مجوزهای متن‌باز','Open-source licenses')}</a><a href="{href(L,'account-deletion')}">{t('حذف حساب','Delete account')}</a></div>
    <div><h3>{t('ارتباط با ما','Contact')}</h3>{''.join(f"<a href='{h}' rel='noopener'>{svg(i,16)}{n}</a>" for i,n,h in social_links(L))}<a href="{href(L,'about')}">{t('درباره سیدنا','About Cidna')}</a></div>
  </div>
  <p class="disclaimer">{t('سیدنا ابزار ثبت و مرور است؛ مشاورهٔ مالی، حسابداری، حقوقی یا مالیاتی نیست و به هیچ حساب بانکی وصل نمی‌شود.','Cidna is a record-keeping tool. It is not financial, accounting, legal or tax advice, and it does not connect to any bank account.')}</p>
  <div class="copyright"><span>© <span data-year>2026</span> Cidna · AGPL-3.0-or-later</span><span>{t('نام و لوگوی سیدنا: همهٔ حقوق برای سازنده محفوظ است. کد برنامه متن‌باز است.','Cidna name and logo: all rights reserved. The app’s code is open source.')}</span></div>
</div></footer>
<nav class="dock" aria-label="{t('ناوبری موبایل','Mobile navigation')}"><div class="dock-pill">{dock}</div><a class="fab" href="{href(L,'download')}" aria-label="{t('دریافت برنامه','Download the app')}">{svg('download',26)}</a></nav>
<script src="{asset('js/site.js')}" defer></script>
</body></html>"""
