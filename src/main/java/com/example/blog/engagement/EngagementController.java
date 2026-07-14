package com.example.blog.engagement;

import com.example.blog.config.CurrentUser;
import com.example.blog.user.User;
import com.example.blog.user.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class EngagementController {

	private final EngagementService engagement;
	private final UserService users;

	public EngagementController(EngagementService engagement, UserService users) {
		this.engagement = engagement;
		this.users = users;
	}

	@GetMapping("/saved")
	public String saved(Model model) {
		model.addAttribute("posts", engagement.savedPosts(CurrentUser.requireUser()));
		return "saved";
	}

	@GetMapping("/subscriptions")
	public String subscriptions(Model model) {
		model.addAttribute("posts", engagement.subscriptionFeed(CurrentUser.requireUser()));
		return "subscriptions";
	}

	@PostMapping("/engagement/posts/{id}/save")
	public String toggleSave(@PathVariable Long id, HttpServletRequest request) {
		engagement.toggleSave(CurrentUser.requireUser(), id);
		return "redirect:" + backUrl(request);
	}

	@PostMapping("/engagement/posts/{id}/useful")
	public String toggleUseful(@PathVariable Long id, HttpServletRequest request) {
		engagement.toggleUseful(CurrentUser.requireUser(), id);
		return "redirect:" + backUrl(request);
	}

	@PostMapping("/engagement/creators/{username}/subscribe")
	public String toggleSubscribe(@PathVariable String username, HttpServletRequest request) {
		User subscriber = CurrentUser.requireUser();
		User creator = users.requireByUsername(username);
		engagement.toggleSubscribe(subscriber, creator);
		return "redirect:" + backUrl(request);
	}

	private String backUrl(HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		return referer == null ? "/" : referer;
	}
}
