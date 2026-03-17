package io.learnsharegrow.freezertracker.api.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_api_keys")
public class AccountApiKey {
  @Id
  @Column(name = "api_key", nullable = false)
  private UUID apiKey;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "device_label", length = 100)
  private String deviceLabel;

  @Column(name = "api_key_hash", length = 64)
  private String apiKeyHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "client_type", nullable = false, length = 16)
  private ApiClientType clientType;

  public UUID getApiKey() {
    return apiKey;
  }

  public void setApiKey(UUID apiKey) {
    this.apiKey = apiKey;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public String getDeviceLabel() {
    return deviceLabel;
  }

  public void setDeviceLabel(String deviceLabel) {
    this.deviceLabel = deviceLabel;
  }

  public String getApiKeyHash() {
    return apiKeyHash;
  }

  public void setApiKeyHash(String apiKeyHash) {
    this.apiKeyHash = apiKeyHash;
  }

  public ApiClientType getClientType() {
    return clientType;
  }

  public void setClientType(ApiClientType clientType) {
    this.clientType = clientType;
  }
}
