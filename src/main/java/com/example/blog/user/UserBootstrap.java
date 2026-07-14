package com.example.blog.user;

import com.example.blog.admin.SiteSettingsService;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserBootstrap {

	private final UserService users;
	private final SiteSettingsService siteSettings;

	public UserBootstrap(UserService users, SiteSettingsService siteSettings) {
		this.users = users;
		this.siteSettings = siteSettings;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void bootstrap() {
		users.ensureBootstrapAdmin();
		siteSettings.settings();
	}
}
