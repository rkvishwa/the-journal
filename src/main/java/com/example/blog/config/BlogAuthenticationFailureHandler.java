package com.example.blog.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.blog.user.UserRepository;
import com.example.blog.user.UserService;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class BlogAuthenticationFailureHandler implements AuthenticationFailureHandler {

	private final UserRepository users;
	private final UserService userService;

	public BlogAuthenticationFailureHandler(UserRepository users, UserService userService) {
		this.users = users;
		this.userService = userService;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		String identifier = request.getParameter("username");
		if (identifier != null) {
			users.findByEmailOrUsername(identifier).ifPresent(userService::recordFailedLogin);
		}
		response.sendRedirect("/login?error");
	}
}
