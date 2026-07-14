package com.example.blog.user;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(Long id) {
		super("User not found: " + id);
	}

	public UserNotFoundException(String username) {
		super("User not found: " + username);
	}
}
