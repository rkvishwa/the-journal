package com.example.blog.engagement;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import com.example.blog.post.Post;
import com.example.blog.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_reactions")
public class PostReaction {

	@EmbeddedId
	private PostReactionId id = new PostReactionId();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("postId")
	@JoinColumn(name = "post_id")
	private Post post;

	@Enumerated(EnumType.STRING)
	@Column(name = "reaction_type", length = 20, insertable = false, updatable = false)
	private ReactionType reactionType;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public PostReaction() {
	}

	public PostReaction(User user, Post post, ReactionType reactionType) {
		this.user = user;
		this.post = post;
		this.reactionType = reactionType;
		this.id = new PostReactionId(user.getId(), post.getId(), reactionType);
	}

	public PostReactionId getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Post getPost() {
		return post;
	}

	public ReactionType getReactionType() {
		return reactionType;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Embeddable
	public static class PostReactionId implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "post_id")
		private Long postId;

		@Enumerated(EnumType.STRING)
		@Column(name = "reaction_type")
		private ReactionType reactionType;

		public PostReactionId() {
		}

		public PostReactionId(Long userId, Long postId, ReactionType reactionType) {
			this.userId = userId;
			this.postId = postId;
			this.reactionType = reactionType;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof PostReactionId that)) {
				return false;
			}
			return Objects.equals(userId, that.userId)
					&& Objects.equals(postId, that.postId)
					&& reactionType == that.reactionType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, postId, reactionType);
		}
	}
}
