package com.example.blog.studio;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudioUploadControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithUserDetails("creator")
	void authenticatedMemberCanUploadImage() throws Exception {
		MockMultipartFile image = new MockMultipartFile("upload", "sample.png", "image/png", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/studio/uploads/images")
						.file(image)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url", startsWith("/uploads/")));
	}

	@Test
	@WithUserDetails("creator")
	void uploadRejectsNonImages() throws Exception {
		MockMultipartFile text = new MockMultipartFile("upload", "notes.txt", "text/plain", "not an image".getBytes());

		mockMvc.perform(multipart("/studio/uploads/images")
						.file(text)
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}
}
