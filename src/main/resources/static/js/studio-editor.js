(function () {
	const statusInput = document.querySelector("#post-status");
	const schedulePanel = document.querySelector("#schedule-panel");
	const scheduleToggle = document.querySelector("#schedule-toggle");

	function setStatus(value) {
		if (statusInput) {
			statusInput.value = value;
		}
	}

	function toggleSchedulePanel(show) {
		if (!schedulePanel || !scheduleToggle) {
			return;
		}
		const visible = show !== undefined ? show : schedulePanel.classList.contains("is-hidden");
		schedulePanel.classList.toggle("is-hidden", !visible);
		scheduleToggle.classList.toggle("is-active", visible);
		if (visible) {
			setStatus("SCHEDULED");
		}
	}

	if (scheduleToggle && schedulePanel) {
		scheduleToggle.addEventListener("click", function () {
			toggleSchedulePanel(schedulePanel.classList.contains("is-hidden"));
		});
	}

	document.querySelectorAll(".studio-action-btn[data-status]").forEach(function (button) {
		button.addEventListener("click", function () {
			setStatus(button.dataset.status);
		});
	});

	const textarea = document.querySelector(".rich-text-input");
	const editorElement = document.querySelector("#blog-editor");
	if (!textarea || !editorElement || !window.Quill) {
		return;
	}

	const quill = new window.Quill(editorElement, {
		theme: "snow",
		placeholder: "Tell your story…",
		modules: {
			toolbar: {
				container: [
					[{ header: [1, 2, 3, false] }],
					["bold", "italic", "underline"],
					[{ list: "ordered" }, { list: "bullet" }],
					["blockquote", "link", "image"],
					["clean"]
				],
				handlers: {
					image: function () {
						selectAndUploadImage(quill);
					}
				}
			}
		}
	});

	function selectAndUploadImage(editor) {
		const input = document.createElement("input");
		input.type = "file";
		input.accept = "image/png,image/jpeg,image/gif,image/webp";
		input.addEventListener("change", function () {
			const file = input.files && input.files[0];
			if (!file) {
				return;
			}
			uploadImage(file).then(function (url) {
				const range = editor.getSelection(true);
				let index = range ? range.index : editor.getLength();
				editor.insertEmbed(index, "image", url, "user");
				syncTextarea();
			});
		});
		input.click();
	}

	function uploadImage(file) {
		const body = new FormData();
		body.append("upload", file);
		return fetch("/studio/uploads/images", {
			method: "POST",
			body,
			headers: csrfHeaders()
		}).then(function (response) {
			if (!response.ok) {
				throw new Error("Upload failed");
			}
			return response.json();
		}).then(function (result) {
			return result.url;
		});
	}

	function syncTextarea() {
		const html = typeof quill.getSemanticHTML === "function" ? quill.getSemanticHTML() : quill.root.innerHTML;
		textarea.value = html === "<p></p>" || html === "<p><br></p>" ? "" : html;
	}

	function csrfHeaders() {
		const token = document.querySelector("input[name='_csrf']");
		return token ? { "X-CSRF-TOKEN": token.value } : {};
	}

	quill.on("text-change", syncTextarea);
	syncTextarea();

	const form = textarea.closest("form");
	if (form) {
		form.addEventListener("submit", syncTextarea);
	}

	setInterval(function () {
		syncTextarea();
		const draft = {
			title: form.querySelector(".studio-title-input")?.value || "",
			excerpt: form.querySelector(".studio-excerpt-input")?.value || "",
			content: textarea.value
		};
		localStorage.setItem("studio-draft", JSON.stringify(draft));
	}, 30000);
})();
