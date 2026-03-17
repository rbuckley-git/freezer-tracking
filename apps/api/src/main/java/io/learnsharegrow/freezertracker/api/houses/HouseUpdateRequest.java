package io.learnsharegrow.freezertracker.api.houses;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HouseUpdateRequest {
  @NotBlank
  @NoUnsafeChars
  @Size(max = 255)
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
