# -*- coding: utf-8 -*-
"""Run when the screenshot or fonts change:
    python3 tools/prepare_assets.py <app-screenshot.jpg> <fonts-dir-with-ttf>
Produces static/img/app-home*.webp, og-cover-fa.png, og-cover-en.png and static/fonts/*.woff2"""
import sys, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from fontTools.ttLib import TTFont
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
shot, fonts = sys.argv[1], sys.argv[2]

for src, dst in [("vazirmatn_regular","vazirmatn-400"),("vazirmatn_semibold","vazirmatn-600"),("vazirmatn_bold","vazirmatn-700"),
                 ("inter_18pt_regular","inter-400"),("inter_18pt_semibold","inter-600"),("inter_18pt_bold","inter-700")]:
    f = TTFont(os.path.join(fonts, src + ".ttf")); f.flavor = "woff2"
    f.save(os.path.join(ROOT, "static/fonts", dst + ".woff2"))

im = Image.open(shot).convert("RGB")
im.save(os.path.join(ROOT, "static/img/app-home.webp"), "WEBP", quality=84, method=6)
im.resize((314, 640), Image.LANCZOS).save(os.path.join(ROOT, "static/img/app-home-sm.webp"), "WEBP", quality=84, method=6)

def cover(lang):
    W, H = 1200, 630
    bg = Image.new("RGB", (W, H), (9, 20, 19))
    glow = Image.new("RGB", (W, H), (9, 20, 19)); d = ImageDraw.Draw(glow)
    d.ellipse((-200, -300, 900, 500), fill=(48, 110, 88)); d.ellipse((700, 150, 1400, 800), fill=(32, 78, 62))
    glow = glow.filter(ImageFilter.GaussianBlur(140)); bg = Image.blend(bg, glow, .9)
    ph = im.resize((300, int(300 * 1280 / 628)), Image.LANCZOS)
    mask = Image.new("L", ph.size, 0); ImageDraw.Draw(mask).rounded_rectangle((0, 0, ph.size[0], ph.size[1]), 36, fill=255)
    frame = Image.new("RGBA", (ph.size[0] + 16, ph.size[1] + 16), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, frame.size[0] - 1, frame.size[1] - 1), 44, fill=(30, 44, 41, 255), outline=(176, 228, 204, 90), width=2)
    rtl = lang == "fa"
    px = 70 if rtl else W - 70 - frame.size[0]
    py = 70
    bg.paste(frame, (px, py), frame); bg.paste(ph, (px + 8, py + 8), mask)
    d = ImageDraw.Draw(bg)
    fam = "vazirmatn" if rtl else "inter"
    L = ImageFont.Layout.RAQM
    big = ImageFont.truetype(os.path.join(fonts, fam + ("_bold" if rtl else "_18pt_bold") + ".ttf"), 112 if rtl else 96, layout_engine=L)
    mid = ImageFont.truetype(os.path.join(fonts, fam + ("_semibold" if rtl else "_18pt_semibold") + ".ttf"), 40 if rtl else 36, layout_engine=L)
    sm = ImageFont.truetype(os.path.join(fonts, fam + ("_regular" if rtl else "_18pt_regular") + ".ttf"), 30 if rtl else 28, layout_engine=L)
    x0, x1 = (480, W - 70) if rtl else (70, W - 480)
    def put(txt, font, y, fill):
        kw = dict(direction="rtl", language="fa") if rtl else dict(language="en")
        w = d.textlength(txt, font=font, **kw)
        d.text((x1 - w if rtl else x0, y), txt, font=font, fill=fill, **kw)
    if rtl:
        put("سیدنا", big, 130, (176, 228, 204)); put("پولتان، بالاخره آرام و روشن.", mid, 290, (225, 227, 224))
        put("رایگان · رمزنگاری سرتاسری · آفلاین پس از ورود", sm, 370, (191, 201, 195)); put("مدیریت مالی شخصی برای اندروید", sm, 440, (137, 147, 142))
    else:
        put("Cidna", big, 120, (176, 228, 204)); put("Your money, finally calm.", mid, 280, (225, 227, 224))
        put("Free · End-to-end encrypted · Offline after sign-in", sm, 350, (191, 201, 195)); put("Personal finance for Android", sm, 410, (137, 147, 142))
    bg.save(os.path.join(ROOT, f"static/img/og-cover-{lang}.png"), optimize=True)
cover("fa"); cover("en")
print("assets ok")
