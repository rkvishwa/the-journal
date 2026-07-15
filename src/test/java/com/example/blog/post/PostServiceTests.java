package com.example.blog.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
	void searchPublishedMatchesTitleAndAuthor() {
		posts.create(form("Spring Boot Guide", PostStatus.PUBLISHED), author);
		posts.create(form("Other Topic", PostStatus.PUBLISHED), author);

		assertThat(posts.searchPublished("Spring", null, null, "latest"))
				.extracting(Post::getTitle)
				.containsExactly("Spring Boot Guide");
		assertThat(posts.searchPublished("creator", null, null, "latest"))
				.extracting(Post::getTitle)
				.contains("Spring Boot Guide", "Other Topic");
	}

	@Test
	void searchPublishedFiltersByDateAndSortOrder() {
		posts.create(form("Today Post", PostStatus.PUBLISHED), author);
		LocalDate today = LocalDate.now();

		assertThat(posts.searchPublished(null, today, today, "latest"))
				.extracting(Post::getTitle)
				.contains("Today Post");
		assertThat(posts.searchPublished(null, today.minusDays(7), today.minusDays(1), "latest")).isEmpty();

		var newest = posts.searchPublished(null, null, null, "latest");
		var oldest = posts.searchPublished(null, null, null, "oldest");
		assertThat(newest).isNotEmpty();
		assertThat(oldest).isNotEmpty();
		assertThat(newest.get(0).getPublishedAt()).isAfterOrEqualTo(newest.get(newest.size() - 1).getPublishedAt());
		assertThat(oldest.get(0).getPublishedAt()).isBeforeOrEqualTo(oldest.get(oldest.size() - 1).getPublishedAt());
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

	@Test
	void coverDisplayUrlUsesUploadedImageOrTitleFallback() {
		Post post = posts.create(form("Cover Story", PostStatus.PUBLISHED), author);

		assertThat(posts.coverDisplayUrl(post)).contains("/posts/cover.svg?title=");
		assertThat(posts.coverDisplayUrl(post)).contains("Cover");

		post.setCoverImageUrl("/uploads/cover.jpg");
		assertThat(posts.coverDisplayUrl(post)).isEqualTo("/uploads/cover.jpg");
		assertThat(post.getCoverDisplayUrl()).isEqualTo("/uploads/cover.jpg");
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
