package com.example.blog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostCoverControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void coverSvgReturnsSvgWithTitle() throws Exception {
		mockMvc.perform(get("/posts/cover.svg").param("title", "Hello World"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/svg+xml;charset=UTF-8"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hello World")));
	}
}
