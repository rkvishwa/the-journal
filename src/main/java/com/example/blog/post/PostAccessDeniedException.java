package com.example.blog.post;

public class PostAccessDeniedException extends RuntimeException {

	public PostAccessDeniedException(Long id) {
		super("Not allowed to access post: " + id);
	}
}
