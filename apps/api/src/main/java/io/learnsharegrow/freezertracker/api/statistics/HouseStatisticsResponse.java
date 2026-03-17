package io.learnsharegrow.freezertracker.api.statistics;

import java.util.List;
import java.util.UUID;

public class HouseStatisticsResponse {
  private UUID houseId;
  private String houseName;
  private int freezerCount;
  private List<FreezerStatisticsResponse> freezers;

  public UUID getHouseId() {
    return houseId;
  }

  public void setHouseId(UUID houseId) {
    this.houseId = houseId;
  }

  public String getHouseName() {
    return houseName;
  }

  public void setHouseName(String houseName) {
    this.houseName = houseName;
  }

  public int getFreezerCount() {
    return freezerCount;
  }

  public void setFreezerCount(int freezerCount) {
    this.freezerCount = freezerCount;
  }

  public List<FreezerStatisticsResponse> getFreezers() {
    return freezers;
  }

  public void setFreezers(List<FreezerStatisticsResponse> freezers) {
    this.freezers = freezers;
  }
}
