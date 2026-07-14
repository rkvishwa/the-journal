package com.example.blog.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImageStorageService {

	private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/gif", ".gif",
			"image/webp", ".webp");

	private final Path uploadDirectory;

	public ImageStorageService(@Value("${blog.upload-dir:./data/uploads}") String uploadDirectory) {
		this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
	}

	public StoredImage store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image to upload.");
		}

		String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
		if (extension == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, GIF, and WebP images are allowed.");
		}

		String filename = UUID.randomUUID() + extension;
		Path target = uploadDirectory.resolve(filename).normalize();
		if (!target.startsWith(uploadDirectory)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image filename.");
		}

		try {
			Files.createDirectories(uploadDirectory);
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image upload failed.", ex);
		}

		return new StoredImage(filename, "/uploads/" + filename);
	}

	public String resourceLocation() {
		return uploadDirectory.toUri().toString();
	}

	public record StoredImage(String filename, String url) {
	}
}
