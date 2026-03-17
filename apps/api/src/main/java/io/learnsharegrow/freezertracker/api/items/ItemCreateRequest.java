package io.learnsharegrow.freezertracker.api.items;

import io.learnsharegrow.freezertracker.api.common.NoUnsafeChars;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ItemCreateRequest {
  @NotBlank
  @Pattern(regexp = "^[0-9]{8}$")
  @Size(min = 8, max = 8)
  private String reference;

  @NotNull
  private LocalDate freezeDate;

  @NotNull
  private LocalDate bestBefore;

  @NotBlank
  @NoUnsafeChars
  @Size(max = 1024)
  private String description;

  @NotNull
  @Min(1)
  private Integer freezerId;

  @NotNull
  @Min(1)
  private Integer shelfNumber;

  @NoUnsafeChars
  @Size(max = 8)
  private String weight;

  @Pattern(regexp = "^(S|M|L)$")
  private String size;

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public LocalDate getFreezeDate() {
    return freezeDate;
  }

  public void setFreezeDate(LocalDate freezeDate) {
    this.freezeDate = freezeDate;
  }

  public LocalDate getBestBefore() {
    return bestBefore;
  }

  public void setBestBefore(LocalDate bestBefore) {
    this.bestBefore = bestBefore;
  }

  @AssertTrue(message = "Best before must be after freeze date")
  public boolean isBestBeforeAfterFreezeDate() {
    if (freezeDate == null || bestBefore == null) {
      return true;
    }
    return bestBefore.isAfter(freezeDate);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getFreezerId() {
    return freezerId;
  }

  public void setFreezerId(Integer freezerId) {
    this.freezerId = freezerId;
  }

  public Integer getShelfNumber() {
    return shelfNumber;
  }

  public void setShelfNumber(Integer shelfNumber) {
    this.shelfNumber = shelfNumber;
  }

  public String getWeight() {
    return weight;
  }

  public void setWeight(String weight) {
    this.weight = weight;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }
}
