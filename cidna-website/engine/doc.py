# -*- coding: utf-8 -*-
import config as C
from engine.ui import *
from engine.layout import page

def doc(L, slug, title, desc, intro, sections, crumbs, schema=None, meta=None, og_type="website", published=None,
        note="", after="", noindex=False, keywords=None, nav_slug=None):
    """sections: [(id, heading, html)], crumbs: [(label, slug)] (بعد از «خانه»)"""
    t = T(L)
    toc = "".join(f"<a href='#{i}'>{h}</a>" for i, h, _ in sections)
    secs = "".join(f"<h2 id='{i}'>{h}</h2>{b}" for i, h, b in sections)
    m = f"<p class='meta'>{meta}</p>" if meta else ""
    body = f"""<section class='page-hero'><div class='wrap'>{{{{CRUMBS}}}}<h1>{title}</h1><p class='lead'>{intro}</p>{m}</div></section>
<div class='wrap doc'><aside class='toc glass' aria-label='{t('فهرست مطالب','Table of contents')}'><b>{t('در این صفحه','On this page')}</b>{toc}</aside>
<article class='prose'>{note}{secs}</article></div>{after}"""
    cr = [(t("خانه", "Home"), url(L, ""))] + [(n, url(L, s)) for n, s in crumbs]
    return page(L, slug, title, desc, body, schema, crumbs=cr, og_type=og_type, published=published, noindex=noindex, keywords=keywords, nav_slug=nav_slug)

def simple_page(L, slug, title, desc, intro, inner, crumbs=None, schema=None, keywords=None, nav_slug=None):
    t = T(L)
    cr = [(t("خانه", "Home"), url(L, ""))] + [(n, url(L, s)) for n, s in (crumbs or [(title, slug)])]
    body = f"""<section class='page-hero'><div class='wrap'>{{{{CRUMBS}}}}<h1>{title}</h1>{f'<p class=lead>{intro}</p>' if intro else ''}</div></section>{inner}"""
    return page(L, slug, title, desc, body, schema, crumbs=cr, keywords=keywords, nav_slug=nav_slug)
