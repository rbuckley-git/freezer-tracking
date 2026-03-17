package io.learnsharegrow.freezertracker.api.statistics;

public class ShelfStatisticsResponse {
  private int shelfNumber;
  private long itemCount;

  public int getShelfNumber() {
    return shelfNumber;
  }

  public void setShelfNumber(int shelfNumber) {
    this.shelfNumber = shelfNumber;
  }

  public long getItemCount() {
    return itemCount;
  }

  public void setItemCount(long itemCount) {
    this.itemCount = itemCount;
  }
}
