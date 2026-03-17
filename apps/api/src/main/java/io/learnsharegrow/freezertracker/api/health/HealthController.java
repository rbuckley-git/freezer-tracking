package io.learnsharegrow.freezertracker.api.health;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
  private final DataSource dataSource;
  private final String appVersion;

  public HealthController(DataSource dataSource, @Value("${app.version}") String appVersion) {
    this.dataSource = dataSource;
    this.appVersion = appVersion;
  }

  @GetMapping
  public ResponseEntity<HealthResponse> health() {
    boolean databaseOk = isDatabaseOk();
    return ResponseEntity.status(databaseOk ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
        .body(new HealthResponse(databaseOk, appVersion));
  }

  private boolean isDatabaseOk() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(2);
    } catch (Exception ex) {
      return false;
    }
  }
}
