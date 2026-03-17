package io.learnsharegrow.freezertracker.api.config;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfigUpdateRequest {
  @NotBlank
  @NoUnsafeChars
  @Size(max = 255)
  private String value;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
