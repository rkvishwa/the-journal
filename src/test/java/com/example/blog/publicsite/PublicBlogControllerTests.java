package com.example.blog.publicsite;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.post.Post;
import com.example.blog.post.PostForm;
import com.example.blog.post.PostService;
import com.example.blog.post.PostStatus;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicBlogControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostService posts;

	@Autowired
	private UserRepository users;

	private User author;

	@BeforeEach
	void setUp() {
		author = users.findByUsernameIgnoreCase("creator").orElseThrow();
	}

	@Test
	void publicHomeIsAvailable() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
	}

	@Test
	void homePageShowsSearchToolbar() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("catalog-search-form")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Search by title or author")));
	}

	@Test
	void homePageSearchByTitle() throws Exception {
		posts.create(form("Unique Alpha Post", PostStatus.PUBLISHED), author);
		posts.create(form("Unique Beta Post", PostStatus.PUBLISHED), author);

		mockMvc.perform(get("/").param("q", "Alpha"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Unique Alpha Post")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Unique Beta Post"))));
	}

	@Test
	void adminRequiresLogin() throws Exception {
		mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithUserDetails("creator")
	void authenticatedMemberCanCreatePost() throws Exception {
		mockMvc.perform(post("/studio/posts")
						.with(csrf())
						.param("title", "Controller Post")
						.param("excerpt", "Excerpt")
						.param("contentHtml", "<p>Body</p>")
						.param("keywords", "spring, writing")
						.param("status", "PUBLISHED"))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/posts/controller-post")).andExpect(status().is3xxRedirection());
	}

	@Test
	@WithUserDetails("creator")
	void authenticatedMemberCanCreateFormattedPost() throws Exception {
		mockMvc.perform(post("/studio/posts")
						.with(csrf())
						.param("title", "Formatted Controller Post")
						.param("excerpt", "Excerpt")
						.param("contentHtml", "<h2>Intro</h2><p><strong>Bold</strong><script>bad()</script></p>")
						.param("keywords", "spring, writing")
						.param("status", "PUBLISHED"))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/@creator/formatted-controller-post"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<h2>Intro</h2>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>Bold</strong>")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bad()"))));
	}

	@Test
	@WithUserDetails("creator")
	void authenticatedMemberCanOpenEditForm() throws Exception {
		Post post = posts.create(form("Editable Post", PostStatus.DRAFT), author);

		mockMvc.perform(get("/studio/posts/{id}/edit", post.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Editable Post")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("java, cms")));
	}

	@Test
	void draftPostIsNotPublic() throws Exception {
		posts.create(form("Private Draft", PostStatus.DRAFT), author);

		mockMvc.perform(get("/posts/private-draft")).andExpect(status().isNotFound());
	}

	@Test
	void missingPageReturns404() throws Exception {
		mockMvc.perform(get("/missing-page"))
				.andExpect(status().isNotFound());
	}

	@Test
	void custom404PageRenders() throws Exception {
		mockMvc.perform(get("/error")
						.accept(MediaType.TEXT_HTML)
						.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
				.andExpect(status().isNotFound())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Page not found.")));
	}

	@Test
	void custom500PageRenders() throws Exception {
		mockMvc.perform(get("/error")
						.accept(MediaType.TEXT_HTML)
						.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Something went wrong.")));
	}

	@Test
	@WithUserDetails("admin")
	void adminCannotAccessStudio() throws Exception {
		mockMvc.perform(get("/studio"))
				.andExpect(status().isForbidden())
				.andExpect(forwardedUrl("/error"));
	}

	@Test
	@WithUserDetails("creator")
	void creatorCannotAccessAdmin() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().isForbidden())
				.andExpect(forwardedUrl("/error"));
	}

	@Test
	@WithMockUser(roles = "MEMBER")
	void forbiddenRequestForAdminWithoutAdminRole() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().isForbidden())
				.andExpect(forwardedUrl("/error"));
	}

	@Test
	void custom403PageRenders() throws Exception {
		mockMvc.perform(get("/error")
						.accept(MediaType.TEXT_HTML)
						.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
				.andExpect(status().isForbidden())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Access denied.")));
	}

	private PostForm form(String title, PostStatus status) {
		PostForm form = new PostForm();
		form.setTitle(title);
		form.setExcerpt("A short excerpt");
		form.setContentHtml("<p>Body</p>");
		form.setKeywords("java, cms");
		form.setStatus(status);
		return form;
	}
}
