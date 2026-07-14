package com.example.blog.config;

import com.example.blog.user.OAuthUserProvisioningService;
import com.example.blog.user.RegistrationException;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {

	private final OAuthUserProvisioningService provisioning;

	public CustomOidcUserService(OAuthUserProvisioningService provisioning) {
		this.provisioning = provisioning;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser;
		try {
			oidcUser = super.loadUser(userRequest);
		}
		catch (OAuth2AuthenticationException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw oauthFailed(exception);
		}

		try {
			return provisioning.provisionOidc(oidcUser);
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
