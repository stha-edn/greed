// When plain htmx isn't quite enough, you can stick some custom JS here.

Number.prototype.comma_formatter = function() {
    return this.toString().replace(/\B(?<!\.\d*)(?=(\d{3})+(?!\d))/g, ",");
}

let chartData = function(){
    return {
        date: 'today',
        options: [
            {
                label: 'Today',
                value: 'today',
            },
            {
                label: 'Last 7 Days',
                value: '7days',
            },
            {
                label: 'Last 30 Days',
                value: '30days',
            },
            {
                label: 'Last 6 Months',
                value: '6months',
            },
            {
                label: 'This Year',
                value: 'year',
            },
        ],
        showDropdown: false,
        selectedOption: 0,
        selectOption: function(index){
            this.selectedOption = index;
            this.date = this.options[index].value;
            this.renderChart();
        },
        data: null,
        fetch: function(){
            fetch('https://cdn.jsdelivr.net/gh/swindon/fake-api@master/tailwindAlpineJsChartJsEx1.json')
                .then(res => res.json())
                .then(res => {
                    this.data = res.dates;
                    this.renderChart();
                })
        },
        renderChart: function(){
            let c = false;

            Chart.helpers.each(Chart.instances, function(instance) {
                if (instance.chart.canvas.id == 'chart') {
                    c = instance;
                }
            });

            if(c) {
                c.destroy();
            }

            let ctx = document.getElementById('chart').getContext('2d');

            let chart = new Chart(ctx, {
                type: "line",
                data: {
                    labels: this.data[this.date].data.labels,
                    datasets: [
                        {
                            label: "Income",
                            backgroundColor: "rgba(102, 126, 234, 0.25)",
                            borderColor: "rgba(102, 126, 234, 1)",
                            pointBackgroundColor: "rgba(102, 126, 234, 1)",
                            data: this.data[this.date].data.income,
                        },
                        {
                            label: "Expenses",
                            backgroundColor: "rgba(237, 100, 166, 0.25)",
                            borderColor: "rgba(237, 100, 166, 1)",
                            pointBackgroundColor: "rgba(237, 100, 166, 1)",
                            data: this.data[this.date].data.expenses,
                        },
                    ],
                },
                layout: {
                    padding: {
                        right: 10
                    }
                },
                options: {
                    scales: {
                        yAxes: [{
                            gridLines: {
                                display: false
                            },
                            ticks: {
                                callback: function(value,index,array) {
                                    return value > 1000 ? ((value < 1000000) ? value/1000 + 'K' : value/1000000 + 'M') : value;
                                }
                            }
                        }]
                    }
                }
            });
        }
    }
}

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
