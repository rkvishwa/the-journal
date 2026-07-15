(function () {
	var STORAGE_KEY = "theme";

	function getStoredTheme() {
		try {
			return localStorage.getItem(STORAGE_KEY);
		} catch (error) {
			return null;
		}
	}

	function getSystemTheme() {
		return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
	}

	function getPreferredTheme() {
		var stored = getStoredTheme();
		if (stored === "light" || stored === "dark") {
			return stored;
		}
		return getSystemTheme();
	}

	function applyTheme(theme) {
		var isDark = theme === "dark";
		document.documentElement.setAttribute("data-theme", isDark ? "dark" : "light");

		document.querySelectorAll(".theme-toggle").forEach(function (toggle) {
			toggle.setAttribute("aria-pressed", isDark ? "true" : "false");
			toggle.setAttribute("aria-label", isDark ? "Switch to light mode" : "Switch to dark mode");
		});

		document.dispatchEvent(new CustomEvent("themechange", {
			detail: { theme: isDark ? "dark" : "light" }
		}));
	}

	function persistTheme(theme) {
		try {
			localStorage.setItem(STORAGE_KEY, theme);
		} catch (error) {
			/* ignore */
		}
	}

	function toggleTheme() {
		var nextTheme = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
		persistTheme(nextTheme);
		applyTheme(nextTheme);
	}

	function wireScrollProgress() {
		var track = document.querySelector(".scroll-progress");
		var bar = track ? track.querySelector(".scroll-progress-bar") : null;

		if (!track || !bar) {
			return;
		}

		document.documentElement.classList.add("has-scroll-progress");

		function updateProgress() {
			var scrollTop = window.scrollY;
			var scrollable = document.documentElement.scrollHeight - window.innerHeight;
			var progress = scrollable > 0 ? scrollTop / scrollable : 0;
			bar.style.setProperty("--scroll-progress", String(progress));
		}

		window.addEventListener("scroll", updateProgress, { passive: true });
		window.addEventListener("resize", updateProgress);
		updateProgress();
	}

	function wireBackToTop() {
		var button = document.querySelector(".back-to-top");

		if (!button) {
			return;
		}

		var scrollThreshold = 400;

		function setVisible(visible) {
			button.classList.toggle("is-visible", visible);
			button.setAttribute("aria-hidden", visible ? "false" : "true");
			if (visible) {
				button.removeAttribute("tabindex");
			} else {
				button.setAttribute("tabindex", "-1");
			}
		}

		function updateVisibility() {
			setVisible(window.scrollY > scrollThreshold);
		}

		button.addEventListener("click", function () {
			window.scrollTo({ top: 0, behavior: "smooth" });
		});

		window.addEventListener("scroll", updateVisibility, { passive: true });
		updateVisibility();
	}

	document.addEventListener("DOMContentLoaded", function () {
		applyTheme(getPreferredTheme());

		document.querySelectorAll(".theme-toggle").forEach(function (toggle) {
			toggle.addEventListener("click", toggleTheme);
		});

		window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function (event) {
			if (getStoredTheme()) {
				return;
			}
			applyTheme(event.matches ? "dark" : "light");
		});

		wireBackToTop();
		wireScrollProgress();
	});
})();
