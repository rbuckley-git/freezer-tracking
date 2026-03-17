package io.learnsharegrow.freezertracker.api.freezers;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FreezerCreateRequest {
  @NotBlank
  @NoUnsafeChars
  @Size(max = 255)
  private String name;

  @NotNull
  @Min(1)
  @Max(10)
  private Integer shelfCount;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getShelfCount() {
    return shelfCount;
  }

  public void setShelfCount(Integer shelfCount) {
    this.shelfCount = shelfCount;
  }
}
