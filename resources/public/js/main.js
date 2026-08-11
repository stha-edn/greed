// ---------------------------------------------------------------------------
// Styled confirm dialog. Replaces window.confirm() across the app without
// depending on Alpine. The dialog markup is the static confirm-dialog
// component (rendered once per app page); this module toggles the
// `data-open` attribute (display) and the `confirm-visible` class
// (opacity/scale transition). Usage:
//   GreedConfirm.ask('Delete this item?').then(ok => { if (ok) ... })
// Any submit button carrying a data-confirm attribute is intercepted by the
// delegated click listener below and its form is submitted only on accept.
// ---------------------------------------------------------------------------
(function () {
    'use strict';

    var dialog = null;
    var resolveFn = null;
    var lastFocused = null;
    var prevOverflow = null;
    var hideTimer = null;

    function id(name) {
        return dialog ? dialog.querySelector('[data-cf-' + name + ']') : null;
    }

    function hide() {
        if (!dialog) { return; }
        dialog.classList.remove('confirm-visible');
        hideTimer = setTimeout(function () { dialog.removeAttribute('data-open'); }, 180);
        if (prevOverflow !== null) { document.body.style.overflow = prevOverflow; prevOverflow = null; }
        if (lastFocused && lastFocused.focus) { lastFocused.focus(); }
        lastFocused = null;
    }

    function finish(ok) {
        if (resolveFn) { var r = resolveFn; resolveFn = null; r(ok); }
        hide();
    }

    function init() {
        if (dialog) { return; }
        dialog = document.getElementById('confirm-dialog');
        if (!dialog) { return; }
        var cancel = id('cancel');
        var accept = id('accept');
        var overlay = id('overlay');
        if (cancel) { cancel.addEventListener('click', function () { finish(false); }); }
        if (accept) { accept.addEventListener('click', function () { finish(true); }); }
        if (overlay) { overlay.addEventListener('click', function () { finish(false); }); }
        document.addEventListener('keydown', function (e) {
            if (dialog.hasAttribute('data-open') && e.key === 'Escape') { finish(false); }
        });
    }

    function show(message) {
        init();
        if (!dialog) { return Promise.resolve(false); }
        if (hideTimer) { clearTimeout(hideTimer); hideTimer = null; }
        if (resolveFn) { var r = resolveFn; resolveFn = null; r(false); }
        var msg = id('message');
        if (msg) { msg.textContent = message; }
        lastFocused = document.activeElement;
        prevOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        dialog.setAttribute('data-open', '');
        void dialog.offsetWidth;
        dialog.classList.add('confirm-visible');
        var cancel = id('cancel');
        if (cancel) { cancel.focus(); }
        return new Promise(function (resolve) { resolveFn = resolve; });
    }

    window.GreedConfirm = {
        ask: function (message) { return show(message); }
    };

    document.addEventListener('click', function (e) {
        var el = e.target && e.target.closest ? e.target.closest('[data-confirm]') : null;
        if (!el) { return; }
        e.preventDefault();
        var form = el.closest('form');
        var hx = window.htmx && form && (form.hasAttribute('hx-post') ||
                                        form.hasAttribute('hx-put') ||
                                        form.hasAttribute('hx-delete') ||
                                        form.hasAttribute('hx-get') ||
                                        form.hasAttribute('hx-patch'));
        show(el.getAttribute('data-confirm') || 'Are you sure?').then(function (ok) {
            if (ok && form) {
                if (hx) { htmx.trigger(form, 'submit'); }
                else { form.submit(); }
            }
        });
    });
})();

// ---------------------------------------------------------------------------
// Marketing-page scroll reveal (home/about/team). A one-shot fade-up: each
// `.reveal` element is observed until it first crosses the viewport, then
// unobserved — nothing repeats or loops on re-scroll. Below-the-fold content
// only; hero content renders visible immediately (no latency on the first
// thing a visitor sees). Degrades to "just show it" with no IntersectionObserver,
// or when the user has asked for reduced motion.
// ---------------------------------------------------------------------------
(function () {
    'use strict';

    function revealAllNow() {
        document.querySelectorAll('.reveal').forEach(function (el) { el.classList.add('is-visible'); });
    }

    var reducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reducedMotion || !('IntersectionObserver' in window)) {
        document.addEventListener('DOMContentLoaded', revealAllNow);
        return;
    }

    var observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.15, rootMargin: '0px 0px -60px 0px' });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.reveal').forEach(function (el) { observer.observe(el); });
    });
})();
