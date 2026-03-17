package io.learnsharegrow.freezertracker.api.freezers;

import static org.hamcrest.Matchers.hasSize;
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
import io.learnsharegrow.freezertracker.api.houses.House;
import io.learnsharegrow.freezertracker.api.houses.HouseRepository;
import java.time.Instant;
import java.time.LocalDate;
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
class FreezerControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private FreezerRepository freezerRepository;
  @Autowired private io.learnsharegrow.freezertracker.api.items.ItemRepository itemRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private HouseRepository houseRepository;
  private UUID apiKey;

  @BeforeEach
  void cleanDatabase() {
    itemRepository.deleteAll();
    freezerRepository.deleteAll();
    accountApiKeyRepository.deleteAll();
    accountRepository.deleteAll();
    houseRepository.deleteAll();
    apiKey = seedApiKey();
  }

  @Test
  void shouldCreateGetUpdateAndDeleteFreezer() throws Exception {
    FreezerCreateRequest createRequest = new FreezerCreateRequest();
    createRequest.setName("Garage");
    createRequest.setShelfCount(10);

    String createResponse =
        mockMvc
            .perform(
                post("/freezers")
                    .header("X-API-Key", apiKey.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name", is("Garage")))
            .andExpect(jsonPath("$.shelfCount", is(10)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    FreezerResponse created = objectMapper.readValue(createResponse, FreezerResponse.class);
    Integer id = created.getId();

    mockMvc
        .perform(get("/freezers/{id}", id).header("X-API-Key", apiKey.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.name", is("Garage")));

    FreezerUpdateRequest updateRequest = new FreezerUpdateRequest();
    updateRequest.setName("Basement");
    updateRequest.setShelfCount(3);

    mockMvc
        .perform(
            put("/freezers/{id}", id)
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Basement")))
        .andExpect(jsonPath("$.shelfCount", is(3)));

    mockMvc.perform(delete("/freezers/{id}", id).header("X-API-Key", apiKey.toString())).andExpect(status().isNoContent());

    mockMvc.perform(get("/freezers/{id}", id).header("X-API-Key", apiKey.toString())).andExpect(status().isNotFound());
  }

  @Test
  void shouldListFreezers() throws Exception {
    createFreezer("Kitchen");
    createFreezer("Garage");

    mockMvc
        .perform(get("/freezers").header("X-API-Key", apiKey.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void shouldRejectUnsafeCharacters() throws Exception {
    FreezerCreateRequest createRequest = new FreezerCreateRequest();
    createRequest.setName("<script>");

    mockMvc
        .perform(
            post("/freezers")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectDeleteWhenItemsExist() throws Exception {
    Integer freezerId = createFreezer("Garage");

    io.learnsharegrow.freezertracker.api.items.ItemCreateRequest itemRequest =
        new io.learnsharegrow.freezertracker.api.items.ItemCreateRequest();
    itemRequest.setReference("33333333");
    itemRequest.setFreezeDate(LocalDate.of(2024, 3, 1));
    itemRequest.setBestBefore(LocalDate.of(2025, 3, 1));
    itemRequest.setDescription("Frozen berries");
    itemRequest.setFreezerId(freezerId);
    itemRequest.setShelfNumber(1);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemRequest)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(delete("/freezers/{id}", freezerId).header("X-API-Key", apiKey.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectCreateWhenHouseHasTenFreezers() throws Exception {
    for (int i = 1; i <= 10; i++) {
      createFreezer("Freezer " + i);
    }

    FreezerCreateRequest createRequest = new FreezerCreateRequest();
    createRequest.setName("Overflow freezer");
    createRequest.setShelfCount(5);

    mockMvc
        .perform(
            post("/freezers")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("House cannot have more than 10 freezers")));
  }

  @Test
  void shouldNotAccessFreezerFromAnotherHouse() throws Exception {
    Integer freezerId = createFreezer("Primary house freezer");
    UUID otherHouseApiKey = seedApiKey("other-house-tester@example.com", "Other test house");

    mockMvc
        .perform(get("/freezers/{id}", freezerId).header("X-API-Key", otherHouseApiKey.toString()))
        .andExpect(status().isNotFound());

    FreezerUpdateRequest updateRequest = new FreezerUpdateRequest();
    updateRequest.setName("Should not update");
    updateRequest.setShelfCount(2);

    mockMvc
        .perform(
            put("/freezers/{id}", freezerId)
                .header("X-API-Key", otherHouseApiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/freezers/{id}", freezerId).header("X-API-Key", otherHouseApiKey.toString()))
        .andExpect(status().isNotFound());
  }

  private Integer createFreezer(String name) throws Exception {
    FreezerCreateRequest createRequest = new FreezerCreateRequest();
    createRequest.setName(name);
    createRequest.setShelfCount(10);

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
    return seedApiKey("tester@example.com", "Test house");
  }

  private UUID seedApiKey(String username, String houseName) {
    House house = new House();
    house.setId(UUID.randomUUID());
    house.setName(houseName);
    houseRepository.save(house);

    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
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
