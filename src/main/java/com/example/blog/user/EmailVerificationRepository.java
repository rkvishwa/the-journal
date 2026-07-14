package com.example.blog.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	Optional<EmailVerification> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
