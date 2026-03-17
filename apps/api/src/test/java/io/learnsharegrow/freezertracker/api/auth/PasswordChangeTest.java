package io.learnsharegrow.freezertracker.api.auth;

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
class PasswordChangeTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private io.learnsharegrow.freezertracker.api.houses.HouseRepository houseRepository;

  private static final String PEPPER = "change-me-pepper";

  @Test
  void shouldChangePasswordWithValidCurrentPassword() throws Exception {
    SeededAccount account = seedAccount("change.pass@example.com", "old-pass");

    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("old-pass");
    request.setNewPassword("new-pass-1234");

    mockMvc
        .perform(
            post("/account/password")
                .header("X-API-Key", account.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldRejectPasswordChangeWithWrongCurrentPassword() throws Exception {
    SeededAccount account = seedAccount("change.pass.fail@example.com", "old-pass");

    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("wrong-pass");
    request.setNewPassword("new-pass-1234");

    mockMvc
        .perform(
            post("/account/password")
                .header("X-API-Key", account.apiKey().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Current password is incorrect"));
  }

  private SeededAccount seedAccount(String username, String password) {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Password house " + UUID.randomUUID());
    houseRepository.save(house);
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setPasswordSalt("salt1234");
    account.setPasswordHash(sha256("salt1234" + password + PEPPER));
    account.setAdmin(false);
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
