package com.example.blog.engagement;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import com.example.blog.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "creator_subscriptions")
public class CreatorSubscription {

	@EmbeddedId
	private CreatorSubscriptionId id = new CreatorSubscriptionId();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("subscriberId")
	@JoinColumn(name = "subscriber_id")
	private User subscriber;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("creatorId")
	@JoinColumn(name = "creator_id")
	private User creator;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public CreatorSubscription() {
	}

	public CreatorSubscription(User subscriber, User creator) {
		this.subscriber = subscriber;
		this.creator = creator;
		this.id = new CreatorSubscriptionId(subscriber.getId(), creator.getId());
	}

	public CreatorSubscriptionId getId() {
		return id;
	}

	public User getSubscriber() {
		return subscriber;
	}

	public User getCreator() {
		return creator;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Embeddable
	public static class CreatorSubscriptionId implements Serializable {

		@Column(name = "subscriber_id")
		private Long subscriberId;

		@Column(name = "creator_id")
		private Long creatorId;

		public CreatorSubscriptionId() {
		}

		public CreatorSubscriptionId(Long subscriberId, Long creatorId) {
			this.subscriberId = subscriberId;
			this.creatorId = creatorId;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof CreatorSubscriptionId that)) {
				return false;
			}
			return Objects.equals(subscriberId, that.subscriberId) && Objects.equals(creatorId, that.creatorId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(subscriberId, creatorId);
		}
	}
}
