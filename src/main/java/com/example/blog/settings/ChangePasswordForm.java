package com.example.blog.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {

	@NotBlank
	@Size(min = 8, max = 100)
	private String currentPassword;

	@NotBlank
	@Size(min = 8, max = 100)
	private String password;

	@NotBlank
	private String confirmPassword;

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public boolean passwordsMatch() {
		return password != null && password.equals(confirmPassword);
	}
}
