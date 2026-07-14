package com.example.blog.config;

import com.example.blog.user.OAuthUserProvisioningService;
import com.example.blog.user.RegistrationException;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	public static final String LINK_MODE_SESSION_KEY = "OAUTH_LINK_MODE";
	public static final String OAUTH_PENDING_ROLE = "ROLE_OAUTH_PENDING";
	public static final String PENDING_SUB = "OAUTH_PENDING_SUB";
	public static final String PENDING_EMAIL = "OAUTH_PENDING_EMAIL";
	public static final String PENDING_NAME = "OAUTH_PENDING_NAME";
	public static final String PENDING_PICTURE = "OAUTH_PENDING_PICTURE";

	private final OAuthUserProvisioningService provisioning;

	public CustomOAuth2UserService(OAuthUserProvisioningService provisioning) {
		this.provisioning = provisioning;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauthUser;
		try {
			oauthUser = super.loadUser(userRequest);
		}
		catch (OAuth2AuthenticationException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw oauthFailed(exception);
		}

		try {
			return provisioning.provision(oauthUser);
		}
		catch (RegistrationException exception) {
			throw oauthFailed(exception);
		}
		catch (RuntimeException exception) {
			throw oauthFailed(exception);
		}
	}

	private OAuth2AuthenticationException oauthFailed(Throwable cause) {
		String message = cause.getMessage();
		if (message == null || message.isBlank()) {
			message = "Sign-in with Google failed. Please try again.";
		}
		return new OAuth2AuthenticationException(new OAuth2Error("oauth_failed", message, null), cause);
	}

}
