package com.example.blog.engagement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.example.blog.post.Post;
import com.example.blog.post.PostRepository;
import com.example.blog.post.PostStatus;
import com.example.blog.user.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EngagementService {

	private final SavedPostRepository savedPosts;
	private final PostReactionRepository reactions;
	private final CreatorSubscriptionRepository subscriptions;
	private final PostRepository posts;

	public EngagementService(SavedPostRepository savedPosts, PostReactionRepository reactions,
			CreatorSubscriptionRepository subscriptions, PostRepository posts) {
		this.savedPosts = savedPosts;
		this.reactions = reactions;
		this.subscriptions = subscriptions;
		this.posts = posts;
	}

	@Transactional
	public boolean toggleSave(User user, Long postId) {
		Post post = posts.findById(postId).orElseThrow();
		if (savedPosts.existsByIdUserIdAndIdPostId(user.getId(), postId)) {
			savedPosts.deleteByIdUserIdAndIdPostId(user.getId(), postId);
			return false;
		}
		savedPosts.save(new SavedPost(user, post));
		return true;
	}

	@Transactional
	public boolean toggleUseful(User user, Long postId) {
		Post post = posts.findById(postId).orElseThrow();
		if (reactions.existsByIdUserIdAndIdPostIdAndReactionType(user.getId(), postId, ReactionType.USEFUL)) {
			reactions.deleteByIdUserIdAndIdPostIdAndReactionType(user.getId(), postId, ReactionType.USEFUL);
			return false;
		}
		reactions.save(new PostReaction(user, post, ReactionType.USEFUL));
		return true;
	}

	@Transactional
	public boolean toggleSubscribe(User subscriber, User creator) {
		if (subscriber.getId().equals(creator.getId())) {
			throw new IllegalArgumentException("You cannot subscribe to yourself.");
		}
		if (subscriptions.existsByIdSubscriberIdAndIdCreatorId(subscriber.getId(), creator.getId())) {
			subscriptions.deleteByIdSubscriberIdAndIdCreatorId(subscriber.getId(), creator.getId());
			return false;
		}
		subscriptions.save(new CreatorSubscription(subscriber, creator));
		return true;
	}

	@Transactional(readOnly = true)
	public boolean isSaved(User user, Long postId) {
		return savedPosts.existsByIdUserIdAndIdPostId(user.getId(), postId);
	}

	@Transactional(readOnly = true)
	public boolean isUseful(User user, Long postId) {
		return reactions.existsByIdUserIdAndIdPostIdAndReactionType(user.getId(), postId, ReactionType.USEFUL);
	}

	@Transactional(readOnly = true)
	public boolean isSubscribed(User subscriber, User creator) {
		return subscriptions.existsByIdSubscriberIdAndIdCreatorId(subscriber.getId(), creator.getId());
	}

	@Transactional(readOnly = true)
	public long usefulCount(Long postId) {
		return reactions.countByIdPostIdAndReactionType(postId, ReactionType.USEFUL);
	}

	@Transactional(readOnly = true)
	public List<Post> savedPosts(User user) {
		return savedPosts.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
				.map(SavedPost::getPost)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Post> subscriptionFeed(User user) {
		List<Long> creatorIds = subscriptions.findBySubscriberIdOrderByCreatedAtDesc(user.getId()).stream()
				.map(subscription -> subscription.getCreator().getId())
				.collect(Collectors.toList());
		if (creatorIds.isEmpty()) {
			return Collections.emptyList();
		}
		return posts.findByAuthorIdInAndStatusOrderByPublishedAtDescCreatedAtDesc(creatorIds, PostStatus.PUBLISHED);
	}
}
