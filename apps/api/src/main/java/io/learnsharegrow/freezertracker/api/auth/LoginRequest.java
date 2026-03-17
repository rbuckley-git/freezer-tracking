package io.learnsharegrow.freezertracker.api.auth;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
  @NotBlank
  @Email
  @NoUnsafeChars
  @Size(max = 254)
  private String username;

  @NotBlank
  private String password;

  @NotNull
  private ApiClientType clientType;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ApiClientType getClientType() {
    return clientType;
  }

  public void setClientType(ApiClientType clientType) {
    this.clientType = clientType;
  }
}
