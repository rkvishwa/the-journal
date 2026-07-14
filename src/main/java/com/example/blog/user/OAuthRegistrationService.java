package com.example.blog.user;

import java.util.Optional;

import com.example.blog.config.CustomOAuth2UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthRegistrationService {

	private final UserRepository users;
	private final UserService userService;
	private final OAuthAccountLinkService oauthLinks;

	public OAuthRegistrationService(UserRepository users, UserService userService,
			OAuthAccountLinkService oauthLinks) {
		this.users = users;
		this.userService = userService;
		this.oauthLinks = oauthLinks;
	}

	public boolean hasPendingRegistration(HttpSession session) {
		return session != null && session.getAttribute(CustomOAuth2UserService.PENDING_SUB) != null;
	}

	public String pendingEmail(HttpSession session) {
		return (String) session.getAttribute(CustomOAuth2UserService.PENDING_EMAIL);
	}

	public String pendingName(HttpSession session) {
		return (String) session.getAttribute(CustomOAuth2UserService.PENDING_NAME);
	}

	public String suggestUsername(HttpSession session) {
		return suggestUsername(pendingEmail(session));
	}

	public String suggestUsername(String email) {
		if (email == null || !email.contains("@")) {
			return "user";
		}
		String base = email.substring(0, email.indexOf('@')).toLowerCase().replaceAll("[^a-zA-Z0-9_]", "");
		if (base.length() < 3) {
			base = base + "user";
		}
		return base.substring(0, Math.min(80, base.length()));
	}

	@Transactional(readOnly = true)
	public Optional<User> findExistingAccount(HttpSession session) {
		String providerUserId = (String) session.getAttribute(CustomOAuth2UserService.PENDING_SUB);
		if (providerUserId == null) {
			return Optional.empty();
		}
		User linked = oauthLinks.findUserByGoogleId(providerUserId);
		if (linked != null) {
			return Optional.of(linked);
		}
		String email = pendingEmail(session);
		if (email == null || email.isBlank()) {
			return Optional.empty();
		}
		return users.findByEmailIgnoreCase(email);
	}

	@Transactional
	public User resumeExistingAccount(HttpSession session) {
		User existing = findExistingAccount(session).orElseThrow();
		String providerUserId = (String) session.getAttribute(CustomOAuth2UserService.PENDING_SUB);
		String email = pendingEmail(session);
		String picture = (String) session.getAttribute(CustomOAuth2UserService.PENDING_PICTURE);
		if (oauthLinks.findUserByGoogleId(providerUserId) == null) {
			oauthLinks.linkGoogle(existing, providerUserId, email);
		}
		if (picture != null && (existing.getAvatarUrl() == null || existing.getAvatarUrl().isBlank())) {
			existing.setAvatarUrl(picture);
		}
		User repaired = userService.repairOAuthLogin(existing);
		clearPending(session);
		return repaired;
	}

	@Transactional
	public User completeRegistration(HttpSession session, String username) {
		String providerUserId = (String) session.getAttribute(CustomOAuth2UserService.PENDING_SUB);
		if (providerUserId == null) {
			throw new RegistrationException("Your Google sign-in session expired. Please try again.");
		}

		User existingByGoogle = oauthLinks.findUserByGoogleId(providerUserId);
		if (existingByGoogle != null) {
			clearPending(session);
			return userService.repairOAuthLogin(existingByGoogle);
		}

		String email = pendingEmail(session);
		String displayName = pendingName(session);
		String picture = (String) session.getAttribute(CustomOAuth2UserService.PENDING_PICTURE);
		String trimmedUsername = username.trim();

		if (email != null && !email.isBlank()) {
			User existingByEmail = users.findByEmailIgnoreCase(email).orElse(null);
			if (existingByEmail != null) {
				oauthLinks.linkGoogle(existingByEmail, providerUserId, email);
				if (picture != null
						&& (existingByEmail.getAvatarUrl() == null || existingByEmail.getAvatarUrl().isBlank())) {
					existingByEmail.setAvatarUrl(picture);
				}
				clearPending(session);
				return userService.repairOAuthLogin(existingByEmail);
			}
		}

		if (users.existsByUsernameIgnoreCase(trimmedUsername)) {
			throw new RegistrationException("Username is already taken.");
		}

		User created = userService.createOAuthUser(
				email == null || email.isBlank() ? providerUserId + "@google.local" : email,
				trimmedUsername,
				displayName);
		if (picture != null) {
			created.setAvatarUrl(picture);
		}
		oauthLinks.linkGoogle(created, providerUserId, email);
		clearPending(session);
		return userService.repairOAuthLogin(created);
	}

	public void clearPending(HttpSession session) {
		if (session == null) {
			return;
		}
		session.removeAttribute(CustomOAuth2UserService.PENDING_SUB);
		session.removeAttribute(CustomOAuth2UserService.PENDING_EMAIL);
		session.removeAttribute(CustomOAuth2UserService.PENDING_NAME);
		session.removeAttribute(CustomOAuth2UserService.PENDING_PICTURE);
	}

}
