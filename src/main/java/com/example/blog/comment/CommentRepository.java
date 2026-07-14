package com.example.blog.comment;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@EntityGraph(attributePaths = { "author" })
	List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommentStatus status);

	@EntityGraph(attributePaths = { "author", "post", "post.author" })
	List<Comment> findByPostAuthorIdAndStatusOrderByCreatedAtDesc(Long postAuthorId, CommentStatus status);

	long countByStatus(CommentStatus status);

	long countByPostAuthorIdAndStatus(Long postAuthorId, CommentStatus status);

	@EntityGraph(attributePaths = { "author", "post" })
	List<Comment> findByStatusOrderByCreatedAtDesc(CommentStatus status);
}
