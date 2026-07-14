package com.example.blog.user;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthAccountLinkService {

	public static final String GOOGLE = "GOOGLE";

	private final OAuthAccountRepository oauthAccounts;
	private final UserRepository users;

	public OAuthAccountLinkService(OAuthAccountRepository oauthAccounts, UserRepository users) {
		this.oauthAccounts = oauthAccounts;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public boolean isGoogleLinked(User user) {
		return oauthAccounts.existsByUserIdAndProvider(user.getId(), GOOGLE);
	}

	@Transactional
	public void linkGoogle(User user, String providerUserId, String email) {
		Optional<OAuthAccount> existingLink = oauthAccounts.findByProviderAndProviderUserId(GOOGLE, providerUserId);
		if (existingLink.isPresent()) {
			User owner = existingLink.get().getUser();
			if (owner != null && !owner.getId().equals(user.getId())) {
				throw new RegistrationException("This Google account is already linked to another user.");
			}
			return;
		}
		oauthAccounts.findByUserIdAndProvider(user.getId(), GOOGLE).ifPresentOrElse(existing -> {
		}, () -> oauthAccounts.save(new OAuthAccount(user, GOOGLE, providerUserId, email)));
	}

	@Transactional
	public void unlinkGoogle(User user) {
		if (!user.hasPassword()) {
			throw new RegistrationException("Set a password before disconnecting Google.");
		}
		oauthAccounts.findByUserIdAndProvider(user.getId(), GOOGLE).ifPresent(oauthAccounts::delete);
	}

	@Transactional(readOnly = true)
	public User findUserByGoogleId(String providerUserId) {
		return oauthAccounts.findLinkedUser(GOOGLE, providerUserId).orElse(null);
	}
}
