package com.example.blog.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

	private final PasswordResetRepository resets;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final MailService mailService;

	public PasswordResetService(PasswordResetRepository resets, UserRepository users, PasswordEncoder passwordEncoder,
			MailService mailService) {
		this.resets = resets;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.mailService = mailService;
	}

	@Transactional
	public void requestReset(String email) {
		User user = users.findByEmailIgnoreCase(email.trim())
				.orElseThrow(() -> new VerificationException("No account found with that email."));
		String code = UserService.generateOtp();
		PasswordReset reset = new PasswordReset(user, passwordEncoder.encode(code),
				Instant.now().plus(15, ChronoUnit.MINUTES));
		resets.save(reset);
		mailService.sendPasswordResetCode(user.getEmail(), code);
	}

	@Transactional
	public User verifyAndReset(String email, String code, String newPassword) {
		User user = users.findByEmailIgnoreCase(email.trim())
				.orElseThrow(() -> new VerificationException("No account found with that email."));
		PasswordReset reset = resets.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
				.orElseThrow(() -> new VerificationException("No active reset code."));
		if (reset.isExpired()) {
			throw new VerificationException("Reset code has expired.");
		}
		if (!passwordEncoder.matches(code, reset.getCodeHash())) {
			throw new VerificationException("Invalid reset code.");
		}
		reset.setUsed(true);
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		return user;
	}
}
