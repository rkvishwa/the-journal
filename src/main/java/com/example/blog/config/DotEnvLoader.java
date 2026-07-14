package com.example.blog.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DotEnvLoader {

	public static final String LOADED_PATH_PROPERTY = "dotenv.loaded.path";

	private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);
	private static final String ENV_FILENAME = ".env";

	private DotEnvLoader() {
	}

	public static Map<String, Object> load() {
		Path envFile = findEnvFile();
		if (envFile == null) {
			log.warn(".env file not found (cwd: {}). Using OS env vars and application.properties defaults only.",
					Paths.get("").toAbsolutePath());
			return Collections.emptyMap();
		}

		Map<String, Object> properties = loadEnvFile(envFile);
		if (properties.isEmpty()) {
			log.warn(".env file is empty: {}", envFile.toAbsolutePath());
			return properties;
		}

		properties.put(LOADED_PATH_PROPERTY, envFile.toAbsolutePath().toString());
		log.info("Loaded {} variables from {}", properties.size() - 1, envFile.toAbsolutePath());
		return properties;
	}

	static Path findEnvFile() {
		Path cwd = Paths.get("").toAbsolutePath();
		try (Stream<Path> candidates = Stream.of(
				cwd.resolve(ENV_FILENAME),
				Paths.get(System.getProperty("user.dir", ".")).resolve(ENV_FILENAME))) {
			return candidates.filter(Files::isRegularFile).findFirst().orElseGet(() -> walkUpForEnv(cwd));
		}
	}

	private static Path walkUpForEnv(Path start) {
		Path current = start;
		for (int depth = 0; depth < 6 && current != null; depth++) {
			Path candidate = current.resolve(ENV_FILENAME);
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
			current = current.getParent();
		}
		return null;
	}

	private static Map<String, Object> loadEnvFile(Path envFile) {
		Map<String, Object> properties = new LinkedHashMap<>();
		try {
			for (String line : Files.readAllLines(envFile)) {
				parseLine(line).ifPresent(entry -> properties.put(entry.key(), entry.value()));
			}
		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to read " + envFile.toAbsolutePath(), exception);
		}
		return properties;
	}

	private static java.util.Optional<EnvEntry> parseLine(String line) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("#")) {
			return java.util.Optional.empty();
		}
		if (trimmed.startsWith("export ")) {
			trimmed = trimmed.substring("export ".length()).trim();
		}
		int separator = trimmed.indexOf('=');
		if (separator <= 0) {
			return java.util.Optional.empty();
		}
		String key = trimmed.substring(0, separator).trim();
		String value = unquote(trimmed.substring(separator + 1).trim()).trim();
		if (key.isEmpty()) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(new EnvEntry(key, value));
	}

	private static String unquote(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			char last = value.charAt(value.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}

	private record EnvEntry(String key, String value) {
	}
}
