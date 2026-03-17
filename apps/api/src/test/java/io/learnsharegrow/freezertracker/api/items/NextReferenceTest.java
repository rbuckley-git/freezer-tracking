package io.learnsharegrow.freezertracker.api.items;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.learnsharegrow.freezertracker.api.auth.ApiClientType;
import io.learnsharegrow.freezertracker.api.auth.Account;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKey;
import io.learnsharegrow.freezertracker.api.auth.AccountApiKeyRepository;
import io.learnsharegrow.freezertracker.api.auth.AccountRepository;
import java.time.LocalDate;
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
import io.learnsharegrow.freezertracker.api.freezers.FreezerCreateRequest;
import io.learnsharegrow.freezertracker.api.freezers.FreezerResponse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NextReferenceTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private io.learnsharegrow.freezertracker.api.houses.HouseRepository houseRepository;
  @Autowired private ItemRepository itemRepository;
  @Autowired private io.learnsharegrow.freezertracker.api.freezers.FreezerRepository freezerRepository;
  private UUID apiKey;

  @org.junit.jupiter.api.BeforeEach
  void cleanDatabase() {
    itemRepository.deleteAll();
    freezerRepository.deleteAll();
    accountApiKeyRepository.deleteAll();
    accountRepository.deleteAll();
    houseRepository.deleteAll();
  }

  @Test
  void shouldReturnNextReference() throws Exception {
    apiKey = seedApiKey();
    mockMvc
        .perform(get("/items/next-reference").header("X-API-Key", apiKey.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextReference", is("00000001")));

    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("00000005");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("Frozen peas");
    Integer kitchenId = createFreezer("Kitchen");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(2);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/items/next-reference").header("X-API-Key", apiKey.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextReference", is("00000006")));
  }

  private Integer createFreezer(String name) throws Exception {
    FreezerCreateRequest createRequest = new FreezerCreateRequest();
    createRequest.setName(name);
    createRequest.setShelfCount(5);

    String response =
        mockMvc
            .perform(
                post("/freezers")
                    .header("X-API-Key", apiKey.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    FreezerResponse created = objectMapper.readValue(response, FreezerResponse.class);
    return created.getId();
  }

  private UUID seedApiKey() {
    io.learnsharegrow.freezertracker.api.houses.House house = new io.learnsharegrow.freezertracker.api.houses.House();
    house.setId(UUID.randomUUID());
    house.setName("Test house");
    houseRepository.save(house);

    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername("tester@example.com");
    account.setPasswordSalt("salt");
    account.setPasswordHash("hash");
    account.setAdmin(true);
    account.setHouseId(house.getId());
    accountRepository.save(account);

    UUID key = UUID.randomUUID();
    AccountApiKey apiKeyEntity = new AccountApiKey();
    apiKeyEntity.setApiKey(key);
    apiKeyEntity.setAccountId(account.getId());
    apiKeyEntity.setCreatedAt(Instant.now());
    apiKeyEntity.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    apiKeyEntity.setClientType(ApiClientType.WEB);
    accountApiKeyRepository.save(apiKeyEntity);
    return key;
  }
}
