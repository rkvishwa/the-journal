package com.example.blog.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final int MAX_OTP_REQUESTS = 5;
	private static final int MAX_LOGIN_ATTEMPTS = 10;
	private static final long WINDOW_SECONDS = 900;

	private final Map<String, WindowCounter> otpCounters = new ConcurrentHashMap<>();
	private final Map<String, WindowCounter> loginCounters = new ConcurrentHashMap<>();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		String method = request.getMethod();

		if ("POST".equals(method)) {
			if (isOtpPath(path) && isLimited(otpCounters, otpKey(request))) {
				response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests. Try again later.");
				return;
			}
			if ("/login".equals(path) && isLimited(loginCounters, clientIp(request))) {
				response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many login attempts. Try again later.");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isOtpPath(String path) {
		return "/register".equals(path) || "/forgot-password".equals(path) || "/verify-email".equals(path)
				|| "/reset-password".equals(path);
	}

	private String otpKey(HttpServletRequest request) {
		String email = request.getParameter("email");
		return email == null ? clientIp(request) : email.toLowerCase();
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private boolean isLimited(Map<String, WindowCounter> counters, String key) {
		WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter());
		synchronized (counter) {
			Instant now = Instant.now();
			if (counter.windowStart == null || counter.windowStart.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
				counter.windowStart = now;
				counter.count = 0;
			}
			counter.count++;
			int limit = counters == otpCounters ? MAX_OTP_REQUESTS : MAX_LOGIN_ATTEMPTS;
			return counter.count > limit;
		}
	}

	private static final class WindowCounter {
		private Instant windowStart;
		private int count;
	}
}
