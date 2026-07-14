package com.example.blog.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OAuthCompleteForm {

	@NotBlank(message = "Choose a username.")
	@Size(min = 3, max = 80, message = "Username must be 3–80 characters.")
	@Pattern(regexp = "[a-zA-Z0-9_]+", message = "Use letters, numbers, and underscores only.")
	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
