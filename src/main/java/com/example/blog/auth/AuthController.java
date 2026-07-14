package com.example.blog.auth;

import com.example.blog.config.CurrentUser;
import com.example.blog.user.EmailVerificationService;
import com.example.blog.user.PasswordResetService;
import com.example.blog.user.RegistrationException;
import com.example.blog.user.User;
import com.example.blog.user.UserService;
import com.example.blog.user.VerificationException;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

	public static final String PENDING_USER_ID = "PENDING_USER_ID";

	private final UserService users;
	private final EmailVerificationService emailVerification;
	private final PasswordResetService passwordReset;

	public AuthController(UserService users, EmailVerificationService emailVerification,
			PasswordResetService passwordReset) {
		this.users = users;
		this.emailVerification = emailVerification;
		this.passwordReset = passwordReset;
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("registerForm", new RegisterForm());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute RegisterForm registerForm, BindingResult bindingResult,
			HttpSession session, RedirectAttributes redirectAttributes) {
		if (!registerForm.passwordsMatch()) {
			bindingResult.rejectValue("confirmPassword", "match", "Passwords must match.");
		}
		if (bindingResult.hasErrors()) {
			return "auth/register";
		}
		try {
			User user = users.register(registerForm.getEmail(), registerForm.getUsername(), registerForm.getPassword(),
					registerForm.getUsername());
			emailVerification.sendVerification(user);
			session.setAttribute(PENDING_USER_ID, user.getId());
			redirectAttributes.addFlashAttribute("message", "Check your email for a verification code.");
			return "redirect:/verify-email";
		}
		catch (RegistrationException exception) {
			bindingResult.reject("register", exception.getMessage());
			return "auth/register";
		}
	}

	@GetMapping("/verify-email")
	public String verifyForm(HttpSession session, Model model) {
		if (session.getAttribute(PENDING_USER_ID) == null && !needsVerification(CurrentUser.optionalUser())) {
			return "redirect:/login";
		}
		model.addAttribute("verifyForm", new VerifyEmailForm());
		return "auth/verify-email";
	}

	@PostMapping("/verify-email")
	public String verify(@Valid @ModelAttribute VerifyEmailForm verifyForm, BindingResult bindingResult,
			HttpSession session, RedirectAttributes redirectAttributes) {
		Long userId = (Long) session.getAttribute(PENDING_USER_ID);
		User user = userId != null ? users.requireUser(userId) : CurrentUser.requireUser();
		if (bindingResult.hasErrors()) {
			return "auth/verify-email";
		}
		try {
			emailVerification.verify(user, verifyForm.getCode());
			users.markEmailVerified(user);
			session.removeAttribute(PENDING_USER_ID);
			redirectAttributes.addFlashAttribute("message", "Email verified. You can sign in now.");
			return "redirect:/login";
		}
		catch (VerificationException exception) {
			bindingResult.reject("code", exception.getMessage());
			return "auth/verify-email";
		}
	}

	@GetMapping("/forgot-password")
	public String forgotForm(Model model) {
		model.addAttribute("forgotForm", new ForgotPasswordForm());
		return "auth/forgot-password";
	}

	@PostMapping("/forgot-password")
	public String forgot(@Valid @ModelAttribute ForgotPasswordForm forgotForm, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "auth/forgot-password";
		}
		try {
			passwordReset.requestReset(forgotForm.getEmail());
		}
		catch (VerificationException ignored) {
		}
		redirectAttributes.addFlashAttribute("message", "If that email exists, a reset code was sent.");
		return "redirect:/reset-password?email=" + forgotForm.getEmail();
	}

	@GetMapping("/reset-password")
	public String resetForm(@RequestParam(required = false) String email, Model model) {
		ResetPasswordForm resetForm = new ResetPasswordForm();
		resetForm.setEmail(email);
		model.addAttribute("resetForm", resetForm);
		return "auth/reset-password";
	}

	@PostMapping("/reset-password")
	public String reset(@Valid @ModelAttribute ResetPasswordForm resetForm, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (!resetForm.passwordsMatch()) {
			bindingResult.rejectValue("confirmPassword", "match", "Passwords must match.");
		}
		if (bindingResult.hasErrors()) {
			return "auth/reset-password";
		}
		try {
			passwordReset.verifyAndReset(resetForm.getEmail(), resetForm.getCode(), resetForm.getPassword());
			redirectAttributes.addFlashAttribute("message", "Password updated. You can sign in now.");
			return "redirect:/login";
		}
		catch (VerificationException exception) {
			bindingResult.reject("code", exception.getMessage());
			return "auth/reset-password";
		}
	}

	private boolean needsVerification(User user) {
		return user != null && !user.isEmailVerified();
	}
}
