package com.example.blog.config;

import java.io.IOException;
import java.util.Collection;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.blog.user.BlogUserDetails;
import com.example.blog.user.CustomUserDetailsService;
import com.example.blog.user.User;
import com.example.blog.user.UserRole;
import com.example.blog.user.UserService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final CustomUserDetailsService userDetailsService;
	private final UserService userService;

	public OAuth2LoginSuccessHandler(CustomUserDetailsService userDetailsService, UserService userService) {
		this.userDetailsService = userDetailsService;
		this.userService = userService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		if (requiresUsernameCompletion(request, authentication)) {
			getRedirectStrategy().sendRedirect(request, response, "/oauth/complete");
			return;
		}

		UserDetails details = userDetailsService.loadUserByUsername(authentication.getName());
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(details, null,
				details.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(token);
		userService.resetFailedLogins(((BlogUserDetails) details).getUser());

		HttpSession session = request.getSession(false);
		if (session != null && "/settings".equals(session.getAttribute("OAUTH_RETURN_PATH"))) {
			session.removeAttribute("OAUTH_RETURN_PATH");
			getRedirectStrategy().sendRedirect(request, response, "/settings?linked=google");
			return;
		}

		User user = ((BlogUserDetails) details).getUser();
		if (user.hasRole(UserRole.ADMIN)) {
			getRedirectStrategy().sendRedirect(request, response, "/admin");
			return;
		}
		if (!user.isEmailVerified()) {
			getRedirectStrategy().sendRedirect(request, response, "/verify-email");
			return;
		}
		getRedirectStrategy().sendRedirect(request, response, "/studio");
	}

	private boolean requiresUsernameCompletion(HttpServletRequest request, Authentication authentication) {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(CustomOAuth2UserService.PENDING_SUB) != null) {
			return true;
		}
		return resolveAuthorities(authentication).stream()
				.anyMatch(authority -> CustomOAuth2UserService.OAUTH_PENDING_ROLE.equals(authority.getAuthority()));
	}

	private Collection<? extends GrantedAuthority> resolveAuthorities(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
			return oauth2User.getAuthorities();
		}
		return authentication.getAuthorities();
	}
}
