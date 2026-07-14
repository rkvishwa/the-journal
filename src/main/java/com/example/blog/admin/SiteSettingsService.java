package com.example.blog.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingsService {

	private final SiteSettingsRepository settings;

	public SiteSettingsService(SiteSettingsRepository settings) {
		this.settings = settings;
	}

	@Transactional(readOnly = true)
	public SiteSettings settings() {
		return settings.findById(1L).orElseGet(this::createDefault);
	}

	@Transactional
	public SiteSettings update(String siteName, String tagline) {
		SiteSettings current = settings();
		current.setSiteName(siteName.trim());
		current.setTagline(tagline.trim());
		return settings.save(current);
	}

	private SiteSettings createDefault() {
		SiteSettings defaults = new SiteSettings();
		return settings.save(defaults);
	}
}
