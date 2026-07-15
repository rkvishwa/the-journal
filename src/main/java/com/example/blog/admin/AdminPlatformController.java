package com.example.blog.admin;

import com.example.blog.comment.CommentService;
import com.example.blog.config.CurrentUser;
import com.example.blog.config.CustomOAuth2UserService;
import com.example.blog.upload.ImageStorageService;
import com.example.blog.post.PostRepository;
import com.example.blog.post.PostStatus;
import com.example.blog.settings.ChangePasswordForm;
import com.example.blog.user.EmailVerificationService;
import com.example.blog.user.OAuthAccountLinkService;
import com.example.blog.user.RegistrationException;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;
import com.example.blog.user.UserService;
import com.example.blog.user.VerificationException;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPlatformController {

	private final UserRepository users;
	private final UserService userService;
	private final PostRepository posts;
	private final CommentService comments;
	private final SiteSettingsService siteSettings;
	private final AdminAnalyticsService analytics;
	private final EmailVerificationService emailVerification;
	private final OAuthAccountLinkService oauthLinks;
	private final PasswordEncoder passwordEncoder;
	private final ImageStorageService images;

	public AdminPlatformController(UserRepository users, UserService userService, PostRepository posts,
			CommentService comments, SiteSettingsService siteSettings, AdminAnalyticsService analytics,
			EmailVerificationService emailVerification, OAuthAccountLinkService oauthLinks,
			PasswordEncoder passwordEncoder, ImageStorageService images) {
		this.users = users;
		this.userService = userService;
		this.posts = posts;
		this.comments = comments;
		this.siteSettings = siteSettings;
		this.analytics = analytics;
		this.emailVerification = emailVerification;
		this.oauthLinks = oauthLinks;
		this.passwordEncoder = passwordEncoder;
		this.images = images;
	}

	@ModelAttribute
	public void addAdminUser(Model model) {
		User user = CurrentUser.optionalUser();
		if (user != null) {
			model.addAttribute("adminUser", user);
		}
	}

	@GetMapping("/admin")
	public String dashboard(Model model) {
		AdminAnalyticsService.AdminDashboardStats stats = analytics.dashboardStats();
		model.addAttribute("stats", stats);
		model.addAttribute("userCount", stats.userCount());
		model.addAttribute("postCount", stats.postCount());
		model.addAttribute("publishedCount", stats.publishedCount());
		model.addAttribute("pendingComments", stats.pendingComments());
		return "admin/dashboard";
	}

	@GetMapping("/admin/users")
	public String users(Model model) {
		model.addAttribute("users", users.findAllByOrderByCreatedAtDesc());
		return "admin/users";
	}

	@PostMapping("/admin/users/{id}/toggle")
	public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		var user = userService.requireUser(id);
		userService.setEnabled(id, !user.isEnabled());
		redirectAttributes.addFlashAttribute("message", "User updated.");
		return "redirect:/admin/users";
	}

	@GetMapping("/admin/posts")
	public String posts(Model model) {
		model.addAttribute("posts", posts.findAllByOrderByUpdatedAtDesc());
		return "admin/posts";
	}

	@PostMapping("/admin/posts/{id}/unpublish")
	public String unpublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		posts.findById(id).ifPresent(post -> {
			post.setStatus(PostStatus.DRAFT);
			post.setPublishedAt(null);
			posts.save(post);
		});
		redirectAttributes.addFlashAttribute("message", "Post unpublished.");
		return "redirect:/admin/posts";
	}

	@GetMapping("/admin/comments")
	public String comments(Model model) {
		model.addAttribute("comments", comments.pendingAll());
		return "admin/comments";
	}

	@PostMapping("/admin/comments/{id}/approve")
	public String approveComment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		comments.approveAsAdmin(id);
		redirectAttributes.addFlashAttribute("message", "Comment approved.");
		return "redirect:/admin/comments";
	}

	@PostMapping("/admin/comments/{id}/reject")
	public String rejectComment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		comments.rejectAsAdmin(id);
		redirectAttributes.addFlashAttribute("message", "Comment rejected.");
		return "redirect:/admin/comments";
	}

	@GetMapping("/admin/settings")
	public String siteSettings(Model model) {
		SiteSettings settings = siteSettings.settings();
		model.addAttribute("siteName", settings.getSiteName());
		model.addAttribute("tagline", settings.getTagline());
		return "admin/site-settings";
	}

	@PostMapping("/admin/settings")
	public String updateSiteSettings(String siteName, String tagline, RedirectAttributes redirectAttributes) {
		siteSettings.update(siteName, tagline);
		redirectAttributes.addFlashAttribute("message", "Site settings saved.");
		return "redirect:/admin/settings";
	}

	@GetMapping("/admin/profile")
	public String profile(Model model) {
		populateProfileModel(model, CurrentUser.requireUser());
		return "admin/profile";
	}

	private void populateProfileModel(Model model, User user) {
		if (!model.containsAttribute("profileForm")) {
			AdminProfileForm profileForm = new AdminProfileForm();
			profileForm.setDisplayName(user.getDisplayName());
			profileForm.setUsername(user.getUsername());
			model.addAttribute("profileForm", profileForm);
		}
		if (!model.containsAttribute("changePasswordForm")) {
			model.addAttribute("changePasswordForm", new ChangePasswordForm());
		}
		if (!model.containsAttribute("changeEmailForm")) {
			model.addAttribute("changeEmailForm", new ChangeEmailForm());
		}
		if (!model.containsAttribute("verifyEmailChangeForm")) {
			model.addAttribute("verifyEmailChangeForm", new VerifyEmailChangeForm());
		}
		model.addAttribute("currentEmail", user.getEmail());
		model.addAttribute("googleLinked", oauthLinks.isGoogleLinked(user));
		model.addAttribute("hasPassword", user.hasPassword());
	}

	@PostMapping("/admin/profile/avatar")
	public String updateAvatar(@RequestParam("avatar") MultipartFile avatar, RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		try {
			String avatarUrl = images.store(avatar).url();
			userService.updateAvatar(user, avatarUrl);
			user.setAvatarUrl(avatarUrl);
			redirectAttributes.addFlashAttribute("message", "Profile picture updated.");
		}
		catch (ResponseStatusException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getReason());
		}
		return "redirect:/admin/profile";
	}

	@PostMapping("/admin/profile")
	public String updateProfile(@Valid @ModelAttribute AdminProfileForm profileForm, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		if (bindingResult.hasErrors()) {
			populateProfileModel(model, user);
			return "admin/profile";
		}
		try {
			userService.updateProfile(user, profileForm.getDisplayName(), user.getBio(), profileForm.getUsername());
			redirectAttributes.addFlashAttribute("message", "Profile updated.");
		}
		catch (RegistrationException exception) {
			bindingResult.rejectValue("username", "username", exception.getMessage());
			populateProfileModel(model, user);
			return "admin/profile";
		}
		return "redirect:/admin/profile";
	}

	@PostMapping("/admin/profile/password")
	public String changePassword(@Valid @ModelAttribute ChangePasswordForm changePasswordForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		if (!changePasswordForm.passwordsMatch()) {
			bindingResult.rejectValue("confirmPassword", "match", "Passwords must match.");
		}
		if (user.hasPassword() && !passwordEncoder.matches(changePasswordForm.getCurrentPassword(),
				user.getPasswordHash())) {
			bindingResult.rejectValue("currentPassword", "invalid", "Current password is incorrect.");
		}
		if (bindingResult.hasErrors()) {
			populateProfileModel(model, user);
			return "admin/profile";
		}
		userService.updatePassword(user, changePasswordForm.getPassword());
		redirectAttributes.addFlashAttribute("message", "Password updated.");
		return "redirect:/admin/profile";
	}

	@PostMapping("/admin/profile/email")
	public String requestEmailChange(@Valid @ModelAttribute ChangeEmailForm changeEmailForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		if (bindingResult.hasErrors()) {
			populateProfileModel(model, user);
			return "admin/profile";
		}
		try {
			emailVerification.requestEmailChange(user, changeEmailForm.getNewEmail());
			redirectAttributes.addFlashAttribute("message",
					"Verification code sent to " + changeEmailForm.getNewEmail().trim() + ".");
			redirectAttributes.addFlashAttribute("emailChangePending", true);
		}
		catch (RegistrationException | VerificationException exception) {
			bindingResult.rejectValue("newEmail", "email", exception.getMessage());
			populateProfileModel(model, user);
			return "admin/profile";
		}
		return "redirect:/admin/profile";
	}

	@PostMapping("/admin/profile/email/verify")
	public String verifyEmailChange(@Valid @ModelAttribute VerifyEmailChangeForm verifyEmailChangeForm,
			BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("emailChangePending", true);
			return "redirect:/admin/profile";
		}
		try {
			emailVerification.confirmEmailChange(user, verifyEmailChangeForm.getCode());
			redirectAttributes.addFlashAttribute("message", "Email updated successfully.");
		}
		catch (VerificationException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
			redirectAttributes.addFlashAttribute("emailChangePending", true);
		}
		return "redirect:/admin/profile";
	}

	@GetMapping("/admin/profile/connect/google")
	public String connectGoogle(HttpSession session) {
		User user = CurrentUser.requireUser();
		session.setAttribute(CustomOAuth2UserService.LINK_MODE_SESSION_KEY, Boolean.TRUE);
		session.setAttribute("OAUTH_LINK_USER_ID", user.getId());
		session.setAttribute("OAUTH_RETURN_PATH", "/admin/profile");
		return "redirect:/oauth2/authorization/google";
	}

	@PostMapping("/admin/profile/disconnect/google")
	public String disconnectGoogle(RedirectAttributes redirectAttributes) {
		User user = CurrentUser.requireUser();
		try {
			oauthLinks.unlinkGoogle(user);
			redirectAttributes.addFlashAttribute("message", "Google account disconnected.");
		}
		catch (RegistrationException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/admin/profile";
	}
}
