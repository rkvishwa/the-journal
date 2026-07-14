package com.example.blog.studio;

import com.example.blog.config.CurrentUser;
import com.example.blog.settings.SettingsController;
import com.example.blog.user.User;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = { StudioPostController.class, StudioCommentController.class, SettingsController.class })
public class StudioPanelModelAdvice {

	@ModelAttribute
	public void addStudioUser(Model model) {
		User user = CurrentUser.optionalUser();
		if (user != null) {
			model.addAttribute("studioUser", user);
		}
	}
}
