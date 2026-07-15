(function () {
	const fileInput = document.getElementById("cover-image-input");
	const preview = document.getElementById("cover-preview");
	const placeholder = document.getElementById("cover-placeholder");
	const uploadLabel = document.getElementById("cover-upload-label");
	const changeBtn = document.getElementById("cover-change-btn");
	const removeBtn = document.getElementById("cover-remove-btn");
	const removeField = document.getElementById("remove-cover");
	const coverUrlField = document.getElementById("coverImageUrl");
	const titleInput = document.getElementById("post-title");

	if (!fileInput || !preview || !placeholder || !uploadLabel) {
		return;
	}

	let objectUrl = null;

	function hasCustomCover() {
		return coverUrlField && coverUrlField.value && removeField && removeField.value !== "true";
	}

	function fallbackCoverUrl() {
		const title = titleInput && titleInput.value.trim() ? titleInput.value.trim() : "Untitled";
		return "/posts/cover.svg?title=" + encodeURIComponent(title);
	}

	function setPreview(url, custom) {
		preview.src = url;
		uploadLabel.classList.toggle("has-image", Boolean(url));
		placeholder.hidden = Boolean(url);
		if (removeBtn) {
			removeBtn.classList.toggle("is-hidden", !custom);
		}
	}

	function clearObjectUrl() {
		if (objectUrl) {
			URL.revokeObjectURL(objectUrl);
			objectUrl = null;
		}
	}

	function refreshFallback() {
		if (objectUrl || hasCustomCover()) {
			return;
		}
		setPreview(fallbackCoverUrl(), false);
	}

	fileInput.addEventListener("change", function () {
		clearObjectUrl();
		const file = fileInput.files && fileInput.files[0];
		if (!file) {
			refreshFallback();
			return;
		}
		objectUrl = URL.createObjectURL(file);
		if (removeField) {
			removeField.value = "false";
		}
		if (coverUrlField) {
			coverUrlField.value = "";
		}
		setPreview(objectUrl, true);
	});

	if (changeBtn) {
		changeBtn.addEventListener("click", function () {
			fileInput.click();
		});
	}

	if (removeBtn) {
		removeBtn.addEventListener("click", function () {
			clearObjectUrl();
			fileInput.value = "";
			if (removeField) {
				removeField.value = "true";
			}
			if (coverUrlField) {
				coverUrlField.value = "";
			}
			setPreview(fallbackCoverUrl(), false);
		});
	}

	if (titleInput) {
		titleInput.addEventListener("input", refreshFallback);
	}

	uploadLabel.addEventListener("click", function (event) {
		if (event.target === fileInput || event.target.closest("#cover-change-btn, #cover-remove-btn")) {
			return;
		}
		if (!uploadLabel.classList.contains("has-image")) {
			fileInput.click();
		}
	});

	if (hasCustomCover()) {
		setPreview(coverUrlField.value, true);
	} else {
		setPreview(fallbackCoverUrl(), false);
	}
})();
