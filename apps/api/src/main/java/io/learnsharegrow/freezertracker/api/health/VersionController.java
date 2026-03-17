package io.learnsharegrow.freezertracker.api.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {
  private final String appVersion;

  public VersionController(@Value("${app.version}") String appVersion) {
    this.appVersion = appVersion;
  }

  @GetMapping("/version")
  public ResponseEntity<VersionResponse> version() {
    return ResponseEntity.ok(new VersionResponse(appVersion));
  }
}
