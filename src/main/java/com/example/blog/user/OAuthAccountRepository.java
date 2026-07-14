package com.example.blog.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

	Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

	Optional<OAuthAccount> findByUserIdAndProvider(Long userId, String provider);

	boolean existsByUserIdAndProvider(Long userId, String provider);

	@Query("SELECT oa.user FROM OAuthAccount oa WHERE oa.provider = :provider AND oa.providerUserId = :providerUserId")
	Optional<User> findLinkedUser(@Param("provider") String provider, @Param("providerUserId") String providerUserId);

}
