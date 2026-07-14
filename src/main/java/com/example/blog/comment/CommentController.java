package com.example.blog.comment;

import com.example.blog.config.CurrentUser;
import com.example.blog.post.PostService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CommentController {

	private final CommentService comments;
	private final PostService posts;

	public CommentController(CommentService comments, PostService posts) {
		this.comments = comments;
		this.posts = posts;
	}

	@PostMapping("/comments/posts/{id}")
	public String addComment(@PathVariable Long id, String body, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		comments.addComment(id, CurrentUser.requireUser(), body);
		redirectAttributes.addFlashAttribute("message", "Comment submitted for approval.");
		String referer = request.getHeader("Referer");
		return "redirect:" + (referer == null ? "/" : referer);
	}
}
