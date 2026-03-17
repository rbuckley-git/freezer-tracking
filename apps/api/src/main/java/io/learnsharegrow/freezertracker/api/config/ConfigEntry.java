package io.learnsharegrow.freezertracker.api.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "config")
public class ConfigEntry {
  @Id
  @Column(name = "config_key", nullable = false, length = 64)
  private String key;

  @Column(name = "config_value", nullable = false, length = 255)
  private String value;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
