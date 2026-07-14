package com.example.blog.config;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Fallback loader for tests and tools that start Spring without {@code main()}.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROPERTY_SOURCE_NAME = "dotenv";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
			return;
		}
		Map<String, Object> properties = DotEnvLoader.load();
		if (properties.isEmpty()) {
			return;
		}
		environment.getPropertySources().addAfter("systemEnvironment",
				new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
	}
}
