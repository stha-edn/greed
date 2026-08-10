// ---------------------------------------------------------------------------
// Tax Overview charts (dashboard). Reads values from canvas data-* attributes
// so there is no inline JS in the server-rendered HTML. Chart.js is loaded
// after this file, but is available by the time DOMContentLoaded fires.
// ---------------------------------------------------------------------------
function fmtRand(v) {
    return 'R' + Math.round(v).toLocaleString('en-ZA');
}

function initTaxCharts() {
    if (typeof Chart === 'undefined') { return; }

    var split = document.getElementById('incomeSplitChart');
    if (split && !split.dataset.rendered) {
        split.dataset.rendered = '1';
        var ni = parseFloat(split.dataset.netIncome) || 0;
        var nt = parseFloat(split.dataset.netTax) || 0;
        var eff = split.dataset.effective || '';
        new Chart(split, {
            type: 'doughnut',
            data: {
                labels: ['Take-home', 'Tax'],
                datasets: [{ data: [ni, nt], backgroundColor: ['#10b981', '#e4e4e7'], borderWidth: 0, hoverOffset: 6 }]
            },
            options: {
                responsive: true, maintainAspectRatio: false, cutout: '72%', layout: { padding: 6 },
                plugins: {
                    legend: { position: 'bottom', labels: { boxWidth: 8, usePointStyle: true, pointStyle: 'circle', color: '#71717a', font: { size: 12 }, padding: 16 } },
                    tooltip: { callbacks: { label: function(ctx) { return ' ' + ctx.label + ': ' + fmtRand(ctx.parsed); } } }
                }
            },
            plugins: [{
                id: 'centerText',
                afterDraw: function(ch) {
                    var m = ch.getDatasetMeta(0);
                    if (!m.data.length) { return; }
                    var e = m.data[0], g = ch.ctx;
                    g.save();
                    g.textAlign = 'center'; g.textBaseline = 'middle';
                    g.fillStyle = '#18181b'; g.font = '600 20px Inter, sans-serif';
                    g.fillText(eff, e.x, e.y - 6);
                    g.fillStyle = '#a1a1aa'; g.font = '500 11px Inter, sans-serif';
                    g.fillText('effective rate', e.x, e.y + 13);
                    g.restore();
                }
            }]
        });
    }

    var bd = document.getElementById('taxBreakdownChart');
    if (bd && !bd.dataset.rendered) {
        bd.dataset.rendered = '1';
        var gt = parseFloat(bd.dataset.grossTax) || 0;
        var rb = parseFloat(bd.dataset.rebates) || 0;
        var ntx = parseFloat(bd.dataset.netTax) || 0;
        new Chart(bd, {
            type: 'bar',
            data: {
                labels: ['Gross tax', 'Rebates', 'Net tax'],
                datasets: [{ data: [gt, rb, ntx], backgroundColor: ['#a1a1aa', '#10b981', '#18181b'], borderRadius: 6, maxBarThickness: 56 }]
            },
            options: {
                responsive: true, maintainAspectRatio: false, layout: { padding: { top: 6 } },
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: function(ctx) { return ' ' + fmtRand(ctx.parsed.y); } } }
                },
                scales: {
                    y: { beginAtZero: true, grid: { color: '#f4f4f5' }, border: { display: false }, ticks: { color: '#a1a1aa', font: { size: 11 }, callback: function(v) { return 'R' + Math.round(v / 1000) + 'k'; } } },
                    x: { grid: { display: false }, border: { display: false }, ticks: { color: '#71717a', font: { size: 12 } } }
                }
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', initTaxCharts);
document.addEventListener('htmx:afterSwap', initTaxCharts);

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
