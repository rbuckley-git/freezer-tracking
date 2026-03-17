package io.learnsharegrow.freezertracker.api.config;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ConfigController {
  private final ConfigService configService;

  public ConfigController(ConfigService configService) {
    this.configService = configService;
  }

  @GetMapping
  public List<ConfigResponse> listConfig() {
    return configService.listConfig();
  }

  @GetMapping("/{key}")
  public ConfigResponse getConfig(@PathVariable String key) {
    return configService.getConfig(key);
  }

  @PostMapping
  public ResponseEntity<ConfigResponse> createConfig(@Valid @RequestBody ConfigCreateRequest request) {
    ConfigResponse response = configService.createConfig(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{key}")
  public ConfigResponse updateConfig(@PathVariable String key, @Valid @RequestBody ConfigUpdateRequest request) {
    return configService.updateConfig(key, request);
  }

  @DeleteMapping("/{key}")
  public ResponseEntity<Void> deleteConfig(@PathVariable String key) {
    configService.deleteConfig(key);
    return ResponseEntity.noContent().build();
  }
}
