package com.example.blog.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

	Optional<PasswordReset> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
