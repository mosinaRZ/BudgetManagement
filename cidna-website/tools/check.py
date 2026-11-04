# -*- coding: utf-8 -*-
"""python3 tools/check.py  — اعتبارسنجی ساختاری خروجی dist/"""
import os, re, json, sys
from html.parser import HTMLParser
D = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dist")
class P(HTMLParser):
    def __init__(s): super().__init__(); s.links=[]; s.ids=[]; s.h1=0; s.ld=[]; s.inld=False; s.meta={}; s.title=""; s.int=False; s.stack=[]; s.alts=[]
    def handle_starttag(s,t,a):
        a=dict(a)
        if t=="h1": s.h1+=1
        if "id" in a: s.ids.append(a["id"])
        for k in ("href","src"):
            if k in a and t!="link" or (t=="link" and k=="href" and a.get("rel") in ("stylesheet","icon","manifest","apple-touch-icon","preload")): s.links.append(a[k])
        if t=="script" and a.get("type")=="application/ld+json": s.inld=True
        if t=="meta" and "name" in a: s.meta[a["name"]]=a.get("content","")
        if t=="title": s.int=True
        if t=="link" and a.get("rel")=="alternate" and "hreflang" in a: s.alts.append((a["hreflang"],a["href"]))
        if t=="img" and "alt" not in a: s.links.append("!!img-without-alt")
    def handle_endtag(s,t):
        if t=="script": s.inld=False
        if t=="title": s.int=False
    def handle_data(s,d):
        if s.inld: s.ld.append(d)
        if s.int: s.title+=d
err=[]; n=0
for root,_,fs in os.walk(D):
    for f in fs:
        if not f.endswith(".html"): continue
        n+=1; path=os.path.join(root,f); rel=os.path.relpath(path,D); s=open(path,encoding="utf8").read()
        p=P(); p.feed(s)
        if p.h1!=1: err.append(f"{rel}: h1 count {p.h1}")
        if len(p.ids)!=len(set(p.ids)): err.append(f"{rel}: duplicate ids")
        if rel!="404.html":
            d=p.meta.get("description","")
            if not 70<=len(d)<=200: err.append(f"{rel}: description length {len(d)}")
            if len(p.title)>75: err.append(f"{rel}: title length {len(p.title)}")
            if len(p.alts)!=3: err.append(f"{rel}: hreflang {len(p.alts)}")
        try: json.loads("".join(p.ld))
        except Exception as e: err.append(f"{rel}: bad JSON-LD {e}")
        for l in p.links:
            if l=="!!img-without-alt": err.append(f"{rel}: img w/o alt"); continue
            if l.startswith(("http","mailto:","#","data:")): continue
            if l.startswith("/downloads/"): continue        # فایل APK روی سرور است (/var/www/cidna/downloads)، نه داخل dist
            tgt=l.split("#")[0].split("?")[0]
            if not tgt: continue
            fp=os.path.join(D,tgt.lstrip("/"))
            if tgt.endswith("/"): fp=os.path.join(fp,"index.html")
            if not os.path.exists(fp): err.append(f"{rel}: broken {l}")
        for a in re.findall(r"href='#([^']+)'",s):
            if a not in p.ids: err.append(f"{rel}: anchor #{a} missing")
print(n,"html pages;", len(err),"problems")
for e in sorted(set(err)): print(" -",e)
sys.exit(1 if err else 0)
