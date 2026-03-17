package io.learnsharegrow.freezertracker.api.freezers;

public class FreezerResponse {
  private Integer id;
  private String name;
  private Integer shelfCount;

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
}
