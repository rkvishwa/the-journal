package com.example.blog.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.user.User;
import com.example.blog.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTests {

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
	void slugifiesTitlesAndKeepsSlugsUniquePerAuthor() {
		PostForm first = form("My First Post", PostStatus.PUBLISHED);
		PostForm second = form("My First Post", PostStatus.PUBLISHED);

		Post firstPost = posts.create(first, author);
		Post secondPost = posts.create(second, author);

		assertThat(firstPost.getSlug()).isEqualTo("my-first-post");
		assertThat(secondPost.getSlug()).isEqualTo("my-first-post-2");
	}

	@Test
	void onlyPublishedPostsAppearInPublicList() {
		posts.create(form("Draft", PostStatus.DRAFT), author);
		posts.create(form("Published", PostStatus.PUBLISHED), author);
		PostForm scheduled = form("Scheduled", PostStatus.SCHEDULED);
		scheduled.setScheduledAt(java.time.LocalDateTime.now().plusDays(1));
		posts.create(scheduled, author);

		assertThat(posts.publishedPosts()).extracting(Post::getTitle).containsExactly("Published");
	}

	@Test
	void publishesScheduledPostsWhenDue() {
		PostForm scheduled = form("Future Post", PostStatus.SCHEDULED);
		scheduled.setScheduledAt(java.time.LocalDateTime.now().minusMinutes(1));

		Post post = posts.create(scheduled, author);

		assertThat(post.getStatus()).isEqualTo(PostStatus.SCHEDULED);
		assertThat(posts.publishDueScheduledPosts()).isEqualTo(1);
		assertThat(posts.getPost(post.getId()).getStatus()).isEqualTo(PostStatus.PUBLISHED);
		assertThat(posts.getPost(post.getId()).getPublishedAt()).isNotNull();
		assertThat(posts.getPost(post.getId()).getScheduledAt()).isNull();
		assertThat(posts.publishedPosts()).extracting(Post::getTitle).containsExactly("Future Post");
	}

	@Test
	void sanitizesUnsafeHtmlAndKeepsFormatting() {
		PostForm form = form("Formatted Post", PostStatus.PUBLISHED);
		form.setContentHtml("""
				<h2>Heading</h2>
				<p onclick="alert('bad')"><strong>Bold</strong> and <u>underline</u></p>
				<script>alert('bad')</script>
				<a href="javascript:alert('bad')">bad link</a>
				<table><tbody><tr><td colspan="2">Cell</td></tr></tbody></table>
				<figure class="image"><img src="/uploads/photo.png" alt="Photo" onerror="bad()"><figcaption>Caption</figcaption></figure>
				<img src="https://example.com/external.png" alt="External">
				""");

		Post post = posts.create(form, author);

		assertThat(post.getContentHtml())
				.contains("<h2>Heading</h2>")
				.contains("<strong>Bold</strong>")
				.contains("<u>underline</u>")
				.contains("<table>")
				.contains("<figure class=\"image\"><img src=\"/uploads/photo.png\" alt=\"Photo\" />")
				.contains("<figcaption>Caption</figcaption>")
				.doesNotContain("onclick")
				.doesNotContain("onerror")
				.doesNotContain("<script")
				.doesNotContain("javascript:")
				.doesNotContain("https://example.com/external.png");
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
