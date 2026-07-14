package com.example.blog.engagement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReaction.PostReactionId> {

	long countByIdPostIdAndReactionType(Long postId, ReactionType reactionType);

	boolean existsByIdUserIdAndIdPostIdAndReactionType(Long userId, Long postId, ReactionType reactionType);

	void deleteByIdUserIdAndIdPostIdAndReactionType(Long userId, Long postId, ReactionType reactionType);
}
