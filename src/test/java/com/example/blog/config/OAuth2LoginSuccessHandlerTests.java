package com.example.blog.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

class OAuth2LoginSuccessHandlerTests {

	@Test
	void redirectsPendingGoogleUsersToCompletePage() throws Exception {
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(null, null);
		RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
		handler.setRedirectStrategy(redirectStrategy);

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		OAuth2User pendingUser = new DefaultOAuth2User(
				java.util.List.of(new SimpleGrantedAuthority(CustomOAuth2UserService.OAUTH_PENDING_ROLE)),
				java.util.Map.of("sub", "google-sub"),
				"sub");

		handler.onAuthenticationSuccess(request, response,
				new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
						pendingUser, pendingUser.getAuthorities(), "google"));

		verify(redirectStrategy).sendRedirect(request, response, "/oauth/complete");
	}

	@Test
	void redirectsWhenPendingSessionExistsEvenWithoutPendingRole() throws Exception {
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(null, null);
		RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
		handler.setRedirectStrategy(redirectStrategy);

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		HttpSession session = mock(HttpSession.class);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(CustomOAuth2UserService.PENDING_SUB)).thenReturn("google-sub-1");

		OAuth2User user = new DefaultOAuth2User(List.of(), Map.of("sub", "google-sub-1"), "sub");
		handler.onAuthenticationSuccess(request, response,
				new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
						user, user.getAuthorities(), "google"));

		verify(redirectStrategy).sendRedirect(request, response, "/oauth/complete");
	}

}
