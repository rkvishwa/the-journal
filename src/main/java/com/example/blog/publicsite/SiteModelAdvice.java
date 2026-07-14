package com.example.blog.publicsite;

import com.example.blog.admin.SiteSettings;
import com.example.blog.admin.SiteSettingsService;
import com.example.blog.config.BlogProperties;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SiteModelAdvice {

	private final SiteSettingsService siteSettings;
	private final BlogProperties properties;

	public SiteModelAdvice(SiteSettingsService siteSettings, BlogProperties properties) {
		this.siteSettings = siteSettings;
		this.properties = properties;
	}

	@ModelAttribute("siteSettings")
	public SiteSettings siteSettings() {
		return siteSettings.settings();
	}

	@ModelAttribute("siteBaseUrl")
	public String siteBaseUrl() {
		return properties.baseUrl().replaceAll("/+$", "");
	}

	@ModelAttribute("pageUrl")
	public String pageUrl(HttpServletRequest request) {
		String base = siteBaseUrl();
		if (request == null || request.getRequestURI() == null) {
			return base;
		}
		return base + request.getRequestURI();
	}
}
