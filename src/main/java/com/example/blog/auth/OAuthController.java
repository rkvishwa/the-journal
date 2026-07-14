package com.example.blog.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import com.example.blog.user.BlogUserDetails;
import com.example.blog.user.CustomUserDetailsService;
import com.example.blog.user.OAuthRegistrationService;
import com.example.blog.user.RegistrationException;
import com.example.blog.user.User;
import com.example.blog.user.UserRole;
import com.example.blog.user.UserService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class OAuthController {

	private final OAuthRegistrationService oauthRegistration;
	private final CustomUserDetailsService userDetailsService;
	private final UserService userService;

	public OAuthController(OAuthRegistrationService oauthRegistration, CustomUserDetailsService userDetailsService,
			UserService userService) {
		this.oauthRegistration = oauthRegistration;
		this.userDetailsService = userDetailsService;
		this.userService = userService;
	}

	@GetMapping("/oauth/complete")
	public String completeForm(HttpSession session, HttpServletRequest request, HttpServletResponse response,
			Model model) throws IOException {
		if (!oauthRegistration.hasPendingRegistration(session)) {
			return "redirect:/register";
		}
		if (oauthRegistration.findExistingAccount(session).isPresent()) {
			User user = oauthRegistration.resumeExistingAccount(session);
			signIn(user, request, response);
			userService.resetFailedLogins(user);
			if (user.hasRole(UserRole.ADMIN)) {
				return "redirect:/admin";
			}
			return "redirect:/studio";
		}
		if (!model.containsAttribute("oauthCompleteForm")) {
			OAuthCompleteForm form = new OAuthCompleteForm();
			form.setUsername(oauthRegistration.suggestUsername(session));
			model.addAttribute("oauthCompleteForm", form);
		}
		model.addAttribute("googleEmail", oauthRegistration.pendingEmail(session));
		model.addAttribute("googleName", oauthRegistration.pendingName(session));
		return "auth/oauth-complete";
	}

	@PostMapping("/oauth/complete")
	public String complete(@Valid @ModelAttribute OAuthCompleteForm oauthCompleteForm, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request, HttpServletResponse response, Model model)
			throws IOException {
		if (!oauthRegistration.hasPendingRegistration(session)) {
			return "redirect:/register";
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("googleEmail", oauthRegistration.pendingEmail(session));
			model.addAttribute("googleName", oauthRegistration.pendingName(session));
			return "auth/oauth-complete";
		}
		try {
			User user = oauthRegistration.completeRegistration(session, oauthCompleteForm.getUsername());
			signIn(user, request, response);
			userService.resetFailedLogins(user);
			if (user.hasRole(UserRole.ADMIN)) {
				return "redirect:/admin";
			}
			return "redirect:/studio";
		}
		catch (RegistrationException exception) {
			bindingResult.reject("username", exception.getMessage());
			model.addAttribute("googleEmail", oauthRegistration.pendingEmail(session));
			model.addAttribute("googleName", oauthRegistration.pendingName(session));
			return "auth/oauth-complete";
		}
	}

	private void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
		BlogUserDetails details = (BlogUserDetails) userDetailsService.loadUserByUsername(user.getUsername());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(details, null,
				details.getAuthorities());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		HttpSession session = request.getSession(true);
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
	}

}
