package com.example.blog.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.blog.user.BlogUserDetails;
import com.example.blog.user.User;
import com.example.blog.user.UserService;
import com.example.blog.user.UserRole;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class BlogAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final UserService userService;

	public BlogAuthenticationSuccessHandler(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		User user = ((BlogUserDetails) authentication.getPrincipal()).getUser();
		userService.resetFailedLogins(user);
		if (user.hasRole(UserRole.ADMIN) && request.getRequestURI().equals("/login")) {
			response.sendRedirect("/admin");
			return;
		}
		if (!user.isEmailVerified()) {
			response.sendRedirect("/verify-email");
			return;
		}
		response.sendRedirect("/studio");
	}
}
