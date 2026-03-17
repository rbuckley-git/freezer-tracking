package io.learnsharegrow.freezertracker.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.learnsharegrow.freezertracker.api.common.BadRequestException;
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BootstrapAdminServiceTest {
  @Autowired private BootstrapAdminService bootstrapAdminService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private HouseRepository houseRepository;

  @BeforeEach
  void cleanDatabase() {
    accountApiKeyRepository.deleteAll();
    accountRepository.deleteAll();
    houseRepository.deleteAll();
  }

  @Test
  void shouldCreateAdminAndHouseWhenMissing() {
    String username = "bootstrap.admin+" + UUID.randomUUID() + "@example.com";
    String houseName = "Bootstrap House " + UUID.randomUUID();
    BootstrapAdminResult result = bootstrapAdminService.bootstrapAdmin(
        username,
        "bootstrap-pass-1234",
        houseName);

    assertTrue(result.accountCreated());
    assertTrue(result.houseCreated());

    Account account = accountRepository.findById(result.accountId()).orElseThrow();
    assertEquals(username, account.getUsername());
    assertTrue(account.isAdmin());
    assertTrue(account.isSuperAdmin());
    assertEquals(result.houseId(), account.getHouseId());
    assertTrue(PasswordHasher.matchesBcrypt("bootstrap-pass-1234change-me-pepper", account.getPasswordHash()));
  }

  @Test
  void shouldBeIdempotentForExistingUserAndHouse() {
    String username = "bootstrap.idempotent+" + UUID.randomUUID() + "@example.com";
    String houseName = "Idempotent House " + UUID.randomUUID();
    BootstrapAdminResult first = bootstrapAdminService.bootstrapAdmin(
        username,
        "bootstrap-pass-1234",
        houseName);

    BootstrapAdminResult second = bootstrapAdminService.bootstrapAdmin(
        username,
        "different-pass-1234",
        houseName);

    assertFalse(second.accountCreated());
    assertFalse(second.houseCreated());
    assertEquals(first.accountId(), second.accountId());
    assertEquals(first.houseId(), second.houseId());

    Account account = accountRepository.findById(second.accountId()).orElseThrow();
    assertTrue(account.isAdmin());
    assertTrue(account.isSuperAdmin());
    assertNotNull(account.getPasswordHash());

    House house = houseRepository.findById(second.houseId()).orElseThrow();
    assertEquals(houseName, house.getName());
  }

  @Test
  void shouldRejectBootstrapWhenTargetHouseHasTenAccounts() {
    House house = new House();
    house.setId(UUID.randomUUID());
    house.setName("Full bootstrap house " + UUID.randomUUID());
    houseRepository.save(house);
    seedAccountsInHouse(house.getId(), 10);

    BadRequestException exception = assertThrows(
        BadRequestException.class,
        () -> bootstrapAdminService.bootstrapAdmin(
            "bootstrap.limit+" + UUID.randomUUID() + "@example.com",
            "bootstrap-pass-1234",
            house.getName()));

    assertEquals("House cannot have more than 10 accounts", exception.getMessage());
  }

  private void seedAccountsInHouse(UUID houseId, int count) {
    for (int i = 0; i < count; i++) {
      Account account = new Account();
      account.setId(UUID.randomUUID());
      account.setUsername("bootstrap.house.user+" + UUID.randomUUID() + "@example.com");
      account.setPasswordSalt("");
      account.setPasswordHash("hash-" + i);
      account.setAdmin(false);
      account.setSuperAdmin(false);
      account.setHouseId(houseId);
      account.setFailedLoginCount(0);
      account.setLockoutUntil(null);
      accountRepository.save(account);
    }
  }
}
