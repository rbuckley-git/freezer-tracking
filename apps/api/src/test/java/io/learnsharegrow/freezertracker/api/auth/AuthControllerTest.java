package io.learnsharegrow.freezertracker.api.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private io.learnsharegrow.freezertracker.api.houses.HouseRepository houseRepository;

  private static final String PEPPER = "change-me-pepper";

  @Test
  void shouldLoginWithValidCredentials() throws Exception {
    SeededAccount account = seedAccount("login.success@example.com", "valid-pass");
    LoginRequest request = new LoginRequest();
    request.setUsername(account.account().getUsername());
    request.setPassword("valid-pass");
    request.setClientType(ApiClientType.WEB);

    String responseBody = mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiKey").exists())
        .andExpect(jsonPath("$.apiKeyExpiry").exists())
        .andExpect(jsonPath("$.isSuperAdmin", is(false)))
        .andReturn()
        .getResponse()
        .getContentAsString();

    UUID loginApiKey = UUID.fromString(objectMapper.readTree(responseBody).get("apiKey").asText());
    AccountApiKey storedKey = accountApiKeyRepository.findByAccountIdOrderByCreatedAtDesc(account.account().getId()).stream()
        .findFirst()
        .orElseThrow();
    assertNotNull(storedKey.getApiKeyHash());
    assertEquals(ApiKeyHasher.sha256(loginApiKey.toString()), storedKey.getApiKeyHash());
    assertNotEquals(loginApiKey, storedKey.getApiKey());
    assertEquals(ApiClientType.WEB, storedKey.getClientType());
  }

  @Test
  void shouldRejectInvalidCredentials() throws Exception {
    SeededAccount account = seedAccount("login.fail@example.com", "valid-pass");
    LoginRequest request = new LoginRequest();
    request.setUsername(account.account().getUsername());
    request.setPassword("wrong-pass");
    request.setClientType(ApiClientType.WEB);

    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectMissingApiKey() throws Exception {
    mockMvc.perform(get("/items")).andExpect(status().isForbidden());
  }

  @Test
  void shouldInvalidateApiKeyOnLogout() throws Exception {
    SeededAccount account = seedAccount("logout@example.com", "valid-pass");
    String apiKey = account.apiKey().toString();

    mockMvc.perform(post("/logout").header("X-API-Key", apiKey)).andExpect(status().isNoContent());

    mockMvc.perform(get("/items").header("X-API-Key", apiKey)).andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnAccountHouse() throws Exception {
    String houseName = "Account house " + UUID.randomUUID();
    SeededAccount account = seedAccountWithHouseName("house.lookup@example.com", "valid-pass", houseName);

    mockMvc
        .perform(get("/account/house").header("X-API-Key", account.apiKey().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name", is(houseName)));
  }

  @Test
  void shouldAuthenticateLegacyUnhashedApiKey() throws Exception {
    SeededAccount account = seedLegacyAccount("legacy.key@example.com", "valid-pass");

    mockMvc
        .perform(get("/account/house").header("X-API-Key", account.apiKey().toString()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldLockOutAfterThreeFailedAttempts() throws Exception {
    SeededAccount account = seedAccount("lockout@example.com", "valid-pass");
    LoginRequest badRequest = new LoginRequest();
    badRequest.setUsername(account.account().getUsername());
    badRequest.setPassword("wrong-pass");
    badRequest.setClientType(ApiClientType.WEB);

    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              post("/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(badRequest)))
          .andExpect(status().isForbidden());
    }

    LoginRequest goodRequest = new LoginRequest();
    goodRequest.setUsername(account.account().getUsername());
    goodRequest.setPassword("valid-pass");
    goodRequest.setClientType(ApiClientType.WEB);

    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goodRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReplaceExistingSessionForSameClientType() throws Exception {
    SeededAccount account = seedAccount("login.replace@example.com", "valid-pass");

    LoginRequest request = new LoginRequest();
    request.setUsername(account.account().getUsername());
    request.setPassword("valid-pass");
    request.setClientType(ApiClientType.WEB);

    String responseBody = mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String newApiKey = objectMapper.readTree(responseBody).get("apiKey").asText();

    mockMvc.perform(get("/items").header("X-API-Key", account.apiKey().toString()))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/items").header("X-API-Key", newApiKey))
        .andExpect(status().isOk());
  }

  @Test
  void shouldKeepSessionForDifferentClientType() throws Exception {
    SeededAccount account = seedAccount("login.multiclient@example.com", "valid-pass");

    LoginRequest request = new LoginRequest();
    request.setUsername(account.account().getUsername());
    request.setPassword("valid-pass");
    request.setClientType(ApiClientType.IOS);

    String responseBody = mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String iosApiKey = objectMapper.readTree(responseBody).get("apiKey").asText();

    mockMvc.perform(get("/items").header("X-API-Key", account.apiKey().toString()))
        .andExpect(status().isOk());
    mockMvc.perform(get("/items").header("X-API-Key", iosApiKey))
        .andExpect(status().isOk());
  }

  private SeededAccount seedAccount(String username, String password) {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Test house " + UUID.randomUUID());
    houseRepository.save(house);
    return seedAccountWithHouseId(username, password, house.getId());
  }

  private SeededAccount seedAccountWithHouseName(String username, String password, String houseName) {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName(houseName);
    houseRepository.save(house);
    return seedAccountWithHouseId(username, password, house.getId());
  }

  private SeededAccount seedAccountWithHouseId(String username, String password, UUID houseId) {
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setPasswordSalt("salt1234");
    account.setPasswordHash(PasswordHasher.sha256("salt1234" + password + PEPPER));
    account.setAdmin(false);
    account.setSuperAdmin(false);
    account.setHouseId(houseId);
    Account saved = accountRepository.save(account);
    UUID apiKey = UUID.randomUUID();
    AccountApiKey key = new AccountApiKey();
    key.setApiKey(apiKey);
    key.setApiKeyHash(ApiKeyHasher.sha256(apiKey.toString()));
    key.setAccountId(saved.getId());
    key.setCreatedAt(Instant.now());
    key.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    key.setClientType(ApiClientType.WEB);
    accountApiKeyRepository.save(key);
    return new SeededAccount(saved, apiKey);
  }

  private SeededAccount seedLegacyAccount(String username, String password) {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Legacy house " + UUID.randomUUID());
    houseRepository.save(house);

    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setPasswordSalt("salt1234");
    account.setPasswordHash(PasswordHasher.sha256("salt1234" + password + PEPPER));
    account.setAdmin(false);
    account.setSuperAdmin(false);
    account.setHouseId(house.getId());
    Account saved = accountRepository.save(account);

    UUID apiKey = UUID.randomUUID();
    AccountApiKey key = new AccountApiKey();
    key.setApiKey(apiKey);
    key.setApiKeyHash(null);
    key.setAccountId(saved.getId());
    key.setCreatedAt(Instant.now());
    key.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    key.setClientType(ApiClientType.LEGACY);
    accountApiKeyRepository.save(key);

    return new SeededAccount(saved, apiKey);
  }

  private record SeededAccount(Account account, UUID apiKey) {}
}
