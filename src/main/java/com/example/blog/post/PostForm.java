package com.example.blog.post;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostForm {

	@NotBlank
	@Size(max = 160)
	private String title;

	@NotBlank
	@Size(max = 320)
	private String excerpt;

	@NotBlank
	private String contentHtml;

	private String keywords = "";

	private String coverImageUrl;

	private boolean removeCover;

	private PostStatus status = PostStatus.DRAFT;

	private LocalDateTime scheduledAt;

	public static PostForm from(Post post) {
		PostForm form = new PostForm();
		form.setTitle(post.getTitle());
		form.setExcerpt(post.getExcerpt());
		form.setContentHtml(post.getContentHtml());
		form.setKeywords(post.getKeywordText());
		form.setCoverImageUrl(post.getCoverImageUrl());
		form.setStatus(post.getStatus());
		if (post.getScheduledAt() != null) {
			form.setScheduledAt(LocalDateTime.ofInstant(post.getScheduledAt(), ZoneId.systemDefault()));
		}
		return form;
	}

	@AssertTrue(message = "Choose a future date and time to schedule publishing.")
	public boolean isScheduleValid() {
		if (status != PostStatus.SCHEDULED) {
			return true;
		}
		return scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getExcerpt() {
		return excerpt;
	}

	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}

	public String getContentHtml() {
		return contentHtml;
	}

	public void setContentHtml(String contentHtml) {
		this.contentHtml = contentHtml;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getCoverImageUrl() {
		return coverImageUrl;
	}

	public void setCoverImageUrl(String coverImageUrl) {
		this.coverImageUrl = coverImageUrl;
	}

	public boolean isRemoveCover() {
		return removeCover;
	}

	public void setRemoveCover(boolean removeCover) {
		this.removeCover = removeCover;
	}

	public String getCoverDisplayUrl() {
		return PostCovers.displayUrl(title, coverImageUrl);
	}

	public PostStatus getStatus() {
		return status;
	}

	public void setStatus(PostStatus status) {
		this.status = status;
	}

	public LocalDateTime getScheduledAt() {
		return scheduledAt;
	}

	public void setScheduledAt(LocalDateTime scheduledAt) {
		this.scheduledAt = scheduledAt;
	}
}
