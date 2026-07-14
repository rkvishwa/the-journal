package com.example.blog.publicsite;

import com.example.blog.admin.SiteSettings;
import com.example.blog.admin.SiteSettingsService;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SiteModelAdvice {

	private final SiteSettingsService siteSettings;

	public SiteModelAdvice(SiteSettingsService siteSettings) {
		this.siteSettings = siteSettings;
	}

	@ModelAttribute("siteSettings")
	public SiteSettings siteSettings() {
		return siteSettings.settings();
	}
}
