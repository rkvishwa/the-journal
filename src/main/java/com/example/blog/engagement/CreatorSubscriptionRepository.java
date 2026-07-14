package com.example.blog.engagement;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorSubscriptionRepository extends JpaRepository<CreatorSubscription, CreatorSubscription.CreatorSubscriptionId> {

	boolean existsByIdSubscriberIdAndIdCreatorId(Long subscriberId, Long creatorId);

	void deleteByIdSubscriberIdAndIdCreatorId(Long subscriberId, Long creatorId);

	long countByIdCreatorId(Long creatorId);

	@EntityGraph(attributePaths = "creator")
	List<CreatorSubscription> findBySubscriberIdOrderByCreatedAtDesc(Long subscriberId);

	List<CreatorSubscription> findByCreatorId(Long creatorId);
}
