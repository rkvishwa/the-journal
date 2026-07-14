package com.example.blog.post;

import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class BlogHtmlSanitizer {

	private static final Pattern SIMPLE_NUMBER = Pattern.compile("[1-9][0-9]?");
	private static final Pattern LOCAL_UPLOAD_IMAGE = Pattern.compile("/uploads/[A-Za-z0-9._-]+");

	private final PolicyFactory policy = new HtmlPolicyBuilder()
			.allowCommonBlockElements()
			.allowCommonInlineFormattingElements()
			.allowElements("h1", "h2", "h3", "h4", "h5", "h6", "u", "s", "table", "thead", "tbody", "tfoot",
					"tr", "th", "td", "figcaption", "img")
			.allowElements("figure")
			.allowAttributes("class").matching(true, "table", "image").onElements("figure")
			.allowAttributes("src").matching(LOCAL_UPLOAD_IMAGE).onElements("img")
			.allowAttributes("alt", "title").onElements("img")
			.allowAttributes("width", "height").matching(SIMPLE_NUMBER).onElements("img")
			.allowAttributes("colspan", "rowspan").matching(SIMPLE_NUMBER).onElements("th", "td")
			.allowElements("a")
			.allowUrlProtocols("http", "https", "mailto")
			.allowAttributes("href", "title").onElements("a")
			.requireRelNofollowOnLinks()
			.toFactory();

	public String sanitize(String html) {
		return policy.sanitize(html == null ? "" : html);
	}
}
