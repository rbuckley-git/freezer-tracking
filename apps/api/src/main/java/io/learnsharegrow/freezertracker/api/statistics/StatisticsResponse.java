package io.learnsharegrow.freezertracker.api.statistics;

import java.util.List;

public class StatisticsResponse {
  private int houseCount;
  private List<HouseStatisticsResponse> houses;

  public int getHouseCount() {
    return houseCount;
  }

  public void setHouseCount(int houseCount) {
    this.houseCount = houseCount;
  }

  public List<HouseStatisticsResponse> getHouses() {
    return houses;
  }

  public void setHouses(List<HouseStatisticsResponse> houses) {
    this.houses = houses;
  }
}
