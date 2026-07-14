package com.example.blog.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.blog.user.BlogUserDetails;
import com.example.blog.user.User;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static User requireUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlogUserDetails details)) {
			throw new IllegalStateException("No authenticated user.");
		}
		return details.getUser();
	}

	public static User optionalUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlogUserDetails details)) {
			return null;
		}
		return details.getUser();
	}
}
