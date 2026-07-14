(function () {
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
				scales: {
					y: {
						beginAtZero: true,
						ticks: { precision: 0 }
					}
				}
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
						position: "bottom"
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
				scales: {
					y: {
						beginAtZero: true,
						ticks: { precision: 0 }
					}
				}
			}
		});
	}

	document.addEventListener("DOMContentLoaded", function () {
		if (typeof Chart === "undefined") {
			return;
		}

		var usersChart = document.getElementById("users-chart");
		var postsChart = document.getElementById("posts-chart");
		var postStatusChart = document.getElementById("post-status-chart");
		var commentStatusChart = document.getElementById("comment-status-chart");

		if (usersChart) {
			createLineChart(usersChart, "#2d5a4a");
		}
		if (postsChart) {
			createLineChart(postsChart, "#8b5a2b");
		}
		if (postStatusChart) {
			createDoughnutChart(postStatusChart, ["#2d5a4a", "#8b5a2b", "#4a6fa5"]);
		}
		if (commentStatusChart) {
			createBarChart(commentStatusChart, ["#b8860b", "#2d5a4a", "#8b3a3a"]);
		}
	});
})();
