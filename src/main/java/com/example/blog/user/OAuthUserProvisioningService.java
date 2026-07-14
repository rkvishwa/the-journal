package com.example.blog.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.example.blog.config.CustomOAuth2UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OAuthUserProvisioningService {

	private static final Logger log = LoggerFactory.getLogger(OAuthUserProvisioningService.class);

	private final UserRepository users;
	private final OAuthAccountLinkService oauthLinks;
	private final UserService userService;
	private final HttpServletRequest httpServletRequest;

	public OAuthUserProvisioningService(UserRepository users, OAuthAccountLinkService oauthLinks, UserService userService,
			HttpServletRequest httpServletRequest) {
		this.users = users;
		this.oauthLinks = oauthLinks;
		this.userService = userService;
		this.httpServletRequest = httpServletRequest;
	}

	public OAuth2User provision(OAuth2User oauthUser) {
		ProvisionedAccount account = resolveAccount(oauthUser.getName(), oauthUser.getAttribute("email"),
				oauthUser.getAttribute("name"), oauthUser.getAttribute("picture"),
				ensureSubAttribute(oauthUser.getAttributes(), oauthUser.getName()));
		return toOAuth2User(account);
	}

	public OidcUser provisionOidc(OidcUser oidcUser) {
		ProvisionedAccount account = resolveAccount(oidcUser.getName(), oidcUser.getAttribute("email"),
				oidcUser.getAttribute("name"), oidcUser.getAttribute("picture"),
				ensureSubAttribute(oidcUser.getAttributes(), oidcUser.getName()));
		return toOidcUser(account, oidcUser.getIdToken(), oidcUser.getUserInfo());
	}

	private ProvisionedAccount resolveAccount(String providerUserId, String email, String name, String picture,
			Map<String, Object> attributes) {
		HttpSession session = currentSession(true);
		User linkedUser = oauthLinks.findUserByGoogleId(providerUserId);
		if (linkedUser != null) {
			log.info("Google sign-in matched existing linked account '{}'", linkedUser.getUsername());
			clearPendingRegistration(session);
			return ProvisionedAccount.existing(userService.repairOAuthLogin(linkedUser), attributes);
		}

		if (session != null && Boolean.TRUE.equals(session.getAttribute(CustomOAuth2UserService.LINK_MODE_SESSION_KEY))) {
			session.removeAttribute(CustomOAuth2UserService.LINK_MODE_SESSION_KEY);
			Object userId = session.getAttribute("OAUTH_LINK_USER_ID");
			if (userId instanceof Long id) {
				User current = users.findById(id).orElseThrow();
				oauthLinks.linkGoogle(current, providerUserId, email);
				return ProvisionedAccount.existing(userService.repairOAuthLogin(current), attributes);
			}
		}

		User existingByEmail = email == null || email.isBlank()
				? null
				: users.findByEmailIgnoreCase(email).orElse(null);
		if (existingByEmail != null) {
			log.info("Google sign-in matched existing email account '{}'", existingByEmail.getUsername());
			User resolved = linkOrResolveUser(existingByEmail, providerUserId, email, picture);
			clearPendingRegistration(session);
			return ProvisionedAccount.existing(resolved, attributes);
		}

		if (session == null) {
			throw new RegistrationException("Could not start Google sign-up. Please try again.");
		}
		log.info("Google sign-up requires username completion for {}", email);
		storePendingRegistration(session, providerUserId, email, name, picture);
		return ProvisionedAccount.pending(attributes);
	}

	private OAuth2User toOAuth2User(ProvisionedAccount account) {
		if (account.pending()) {
			return new DefaultOAuth2User(account.authorities(), account.attributes(), "sub");
		}
		return new DefaultOAuth2User(account.authorities(), account.attributes(), "sub") {
			@Override
			public String getName() {
				return account.username();
			}
		};
	}

	private OidcUser toOidcUser(ProvisionedAccount account, OidcIdToken idToken, OidcUserInfo userInfo) {
		if (account.pending()) {
			return new DefaultOidcUser(account.authorities(), idToken, userInfo, "sub");
		}
		return new DefaultOidcUser(account.authorities(), idToken, userInfo, "sub") {
			@Override
			public String getName() {
				return account.username();
			}
		};
	}

	private User linkOrResolveUser(User user, String providerUserId, String email, String picture) {
		try {
			oauthLinks.linkGoogle(user, providerUserId, email);
		}
		catch (RegistrationException exception) {
			User linked = oauthLinks.findUserByGoogleId(providerUserId);
			if (linked != null) {
				log.info("Recovered Google sign-in via existing link for '{}'", linked.getUsername());
				return userService.repairOAuthLogin(linked);
			}
			throw exception;
		}
		if (!user.isEmailVerified()) {
			user.setEmailVerified(true);
		}
		if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
			user.setAvatarUrl(picture);
		}
		return userService.repairOAuthLogin(user);
	}

	private Map<String, Object> ensureSubAttribute(Map<String, Object> attributes, String providerUserId) {
		Map<String, Object> resolved = new HashMap<>(attributes);
		if (providerUserId != null && !providerUserId.isBlank()) {
			resolved.putIfAbsent("sub", providerUserId);
		}
		return resolved;
	}

	private void storePendingRegistration(HttpSession session, String providerUserId, String email, String name,
			String picture) {
		session.setAttribute(CustomOAuth2UserService.PENDING_SUB, providerUserId);
		session.setAttribute(CustomOAuth2UserService.PENDING_EMAIL, email);
		session.setAttribute(CustomOAuth2UserService.PENDING_NAME, name);
		session.setAttribute(CustomOAuth2UserService.PENDING_PICTURE, picture);
	}

	private void clearPendingRegistration(HttpSession session) {
		if (session == null) {
			return;
		}
		session.removeAttribute(CustomOAuth2UserService.PENDING_SUB);
		session.removeAttribute(CustomOAuth2UserService.PENDING_EMAIL);
		session.removeAttribute(CustomOAuth2UserService.PENDING_NAME);
		session.removeAttribute(CustomOAuth2UserService.PENDING_PICTURE);
	}

	private HttpSession currentSession(boolean create) {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
					.currentRequestAttributes();
			return attributes.getRequest().getSession(create);
		}
		catch (IllegalStateException ignored) {
			return httpServletRequest.getSession(create);
		}
	}

	private record ProvisionedAccount(boolean pending, String username, List<GrantedAuthority> authorities,
			Map<String, Object> attributes) {

		private static ProvisionedAccount pending(Map<String, Object> attributes) {
			return new ProvisionedAccount(true, null,
					List.of(new SimpleGrantedAuthority(CustomOAuth2UserService.OAUTH_PENDING_ROLE)), attributes);
		}

		private static ProvisionedAccount existing(User user, Map<String, Object> attributes) {
			List<GrantedAuthority> authorities = user.getRoles().stream()
					.<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
					.toList();
			return new ProvisionedAccount(false, user.getUsername(), authorities, attributes);
		}
	}

}
