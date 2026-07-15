(function () {
	var charts = [];

	function parseCsv(value) {
		if (!value) {
			return [];
		}
		return value.split(",").map(function (item) {
			return item.trim();
		});
	}

	function formatDayLabel(value) {
		if (!value) {
			return "";
		}
		var parts = value.split("-");
		if (parts.length !== 3) {
			return value;
		}
		return parts[1] + "/" + parts[2];
	}

	function cssVar(name) {
		return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
	}

	function chartColors() {
		return [
			cssVar("--chart-1"),
			cssVar("--chart-2"),
			cssVar("--chart-3"),
			cssVar("--chart-4"),
			cssVar("--chart-5")
		];
	}

	function scaleOptions() {
		return {
			y: {
				beginAtZero: true,
				ticks: {
					precision: 0,
					color: cssVar("--chart-tick")
				},
				grid: {
					color: cssVar("--chart-grid")
				}
			},
			x: {
				ticks: {
					color: cssVar("--chart-tick")
				},
				grid: {
					color: cssVar("--chart-grid")
				}
			}
		};
	}

	function createLineChart(canvas, color) {
		var labels = parseCsv(canvas.dataset.labels).map(formatDayLabel);
		var values = parseCsv(canvas.dataset.values).map(function (value) {
			return Number(value);
		});

		return new Chart(canvas, {
			type: "line",
			data: {
				labels: labels,
				datasets: [{
					label: canvas.closest(".chart-card").querySelector("h2").textContent,
					data: values,
					borderColor: color,
					backgroundColor: color + "22",
					fill: true,
					tension: 0.3,
					pointRadius: 2
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: { display: false }
				},
				scales: scaleOptions()
			}
		});
	}

	function createDoughnutChart(canvas, colors) {
		var labels = parseCsv(canvas.dataset.labels);
		var values = parseCsv(canvas.dataset.values).map(function (value) {
			return Number(value);
		});

		return new Chart(canvas, {
			type: "doughnut",
			data: {
				labels: labels,
				datasets: [{
					data: values,
					backgroundColor: colors,
					borderWidth: 0
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: {
						position: "bottom",
						labels: {
							color: cssVar("--chart-tick")
						}
					}
				}
			}
		});
	}

	function createBarChart(canvas, colors) {
		var labels = parseCsv(canvas.dataset.labels);
		var values = parseCsv(canvas.dataset.values).map(function (value) {
			return Number(value);
		});

		return new Chart(canvas, {
			type: "bar",
			data: {
				labels: labels,
				datasets: [{
					data: values,
					backgroundColor: colors,
					borderRadius: 6
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: { display: false }
				},
				scales: scaleOptions()
			}
		});
	}

	function initCharts() {
		charts.forEach(function (chart) {
			chart.destroy();
		});
		charts = [];

		if (typeof Chart === "undefined") {
			return;
		}

		var colors = chartColors();
		var usersChart = document.getElementById("users-chart");
		var postsChart = document.getElementById("posts-chart");
		var postStatusChart = document.getElementById("post-status-chart");
		var commentStatusChart = document.getElementById("comment-status-chart");

		if (usersChart) {
			charts.push(createLineChart(usersChart, colors[0]));
		}
		if (postsChart) {
			charts.push(createLineChart(postsChart, colors[1]));
		}
		if (postStatusChart) {
			charts.push(createDoughnutChart(postStatusChart, colors.slice(0, 3)));
		}
		if (commentStatusChart) {
			charts.push(createBarChart(commentStatusChart, [colors[3], colors[0], colors[4]]));
		}
	}

	document.addEventListener("DOMContentLoaded", initCharts);
	document.addEventListener("themechange", initCharts);
})();
