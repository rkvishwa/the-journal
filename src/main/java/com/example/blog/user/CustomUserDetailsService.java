package com.example.blog.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public CustomUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		User user = users.findByEmailOrUsername(identifier)
				.orElseThrow(() -> new UsernameNotFoundException("Account not found."));
		if (!user.isEnabled()) {
			throw new UsernameNotFoundException("Account is disabled.");
		}
		if (user.isLocked()) {
			throw new UsernameNotFoundException("Account is temporarily locked.");
		}
		return BlogUserDetails.from(user);
	}
}
