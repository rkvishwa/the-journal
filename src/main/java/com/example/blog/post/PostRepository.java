package com.example.blog.post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

	boolean existsByAuthorIdAndSlug(Long authorId, String slug);

	boolean existsByAuthorIdAndSlugAndIdNot(Long authorId, String slug, Long id);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findByStatusOrderByPublishedAtDescCreatedAtDesc(PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findTop12ByStatusOrderByPublishedAtDescCreatedAtDesc(PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findByStatusOrderByPublishedAtDesc(PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findAllByOrderByUpdatedAtDesc();

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);

	@EntityGraph(attributePaths = { "keywords", "author" })
	Optional<Post> findByAuthorUsernameIgnoreCaseAndSlugAndStatus(String username, String slug, PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	Optional<Post> findFirstBySlugAndStatus(String slug, PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findByAuthorIdAndStatusOrderByPublishedAtDescCreatedAtDesc(Long authorId, PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findByKeywordsSlugAndStatusOrderByPublishedAtDescCreatedAtDesc(String keywordSlug, PostStatus status);

	@EntityGraph(attributePaths = { "keywords", "author" })
	List<Post> findByAuthorIdInAndStatusOrderByPublishedAtDescCreatedAtDesc(List<Long> authorIds, PostStatus status);

	List<Post> findByStatusAndScheduledAtLessThanEqual(PostStatus status, Instant scheduledAt);

	long countByStatus(PostStatus status);

	List<Post> findByStatusAndPublishedAtGreaterThanEqual(PostStatus status, Instant since);

	@EntityGraph(attributePaths = { "author" })
	List<Post> findTop10ByOrderByUpdatedAtDesc();

	@EntityGraph(attributePaths = { "keywords", "author" })
	@Query("""
			SELECT p FROM Post p JOIN p.author a
			WHERE p.status = :status
			AND (:q IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(a.username) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(a.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
			AND (:from IS NULL OR p.publishedAt >= :from)
			AND (:to IS NULL OR p.publishedAt <= :to)
			ORDER BY p.publishedAt DESC, p.createdAt DESC
			""")
	List<Post> searchPublishedNewestFirst(@Param("status") PostStatus status, @Param("q") String q,
			@Param("from") Instant from, @Param("to") Instant to);

	@EntityGraph(attributePaths = { "keywords", "author" })
	@Query("""
			SELECT p FROM Post p JOIN p.author a
			WHERE p.status = :status
			AND (:q IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(a.username) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(a.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
			AND (:from IS NULL OR p.publishedAt >= :from)
			AND (:to IS NULL OR p.publishedAt <= :to)
			ORDER BY p.publishedAt ASC, p.createdAt ASC
			""")
	List<Post> searchPublishedOldestFirst(@Param("status") PostStatus status, @Param("q") String q,
			@Param("from") Instant from, @Param("to") Instant to);
}
