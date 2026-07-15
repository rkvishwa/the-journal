package com.example.blog.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.example.blog.config.BlogProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final BlogProperties properties;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder, BlogProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public User requireUser(Long id) {
		return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public User requireByUsername(String username) {
		return users.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new UserNotFoundException(username));
	}

	@Transactional
	public User register(String email, String username, String password, String displayName) {
		if (users.existsByEmailIgnoreCase(email)) {
			throw new RegistrationException("Email is already registered.");
		}
		if (users.existsByUsernameIgnoreCase(username)) {
			throw new RegistrationException("Username is already taken.");
		}
		User user = new User();
		user.setEmail(email.trim().toLowerCase());
		user.setUsername(username.trim());
		user.setDisplayName(displayName == null || displayName.isBlank() ? username.trim() : displayName.trim());
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setEmailVerified(false);
		user.setEnabled(true);
		user.setRoles(Set.of(UserRole.MEMBER));
		return users.save(user);
	}

	@Transactional
	public User createOAuthUser(String email, String username, String displayName) {
		User user = new User();
		user.setEmail(email.trim().toLowerCase());
		user.setUsername(username.trim());
		user.setDisplayName(displayName == null || displayName.isBlank() ? user.getUsername() : displayName.trim());
		user.setPasswordHash(null);
		user.setEmailVerified(true);
		user.setEnabled(true);
		user.setRoles(Set.of(UserRole.MEMBER));
		return users.save(user);
	}

	@Transactional
	public void updateAvatar(User user, String avatarUrl) {
		User managed = requireUser(user.getId());
		managed.setAvatarUrl(avatarUrl);
	}

	@Transactional
	public void updateProfile(User user, String displayName, String bio, String username) {
		if (!user.getUsername().equalsIgnoreCase(username) && users.existsByUsernameIgnoreCase(username)) {
			throw new RegistrationException("Username is already taken.");
		}
		user.setDisplayName(displayName.trim());
		user.setBio(bio == null ? null : bio.trim());
		user.setUsername(username.trim());
	}

	@Transactional
	public void updatePassword(User user, String rawPassword) {
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
	}

	@Transactional
	public void markEmailVerified(User user) {
		user.setEmailVerified(true);
		users.save(user);
	}

	@Transactional
	public void recordFailedLogin(User user) {
		user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
		if (user.getFailedLoginAttempts() >= 10) {
			user.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
			user.setFailedLoginAttempts(0);
		}
		users.save(user);
	}

	@Transactional
	public void resetFailedLogins(User user) {
		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		users.save(user);
	}

	@Transactional
	public void setEnabled(Long userId, boolean enabled) {
		User user = requireUser(userId);
		user.setEnabled(enabled);
		users.save(user);
	}

	@Transactional
	public void ensureBootstrapAdmin() {
		String username = properties.adminUsername().trim();
		User admin = users.findByUsernameIgnoreCase(username).orElseGet(User::new);
		boolean created = admin.getId() == null;
		admin.setUsername(username);
		admin.setEmail(properties.adminEmail().trim());
		admin.setDisplayName(username);
		admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
		admin.setEmailVerified(true);
		admin.setEnabled(true);
		admin.setRoles(new java.util.HashSet<>(Set.of(UserRole.ADMIN)));
		users.save(admin);
		if (created) {
			log.info("Created bootstrap admin account '{}'", username);
		}
	}

	@Transactional
	public User ensureMemberAccount(String email, String username, String password, String displayName) {
		return users.findByUsernameIgnoreCase(username).orElseGet(() -> {
			User user = register(email, username, password, displayName);
			user.setEmailVerified(true);
			user.setEnabled(true);
			user.setRoles(new java.util.HashSet<>(Set.of(UserRole.MEMBER)));
			return users.save(user);
		});
	}

	@Transactional
	public User repairOAuthLogin(User user) {
		user.setEnabled(true);
		user.setEmailVerified(true);
		if (!user.hasRole(UserRole.ADMIN) && !user.hasRole(UserRole.MEMBER)) {
			user.getRoles().add(UserRole.MEMBER);
		}
		return users.save(user);
	}

	public static String generateOtp() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}
}
