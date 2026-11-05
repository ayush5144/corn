// Shared helper for Corn's QA pages (scripts/qa-run.sh).
// Every page is opened with ?r=<run id>. Elements with data-n get an href (or a URL in
// data-url) of the form hit.html?n=<name>&r=<run id>, so each request in the server log is
// unique to one check of one run.
(function () {
  var params = new URLSearchParams(location.search);
  var run = params.get('r') || 'none';
  window.QA = {
    run: run,
    param: function (k) { return params.get(k); },
    hit: function (name) { return location.origin + '/hit.html?n=' + encodeURIComponent(name) + '&r=' + encodeURIComponent(run); },
    log: [],
    taps: [],  // ids of elements that received a click: proves an adb tap landed where intended
    note: function (msg) { QA.log.push(msg); var el = document.getElementById('status'); if (el) el.textContent = QA.log.join(' · '); }
  };
  document.addEventListener('click', function (e) {
    var el = e.target.closest ? e.target.closest('[id]') : null;
    QA.taps.push(el ? el.id : '');
  }, true);
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('a[data-n]').forEach(function (a) { a.href = QA.hit(a.dataset.n); });
  });
})();
