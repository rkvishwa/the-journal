package com.example.blog.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

	private final EmailVerificationRepository verifications;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final MailService mailService;

	public EmailVerificationService(EmailVerificationRepository verifications, UserRepository users,
			PasswordEncoder passwordEncoder, MailService mailService) {
		this.verifications = verifications;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.mailService = mailService;
	}

	@Transactional
	public void sendVerification(User user) {
		String code = UserService.generateOtp();
		EmailVerification verification = new EmailVerification(user, passwordEncoder.encode(code),
				Instant.now().plus(15, ChronoUnit.MINUTES));
		verifications.save(verification);
		mailService.sendVerificationCode(user.getEmail(), code);
	}

	@Transactional
	public boolean verify(User user, String code) {
		EmailVerification verification = verifications.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
				.orElseThrow(() -> new VerificationException("No active verification code."));
		if (verification.isExpired()) {
			throw new VerificationException("Verification code has expired.");
		}
		if (!passwordEncoder.matches(code, verification.getCodeHash())) {
			throw new VerificationException("Invalid verification code.");
		}
		verification.setUsed(true);
		if (verification.isEmailChange()) {
			user.setEmail(verification.getPendingEmail().trim().toLowerCase());
		}
		user.setEmailVerified(true);
		return true;
	}

	@Transactional
	public void requestEmailChange(User user, String newEmail) {
		String normalized = newEmail.trim().toLowerCase();
		if (user.getEmail().equalsIgnoreCase(normalized)) {
			throw new VerificationException("That is already your email address.");
		}
		if (users.existsByEmailIgnoreCase(normalized)) {
			throw new RegistrationException("Email is already registered.");
		}
		String code = UserService.generateOtp();
		EmailVerification verification = new EmailVerification(user, passwordEncoder.encode(code),
				Instant.now().plus(15, ChronoUnit.MINUTES), normalized);
		verifications.save(verification);
		mailService.sendEmailChangeCode(normalized, code);
	}

	@Transactional
	public void confirmEmailChange(User user, String code) {
		verify(user, code);
		users.save(user);
	}
}
