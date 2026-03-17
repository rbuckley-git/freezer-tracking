package io.learnsharegrow.freezertracker.api.auth;

import java.time.Instant;
import java.util.UUID;

public class AccountApiKeyResponse {
  private UUID apiKey;
  private Instant createdAt;
  private Instant expiresAt;
  private Instant lastUsedAt;
  private Instant revokedAt;
  private String deviceLabel;
  private ApiClientType clientType;

  public UUID getApiKey() {
    return apiKey;
  }

  public void setApiKey(UUID apiKey) {
    this.apiKey = apiKey;
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

  public ApiClientType getClientType() {
    return clientType;
  }

  public void setClientType(ApiClientType clientType) {
    this.clientType = clientType;
  }
}
