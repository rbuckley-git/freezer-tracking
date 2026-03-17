package io.learnsharegrow.freezertracker.api.config;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.learnsharegrow.freezertracker.api.auth.ApiClientType;
import io.learnsharegrow.freezertracker.api.auth.Account;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKey;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKeyRepository;
import io.learnsharegrow.freezertracker.api.auth.AccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
class ConfigControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private io.learnsharegrow.freezertracker.api.houses.HouseRepository houseRepository;

  private static final String PEPPER = "change-me-pepper";

  @Test
  void shouldAllowSuperAdminToManageConfig() throws Exception {
    SeededAccount admin = seedAccount("admin.config@example.com", "admin-pass", true);

    ConfigCreateRequest createRequest = new ConfigCreateRequest();
    createRequest.setKey("test_key");
    createRequest.setValue("test_value");

    mockMvc
        .perform(
            post("/config")
                .header("X-API-Key", admin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.key", is("test_key")))
        .andExpect(jsonPath("$.value", is("test_value")));

    ConfigUpdateRequest updateRequest = new ConfigUpdateRequest();
    updateRequest.setValue("updated");

    mockMvc
        .perform(
            put("/config/{key}", "test_key")
                .header("X-API-Key", admin.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value", is("updated")));

    mockMvc
        .perform(delete("/config/{key}", "test_key").header("X-API-Key", admin.apiKey().toString()))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldRejectNonSuperAdminConfigAccess() throws Exception {
    SeededAccount user = seedAccount("user.config@example.com", "user-pass", true, false);

    mockMvc.perform(get("/config").header("X-API-Key", user.apiKey().toString())).andExpect(status().isForbidden());
  }

  private SeededAccount seedAccount(String username, String password, boolean isAdmin) {
    return seedAccount(username, password, isAdmin, isAdmin);
  }

  private SeededAccount seedAccount(String username, String password, boolean isAdmin, boolean isSuperAdmin) {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Config house " + UUID.randomUUID());
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

  private record SeededAccount(Account account, UUID apiKey) {}
}
