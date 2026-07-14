package com.example.blog.user;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestUserBootstrap {

	private final UserService users;

	public TestUserBootstrap(UserService users) {
		this.users = users;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void bootstrap() {
		users.ensureMemberAccount("creator@test.local", "creator", "change-me-creator", "Test Creator");
	}

}
