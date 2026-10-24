(function () {
  var root = document.documentElement;
  var reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  var fine = matchMedia('(hover:hover) and (pointer:fine)').matches;
  clearTimeout(window.__rvT);

  /* ── theme (with circular view-transition reveal) ── */
  function apply(mode, save) {
    root.setAttribute('data-theme', mode);
    if (save) { try { localStorage.setItem('cidna-theme', mode); } catch (e) {} }
    document.querySelectorAll('meta[name="theme-color"]').forEach(function (m) { m.removeAttribute('media'); m.content = mode === 'dark' ? '#091413' : '#F4FBF7'; });
  }
  document.querySelectorAll('[data-theme-toggle]').forEach(function (b) {
    b.addEventListener('click', function () {
      var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      if (reduce || !document.startViewTransition) { apply(next, true); return; }
      var r = b.getBoundingClientRect(), x = r.left + r.width / 2, y = r.top + r.height / 2;
      var R = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y));
      root.classList.add('theme-vt');
      var vt = document.startViewTransition(function () { apply(next, true); });
      vt.ready.then(function () {
        root.animate({ clipPath: ['circle(0px at ' + x + 'px ' + y + 'px)', 'circle(' + R + 'px at ' + x + 'px ' + y + 'px)'] },
          { duration: 650, easing: 'cubic-bezier(.4,0,.2,1)', pseudoElement: '::view-transition-new(root)' });
      }).catch(function () {});
      vt.finished.finally(function () { root.classList.remove('theme-vt'); });
    });
  });
  try {
    matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
      if (!localStorage.getItem('cidna-theme')) apply(e.matches ? 'dark' : 'light', false);
    });
  } catch (e) {}
  document.querySelectorAll('[data-year]').forEach(function (n) { n.textContent = new Date().getFullYear(); });
  document.querySelectorAll('[data-copy]').forEach(function (b) {
    b.addEventListener('click', function () {
      var v = b.getAttribute('data-copy'), ok = b.getAttribute('data-ok') || 'OK';
      (navigator.clipboard ? navigator.clipboard.writeText(v) : Promise.reject()).then(function () {
        var old = b.textContent; b.textContent = ok; setTimeout(function () { b.textContent = old; }, 1600);
      }).catch(function () {});
    });
  });

  if (reduce) { root.classList.add('rv-off'); return; }

  /* ── hero headline: word-by-word rise (words only, so Persian letters stay joined) ── */
  var h1 = document.querySelector('.hero h1');
  if (h1) {
    var txt = h1.textContent.trim(); h1.setAttribute('aria-label', txt); h1.textContent = '';
    txt.split(/\s+/).forEach(function (w, i, a) {
      var o = document.createElement('span'); o.className = 'w'; o.setAttribute('aria-hidden', 'true');
      var s = document.createElement('span'); s.textContent = w; s.style.setProperty('--i', i); o.appendChild(s);
      h1.appendChild(o); if (i < a.length - 1) h1.appendChild(document.createTextNode(' '));
    });
  }

  /* ── scroll reveals with per-group stagger ── */
  var sel = 'main :is(.sec-head,.page-hero h1,.page-hero .lead,.prose,.toc,.faq details,.linklist li,.band,.panel,.tile,.card,.journey li), .footer .foot-grid>div';
  var els = Array.prototype.filter.call(document.querySelectorAll(sel), function (el) { return !el.closest('.hero'); });
  var counts = new Map();
  els.forEach(function (el) {
    var p = el.parentNode, n = counts.get(p) || 0; counts.set(p, n + 1);
    el.style.setProperty('--d', Math.min(n, 7) * 0.075 + 's');
  });
  var flows = document.querySelectorAll('.flow');
  if (!('IntersectionObserver' in window)) { root.classList.add('rv-off'); return; }
  var io = new IntersectionObserver(function (es) {
    es.forEach(function (e) {
      if (!e.isIntersecting) return;
      var el = e.target; io.unobserve(el);
      if (el.classList.contains('flow')) { el.classList.add('in'); scramble(el.querySelector('.cipher')); return; }
      el.classList.add('rv-in');
      setTimeout(function () { el.classList.add('rv-done'); el.classList.remove('rv-in'); el.style.removeProperty('--d'); }, 1700);
    });
  }, { threshold: 0, rootMargin: '0px 0px -8% 0px' });
  els.forEach(function (el) { io.observe(el); });
  flows.forEach(function (f) { io.observe(f); });

  /* cipher text decodes from noise to its final form */
  function scramble(el) {
    if (!el) return;
    var fin = el.getAttribute('data-final') || el.textContent; el.setAttribute('data-final', fin);
    var set = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/=', t0 = performance.now(), D = 1300;
    (function tick(now) {
      var k = Math.min(1, (now - t0 - 1400) / D), out = '';
      if (now - t0 < 1400) k = 0;
      for (var i = 0; i < fin.length; i++) out += (i < k * fin.length || fin[i] === '…') ? fin[i] : set[Math.random() * set.length | 0];
      el.textContent = out; if (k < 1) requestAnimationFrame(tick); else el.textContent = fin;
    })(t0);
  }

  /* ── scroll progress + aurora parallax (one rAF-throttled listener) ── */
  var bar = document.querySelector('.progress'), tick = false;
  function onScroll() {
    if (tick) return; tick = true;
    requestAnimationFrame(function () {
      var y = scrollY, h = root.scrollHeight - innerHeight;
      if (bar) bar.style.setProperty('--p', h > 0 ? Math.min(1, y / h) : 0);
      root.style.setProperty('--sy', y); tick = false;
    });
  }
  addEventListener('scroll', onScroll, { passive: true }); onScroll();

  /* ── cursor spotlight on glass cards + phone tilt (mouse devices only) ── */
  if (fine) {
    document.querySelectorAll('.tile,.card,.panel').forEach(function (c) {
      c.addEventListener('pointermove', function (e) {
        var r = c.getBoundingClientRect(); c.style.setProperty('--mx', (e.clientX - r.left) + 'px'); c.style.setProperty('--my', (e.clientY - r.top) + 'px');
      });
    });
    var st = document.querySelector('.stage'), ph = document.querySelector('.phone');
    if (st && ph) {
      st.addEventListener('pointermove', function (e) {
        var r = st.getBoundingClientRect(), px = (e.clientX - r.left) / r.width - .5, py = (e.clientY - r.top) / r.height - .5;
        ph.style.setProperty('--ry', (px * 12).toFixed(2) + 'deg'); ph.style.setProperty('--rx', (-py * 9).toFixed(2) + 'deg');
      });
      st.addEventListener('pointerleave', function () { ph.style.setProperty('--ry', '0deg'); ph.style.setProperty('--rx', '0deg'); });
    }
  }
})();
