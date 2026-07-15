package com.example.blog.post;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostCoverController {

	private final PostCoverSvgRenderer renderer;

	public PostCoverController(PostCoverSvgRenderer renderer) {
		this.renderer = renderer;
	}

	@GetMapping(value = "/posts/cover.svg", produces = "image/svg+xml")
	public ResponseEntity<String> coverSvg(@RequestParam(defaultValue = "") String title) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
				.body(renderer.render(title));
	}
}
