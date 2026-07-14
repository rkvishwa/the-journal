package com.example.blog.studio;

import com.example.blog.comment.CommentService;
import com.example.blog.config.CurrentUser;
import com.example.blog.user.User;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudioCommentController {

	private final CommentService comments;

	public StudioCommentController(CommentService comments) {
		this.comments = comments;
	}

	@org.springframework.web.bind.annotation.GetMapping("/studio/comments")
	public String pendingComments(Model model) {
		User creator = CurrentUser.requireUser();
		model.addAttribute("comments", comments.pendingForCreator(creator.getId()));
		return "studio/comments";
	}

	@PostMapping("/studio/comments/{id}/approve")
	public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		comments.approve(id, CurrentUser.requireUser());
		redirectAttributes.addFlashAttribute("message", "Comment approved.");
		return "redirect:/studio/comments";
	}

	@PostMapping("/studio/comments/{id}/reject")
	public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		comments.reject(id, CurrentUser.requireUser());
		redirectAttributes.addFlashAttribute("message", "Comment rejected.");
		return "redirect:/studio/comments";
	}
}
