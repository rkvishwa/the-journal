package com.example.blog.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_settings")
public class SiteSettings {

	@Id
	private Long id = 1L;

	@Column(name = "site_name", nullable = false, length = 80)
	private String siteName = "The Journal";

	@Column(nullable = false, length = 200)
	private String tagline = "Ideas worth keeping.";

	public Long getId() {
		return id;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String tagline) {
		this.tagline = tagline;
	}
}
