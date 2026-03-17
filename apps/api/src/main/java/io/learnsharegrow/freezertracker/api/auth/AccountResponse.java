package io.learnsharegrow.freezertracker.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class AccountResponse {
  private UUID id;
  private String username;
  @JsonProperty("isAdmin")
  private boolean isAdmin;
  @JsonProperty("isSuperAdmin")
  private boolean isSuperAdmin;
  private UUID houseId;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public UUID getHouseId() {
    return houseId;
  }

  public void setHouseId(UUID houseId) {
    this.houseId = houseId;
  }
}
