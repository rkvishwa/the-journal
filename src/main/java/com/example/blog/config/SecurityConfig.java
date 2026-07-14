package com.example.blog.config;

import jakarta.servlet.RequestDispatcher;

import com.example.blog.user.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService userDetailsService,
			BlogAuthenticationSuccessHandler successHandler, BlogAuthenticationFailureHandler failureHandler,
			OAuth2LoginSuccessHandler oauthSuccessHandler, OAuth2LoginFailureHandler oauthFailureHandler,
			CustomOAuth2UserService oauthUserService, CustomOidcUserService oidcUserService,
			AuthRateLimitFilter rateLimitFilter, EmailVerifiedFilter emailVerifiedFilter) throws Exception {
		return http
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/", "/posts/**", "/@/**", "/keywords/**", "/uploads/**",
								"/css/**", "/js/**", "/images/**", "/error", "/error/**",
								"/register", "/login", "/verify-email", "/forgot-password", "/reset-password",
								"/oauth/complete", "/oauth2/**", "/login/oauth2/code/**")
							.permitAll()
						.requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
						.requestMatchers("/studio/**", "/settings/**", "/saved", "/subscriptions",
								"/creators", "/creators/**", "/engagement/**", "/comments/**")
							.hasRole("MEMBER")
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, denied) -> {
					request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
					response.setStatus(403);
					request.getRequestDispatcher("/error").forward(request, response);
				}))
				.userDetailsService(userDetailsService)
				.formLogin(login -> login
						.loginPage("/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.successHandler(successHandler)
						.failureHandler(failureHandler)
						.permitAll())
				.oauth2Login(oauth -> oauth
						.loginPage("/login")
						.userInfoEndpoint(userInfo -> userInfo
								.userService(oauthUserService)
								.oidcUserService(oidcUserService))
						.successHandler(oauthSuccessHandler)
						.failureHandler(oauthFailureHandler))
				.logout(logout -> logout.logoutSuccessUrl("/").permitAll())
				.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
