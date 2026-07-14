package com.example.blog.post;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

	Optional<Keyword> findBySlug(String slug);
}
