package com.example.blog.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.blog.user.RegistrationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		if (hasPendingRegistration(request)) {
			response.sendRedirect("/oauth/complete");
			return;
		}

		log.warn("Google OAuth sign-in failed: {}", exception.getMessage(), exception);

		String message = resolveFailureMessage(exception);
		response.sendRedirect("/login?oauth_error="
				+ URLEncoder.encode(message, StandardCharsets.UTF_8));
	}

	private String resolveFailureMessage(AuthenticationException exception) {
		RegistrationException registrationException = findRegistrationException(exception);
		if (registrationException != null && registrationException.getMessage() != null) {
			return registrationException.getMessage();
		}

		OAuth2AuthenticationException oauthException = findOAuthException(exception);
		if (oauthException != null && oauthException.getError() != null) {
			String description = oauthException.getError().getDescription();
			if (description != null && !description.isBlank()) {
				return description;
			}
		}

		String deepest = deepestMessage(exception);
		if (deepest != null && !deepest.isBlank()) {
			return deepest;
		}

		return "Sign-in with Google failed. Please try again.";
	}

	private String deepestMessage(Throwable exception) {
		String message = null;
		Throwable current = exception;
		while (current != null) {
			if (current.getMessage() != null && !current.getMessage().isBlank()) {
				message = current.getMessage();
			}
			current = current.getCause();
		}
		return message;
	}

	private boolean hasPendingRegistration(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session != null && session.getAttribute(CustomOAuth2UserService.PENDING_SUB) != null;
	}

	private RegistrationException findRegistrationException(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof RegistrationException registrationException) {
				return registrationException;
			}
			current = current.getCause();
		}
		return null;
	}

	private OAuth2AuthenticationException findOAuthException(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof OAuth2AuthenticationException oauthException) {
				return oauthException;
			}
			current = current.getCause();
		}
		return null;
	}

}
