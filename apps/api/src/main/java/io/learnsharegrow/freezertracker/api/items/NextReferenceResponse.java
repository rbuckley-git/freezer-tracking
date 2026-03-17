package io.learnsharegrow.freezertracker.api.items;

public class NextReferenceResponse {
  private final String nextReference;

  public NextReferenceResponse(String nextReference) {
    this.nextReference = nextReference;
  }

  public String getNextReference() {
    return nextReference;
  }
}
