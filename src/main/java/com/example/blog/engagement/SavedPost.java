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
@Table(name = "saved_posts")
public class SavedPost {

	@EmbeddedId
	private SavedPostId id = new SavedPostId();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("postId")
	@JoinColumn(name = "post_id")
	private Post post;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public SavedPost() {
	}

	public SavedPost(User user, Post post) {
		this.user = user;
		this.post = post;
		this.id = new SavedPostId(user.getId(), post.getId());
	}

	public SavedPostId getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Post getPost() {
		return post;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Embeddable
	public static class SavedPostId implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "post_id")
		private Long postId;

		public SavedPostId() {
		}

		public SavedPostId(Long userId, Long postId) {
			this.userId = userId;
			this.postId = postId;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof SavedPostId that)) {
				return false;
			}
			return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, postId);
		}
	}
}
