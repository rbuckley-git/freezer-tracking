package io.learnsharegrow.freezertracker.api.freezers;

public class FreezerMapper {
  public Freezer toEntity(FreezerCreateRequest request) {
    Freezer freezer = new Freezer();
    freezer.setName(request.getName());
    freezer.setShelfCount(request.getShelfCount());
    return freezer;
  }

  public void updateEntity(Freezer freezer, FreezerUpdateRequest request) {
    freezer.setName(request.getName());
    freezer.setShelfCount(request.getShelfCount());
  }

  public FreezerResponse toResponse(Freezer freezer) {
    FreezerResponse response = new FreezerResponse();
    response.setId(freezer.getId());
    response.setName(freezer.getName());
    response.setShelfCount(freezer.getShelfCount());
    return response;
  }
}
