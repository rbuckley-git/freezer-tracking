package io.learnsharegrow.freezertracker.api.health;

public class VersionResponse {
  private final String version;

  public VersionResponse(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }
}
