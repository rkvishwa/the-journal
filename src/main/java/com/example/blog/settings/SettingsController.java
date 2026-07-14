package com.example.blog.settings;

import com.example.blog.config.CurrentUser;
import com.example.blog.config.CustomOAuth2UserService;
import com.example.blog.user.OAuthAccountLinkService;
import com.example.blog.user.RegistrationException;
import com.example.blog.user.User;
import com.example.blog.user.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {

	private final UserService users;
	private final OAuthAccountLinkService oauthLinks;
	private final PasswordEncoder passwordEncoder;

	public SettingsController(UserService users, OAuthAccountLinkService oauthLinks, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.oauthLinks = oauthLinks;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/settings")
	public String settings(Model model) {
		User user = CurrentUser.requireUser();
		ProfileForm profileForm = new ProfileForm();
		profileForm.setDisplayName(user.getDisplayName());
		profileForm.setBio(user.getBio());
		profileForm.setUsername(user.getUsername());
		model.addAttribute("profileForm", profileForm);
		model.addAttribute("changePasswordForm", new ChangePasswordForm());
		model.addAttribute("googleLinked", oauthLinks.isGoogleLinked(user));
		model.addAttribute("hasPassword", user.hasPassword());
		return "settings";
	}

	@PostMapping("/settings/profile")
	public String updateProfile(@Valid @ModelAttribute ProfileForm profileForm, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		if (bindingResult.hasErrors()) {
			return "settings";
		}
		try {
			users.updateProfile(user, profileForm.getDisplayName(), profileForm.getBio(), profileForm.getUsername());
			redirectAttributes.addFlashAttribute("message", "Profile updated.");
		}
		catch (RegistrationException exception) {
			bindingResult.reject("username", exception.getMessage());
			return "settings";
		}
		return "redirect:/settings";
	}

	@PostMapping("/settings/password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordForm changePasswordForm,
			BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		if (!changePasswordForm.passwordsMatch()) {
			bindingResult.rejectValue("confirmPassword", "match", "Passwords must match.");
		}
		if (user.hasPassword() && !passwordEncoder.matches(changePasswordForm.getCurrentPassword(),
				user.getPasswordHash())) {
			bindingResult.rejectValue("currentPassword", "invalid", "Current password is incorrect.");
		}
		if (bindingResult.hasErrors()) {
			return "settings";
		}
		users.updatePassword(user, changePasswordForm.getPassword());
		redirectAttributes.addFlashAttribute("message", "Password updated.");
		return "redirect:/settings";
	}

	@GetMapping("/settings/connect/google")
	public String connectGoogle(HttpSession session) {
		User user = CurrentUser.requireUser();
		session.setAttribute(CustomOAuth2UserService.LINK_MODE_SESSION_KEY, Boolean.TRUE);
		session.setAttribute("OAUTH_LINK_USER_ID", user.getId());
		session.setAttribute("OAUTH_RETURN_PATH", "/settings");
		return "redirect:/oauth2/authorization/google";
	}

	@PostMapping("/settings/disconnect/google")
	public String disconnectGoogle(RedirectAttributes redirectAttributes) {
		oauthLinks.unlinkGoogle(CurrentUser.requireUser());
		redirectAttributes.addFlashAttribute("message", "Google account disconnected.");
		return "redirect:/settings";
	}
}
