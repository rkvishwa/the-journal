package com.example.blog.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.config.CustomOAuth2UserService;
import com.example.blog.user.OAuthAccountRepository;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;
import com.example.blog.user.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OAuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private OAuthAccountRepository oauthAccounts;

	@Autowired
	private UserService userService;

	@Test
	void completeFormRequiresPendingSession() throws Exception {
		mockMvc.perform(get("/oauth/complete"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/register"));
	}

	@Test
	void completeFormShowsUsernamePicker() throws Exception {
		MockHttpSession session = pendingSession();

		mockMvc.perform(get("/oauth/complete").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Pick a username")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("newuser")));
	}

	@Test
	void completeCreatesAccountAndSignsIn() throws Exception {
		MockHttpSession session = pendingSession();

		mockMvc.perform(post("/oauth/complete")
						.session(session)
						.with(csrf())
						.param("username", "googleuser"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/studio"));

		org.assertj.core.api.Assertions.assertThat(users.findByUsernameIgnoreCase("googleuser")).isPresent();
		org.assertj.core.api.Assertions.assertThat(oauthAccounts.findByProviderAndProviderUserId("GOOGLE", "google-sub-1"))
				.isPresent();
	}

	@Test
	void completeResumesPreviouslyCreatedAccount() throws Exception {
		User existing = userService.createOAuthUser("returning@test.local", "returning", "Returning User");
		oauthAccounts.save(new com.example.blog.user.OAuthAccount(existing, "GOOGLE", "google-sub-returning",
				"returning@test.local"));

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(CustomOAuth2UserService.PENDING_SUB, "google-sub-returning");
		session.setAttribute(CustomOAuth2UserService.PENDING_EMAIL, "returning@test.local");
		session.setAttribute(CustomOAuth2UserService.PENDING_NAME, "Returning User");

		mockMvc.perform(get("/oauth/complete").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/studio"));
	}

	private MockHttpSession pendingSession() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(CustomOAuth2UserService.PENDING_SUB, "google-sub-1");
		session.setAttribute(CustomOAuth2UserService.PENDING_EMAIL, "newuser@test.local");
		session.setAttribute(CustomOAuth2UserService.PENDING_NAME, "New User");
		return session;
	}

}
