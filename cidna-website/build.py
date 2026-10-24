# -*- coding: utf-8 -*-
"""python3 build.py  →  dist/  (فقط همین پوشه را روی هاست بگذارید)"""
import os, shutil, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import config as C
from engine import seo
import content.home as H, content.pages as P, content.guides as G, content.legal as LG

ROOT = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(ROOT, "dist")
shutil.rmtree(OUT, ignore_errors=True); os.makedirs(OUT)
def write(rel, s):
    p = os.path.join(OUT, rel); os.makedirs(os.path.dirname(p), exist_ok=True); open(p, "w", encoding="utf8").write(s)

REG = {"": (H.home, C.TODAY), "features": (P.features, C.TODAY), "download": (P.download, C.TODAY), "faq": (P.faq, C.TODAY),
       "about": (P.about, C.TODAY), "contact": (P.contact, C.TODAY), "security": (P.security, C.TODAY), "guides": (G.hub, C.TODAY),
       "privacy": (LG.privacy, C.LEGAL_UPDATED_ISO), "terms": (LG.terms, C.LEGAL_UPDATED_ISO),
       "licenses": (LG.licenses, C.LEGAL_UPDATED_ISO), "account-deletion": (LG.account_deletion, C.LEGAL_UPDATED_ISO)}
for g in G.GUIDES: REG["guides/" + g[0]] = ((lambda L, g=g: G.article(L, g)), C.TODAY)

for L in ("fa", "en"):
    for slug, (fn, _) in REG.items():
        write(("en/" if L == "en" else "") + (slug + "/" if slug else "") + "index.html", fn(L))
write("404.html", P.not_found())
write("sitemap.xml", seo.sitemap([(s, lm) for s, (_, lm) in REG.items()]))
write("robots.txt", seo.robots()); write("llms.txt", seo.llms(G.GUIDES)); write("manifest.webmanifest", seo.manifest())
write(".well-known/security.txt", seo.security_txt()); write("_headers", seo.headers()); write("_redirects", seo.redirects())
write("../deploy/nginx-cidna.conf", seo.nginx())
shutil.copytree(os.path.join(ROOT, "static"), os.path.join(OUT, "assets"))
print("pages:", len(REG) * 2 + 1, "→", OUT)
