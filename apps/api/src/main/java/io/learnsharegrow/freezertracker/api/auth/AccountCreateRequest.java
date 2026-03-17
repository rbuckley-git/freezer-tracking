package io.learnsharegrow.freezertracker.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountCreateRequest {
  @NotBlank
  @Email
  @NoUnsafeChars
  @Size(max = 254)
  private String username;

  @NotBlank
  @Size(min = 12, max = 128)
  private String password;

  @JsonProperty("isAdmin")
  private boolean isAdmin;

  @JsonProperty("isSuperAdmin")
  private boolean isSuperAdmin;

  @NotBlank
  @Pattern(regexp = "^[0-9a-fA-F-]{36}$")
  private String houseId;

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

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean admin) {
    isAdmin = admin;
  }

  public boolean isSuperAdmin() {
    return isSuperAdmin;
  }

  public void setSuperAdmin(boolean superAdmin) {
    isSuperAdmin = superAdmin;
  }

  public String getHouseId() {
    return houseId;
  }

  public void setHouseId(String houseId) {
    this.houseId = houseId;
  }
}
