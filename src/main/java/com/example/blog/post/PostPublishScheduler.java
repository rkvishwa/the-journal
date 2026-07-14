package com.example.blog.post;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PostPublishScheduler {

	private final PostService posts;

	public PostPublishScheduler(PostService posts) {
		this.posts = posts;
	}

	@Scheduled(fixedRate = 60_000)
	public void publishDuePosts() {
		posts.publishDueScheduledPosts();
	}
}
