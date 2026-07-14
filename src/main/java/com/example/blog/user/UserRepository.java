package com.example.blog.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByUsernameIgnoreCase(String username);

	@Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
	Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);

	List<User> findAllByOrderByCreatedAtDesc();

	long countByEnabledTrue();

	List<User> findByCreatedAtGreaterThanEqual(Instant since);

	List<User> findTop10ByOrderByCreatedAtDesc();
}
