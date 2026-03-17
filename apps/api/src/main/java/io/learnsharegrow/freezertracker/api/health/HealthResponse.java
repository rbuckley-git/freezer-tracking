package io.learnsharegrow.freezertracker.api.health;

public class HealthResponse {
  private final boolean databaseOk;
  private final String version;

  public HealthResponse(boolean databaseOk, String version) {
    this.databaseOk = databaseOk;
    this.version = version;
  }

  public boolean isDatabaseOk() {
    return databaseOk;
  }

  public String getVersion() {
    return version;
  }
}
