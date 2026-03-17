package io.learnsharegrow.freezertracker.api.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class AccountAdminTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private io.learnsharegrow.freezertracker.api.houses.HouseRepository houseRepository;

  private static final String PEPPER = "change-me-pepper";

  @BeforeEach
  void cleanDatabase() {
    accountApiKeyRepository.deleteAll();
    accountRepository.deleteAll();
    houseRepository.deleteAll();
  }

  @Test
  void shouldAllowAdminToListAccounts() throws Exception {
    SeededAccount admin = seedAccount("admin@example.com", "admin-pass", true);

    mockMvc
        .perform(get("/accounts").header("X-API-Key", admin.apiKey().toString()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectNonAdminAccountList() throws Exception {
    SeededAccount user = seedAccount("user@example.com", "user-pass", false);

    mockMvc
        .perform(get("/accounts").header("X-API-Key", user.apiKey().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowAdminToCreateAccount() throws Exception {
    SeededAccount admin = seedAccount("admin.create@example.com", "admin-pass", true);
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Create house");
    houseRepository.save(house);

    AccountCreateRequest request = new AccountCreateRequest();
    request.setUsername("new.user@example.com");
    request.setPassword("new-pass-1234");
    request.setAdmin(false);
    request.setHouseId(house.getId().toString());

    mockMvc
        .perform(
            post("/accounts")
                .header("X-API-Key", admin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldAllowAdminToRevokeAllApiKeys() throws Exception {
    SeededAccount admin = seedAccount("admin.revoke@example.com", "admin-pass", true);
    SeededAccount user = seedAccount("user.revoke@example.com", "user-pass", false);

    mockMvc
        .perform(post("/accounts/{id}/api-keys/revoke-all", user.account().getId())
            .header("X-API-Key", admin.apiKey().toString()))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldAllowAdminToRevokeApiKey() throws Exception {
    SeededAccount admin = seedAccount("admin.revoke.key@example.com", "admin-pass", true);
    SeededAccount user = seedAccount("user.revoke.key@example.com", "user-pass", false);

    RevokeApiKeyRequest request = new RevokeApiKeyRequest();
    request.setApiKey(user.apiKey().toString());

    mockMvc
        .perform(
            post("/accounts/{id}/api-keys/revoke", user.account().getId())
                .header("X-API-Key", admin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldRejectCreateAccountWhenHouseHasTenAccounts() throws Exception {
    SeededAccount admin = seedAccount("admin.limit.create@example.com", "admin-pass", true);
    io.learnsharegrow.freezertracker.api.houses.House fullHouse = new io.learnsharegrow.freezertracker.api.houses.House();
    fullHouse.setId(UUID.randomUUID());
    fullHouse.setName("Full house create " + UUID.randomUUID());
    houseRepository.save(fullHouse);
    seedAccountsInHouse(fullHouse.getId(), 10);

    AccountCreateRequest request = new AccountCreateRequest();
    request.setUsername("overflow.user@example.com");
    request.setPassword("overflow-pass-1234");
    request.setAdmin(false);
    request.setHouseId(fullHouse.getId().toString());

    mockMvc
        .perform(
            post("/accounts")
                .header("X-API-Key", admin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("House cannot have more than 10 accounts")));
  }

  @Test
  void shouldRejectUpdateWhenMovingAccountToHouseWithTenAccounts() throws Exception {
    SeededAccount admin = seedAccount("admin.limit.update@example.com", "admin-pass", true);
    SeededAccount userToMove = seedAccount("move.target@example.com", "user-pass", false);
    io.learnsharegrow.freezertracker.api.houses.House fullHouse = new io.learnsharegrow.freezertracker.api.houses.House();
    fullHouse.setId(UUID.randomUUID());
    fullHouse.setName("Full house update " + UUID.randomUUID());
    houseRepository.save(fullHouse);
    seedAccountsInHouse(fullHouse.getId(), 10);

    AccountUpdateRequest request = new AccountUpdateRequest();
    request.setUsername(userToMove.account().getUsername());
    request.setAdmin(false);
    request.setHouseId(fullHouse.getId().toString());

    mockMvc
        .perform(
            put("/accounts/{id}", userToMove.account().getId())
                .header("X-API-Key", admin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("House cannot have more than 10 accounts")));
  }

  @Test
  void shouldAllowHouseAdminToCreateNonAdminAccountInOwnHouse() throws Exception {
    SeededAccount houseAdmin = seedAccount("house.admin.create@example.com", "admin-pass", true, false);
    AccountCreateRequest request = new AccountCreateRequest();
    request.setUsername("house.user.create@example.com");
    request.setPassword("new-pass-1234");
    request.setAdmin(false);
    request.setSuperAdmin(false);
    request.setHouseId(houseAdmin.account().getHouseId().toString());

    mockMvc
        .perform(
            post("/accounts")
                .header("X-API-Key", houseAdmin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isAdmin", is(false)))
        .andExpect(jsonPath("$.isSuperAdmin", is(false)));
  }

  @Test
  void shouldRejectHouseAdminCreatingAdminAccount() throws Exception {
    SeededAccount houseAdmin = seedAccount("house.admin.reject@example.com", "admin-pass", true, false);
    AccountCreateRequest request = new AccountCreateRequest();
    request.setUsername("new.admin.reject@example.com");
    request.setPassword("new-pass-1234");
    request.setAdmin(true);
    request.setSuperAdmin(false);
    request.setHouseId(houseAdmin.account().getHouseId().toString());

    mockMvc
        .perform(
            post("/accounts")
                .header("X-API-Key", houseAdmin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectHouseAdminManagingAccountFromAnotherHouse() throws Exception {
    SeededAccount houseAdmin = seedAccount("house.admin.cross@example.com", "admin-pass", true, false);
    SeededAccount otherHouseUser = seedAccount("other.house.user@example.com", "user-pass", false);

    mockMvc
        .perform(
            delete("/accounts/{id}", otherHouseUser.account().getId())
                .header("X-API-Key", houseAdmin.apiKey().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowSuperAdminToDeleteAccountWithActiveApiKeys() throws Exception {
    SeededAccount superAdmin = seedAccount("super.admin.delete@example.com", "admin-pass", true, true);
    SeededAccount user = seedAccount("user.delete.keys@example.com", "user-pass", false);

    mockMvc
        .perform(
            delete("/accounts/{id}", user.account().getId())
                .header("X-API-Key", superAdmin.apiKey().toString()))
        .andExpect(status().isNoContent());
  }

  private SeededAccount seedAccount(String username, String password, boolean isAdmin) {
    return seedAccount(username, password, isAdmin, isAdmin);
  }

  private SeededAccount seedAccount(String username, String password, boolean isAdmin, boolean isSuperAdmin) {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Admin house " + UUID.randomUUID());
    houseRepository.save(house);
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setPasswordSalt("salt1234");
    account.setPasswordHash(sha256("salt1234" + password + PEPPER));
    account.setAdmin(isAdmin);
    account.setSuperAdmin(isSuperAdmin);
    account.setHouseId(house.getId());
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

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }

  private void seedAccountsInHouse(UUID houseId, int count) {
    for (int i = 0; i < count; i++) {
      Account account = new Account();
      account.setId(UUID.randomUUID());
      account.setUsername("house.user+" + UUID.randomUUID() + "@example.com");
      account.setPasswordSalt("salt1234");
      account.setPasswordHash(sha256("salt1234password-" + i + PEPPER));
      account.setAdmin(false);
      account.setSuperAdmin(false);
      account.setHouseId(houseId);
      accountRepository.save(account);
    }
  }

  private record SeededAccount(Account account, UUID apiKey) {}
}
