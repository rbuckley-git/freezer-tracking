package io.learnsharegrow.freezertracker.api.items;

import java.time.LocalDate;
import java.util.UUID;

public class ItemResponse {
  private UUID id;
  private String reference;
  private LocalDate freezeDate;
  private LocalDate bestBefore;
  private String description;
  private Integer freezerId;
  private String freezerName;
  private Integer shelfNumber;
  private String weight;
  private String size;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

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

  public String getFreezerName() {
    return freezerName;
  }

  public void setFreezerName(String freezerName) {
    this.freezerName = freezerName;
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
