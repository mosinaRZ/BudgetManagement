/* Liquid Glass behaviour (progressive enhancement; the CSS material works without it)
   1. light that follows the pointer across glass surfaces
   2. a glass "lens" that slides between nav items / dock tabs
   3. real edge refraction + chromatic dispersion via SVG displacement maps (Chromium only)
   CSP-safe: no inline styles/scripts, only CSSOM writes and a data: image (img-src data:). */
(function () {
  'use strict';
  var root = document.documentElement;
  var fine = matchMedia('(hover:hover) and (pointer:fine)').matches;
  var lessGlass = matchMedia('(prefers-reduced-transparency: reduce)').matches;

  /* ── 1 · light follows the pointer ─────────────────────────────────── */
  if (fine && !lessGlass) {
    document.querySelectorAll('.panel,.tile,.card,.band,.toc,.btn-ghost,.faq details,.linklist a,.journey li').forEach(function (el) {
      el.addEventListener('pointermove', function (e) {
        var r = el.getBoundingClientRect();
        el.style.setProperty('--mx', (e.clientX - r.left) + 'px');
        el.style.setProperty('--my', (e.clientY - r.top) + 'px');
      });
      el.addEventListener('pointerleave', function () { el.style.removeProperty('--mx'); el.style.removeProperty('--my'); });
    });
  }

  /* ── 2 · the glass lens ─────────────────────────────────────────────── */
  function lens(box, sel) {
    var items = Array.prototype.slice.call(box.querySelectorAll(sel));
    if (!items.length) return;
    var l = document.createElement('span');
    l.className = 'lens snap'; l.setAttribute('aria-hidden', 'true');
    box.insertBefore(l, box.firstChild);
    var cur = box.querySelector(sel + '[aria-current="page"]'), at = null;

    function put(a, snap) {
      items.forEach(function (x) { x.removeAttribute('data-lens'); });
      at = a;
      if (!a || !a.offsetWidth) { l.classList.remove('on'); return; }
      if (snap) l.classList.add('snap');
      a.setAttribute('data-lens', '');
      l.style.setProperty('--lx', a.offsetLeft + 'px'); l.style.setProperty('--ly', a.offsetTop + 'px');
      l.style.setProperty('--lw', a.offsetWidth + 'px'); l.style.setProperty('--lh', a.offsetHeight + 'px');
      l.classList.add('on');
      if (snap) requestAnimationFrame(function () { requestAnimationFrame(function () { l.classList.remove('snap'); }); });
    }
    items.forEach(function (a) {
      a.addEventListener('pointerenter', function () { put(a, false); });
      a.addEventListener('focus', function () { put(a, false); });
    });
    box.addEventListener('pointerleave', function () { put(cur, false); });
    box.addEventListener('focusout', function (e) { if (!box.contains(e.relatedTarget)) put(cur, false); });
    var rt; addEventListener('resize', function () { clearTimeout(rt); rt = setTimeout(function () { put(at || cur, true); }, 80); });
    put(cur, true);
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(function () { put(at || cur, true); });
    root.classList.add('lens-on');
  }
  var nav = document.querySelector('.primary'); if (nav) lens(nav, 'a');
  var dock = document.querySelector('.dock-pill'); if (dock) lens(dock, '.tab');

  /* ── 3 · real refraction (Chromium: backdrop-filter accepts SVG filters) ── */
  var ua = navigator.userAgent;
  var chromium = /\bChrome\/\d/.test(ua) && !/\b(CriOS|FxiOS)\b/.test(ua);
  var capable = chromium && !lessGlass && window.CSS && CSS.supports && CSS.supports('backdrop-filter', 'blur(1px)') &&
                !(navigator.deviceMemory && navigator.deviceMemory <= 2) && 'ResizeObserver' in window;
  if (!capable) return;

  var NS = 'http://www.w3.org/2000/svg', XL = 'http://www.w3.org/1999/xlink';
  function el(name, attrs) { var n = document.createElementNS(NS, name); for (var k in attrs) n.setAttribute(k, attrs[k]); return n; }
  var svg = el('svg', { 'class': 'lg-defs', width: '0', height: '0', 'aria-hidden': 'true', focusable: 'false' });
  var defs = el('defs', {}); svg.appendChild(defs); document.body.appendChild(svg);

  /* Displacement map for a rounded rectangle w×h (corner radius r).
     Within the bezel (b px from the edge) every pixel is pushed along the inward normal by an amount that
     falls off steeply, like light entering a convex lens: R = x-shift, G = y-shift, 128 = no shift. */
  var mapCache = {};
  function makeMap(w, h, r, b) {
    var key = w + 'x' + h + 'x' + r + 'x' + b;
    if (mapCache[key]) return mapCache[key];
    var c = document.createElement('canvas'); c.width = w; c.height = h;
    var ctx = c.getContext('2d'), img = ctx.createImageData(w, h), d = img.data;
    var hx = w / 2, hy = h / 2, ix = hx - r, iy = hy - r;
    for (var y = 0; y < h; y++) {
      for (var x = 0; x < w; x++) {
        var px = x + .5 - hx, py = y + .5 - hy;
        var qx = Math.abs(px) - ix, qy = Math.abs(py) - iy;
        var ox = Math.max(qx, 0), oy = Math.max(qy, 0), len = Math.sqrt(ox * ox + oy * oy);
        var dist = r - (len + Math.min(Math.max(qx, qy), 0));        // distance inside the edge (>0 = inside)
        var vx = 0, vy = 0;
        if (dist >= 0 && dist < b) {
          var gx, gy;                                                // outward normal
          if (qx > 0 && qy > 0) { gx = ox / len * (px < 0 ? -1 : 1); gy = oy / len * (py < 0 ? -1 : 1); }
          else if (qx > qy) { gx = px < 0 ? -1 : 1; gy = 0; }
          else { gx = 0; gy = py < 0 ? -1 : 1; }
          var m = Math.pow(1 - dist / b, 2.3);                       // steep at the rim, calm in the middle
          vx = -gx * m; vy = -gy * m;                                // pull light in from further inside
        }
        var i = (y * w + x) * 4;
        d[i] = 128 + 127 * vx; d[i + 1] = 128 + 127 * vy; d[i + 2] = 128; d[i + 3] = 255;
      }
    }
    ctx.putImageData(img, 0, 0);
    return (mapCache[key] = c.toDataURL('image/png'));
  }

  var MATRIX = {
    R: '1 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 1 0',
    G: '0 0 0 0 0  0 1 0 0 0  0 0 0 0 0  0 0 0 1 0',
    B: '0 0 0 0 0  0 0 0 0 0  0 0 1 0 0  0 0 0 1 0'
  };
  var DISPERSION = { R: 1, G: .94, B: .86 };      // each colour bends a little differently, like real glass

  function build(id, w, h, r, b, D, soft) {
    var old = defs.querySelector('#' + id); if (old) defs.removeChild(old);
    var f = el('filter', { id: id, x: '0', y: '0', width: w, height: h, filterUnits: 'userSpaceOnUse', 'color-interpolation-filters': 'sRGB' });
    var url = makeMap(w, h, r, b);
    var im = el('feImage', { x: '0', y: '0', width: w, height: h, preserveAspectRatio: 'none', result: 'map' });
    im.setAttribute('href', url); im.setAttributeNS(XL, 'xlink:href', url);
    f.appendChild(im);
    f.appendChild(el('feGaussianBlur', { 'in': 'SourceGraphic', stdDeviation: soft, result: 'soft' }));
    ['R', 'G', 'B'].forEach(function (c) {
      f.appendChild(el('feDisplacementMap', { 'in': 'soft', in2: 'map', scale: (2 * D * DISPERSION[c]).toFixed(2), xChannelSelector: 'R', yChannelSelector: 'G', result: 'd' + c }));
      f.appendChild(el('feColorMatrix', { 'in': 'd' + c, type: 'matrix', values: MATRIX[c], result: 'c' + c }));
    });
    f.appendChild(el('feBlend', { 'in': 'cR', in2: 'cG', mode: 'screen', result: 'rg' }));
    f.appendChild(el('feBlend', { 'in': 'rg', in2: 'cB', mode: 'screen' }));
    defs.appendChild(f);
  }

  var n = 0, pending = new Set(), scheduled = false;
  function refresh(t) {
    var w = t.offsetWidth, h = t.offsetHeight;
    if (w < 24 || h < 24) return;
    if (t.__lgW === w && t.__lgH === h) return;
    t.__lgW = w; t.__lgH = h;
    var rad = parseFloat(getComputedStyle(t).borderTopLeftRadius) || 0;
    var r = Math.min(rad, w / 2, h / 2);
    var b = Math.max(10, Math.min(26, Math.min(w, h) * .42));         // bezel width
    var big = Math.min(w, h) > 120;
    if (!t.__lgId) t.__lgId = 'lgf' + (++n);
    build(t.__lgId, w, h, r, b, b * (big ? .62 : .85), big ? 3 : 2);
    t.style.setProperty('--lg-ref', 'url(#' + t.__lgId + ')');
    t.classList.add('lg-ref'); root.classList.add('lg-svg');
  }
  function flush() {
    scheduled = false;
    var it = pending.values().next();
    if (it.done) return;
    pending.delete(it.value); refresh(it.value);
    if (pending.size) { scheduled = true; setTimeout(flush, 16); }   // one surface per tick keeps the main thread free
  }
  var ro = new ResizeObserver(function (es) {
    es.forEach(function (e) { pending.add(e.target); });
    if (!scheduled) { scheduled = true; setTimeout(flush, 0); }
  });
  document.querySelectorAll('.bar,.dock-pill,.tag,.band,.pill:not(.bar .pill),.btn-ghost').forEach(function (t) { ro.observe(t); });
})();
