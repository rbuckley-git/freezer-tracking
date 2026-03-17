package io.learnsharegrow.freezertracker.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class LoginResponse {
  private UUID apiKey;
  private Instant apiKeyExpiry;
  @JsonProperty("isAdmin")
  private boolean isAdmin;
  @JsonProperty("isSuperAdmin")
  private boolean isSuperAdmin;

  public LoginResponse(UUID apiKey, Instant apiKeyExpiry, boolean isAdmin, boolean isSuperAdmin) {
    this.apiKey = apiKey;
    this.apiKeyExpiry = apiKeyExpiry;
    this.isAdmin = isAdmin;
    this.isSuperAdmin = isSuperAdmin;
  }

  public UUID getApiKey() {
    return apiKey;
  }

  public Instant getApiKeyExpiry() {
    return apiKeyExpiry;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public boolean isSuperAdmin() {
    return isSuperAdmin;
  }
}
