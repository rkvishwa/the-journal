package com.example.blog.post;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PostCoverSvgRenderer {

	private static final int MAX_LINES = 3;
	private static final int MAX_CHARS_PER_LINE = 28;

	public String render(String title) {
		String safeTitle = title == null || title.isBlank() ? "Untitled" : title.trim();
		List<String> lines = wrapTitle(safeTitle);
		StringBuilder tspans = new StringBuilder();
		double startY = 315 - ((lines.size() - 1) * 28);
		for (int i = 0; i < lines.size(); i++) {
			tspans.append("<tspan x=\"600\" dy=\"")
					.append(i == 0 ? 0 : 56)
					.append("\">")
					.append(escapeXml(lines.get(i)))
					.append("</tspan>");
		}
		return """
				<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630" role="img">
				  <defs>
				    <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
				      <stop offset="0%%" stop-color="#fff8f0"/>
				      <stop offset="100%%" stop-color="#f5ddc4"/>
				    </linearGradient>
				  </defs>
				  <rect width="1200" height="630" fill="url(#bg)"/>
				  <rect x="48" y="48" width="1104" height="534" rx="24" fill="none" stroke="#92400e" stroke-opacity="0.12" stroke-width="2"/>
				  <text x="600" y="%s" text-anchor="middle" dominant-baseline="middle"
				        font-family="Georgia, 'Times New Roman', serif" font-size="48" fill="#1c1917">
				    %s
				  </text>
				</svg>
				""".formatted(startY, tspans);
	}

	public String coverUrl(String title) {
		return PostCovers.fallbackUrl(title);
	}

	private List<String> wrapTitle(String title) {
		String[] words = title.split("\\s+");
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			if (current.isEmpty()) {
				current.append(word);
				continue;
			}
			if (current.length() + 1 + word.length() <= MAX_CHARS_PER_LINE) {
				current.append(' ').append(word);
			}
			else {
				lines.add(current.toString());
				current = new StringBuilder(word);
			}
			if (lines.size() >= MAX_LINES) {
				break;
			}
		}
		if (lines.size() < MAX_LINES && !current.isEmpty()) {
			lines.add(current.toString());
		}
		if (lines.size() == MAX_LINES && words.length > countWords(lines)) {
			String last = lines.get(MAX_LINES - 1);
			if (last.length() > MAX_CHARS_PER_LINE - 1) {
				last = last.substring(0, MAX_CHARS_PER_LINE - 1);
			}
			if (!last.endsWith("…")) {
				last = last + "…";
			}
			lines.set(MAX_LINES - 1, last);
		}
		if (lines.isEmpty()) {
			lines.add("Untitled");
		}
		return lines;
	}

	private int countWords(List<String> lines) {
		return lines.stream().mapToInt(line -> line.split("\\s+").length).sum();
	}

	private String escapeXml(String value) {
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}
