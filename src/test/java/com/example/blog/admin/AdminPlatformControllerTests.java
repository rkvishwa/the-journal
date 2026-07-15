package com.example.blog.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPlatformControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminAnalyticsService analytics;

	@Autowired
	private UserRepository users;

	@Test
	@WithUserDetails("admin")
	void adminCanOpenDashboard() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("users-chart")));
	}

	@Test
	@WithUserDetails("admin")
	void adminCanOpenAllPanelPages() throws Exception {
		mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/posts")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/comments")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/settings")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/profile")).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Connect Google")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Profile picture")));
	}

	@Test
	@WithUserDetails("admin")
	void adminCanUpdateProfile() throws Exception {
		mockMvc.perform(post("/admin/profile")
						.with(csrf())
						.param("displayName", "Platform Admin")
						.param("username", "admin"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/profile"));
	}

	@Test
	@WithUserDetails("admin")
	void adminCanRequestEmailChange() throws Exception {
		mockMvc.perform(post("/admin/profile/email")
						.with(csrf())
						.param("newEmail", "new-admin@test.local"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/profile"));
	}

	@Test
	@WithUserDetails("admin")
	void adminCanUpdateAvatar() throws Exception {
		MockMultipartFile image = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/admin/profile/avatar")
						.file(image)
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/profile"));

		org.junit.jupiter.api.Assertions.assertNotNull(users.findByUsernameIgnoreCase("admin").orElseThrow().getAvatarUrl());
	}

	@Test
	@WithUserDetails("admin")
	void analyticsServiceReturnsStats() {
		AdminAnalyticsService.AdminDashboardStats stats = analytics.dashboardStats();
		org.junit.jupiter.api.Assertions.assertTrue(stats.userCount() >= 1);
		org.junit.jupiter.api.Assertions.assertEquals(3, stats.postStatusLabels().size());
		org.junit.jupiter.api.Assertions.assertFalse(stats.userRegistrations().isEmpty());
	}
}
