package com.example.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvStartupDiagnostics {

	private static final Logger log = LoggerFactory.getLogger(EnvStartupDiagnostics.class);

	private final Environment environment;

	public EnvStartupDiagnostics(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logConfiguration() {
		String dotenvPath = environment.getProperty(DotEnvLoader.LOADED_PATH_PROPERTY);
		if (dotenvPath != null) {
			log.info("Configuration: .env loaded from {}", dotenvPath);
		}
		else {
			log.warn("Configuration: .env was NOT loaded — OAuth/SMTP/admin bootstrap may use defaults");
		}

		String adminUser = environment.getProperty("blog.admin-username");
		String googleClientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id");
		boolean googleConfigured = googleClientId != null && !googleClientId.isBlank()
				&& !"placeholder".equals(googleClientId);
		boolean mailConfigured = environment.getProperty("blog.mail-from") != null
				&& !environment.getProperty("blog.mail-from").isBlank();

		log.info("Configuration: bootstrap admin username={}", adminUser);
		log.info("Configuration: Google OAuth {} (redirect URI: {}/login/oauth2/code/google)",
				googleConfigured ? "configured" : "NOT configured — check GOOGLE_CLIENT_ID in .env",
				environment.getProperty("blog.base-url", "http://localhost:8080"));
		log.info("Configuration: SMTP email {}", mailConfigured ? "configured" : "disabled (OTP logs to console)");
	}
}
