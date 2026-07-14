package com.example.blog.post;

public class PostNotFoundException extends RuntimeException {

	public PostNotFoundException(Long id) {
		super("Post not found: " + id);
	}
}
