package io.learnsharegrow.freezertracker.api.items;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "items")
public class Item {
  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(nullable = false, length = 8)
  private String reference;

  @Column(name = "freeze_date", nullable = false)
  private LocalDate freezeDate;

  @Column(name = "best_before", nullable = false)
  private LocalDate bestBefore;

  @Column(nullable = false, length = 1024)
  private String description;

  @Column(name = "freezer_id", nullable = false)
  private Integer freezerId;

  @Column(name = "shelf_number", nullable = false)
  private Integer shelfNumber;

  @Column(length = 8)
  private String weight;

  @Column(length = 1)
  private String size;

  @Column(name = "house_id", nullable = false)
  private UUID houseId;

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

  public UUID getHouseId() {
    return houseId;
  }

  public void setHouseId(UUID houseId) {
    this.houseId = houseId;
  }
}
