package io.learnsharegrow.freezertracker.api.statistics;

import java.util.List;

public class FreezerStatisticsResponse {
  private int freezerId;
  private String freezerName;
  private int shelfCount;
  private List<ShelfStatisticsResponse> shelves;

  public int getFreezerId() {
    return freezerId;
  }

  public void setFreezerId(int freezerId) {
    this.freezerId = freezerId;
  }

  public String getFreezerName() {
    return freezerName;
  }

  public void setFreezerName(String freezerName) {
    this.freezerName = freezerName;
  }

  public int getShelfCount() {
    return shelfCount;
  }

  public void setShelfCount(int shelfCount) {
    this.shelfCount = shelfCount;
  }

  public List<ShelfStatisticsResponse> getShelves() {
    return shelves;
  }

  public void setShelves(List<ShelfStatisticsResponse> shelves) {
    this.shelves = shelves;
  }
}
