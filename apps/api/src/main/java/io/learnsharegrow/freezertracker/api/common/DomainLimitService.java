package io.learnsharegrow.freezertracker.api.common;

import io.learnsharegrow.freezertracker.api.config.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DomainLimitService {
  private final ConfigService configService;

  public DomainLimitService(ConfigService configService) {
    this.configService = configService;
  }

  @Transactional(readOnly = true)
  public int maxFreezersPerHouse() {
    return resolvePositiveLimit(DomainLimits.MAX_FREEZERS_PER_HOUSE_KEY, DomainLimits.MAX_FREEZERS_PER_HOUSE);
  }

  @Transactional(readOnly = true)
  public int maxAccountsPerHouse() {
    return resolvePositiveLimit(DomainLimits.MAX_ACCOUNTS_PER_HOUSE_KEY, DomainLimits.MAX_ACCOUNTS_PER_HOUSE);
  }

  @Transactional(readOnly = true)
  public int maxItemsPerShelf() {
    return resolvePositiveLimit(DomainLimits.MAX_ITEMS_PER_SHELF_KEY, DomainLimits.MAX_ITEMS_PER_SHELF);
  }

  private int resolvePositiveLimit(String key, int fallback) {
    int configured = configService.getIntValue(key, fallback);
    return configured > 0 ? configured : fallback;
  }
}
