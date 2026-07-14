package com.example.blog.studio;

import java.util.Map;

import com.example.blog.upload.ImageStorageService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class StudioUploadController {

	private final ImageStorageService images;

	public StudioUploadController(ImageStorageService images) {
		this.images = images;
	}

	@PostMapping("/studio/uploads/images")
	public Map<String, String> uploadImage(@RequestParam("upload") MultipartFile file) {
		return Map.of("url", images.store(file).url());
	}
}
