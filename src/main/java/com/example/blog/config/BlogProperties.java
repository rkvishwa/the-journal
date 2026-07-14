package com.example.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog")
public record BlogProperties(
		String adminEmail,
		String adminUsername,
		String adminPassword,
		String uploadDir,
		String siteName,
		String baseUrl,
		String mailFrom) {
}
