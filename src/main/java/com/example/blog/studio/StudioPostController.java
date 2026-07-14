package com.example.blog.studio;

import com.example.blog.config.CurrentUser;
import com.example.blog.post.PostForm;
import com.example.blog.post.PostService;
import com.example.blog.post.PostStatus;
import com.example.blog.user.User;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudioPostController {

	private final PostService posts;

	public StudioPostController(PostService posts) {
		this.posts = posts;
	}

	@GetMapping("/studio")
	public String dashboard(Model model) {
		User author = CurrentUser.requireUser();
		model.addAttribute("posts", posts.postsForAuthor(author));
		return "studio/list";
	}

	@GetMapping({ "/studio/write", "/studio/posts/new" })
	public String newPost(Model model) {
		model.addAttribute("postForm", new PostForm());
		model.addAttribute("statuses", PostStatus.values());
		model.addAttribute("action", "/studio/posts");
		return "studio/write";
	}

	@PostMapping("/studio/posts")
	public String create(@Valid @ModelAttribute PostForm postForm, BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("statuses", PostStatus.values());
			model.addAttribute("action", "/studio/posts");
			return "studio/write";
		}
		posts.create(postForm, CurrentUser.requireUser());
		redirectAttributes.addFlashAttribute("message", "Post created.");
		return "redirect:/studio";
	}

	@GetMapping("/studio/posts/{id}/edit")
	public String edit(@PathVariable Long id, Model model) {
		User author = CurrentUser.requireUser();
		posts.requireOwnedPost(id, author);
		model.addAttribute("postForm", posts.editForm(id));
		model.addAttribute("statuses", PostStatus.values());
		model.addAttribute("action", "/studio/posts/" + id);
		return "studio/write";
	}

	@PostMapping("/studio/posts/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute PostForm postForm, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("statuses", PostStatus.values());
			model.addAttribute("action", "/studio/posts/" + id);
			return "studio/write";
		}
		posts.update(id, postForm, CurrentUser.requireUser());
		redirectAttributes.addFlashAttribute("message", "Post saved.");
		return "redirect:/studio";
	}

	@PostMapping("/studio/posts/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		posts.delete(id, CurrentUser.requireUser());
		redirectAttributes.addFlashAttribute("message", "Post deleted.");
		return "redirect:/studio";
	}
}
