(function () {
	document.querySelectorAll(".delete-form").forEach(function (form) {
		form.addEventListener("submit", function (event) {
			if (!window.confirm("Delete this post? This cannot be undone.")) {
				event.preventDefault();
			}
		});
	});

	function setBodyScrollLocked(locked) {
		document.body.classList.toggle("scroll-locked", locked);
	}

	function wireNavToggle(toggleSelector, navSelector, backdropSelector, closeOnOutsideSelector) {
		var navToggle = document.querySelector(toggleSelector);
		var nav = document.querySelector(navSelector);
		var backdrop = backdropSelector ? document.querySelector(backdropSelector) : null;

		if (!navToggle || !nav) {
			return;
		}

		function setOpen(isOpen) {
			nav.classList.toggle("is-open", isOpen);
			navToggle.setAttribute("aria-expanded", isOpen ? "true" : "false");
			if (backdrop) {
				backdrop.hidden = !isOpen;
			}
			setBodyScrollLocked(isOpen);
		}

		navToggle.addEventListener("click", function (event) {
			event.stopPropagation();
			setOpen(!nav.classList.contains("is-open"));
		});

		nav.querySelectorAll("a, button").forEach(function (control) {
			control.addEventListener("click", function () {
				setOpen(false);
			});
		});

		if (backdrop) {
			backdrop.addEventListener("click", function () {
				setOpen(false);
			});
		}

		document.addEventListener("click", function (event) {
			if (!nav.classList.contains("is-open")) {
				return;
			}

			if (!event.target.closest(closeOnOutsideSelector)) {
				setOpen(false);
			}
		});

		document.addEventListener("keydown", function (event) {
			if (event.key === "Escape" && nav.classList.contains("is-open")) {
				setOpen(false);
			}
		});
	}

	wireNavToggle(".admin-header .admin-nav-toggle", "#admin-nav", null, ".admin-header-inner");

	document.addEventListener("keydown", function (event) {
		if (event.key === "Escape") {
			document.querySelectorAll(".admin-sidebar-checkbox:checked, .site-nav-checkbox:checked").forEach(function (checkbox) {
				checkbox.checked = false;
			});
		}
	});

	function wireTableSearch(inputId, tableId) {
		var input = document.getElementById(inputId);
		var table = document.getElementById(tableId);

		if (!input || !table) {
			return;
		}

		input.addEventListener("input", function () {
			var query = input.value.trim().toLowerCase();
			table.querySelectorAll("tbody tr").forEach(function (row) {
				var haystack = (row.getAttribute("data-search") || "").toLowerCase();
				row.hidden = query !== "" && !haystack.includes(query);
			});
		});
	}

	wireTableSearch("user-search", "users-table");
	wireTableSearch("post-search", "posts-table");

	document.querySelectorAll(".password-toggle").forEach(function (toggle) {
		var field = toggle.closest(".password-field");
		var input = field ? field.querySelector("input") : null;

		if (!input) {
			return;
		}

		toggle.addEventListener("click", function () {
			var visible = input.type === "text";
			input.type = visible ? "password" : "text";
			toggle.setAttribute("aria-pressed", visible ? "false" : "true");
			toggle.setAttribute("aria-label", visible ? "Show password" : "Hide password");
		});
	});

	document.addEventListener("click", function (event) {
		document.querySelectorAll(".catalog-filters[open]").forEach(function (filters) {
			if (!event.target.closest(".catalog-filters")) {
				filters.open = false;
			}
		});
	});

	/* ── Article content font‑size picker ── */
	(function () {
		var STORAGE_KEY = "article-font-size";
		var SIZES = ["small", "default", "large", "xlarge"];
		var content = document.querySelector(".content");
		var buttons = document.querySelectorAll(".font-size-btn");

		if (!content || !buttons.length) {
			return;
		}

		function applySize(size) {
			if (!SIZES.includes(size)) {
				size = "default";
			}
			// Remove all size classes, then add the chosen one
			SIZES.forEach(function (s) {
				content.classList.remove("content-size-" + s);
			});
			content.classList.add("content-size-" + size);

			// Update active button
			buttons.forEach(function (btn) {
				btn.classList.toggle("active", btn.dataset.size === size);
			});

			try {
				localStorage.setItem(STORAGE_KEY, size);
			} catch (e) {}
		}

		// Restore saved preference (fall back to "default")
		var saved;
		try {
			saved = localStorage.getItem(STORAGE_KEY);
		} catch (e) {}
		applySize(saved || "default");

		// Wire up each button
		buttons.forEach(function (btn) {
			btn.addEventListener("click", function () {
				applySize(btn.dataset.size);
			});
		});
	})();
})();
