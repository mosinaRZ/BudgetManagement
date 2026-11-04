/* اجرای هم‌زمان در <head>: تم بدون چشمک + فعال‌سازی حالت JS */
(function () {
  var r = document.documentElement;
  r.classList.add('js');
  try {
    var s = localStorage.getItem('cidna-theme');
    var dark = s ? s === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches;
    r.setAttribute('data-theme', dark ? 'dark' : 'light');
  } catch (e) {}
  /* اگر site.js نرسید، محتوا پنهان نماند */
  window.__rvT = setTimeout(function () { r.classList.add('rv-off'); }, 3500);
})();
