package com.example.blog.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.example.blog.config.BlogProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final BlogProperties properties;

	public MailService(JavaMailSender mailSender, BlogProperties properties) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	public void sendOtpEmail(String to, String subject, String body) {
		if (isMailConfigured()) {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(properties.mailFrom());
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
		}
		else {
			log.warn("[DEV MAIL] To: {} | Subject: {} | Body: {}", to, subject, body);
		}
	}

	public void sendVerificationCode(String email, String code) {
		sendOtpEmail(email, "Verify your email — " + properties.siteName(),
				"Your verification code is: " + code + "\n\nThis code expires in 15 minutes.");
	}

	public void sendPasswordResetCode(String email, String code) {
		sendOtpEmail(email, "Reset your password — " + properties.siteName(),
				"Your password reset code is: " + code + "\n\nThis code expires in 15 minutes.");
	}

	public void sendEmailChangeCode(String email, String code) {
		sendOtpEmail(email, "Confirm your new email — " + properties.siteName(),
				"Your email change verification code is: " + code + "\n\nThis code expires in 15 minutes.");
	}

	private boolean isMailConfigured() {
		return properties.mailFrom() != null && !properties.mailFrom().isBlank();
	}
}
