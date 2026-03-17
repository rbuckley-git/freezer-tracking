package io.learnsharegrow.freezertracker.api.statistics;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.learnsharegrow.freezertracker.api.auth.ApiClientType;
import io.learnsharegrow.freezertracker.api.auth.Account;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKey;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKeyRepository;
import io.learnsharegrow.freezertracker.api.auth.AccountRepository;
import io.learnsharegrow.freezertracker.api.freezers.Freezer;
import io.learnsharegrow.freezertracker.api.freezers.FreezerRepository;
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import io.learnsharegrow.freezertracker.api.items.Item;
import io.learnsharegrow.freezertracker.api.items.ItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private HouseRepository houseRepository;
  @Autowired private FreezerRepository freezerRepository;
  @Autowired private ItemRepository itemRepository;

  @BeforeEach
  void cleanDatabase() {
    itemRepository.deleteAll();
    freezerRepository.deleteAll();
    accountApiKeyRepository.deleteAll();
    accountRepository.deleteAll();
    houseRepository.deleteAll();
  }

  @Test
  void shouldReturnHouseFreezerAndShelfStatisticsForSuperAdmin() throws Exception {
    House alphaHouse = createHouse("Alpha House");
    House betaHouse = createHouse("Beta House");

    Freezer alphaFreezerOne = createFreezer(alphaHouse.getId(), "Alpha One", 3);
    Freezer alphaFreezerTwo = createFreezer(alphaHouse.getId(), "Alpha Two", 2);
    Freezer betaFreezer = createFreezer(betaHouse.getId(), "Beta One", 1);

    createItem(alphaHouse.getId(), alphaFreezerOne.getId(), 1, "10000001");
    createItem(alphaHouse.getId(), alphaFreezerOne.getId(), 1, "10000002");
    createItem(alphaHouse.getId(), alphaFreezerOne.getId(), 3, "10000003");
    createItem(alphaHouse.getId(), alphaFreezerTwo.getId(), 2, "10000004");

    SeededAccount superAdmin = seedAccount("super.statistics@example.com", true, true, alphaHouse.getId());

    mockMvc
        .perform(get("/statistics").header("X-API-Key", superAdmin.apiKey().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.houseCount", is(2)))
        .andExpect(jsonPath("$.houses[0].houseName", is("Alpha House")))
        .andExpect(jsonPath("$.houses[0].freezerCount", is(2)))
        .andExpect(jsonPath("$.houses[0].freezers[0].freezerName", is("Alpha One")))
        .andExpect(jsonPath("$.houses[0].freezers[0].shelfCount", is(3)))
        .andExpect(jsonPath("$.houses[0].freezers[0].shelves[0].shelfNumber", is(1)))
        .andExpect(jsonPath("$.houses[0].freezers[0].shelves[0].itemCount", is(2)))
        .andExpect(jsonPath("$.houses[0].freezers[0].shelves[1].itemCount", is(0)))
        .andExpect(jsonPath("$.houses[0].freezers[0].shelves[2].itemCount", is(1)))
        .andExpect(jsonPath("$.houses[0].freezers[1].freezerName", is("Alpha Two")))
        .andExpect(jsonPath("$.houses[0].freezers[1].shelves[1].itemCount", is(1)))
        .andExpect(jsonPath("$.houses[1].houseName", is("Beta House")))
        .andExpect(jsonPath("$.houses[1].freezerCount", is(1)))
        .andExpect(jsonPath("$.houses[1].freezers[0].freezerName", is("Beta One")))
        .andExpect(jsonPath("$.houses[1].freezers[0].shelves[0].itemCount", is(0)));
  }

  @Test
  void shouldRejectNonSuperAdminStatisticsAccess() throws Exception {
    House house = createHouse("Restricted House");
    SeededAccount admin = seedAccount("admin.statistics@example.com", true, false, house.getId());

    mockMvc.perform(get("/statistics").header("X-API-Key", admin.apiKey().toString())).andExpect(status().isForbidden());
  }

  private House createHouse(String name) {
    House house = new House();
    house.setId(UUID.randomUUID());
    house.setName(name);
    return houseRepository.save(house);
  }

  private Freezer createFreezer(UUID houseId, String name, int shelfCount) {
    Freezer freezer = new Freezer();
    freezer.setName(name);
    freezer.setShelfCount(shelfCount);
    freezer.setHouseId(houseId);
    return freezerRepository.save(freezer);
  }

  private void createItem(UUID houseId, Integer freezerId, int shelfNumber, String reference) {
    Item item = new Item();
    item.setId(UUID.randomUUID());
    item.setReference(reference);
    item.setFreezeDate(LocalDate.of(2025, 1, 1));
    item.setBestBefore(LocalDate.of(2026, 1, 1));
    item.setDescription("Item " + reference);
    item.setFreezerId(freezerId);
    item.setShelfNumber(shelfNumber);
    item.setHouseId(houseId);
    itemRepository.save(item);
  }

  private SeededAccount seedAccount(String username, boolean isAdmin, boolean isSuperAdmin, UUID houseId) {
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setPasswordSalt("salt");
    account.setPasswordHash("hash");
    account.setAdmin(isAdmin);
    account.setSuperAdmin(isSuperAdmin);
    account.setHouseId(houseId);
    Account saved = accountRepository.save(account);

    UUID apiKey = UUID.randomUUID();
    AccountApiKey key = new AccountApiKey();
    key.setApiKey(apiKey);
    key.setAccountId(saved.getId());
    key.setCreatedAt(Instant.now());
    key.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    key.setClientType(ApiClientType.WEB);
    accountApiKeyRepository.save(key);

    return new SeededAccount(saved, apiKey);
  }

  private record SeededAccount(Account account, UUID apiKey) {}
}
