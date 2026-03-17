package io.learnsharegrow.freezertracker.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.learnsharegrow.freezertracker.api.config.ConfigEntry;
import io.learnsharegrow.freezertracker.api.config.ConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DomainLimitServiceTest {
  @Autowired private DomainLimitService domainLimitService;
  @Autowired private ConfigRepository configRepository;

  @AfterEach
  void cleanConfig() {
    configRepository.deleteAll();
  }

  @Test
  void shouldUseDefaultLimitsWhenConfigMissing() {
    assertEquals(DomainLimits.MAX_FREEZERS_PER_HOUSE, domainLimitService.maxFreezersPerHouse());
    assertEquals(DomainLimits.MAX_ACCOUNTS_PER_HOUSE, domainLimitService.maxAccountsPerHouse());
    assertEquals(DomainLimits.MAX_ITEMS_PER_SHELF, domainLimitService.maxItemsPerShelf());
  }

  @Test
  void shouldUseConfiguredLimitsFromConfigTable() {
    saveConfig(DomainLimits.MAX_FREEZERS_PER_HOUSE_KEY, "12");
    saveConfig(DomainLimits.MAX_ACCOUNTS_PER_HOUSE_KEY, "8");
    saveConfig(DomainLimits.MAX_ITEMS_PER_SHELF_KEY, "150");

    assertEquals(12, domainLimitService.maxFreezersPerHouse());
    assertEquals(8, domainLimitService.maxAccountsPerHouse());
    assertEquals(150, domainLimitService.maxItemsPerShelf());
  }

  @Test
  void shouldFallbackToDefaultWhenConfiguredLimitIsNotPositive() {
    saveConfig(DomainLimits.MAX_FREEZERS_PER_HOUSE_KEY, "0");
    saveConfig(DomainLimits.MAX_ACCOUNTS_PER_HOUSE_KEY, "-1");
    saveConfig(DomainLimits.MAX_ITEMS_PER_SHELF_KEY, "not-a-number");

    assertEquals(DomainLimits.MAX_FREEZERS_PER_HOUSE, domainLimitService.maxFreezersPerHouse());
    assertEquals(DomainLimits.MAX_ACCOUNTS_PER_HOUSE, domainLimitService.maxAccountsPerHouse());
    assertEquals(DomainLimits.MAX_ITEMS_PER_SHELF, domainLimitService.maxItemsPerShelf());
  }

  private void saveConfig(String key, String value) {
    ConfigEntry entry = new ConfigEntry();
    entry.setKey(key);
    entry.setValue(value);
    configRepository.save(entry);
  }
}
