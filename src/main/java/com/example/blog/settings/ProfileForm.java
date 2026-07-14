package com.example.blog.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {

	@NotBlank
	@Size(max = 80)
	private String displayName;

	@Size(max = 500)
	private String bio;

	@NotBlank
	@Size(min = 3, max = 80)
	private String username;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
