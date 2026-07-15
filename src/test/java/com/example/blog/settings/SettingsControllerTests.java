package com.example.blog.settings;

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
class SettingsControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Test
	@WithUserDetails("creator")
	void memberCanOpenSettingsPage() throws Exception {
		mockMvc.perform(get("/settings"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Profile")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Profile picture")));
	}

	@Test
	@WithUserDetails("creator")
	void memberCanUpdateProfile() throws Exception {
		mockMvc.perform(post("/settings/profile")
						.with(csrf())
						.param("displayName", "Test Creator")
						.param("username", "creator")
						.param("bio", "Testing"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/settings"));
	}

	@Test
	@WithUserDetails("creator")
	void memberCanUpdateAvatar() throws Exception {
		MockMultipartFile image = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/settings/profile/avatar")
						.file(image)
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/settings"));

		org.junit.jupiter.api.Assertions.assertNotNull(users.findByUsernameIgnoreCase("creator").orElseThrow().getAvatarUrl());
	}

	@Test
	@WithUserDetails("creator")
	void avatarUploadRejectsNonImages() throws Exception {
		MockMultipartFile text = new MockMultipartFile("avatar", "notes.txt", "text/plain", "not an image".getBytes());

		mockMvc.perform(multipart("/settings/profile/avatar")
						.file(text)
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/settings"));
	}

	@Test
	@WithUserDetails("admin")
	void adminCannotAccessStudioSettings() throws Exception {
		mockMvc.perform(get("/settings"))
				.andExpect(status().isForbidden());
	}
}
