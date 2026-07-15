package com.example.blog.publicsite;

import com.example.blog.comment.CommentService;
import com.example.blog.config.CurrentUser;
import com.example.blog.engagement.EngagementService;
import com.example.blog.post.Post;
import com.example.blog.post.PostService;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class PublicBlogController {

	private final PostService posts;
	private final UserRepository users;
	private final EngagementService engagement;
	private final CommentService comments;

	public PublicBlogController(PostService posts, UserRepository users, EngagementService engagement,
			CommentService comments) {
		this.posts = posts;
		this.users = users;
		this.engagement = engagement;
		this.comments = comments;
	}

	@GetMapping("/")
	public String home(@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "latest") String sort, Model model) {
		boolean searching = isNotBlank(q) || from != null || to != null || "oldest".equalsIgnoreCase(sort);
		model.addAttribute("searchQuery", q == null ? "" : q);
		model.addAttribute("searchFrom", from);
		model.addAttribute("searchTo", to);
		model.addAttribute("searchSort", sort);
		model.addAttribute("searching", searching);
		if (searching) {
			model.addAttribute("searchResults", posts.searchPublished(q, from, to, sort));
		}
		else {
			model.addAttribute("latestPosts", posts.latestPublished(12));
			model.addAttribute("discoverPosts", posts.discoverPublished(12));
		}
		return "index";
	}

	private static boolean isNotBlank(String value) {
		return value != null && !value.isBlank();
	}

	@GetMapping("/posts/{slug}")
	public RedirectView legacyPost(@PathVariable String slug) {
		Post post = posts.publishedPost(slug).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		return new RedirectView("/@" + post.getAuthor().getUsername() + "/" + post.getSlug(), true);
	}

	@GetMapping("/@{username}/{slug}")
	public String post(@PathVariable String username, @PathVariable String slug, Model model) {
		Post post = posts.publishedPost(username, slug)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		populatePostPage(model, post);
		return "post";
	}

	@GetMapping("/@{username}")
	public String creatorProfile(@PathVariable String username, Model model) {
		User creator = users.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		model.addAttribute("creator", creator);
		model.addAttribute("posts", posts.postsByAuthor(creator));
		User current = CurrentUser.optionalUser();
		model.addAttribute("subscribed", current != null && engagement.isSubscribed(current, creator));
		return "creator";
	}

	@GetMapping("/creators")
	public String creators(Model model) {
		model.addAttribute("creators", users.findAllByOrderByCreatedAtDesc());
		return "creators";
	}

	@GetMapping("/keywords/{slug}")
	public String keyword(@PathVariable String slug, Model model) {
		model.addAttribute("keywordSlug", slug);
		model.addAttribute("keywordName", posts.keywordDisplayName(slug));
		model.addAttribute("posts", posts.postsForKeyword(slug));
		return "keyword";
	}

	private void populatePostPage(Model model, Post post) {
		model.addAttribute("post", post);
		model.addAttribute("comments", comments.approvedComments(post.getId()));
		model.addAttribute("usefulCount", engagement.usefulCount(post.getId()));
		User current = CurrentUser.optionalUser();
		if (current != null) {
			model.addAttribute("saved", engagement.isSaved(current, post.getId()));
			model.addAttribute("useful", engagement.isUseful(current, post.getId()));
			model.addAttribute("subscribed", engagement.isSubscribed(current, post.getAuthor()));
		}
	}
}
