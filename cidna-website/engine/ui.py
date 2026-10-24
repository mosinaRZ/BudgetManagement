# -*- coding: utf-8 -*-
import html, json
import config as C

FA_DIGITS = str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹")
def fad(s): return str(s).translate(FA_DIGITS)
def e(s): return html.escape(str(s), quote=True)
def T(L): return (lambda fa, en: fa if L == "fa" else en)

def href(L, slug=""):
    return ("/en" if L == "en" else "") + (("/" + slug + "/") if slug else "/")
def url(L, slug=""):
    return C.DOMAIN + href(L, slug)

ICONS = {
 "home":"<path d='M3 11l9-8 9 8'/><path d='M5 10v10h5v-6h4v6h5V10'/>",
 "wallet":"<path d='M3 7a2 2 0 0 1 2-2h12v4'/><path d='M3 7v11a2 2 0 0 0 2 2h14a1 1 0 0 0 1-1v-3'/><path d='M21 9H16a2 2 0 0 0 0 4h5z'/>",
 "tag":"<path d='M20 13l-7 7-9-9V4h7z'/><circle cx='8' cy='8' r='1.4'/>",
 "pie":"<path d='M21 12A9 9 0 1 1 12 3v9z'/><path d='M15 3.5A9 9 0 0 1 20.5 9H15z'/>",
 "target":"<circle cx='12' cy='12' r='9'/><circle cx='12' cy='12' r='5'/><circle cx='12' cy='12' r='1.3'/>",
 "swap":"<path d='M7 4L3 8l4 4'/><path d='M3 8h14'/><path d='M17 20l4-4-4-4'/><path d='M21 16H7'/>",
 "chart":"<path d='M4 20V10'/><path d='M10 20V4'/><path d='M16 20v-7'/><path d='M22 20H2'/>",
 "file":"<path d='M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z'/><path d='M14 3v5h5'/><path d='M9 13h6M9 17h4'/>",
 "share":"<circle cx='18' cy='5' r='2.5'/><circle cx='6' cy='12' r='2.5'/><circle cx='18' cy='19' r='2.5'/><path d='M8.2 10.8l7.6-4.4M8.2 13.2l7.6 4.4'/>",
 "lock":"<rect x='4' y='11' width='16' height='10' rx='2.5'/><path d='M8 11V8a4 4 0 0 1 8 0v3'/><circle cx='12' cy='16' r='1.2'/>",
 "finger":"<path d='M12 11v3a5 5 0 0 1-1.5 3.5'/><path d='M8 11a4 4 0 0 1 8 0v2.5'/><path d='M5 12a7 7 0 0 1 14 0v1'/><path d='M8 15.5a9 9 0 0 1-.5 3'/><path d='M16 14.5c0 2.5-.5 4.2-1.5 5.8'/>",
 "mic":"<rect x='9' y='3' width='6' height='11' rx='3'/><path d='M5 11a7 7 0 0 0 14 0M12 18v3'/>",
 "bell":"<path d='M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7'/><path d='M10 20a2 2 0 0 0 4 0'/>",
 "cal":"<rect x='3.5' y='5' width='17' height='15.5' rx='2.5'/><path d='M3.5 10h17M8 3v4M16 3v4'/>",
 "lang":"<path d='M4 6h9M8.5 4v2M6 6c.5 3.5 3 6 6 7M12 6c-.5 3-3 6-7 8'/><path d='M13 20l4-9 4 9M14.5 17h5'/>",
 "phone":"<rect x='6.5' y='2.5' width='11' height='19' rx='2.5'/><path d='M10.5 18.5h3'/>",
 "device":"<rect x='3' y='5' width='13' height='10' rx='1.5'/><rect x='14' y='9' width='7' height='11' rx='1.5'/>",
 "user":"<circle cx='12' cy='8' r='4'/><path d='M4 21a8 8 0 0 1 16 0'/>",
 "sms":"<path d='M4 5h16v11H9l-5 4z'/><path d='M8 9.5h8M8 12.5h5'/>",
 "shield":"<path d='M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z'/><path d='M8.5 12l2.5 2.5L16 9.5'/>",
 "eye":"<path d='M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z'/><circle cx='12' cy='12' r='3'/>",
 "eye-off":"<path d='M3 3l18 18'/><path d='M10.6 6.2A9.8 9.8 0 0 1 12 6c6.4 0 10 6 10 6a17 17 0 0 1-3.2 3.8M6.5 7.6C3.9 9.3 2 12 2 12s3.6 7 10 7c1.6 0 3-.4 4.3-1'/><path d='M9.9 10a3 3 0 0 0 4.1 4.2'/>",
 "key":"<circle cx='8' cy='15' r='4'/><path d='M11 12l9-9M17 6l3 3M14 9l2 2'/>",
 "code":"<path d='M8 7l-5 5 5 5M16 7l5 5-5 5M14 4l-4 16'/>",
 "mail":"<rect x='3' y='5' width='18' height='14' rx='2.5'/><path d='M3.5 7l8.5 6 8.5-6'/>",
 "book":"<path d='M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z'/><path d='M4 19V5M9 8h6'/>",
 "download":"<path d='M12 3v12M7 11l5 5 5-5M4 20h16'/>",
 "offline":"<path d='M2 8.8a15 15 0 0 1 20 0M5.5 12.5a10 10 0 0 1 13 0M9 16a5 5 0 0 1 6 0'/><path d='M3 3l18 18'/>",
 "trash":"<path d='M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6'/>",
 "gift":"<rect x='3' y='8' width='18' height='4' rx='1'/><path d='M12 8v13M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8'/><path d='M12 8C12 8 10.5 3 8 3.5S6.5 8 12 8zm0 0c0 0 1.5-5 4-4.5S17.5 8 12 8z'/>",
 "bag":"<path d='M5 8h14l-1 12H6z'/><path d='M9 8V6a3 3 0 0 1 6 0v2'/>",
 "package":"<path d='M3 8l9-5 9 5v8l-9 5-9-5z'/><path d='M3 8l9 5 9-5M12 13v8'/>",
 "chat":"<path d='M4 5h16v11H10l-5 4v-4H4z'/>",
 "whatsapp":"<path d='M3.5 20.5l1.2-4.1A8.5 8.5 0 1 1 8 19.4z'/><path d='M9 8.8c.2 2.8 2.6 5.3 5.5 5.9l1.3-1.3-1.9-1-.9.7c-.9-.4-1.7-1.200-2.1-2.100l.7-.9-1-1.900z'/>",
 "x":"<path d='M4 4l16 16M20 4L4 20'/>",
 "instagram":"<rect x='3.5' y='3.5' width='17' height='17' rx='5'/><circle cx='12' cy='12' r='4'/><circle cx='17' cy='7' r='.8'/>",
 "check":"<path d='M5 12.5l4.5 4.5L19 7.500'/>",
 "plus":"<path d='M12 5v14M5 12h14'/>",
 "info":"<circle cx='12' cy='12' r='9'/><path d='M12 11v6M12 7.500v.1'/>",
 "settings":"<circle cx='12' cy='12' r='3'/><path d='M12 2.500v3M12 18.500v3M2.500 12h3M18.500 12h3M5.300 5.300l2.100 2.100M16.600 16.600l2.100 2.100M5.300 18.700l2.100-2.100M16.600 7.400l2.100-2.100'/>",
 "cloud":"<path d='M7 18a4.500 4.500 0 0 1-.6-8.960A6 6 0 0 1 18 10.500 3.800 3.800 0 0 1 17.500 18z'/>",
 "sun":"<circle cx='12' cy='12' r='4'/><path d='M12 2v2M12 20v2M4.900 4.900l1.400 1.400M17.700 17.700l1.400 1.400M2 12h2M20 12h2M4.900 19.100l1.400-1.400M17.700 6.300l1.400-1.400'/>",
 "moon":"<path d='M20 14.500A8 8 0 0 1 9.500 4 8 8 0 1 0 20 14.500z'/>",
 "scale":"<path d='M12 3v18M5 21h14M4 7h16'/><path d='M4 7l-2.500 6a3.500 3.500 0 0 0 5 0zM20 7l-2.500 6a3.500 3.500 0 0 0 5 0z'/>",
}
def ico(name, cls=""):
    return f"<span class='ico {cls}' aria-hidden='true'><svg viewBox='0 0 24 24'>{ICONS[name]}</svg></span>"
def svg(name, size=20):
    return f"<svg class='i' width='{size}' height='{size}' viewBox='0 0 24 24' aria-hidden='true' focusable='false'>{ICONS[name]}</svg>"

LOGO_T = ("<svg class='logo' viewBox='0 0 512 512' aria-hidden='true' focusable='false'><defs><linearGradient id='lg' x1='0' y1='1' x2='1' y2='0'>"
        "<stop offset='0' stop-color='#2f6d58'/><stop offset='1' stop-color='#52a98a'/></linearGradient></defs>"
        "<path fill='url(#lg)' d='M36 370C36 322 74 285 122 285h31v108c0 48-38 86-86 86H36zM196 240c0-45 36-80 80-80h33v236c0 46-38 84-84 84h-29zM353 118c0-47 38-84 84-84h31v285c0 44-36 79-80 79h-35z'/></svg>")

# ── مؤلفه‌های مشترک ────────────────────────────────────────────────────
def store_buttons(L, compact=False):
    """سه دکمهٔ دریافت. اگر url خالی باشد 'به‌زودی' است."""
    t = T(L)
    out = []
    icons = {"myket":"bag", "bazaar":"bag", "direct":"package"}
    for s in C.STORES:
        label = t(("دریافت از " + s["fa"]) if s["id"] != "direct" else "دریافت فایل مستقیم",
                  ("Get it on " + s["en"]) if s["id"] != "direct" else "Direct download (APK)")
        inner = f"{svg(icons[s['id']], 22)}<span class='st-txt'><b>{label}</b>"
        if s["url"]:
            inner += f"<small>{t('نسخهٔ','Version')} {fad(C.APP_VERSION) if L=='fa' else C.APP_VERSION}</small></span>"
            out.append(f"<a class='store' data-store='{s['id']}' href='{e(s['url'])}' rel='noopener'>{inner}</a>")
        else:
            inner += f"<small>{t('به‌زودی','Coming soon')}</small></span>"
            out.append(f"<span class='store soon' data-store='{s['id']}' role='link' aria-disabled='true'>{inner}</span>")
    return f"<div class='stores{' compact' if compact else ''}'>{''.join(out)}</div>"

def faq_html(items):
    return "<div class='faq'>" + "".join(f"<details><summary>{q}</summary><div class='ans'>{a}</div></details>" for q, a, *_ in items) + "</div>"

def faq_schema(items):
    import re
    strip = lambda s: re.sub(r"<[^>]+>", "", s)
    return {"@type":"FAQPage","mainEntity":[{"@type":"Question","name":strip(q),"acceptedAnswer":{"@type":"Answer","text":strip(a)}} for q, a, *_ in items]}

def section(inner, cls="", id=None):
    return f"<section class='sec {cls}'{f' id={chr(34)}{id}{chr(34)}' if id else ''}><div class='wrap'>{inner}</div></section>"

def head(h, lead=""):
    return f"<header class='sec-head'><h2>{h}</h2>{f'<p class=lead>{lead}</p>' if lead else ''}</header>"

LOGO = LOGO_T
LOGO2 = LOGO_T.replace('lg','lg2')
