package io.learnsharegrow.freezertracker.api.config;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfigCreateRequest {
  @NotBlank
  @NoUnsafeChars
  @Size(max = 64)
  private String key;

  @NotBlank
  @NoUnsafeChars
  @Size(max = 255)
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
