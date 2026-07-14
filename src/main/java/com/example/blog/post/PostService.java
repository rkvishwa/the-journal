package com.example.blog.post;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.example.blog.user.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final Pattern NON_LATIN = Pattern.compile("[^\\w\\s-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s_-]+");

	private final PostRepository posts;
	private final KeywordRepository keywords;
	private final BlogHtmlSanitizer sanitizer;

	public PostService(PostRepository posts, KeywordRepository keywords, BlogHtmlSanitizer sanitizer) {
		this.posts = posts;
		this.keywords = keywords;
		this.sanitizer = sanitizer;
	}

	@Transactional(readOnly = true)
	public List<Post> publishedPosts() {
		return posts.findByStatusOrderByPublishedAtDescCreatedAtDesc(PostStatus.PUBLISHED);
	}

	@Transactional(readOnly = true)
	public List<Post> latestPublished(int limit) {
		return posts.findTop12ByStatusOrderByPublishedAtDescCreatedAtDesc(PostStatus.PUBLISHED).stream()
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Post> discoverPublished(int limit) {
		List<Post> all = posts.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED);
		List<Post> shuffled = new ArrayList<>(all);
		Collections.shuffle(shuffled);
		return shuffled.stream().limit(limit).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Post> postsForKeyword(String keywordSlug) {
		return posts.findByKeywordsSlugAndStatusOrderByPublishedAtDescCreatedAtDesc(keywordSlug, PostStatus.PUBLISHED);
	}

	@Transactional(readOnly = true)
	public String keywordDisplayName(String keywordSlug) {
		return keywords.findBySlug(keywordSlug).map(Keyword::getName).orElse(keywordSlug);
	}

	@Transactional(readOnly = true)
	public Optional<Post> publishedPost(String slug) {
		return posts.findFirstBySlugAndStatus(slug, PostStatus.PUBLISHED);
	}

	@Transactional(readOnly = true)
	public Optional<Post> publishedPost(String username, String slug) {
		return posts.findByAuthorUsernameIgnoreCaseAndSlugAndStatus(username, slug, PostStatus.PUBLISHED);
	}

	@Transactional(readOnly = true)
	public List<Post> postsByAuthor(User author) {
		return posts.findByAuthorIdAndStatusOrderByPublishedAtDescCreatedAtDesc(author.getId(), PostStatus.PUBLISHED);
	}

	@Transactional(readOnly = true)
	public List<Post> allPostsForAdmin() {
		return posts.findAllByOrderByUpdatedAtDesc();
	}

	@Transactional(readOnly = true)
	public List<Post> postsForAuthor(User author) {
		return posts.findByAuthorIdOrderByUpdatedAtDesc(author.getId());
	}

	@Transactional(readOnly = true)
	public Post getPost(Long id) {
		return posts.findById(id).orElseThrow(() -> new PostNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public PostForm editForm(Long id) {
		return PostForm.from(getPost(id));
	}

	@Transactional
	public Post create(PostForm form, User author) {
		Post post = new Post();
		post.setAuthor(author);
		applyForm(post, form);
		post.setSlug(uniqueSlug(form.getTitle(), author.getId(), null));
		return posts.save(post);
	}

	@Transactional
	public Post update(Long id, PostForm form, User author) {
		Post post = requireOwnedPost(id, author);
		applyForm(post, form);
		post.setSlug(uniqueSlug(form.getTitle(), author.getId(), id));
		return posts.save(post);
	}

	@Transactional
	public void delete(Long id, User author) {
		posts.delete(requireOwnedPost(id, author));
	}

	@Transactional
	public void unpublish(Long id) {
		Post post = getPost(id);
		post.setStatus(PostStatus.DRAFT);
		post.setPublishedAt(null);
		posts.save(post);
	}

	@Transactional
	public int publishDueScheduledPosts() {
		List<Post> duePosts = posts.findByStatusAndScheduledAtLessThanEqual(PostStatus.SCHEDULED, Instant.now());
		for (Post post : duePosts) {
			post.setStatus(PostStatus.PUBLISHED);
			post.setPublishedAt(Instant.now());
			post.setScheduledAt(null);
		}
		return duePosts.size();
	}

	public Post requireOwnedPost(Long id, User author) {
		Post post = getPost(id);
		if (!post.getAuthor().getId().equals(author.getId())) {
			throw new PostAccessDeniedException(id);
		}
		return post;
	}

	public String slugify(String input) {
		String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD);
		String slug = WHITESPACE.matcher(NON_LATIN.matcher(normalized).replaceAll("").trim()).replaceAll("-");
		slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("^-|-$", "");
		return slug.isBlank() ? "post" : slug;
	}

	private void applyForm(Post post, PostForm form) {
		PostStatus previousStatus = post.getStatus();
		post.setTitle(form.getTitle().trim());
		post.setExcerpt(form.getExcerpt().trim());
		post.setContentHtml(sanitizer.sanitize(form.getContentHtml().trim()));
		PostStatus newStatus = form.getStatus() == null ? PostStatus.DRAFT : form.getStatus();
		post.setStatus(newStatus);
		if (newStatus == PostStatus.SCHEDULED) {
			post.setScheduledAt(toInstant(form.getScheduledAt()));
			post.setPublishedAt(null);
		}
		else if (newStatus == PostStatus.PUBLISHED) {
			post.setScheduledAt(null);
			if (previousStatus != PostStatus.PUBLISHED) {
				post.setPublishedAt(Instant.now());
			}
		}
		else {
			post.setScheduledAt(null);
			post.setPublishedAt(null);
		}
		post.setKeywords(resolveKeywords(form.getKeywords()));
	}

	private Instant toInstant(LocalDateTime localDateTime) {
		return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
	}

	private Set<Keyword> resolveKeywords(String keywordText) {
		if (keywordText == null || keywordText.isBlank()) {
			return new LinkedHashSet<>();
		}
		return Arrays.stream(keywordText.split(","))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.map(this::findOrCreateKeyword)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Keyword findOrCreateKeyword(String name) {
		String slug = slugify(name);
		return keywords.findBySlug(slug).orElseGet(() -> keywords.save(new Keyword(name, slug)));
	}

	private String uniqueSlug(String title, Long authorId, Long currentPostId) {
		String base = slugify(title);
		String candidate = base;
		int suffix = 2;
		while (slugExists(authorId, candidate, currentPostId)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

	private boolean slugExists(Long authorId, String slug, Long currentPostId) {
		return currentPostId == null
				? posts.existsByAuthorIdAndSlug(authorId, slug)
				: posts.existsByAuthorIdAndSlugAndIdNot(authorId, slug, currentPostId);
	}
}
