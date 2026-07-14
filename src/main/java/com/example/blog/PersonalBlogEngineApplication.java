package com.example.blog;

import com.example.blog.config.BlogProperties;
import com.example.blog.config.DotEnvLoader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BlogProperties.class)
public class PersonalBlogEngineApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(PersonalBlogEngineApplication.class);
		application.setDefaultProperties(DotEnvLoader.load());
		application.run(args);
	}

}
