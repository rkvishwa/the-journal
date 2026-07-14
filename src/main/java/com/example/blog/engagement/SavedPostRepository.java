package com.example.blog.engagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPost.SavedPostId> {

	boolean existsByIdUserIdAndIdPostId(Long userId, Long postId);

	void deleteByIdUserIdAndIdPostId(Long userId, Long postId);

	@EntityGraph(attributePaths = { "post", "post.author", "post.keywords" })
	List<SavedPost> findByUserIdOrderByCreatedAtDesc(Long userId);
}
