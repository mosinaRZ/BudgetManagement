# -*- coding: utf-8 -*-
import json
import config as C
from engine.ui import url, store_names

def sitemap(pages):
    out = ['<?xml version="1.0" encoding="UTF-8"?>', '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">']
    for slug, lastmod in pages:
        alts = "".join(f'<xhtml:link rel="alternate" hreflang="{l}" href="{url(l,slug)}"/>' for l in ("fa", "en")) + f'<xhtml:link rel="alternate" hreflang="x-default" href="{url("fa",slug)}"/>'
        for L in ("fa", "en"):
            out.append(f"<url><loc>{url(L,slug)}</loc>{alts}<lastmod>{lastmod}</lastmod></url>")
    out.append("</urlset>")
    return "\n".join(out) + "\n"

def robots():
    return f"User-agent: *\nAllow: /\nDisallow: /404.html\n\nSitemap: {C.DOMAIN}/sitemap.xml\n"

def llms(guides):
    g = "\n".join(f"- [{x[2]}]({url('en','guides/'+x[0])}): {x[4]}" for x in guides)
    return f"""# Cidna (سیدنا)
> Free Android personal finance, budgeting and savings app for Persian and English speakers. End-to-end encrypted financial records, usable offline after sign-in, Jalali & Gregorian calendars, Toman/Rial, no ads or trackers. Open source (AGPL-3.0-or-later).

## Key pages
- [Home]({url('en','')}): overview
- [Features]({url('en','features')})
- [Download]({url('en','download')}): {store_names('en')}
- [Security model & honest limits]({url('en','security')})
- [Privacy policy]({url('en','privacy')})
- [Terms]({url('en','terms')})
- [FAQ]({url('en','faq')})
- [Persian site]({url('fa','')})

## Guides
{g}
"""

def manifest():
    return json.dumps({"name":"Cidna | سیدنا","short_name":"Cidna","lang":"fa","dir":"rtl","start_url":"/","display":"browser",
        "background_color":"#091413","theme_color":"#408A71",
        "icons":[{"src":"/assets/img/apple-touch-icon.png","sizes":"512x512","type":"image/png","purpose":"any"}]}, ensure_ascii=False, indent=2)

def security_txt():
    return f"Contact: mailto:{C.EMAIL}\nExpires: 2027-10-24T00:00:00.000Z\nPreferred-Languages: fa, en\nCanonical: {C.DOMAIN}/.well-known/security.txt\nPolicy: {url('en','security')}\n"

CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"

def redirects():
    """مسیرهای قدیمی/اپ → صفحه‌های وب‌سایت (Netlify / Cloudflare Pages)"""
    return "\n".join(["/privacy-policy /privacy/ 301", "/privacy-policy/ /privacy/ 301", "/en/privacy-policy /en/privacy/ 301", "/en/privacy-policy/ /en/privacy/ 301"]) + "\n"

def headers():
    return f"/*\n  Content-Security-Policy: {CSP}\n  X-Content-Type-Options: nosniff\n  X-Frame-Options: DENY\n  Referrer-Policy: strict-origin-when-cross-origin\n  Permissions-Policy: camera=(), microphone=(), geolocation=(), interest-cohort=()\n  Strict-Transport-Security: max-age=31536000; includeSubDomains\n/assets/*\n  Cache-Control: public, max-age=31536000, immutable\n"

def nginx():
    host = C.DOMAIN.replace("https://", "")
    return f"""# Cidna static site — nginx.  HTTPS: sudo certbot --nginx -d {host} -d www.{host}
server {{
    listen 80; listen [::]:80;
    server_name {host} www.{host};
    location /.well-known/acme-challenge/ {{ root /var/www/cidna; }}
    location / {{ return 301 https://{host}$request_uri; }}
}}
server {{
    listen 443 ssl; listen [::]:443 ssl; http2 on;
    server_name www.{host};
    # ssl_certificate / ssl_certificate_key: added by Certbot
    return 301 https://{host}$request_uri;
}}
server {{
    listen 443 ssl; listen [::]:443 ssl; http2 on;
    server_name {host};
    # ssl_certificate / ssl_certificate_key: added by Certbot
    root /var/www/cidna;
    index index.html;
    charset utf-8;
    server_tokens off;

    gzip on; gzip_vary on; gzip_comp_level 6; gzip_min_length 512;
    gzip_types text/css application/javascript application/json application/xml image/svg+xml application/manifest+json text/plain;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), interest-cohort=()" always;
    add_header Content-Security-Policy "{CSP}" always;

    error_page 404 /404.html;
    location = /404.html {{ internal; }}

    # لینک‌های داخل اپ (BASE_URL + "privacy-policy" ...) → صفحهٔ وب‌سایت
    location = /privacy-policy    {{ return 301 /privacy/; }}
    location = /privacy-policy/   {{ return 301 /privacy/; }}
    location = /en/privacy-policy {{ return 301 /en/privacy/; }}

    # /index.html → آدرس تمیز، و افزودن / انتهایی (جلوگیری از محتوای تکراری)
    location ~ ^(.*)/index\\.html$ {{ return 301 $1/; }}
    rewrite ^([^.?]*[^/])$ $1/ permanent;

    location / {{ try_files $uri $uri/index.html =404; add_header Cache-Control "no-cache"; include /etc/nginx/snippets/cidna-sec.conf; }}
    location /assets/ {{
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
        add_header X-Content-Type-Options nosniff always;
        add_header Access-Control-Allow-Origin "*";
        try_files $uri =404;
    }}
    location = /sitemap.xml {{ add_header Cache-Control "public, max-age=3600"; }}
    location = /robots.txt  {{ add_header Cache-Control "public, max-age=3600"; }}
    location ~ /\\.(?!well-known) {{ deny all; }}
}}
""".replace("include /etc/nginx/snippets/cidna-sec.conf; ", "")
