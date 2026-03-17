package io.learnsharegrow.freezertracker.api.freezers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "freezers")
public class Freezer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "shelf_count", nullable = false)
  private Integer shelfCount;

  @Column(name = "house_id", nullable = false)
  private UUID houseId;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

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

  public UUID getHouseId() {
    return houseId;
  }

  public void setHouseId(UUID houseId) {
    this.houseId = houseId;
  }
}
