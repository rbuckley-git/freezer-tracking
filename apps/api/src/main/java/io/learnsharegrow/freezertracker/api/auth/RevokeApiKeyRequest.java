package io.learnsharegrow.freezertracker.api.auth;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RevokeApiKeyRequest {
  @NotBlank
  @NoUnsafeChars
  @Pattern(regexp = "^[0-9a-fA-F-]{36}$")
  private String apiKey;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
