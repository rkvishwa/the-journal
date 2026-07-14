package com.example.blog.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.blog.user.BlogUserDetails;
import com.example.blog.user.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EmailVerifiedFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		if (path.startsWith("/studio") && !path.startsWith("/studio/uploads")) {
			User user = currentUser();
			if (user != null && !user.isEmailVerified()) {
				response.sendRedirect("/verify-email");
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private User currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof BlogUserDetails details) {
			return details.getUser();
		}
		return null;
	}
}
