package io.learnsharegrow.freezertracker.api.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AccountApiKeyRepository extends JpaRepository<AccountApiKey, UUID> {
  Optional<AccountApiKey> findByApiKeyHashAndExpiresAtAfterAndRevokedAtIsNull(String apiKeyHash, Instant now);

  Optional<AccountApiKey> findByApiKeyAndExpiresAtAfterAndRevokedAtIsNull(UUID apiKey, Instant now);

  List<AccountApiKey> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

  @Modifying
  @Transactional
  @Query("update AccountApiKey key set key.lastUsedAt = :now where key.apiKey = :apiKey")
  void touchLastUsedAt(@Param("apiKey") UUID apiKey, @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("update AccountApiKey key set key.revokedAt = :now, key.expiresAt = :now where key.apiKey = :apiKey")
  void revoke(@Param("apiKey") UUID apiKey, @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("update AccountApiKey key set key.revokedAt = :now, key.expiresAt = :now "
      + "where key.accountId = :accountId and key.clientType = :clientType "
      + "and key.revokedAt is null and key.expiresAt > :now")
  void revokeActiveByAccountIdAndClientType(@Param("accountId") UUID accountId,
      @Param("clientType") ApiClientType clientType, @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("update AccountApiKey key set key.revokedAt = :now, key.expiresAt = :now "
      + "where key.accountId = :accountId and key.revokedAt is null")
  void revokeAllByAccountId(@Param("accountId") UUID accountId, @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("update AccountApiKey key set key.revokedAt = :now, key.expiresAt = :now "
      + "where key.accountId = :accountId and key.apiKey = :apiKey")
  void revokeByAccountIdAndApiKey(@Param("accountId") UUID accountId, @Param("apiKey") UUID apiKey,
      @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("delete from AccountApiKey key where key.revokedAt is not null or key.expiresAt <= :now")
  void deleteExpiredOrRevoked(@Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("delete from AccountApiKey key where key.accountId = :accountId")
  void deleteByAccountId(@Param("accountId") UUID accountId);
}
