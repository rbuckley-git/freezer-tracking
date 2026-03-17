package io.learnsharegrow.freezertracker.api.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "password_salt", length = 32)
  private String passwordSalt;

  @Column(name = "failed_login_count", nullable = false)
  private Integer failedLoginCount = 0;

  @Column(name = "lockout_until")
  private Instant lockoutUntil;

  @Column(name = "is_admin", nullable = false)
  private boolean isAdmin;

  @Column(name = "is_super_admin", nullable = false)
  private boolean isSuperAdmin;

  @Column(name = "house_id", nullable = false)
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getPasswordSalt() {
    return passwordSalt;
  }

  public void setPasswordSalt(String passwordSalt) {
    this.passwordSalt = passwordSalt;
  }

  public Integer getFailedLoginCount() {
    return failedLoginCount;
  }

  public void setFailedLoginCount(Integer failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public Instant getLockoutUntil() {
    return lockoutUntil;
  }

  public void setLockoutUntil(Instant lockoutUntil) {
    this.lockoutUntil = lockoutUntil;
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
