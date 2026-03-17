package io.learnsharegrow.freezertracker.api.config;

import java.util.Optional;
import java.util.List;
import io.learnsharegrow.freezertracker.api.auth.AdminGuard;
import io.learnsharegrow.freezertracker.api.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {
  private final ConfigRepository configRepository;
  private final AdminGuard adminGuard;

  public ConfigService(ConfigRepository configRepository, AdminGuard adminGuard) {
    this.configRepository = configRepository;
    this.adminGuard = adminGuard;
  }

  @Transactional(readOnly = true)
  public Optional<String> getValue(String key) {
    return configRepository.findById(key).map(ConfigEntry::getValue);
  }

  @Transactional(readOnly = true)
  public int getIntValue(String key, int fallback) {
    return getValue(key).map(value -> {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException ex) {
        return fallback;
      }
    }).orElse(fallback);
  }

  @Transactional(readOnly = true)
  public List<ConfigResponse> listConfig() {
    adminGuard.requireSuperAdmin();
    return configRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ConfigResponse getConfig(String key) {
    adminGuard.requireSuperAdmin();
    ConfigEntry entry = configRepository.findById(key).orElseThrow(() -> NotFoundException.config(key));
    return toResponse(entry);
  }

  @Transactional
  public ConfigResponse createConfig(ConfigCreateRequest request) {
    adminGuard.requireSuperAdmin();
    ConfigEntry entry = new ConfigEntry();
    entry.setKey(request.getKey());
    entry.setValue(request.getValue());
    return toResponse(configRepository.save(entry));
  }

  @Transactional
  public ConfigResponse updateConfig(String key, ConfigUpdateRequest request) {
    adminGuard.requireSuperAdmin();
    ConfigEntry entry = configRepository.findById(key).orElseThrow(() -> NotFoundException.config(key));
    entry.setValue(request.getValue());
    return toResponse(configRepository.save(entry));
  }

  @Transactional
  public void deleteConfig(String key) {
    adminGuard.requireSuperAdmin();
    ConfigEntry entry = configRepository.findById(key).orElseThrow(() -> NotFoundException.config(key));
    configRepository.delete(entry);
  }

  private ConfigResponse toResponse(ConfigEntry entry) {
    ConfigResponse response = new ConfigResponse();
    response.setKey(entry.getKey());
    response.setValue(entry.getValue());
    return response;
  }
}
