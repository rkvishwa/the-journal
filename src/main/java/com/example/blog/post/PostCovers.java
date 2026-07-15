package com.example.blog.post;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PostCovers {

	private PostCovers() {
	}

	public static String displayUrl(String title, String coverImageUrl) {
		if (coverImageUrl != null && !coverImageUrl.isBlank()) {
			return coverImageUrl;
		}
		return fallbackUrl(title);
	}

	public static String fallbackUrl(String title) {
		String safeTitle = title == null || title.isBlank() ? "Untitled" : title.trim();
		return "/posts/cover.svg?title=" + URLEncoder.encode(safeTitle, StandardCharsets.UTF_8);
	}
}
