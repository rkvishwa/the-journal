package com.example.blog.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.blog.comment.CommentRepository;
import com.example.blog.comment.CommentStatus;
import com.example.blog.engagement.PostReactionRepository;
import com.example.blog.post.Post;
import com.example.blog.post.PostRepository;
import com.example.blog.post.PostStatus;
import com.example.blog.user.User;
import com.example.blog.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnalyticsService {

	private static final int CHART_DAYS = 30;

	private final UserRepository users;
	private final PostRepository posts;
	private final CommentRepository comments;
	private final PostReactionRepository reactions;

	public AdminAnalyticsService(UserRepository users, PostRepository posts, CommentRepository comments,
			PostReactionRepository reactions) {
		this.users = users;
		this.posts = posts;
		this.comments = comments;
		this.reactions = reactions;
	}

	@Transactional(readOnly = true)
	public AdminDashboardStats dashboardStats() {
		Instant since = Instant.now().minus(CHART_DAYS, ChronoUnit.DAYS);
		return new AdminDashboardStats(
				users.count(),
				users.countByEnabledTrue(),
				posts.count(),
				posts.countByStatus(PostStatus.PUBLISHED),
				posts.countByStatus(PostStatus.DRAFT),
				posts.countByStatus(PostStatus.SCHEDULED),
				comments.countByStatus(CommentStatus.PENDING),
				comments.countByStatus(CommentStatus.APPROVED),
				comments.countByStatus(CommentStatus.REJECTED),
				reactions.count(),
				dailyUserRegistrations(since),
				dailyPublishedPosts(since),
				labelsFrom(postStatusBreakdown()),
				valuesFrom(postStatusBreakdown()),
				labelsFrom(commentStatusBreakdown()),
				valuesFrom(commentStatusBreakdown()),
				users.findTop10ByOrderByCreatedAtDesc(),
				posts.findTop10ByOrderByUpdatedAtDesc());
	}

	private List<String> labelsFrom(Map<String, Long> breakdown) {
		return new ArrayList<>(breakdown.keySet());
	}

	private List<Long> valuesFrom(Map<String, Long> breakdown) {
		return new ArrayList<>(breakdown.values());
	}

	private List<DailyCount> dailyUserRegistrations(Instant since) {
		Map<LocalDate, Long> counts = users.findByCreatedAtGreaterThanEqual(since).stream()
				.collect(Collectors.groupingBy(this::toUtcDate, Collectors.counting()));
		return fillDailySeries(since, counts);
	}

	private List<DailyCount> dailyPublishedPosts(Instant since) {
		Map<LocalDate, Long> counts = posts.findByStatusAndPublishedAtGreaterThanEqual(PostStatus.PUBLISHED, since)
				.stream()
				.collect(Collectors.groupingBy(post -> toUtcDate(post.getPublishedAt()), Collectors.counting()));
		return fillDailySeries(since, counts);
	}

	private List<DailyCount> fillDailySeries(Instant since, Map<LocalDate, Long> counts) {
		LocalDate start = toUtcDate(since);
		LocalDate end = LocalDate.now(ZoneOffset.UTC);
		List<DailyCount> series = new ArrayList<>();
		for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
			series.add(new DailyCount(day, counts.getOrDefault(day, 0L)));
		}
		return series;
	}

	private LocalDate toUtcDate(Instant instant) {
		return instant.atZone(ZoneOffset.UTC).toLocalDate();
	}

	private LocalDate toUtcDate(User user) {
		return toUtcDate(user.getCreatedAt());
	}

	private Map<String, Long> postStatusBreakdown() {
		Map<String, Long> breakdown = new LinkedHashMap<>();
		breakdown.put("Published", posts.countByStatus(PostStatus.PUBLISHED));
		breakdown.put("Draft", posts.countByStatus(PostStatus.DRAFT));
		breakdown.put("Scheduled", posts.countByStatus(PostStatus.SCHEDULED));
		return breakdown;
	}

	private Map<String, Long> commentStatusBreakdown() {
		Map<String, Long> breakdown = new LinkedHashMap<>();
		breakdown.put("Pending", comments.countByStatus(CommentStatus.PENDING));
		breakdown.put("Approved", comments.countByStatus(CommentStatus.APPROVED));
		breakdown.put("Rejected", comments.countByStatus(CommentStatus.REJECTED));
		return breakdown;
	}

	public record AdminDashboardStats(
			long userCount,
			long activeUserCount,
			long postCount,
			long publishedCount,
			long draftCount,
			long scheduledCount,
			long pendingComments,
			long approvedComments,
			long rejectedComments,
			long totalReactions,
			List<DailyCount> userRegistrations,
			List<DailyCount> publishedPosts,
			List<String> postStatusLabels,
			List<Long> postStatusValues,
			List<String> commentStatusLabels,
			List<Long> commentStatusValues,
			List<User> recentUsers,
			List<Post> recentPosts) {
	}
}
