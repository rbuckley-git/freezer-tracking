package io.learnsharegrow.freezertracker.api.items;

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
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ItemRepository itemRepository;
  @Autowired private io.learnsharegrow.freezertracker.api.freezers.FreezerRepository freezerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountApiKeyRepository accountApiKeyRepository;
  @Autowired private HouseRepository houseRepository;
  private UUID apiKey;
  private UUID houseId;

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
  void shouldCreateGetUpdateAndDeleteItem() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    Integer garageId = createFreezer("Garage");
    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("12345678");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("Frozen peas");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(2);
    createRequest.setWeight("500g");
    createRequest.setSize("M");

    String createResponse =
        mockMvc
            .perform(
                post("/items")
                    .header("X-API-Key", apiKey.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.reference", is("12345678")))
            .andExpect(jsonPath("$.weight", is("500g")))
            .andExpect(jsonPath("$.size", is("M")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    ItemResponse created = objectMapper.readValue(createResponse, ItemResponse.class);
    UUID id = created.getId();

    mockMvc
        .perform(get("/items/{id}", id).header("X-API-Key", apiKey.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id.toString())))
        .andExpect(jsonPath("$.description", is("Frozen peas")))
        .andExpect(jsonPath("$.weight", is("500g")))
        .andExpect(jsonPath("$.size", is("M")));

    ItemUpdateRequest updateRequest = new ItemUpdateRequest();
    updateRequest.setReference("87654321");
    updateRequest.setFreezeDate(LocalDate.of(2024, 2, 1));
    updateRequest.setBestBefore(LocalDate.of(2025, 2, 1));
    updateRequest.setDescription("Frozen sweetcorn");
    updateRequest.setFreezerId(garageId);
    updateRequest.setShelfNumber(1);
    updateRequest.setWeight("750g");
    updateRequest.setSize("L");

    mockMvc
        .perform(
            put("/items/{id}", id)
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reference", is("87654321")))
        .andExpect(jsonPath("$.freezerName", is("Garage")))
        .andExpect(jsonPath("$.weight", is("750g")))
        .andExpect(jsonPath("$.size", is("L")));

    mockMvc.perform(delete("/items/{id}", id).header("X-API-Key", apiKey.toString())).andExpect(status().isNoContent());

    mockMvc.perform(get("/items/{id}", id).header("X-API-Key", apiKey.toString())).andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnPagedItems() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    createItem("11111111", "Item One", kitchenId);
    createItem("22222222", "Item Two", kitchenId);

    mockMvc
        .perform(get("/items").param("page", "0").param("size", "1").header("X-API-Key", apiKey.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.page", is(0)))
        .andExpect(jsonPath("$.size", is(1)))
        .andExpect(jsonPath("$.totalItems", is(2)))
        .andExpect(jsonPath("$.totalPages", is(2)));
  }

  @Test
  void shouldRejectUnsafeCharacters() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("99999999");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("<script>");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(2);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectInvalidSize() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("98989898");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("Frozen spinach");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(2);
    createRequest.setSize("XL");

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectBestBeforeBeforeFreezeDate() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("12345679");
    createRequest.setFreezeDate(LocalDate.of(2024, 2, 1));
    createRequest.setBestBefore(LocalDate.of(2024, 1, 1));
    createRequest.setDescription("Frozen carrots");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(1);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("Best before must be after freeze date")));
  }

  @Test
  void shouldRejectDuplicateReference() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    ItemCreateRequest first = new ItemCreateRequest();
    first.setReference("33333333");
    first.setFreezeDate(LocalDate.of(2024, 1, 10));
    first.setBestBefore(LocalDate.of(2025, 1, 10));
    first.setDescription("Frozen beans");
    first.setFreezerId(kitchenId);
    first.setShelfNumber(2);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
        .andExpect(status().isCreated());

    ItemCreateRequest duplicate = new ItemCreateRequest();
    duplicate.setReference("33333333");
    duplicate.setFreezeDate(LocalDate.of(2024, 2, 1));
    duplicate.setBestBefore(LocalDate.of(2025, 2, 1));
    duplicate.setDescription("Frozen carrots");
    duplicate.setFreezerId(kitchenId);
    duplicate.setShelfNumber(1);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("Value must be unique")));
  }

  @Test
  void shouldRejectCreateWhenShelfHasHundredItems() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    seedShelfItems(kitchenId, 1, 100, 10000000);

    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("99999998");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("Shelf overflow item");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(1);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("Shelf cannot contain more than 100 items")));
  }

  @Test
  void shouldRejectMoveWhenDestinationShelfHasHundredItems() throws Exception {
    Integer kitchenId = createFreezer("Kitchen");
    Integer garageId = createFreezer("Garage");
    seedShelfItems(garageId, 1, 100, 20000000);

    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("99999997");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("Movable item");
    createRequest.setFreezerId(kitchenId);
    createRequest.setShelfNumber(1);

    String createResponse =
        mockMvc
            .perform(
                post("/items")
                    .header("X-API-Key", apiKey.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    ItemResponse created = objectMapper.readValue(createResponse, ItemResponse.class);

    ItemUpdateRequest updateRequest = new ItemUpdateRequest();
    updateRequest.setReference(created.getReference());
    updateRequest.setFreezeDate(created.getFreezeDate());
    updateRequest.setBestBefore(created.getBestBefore());
    updateRequest.setDescription(created.getDescription());
    updateRequest.setFreezerId(garageId);
    updateRequest.setShelfNumber(1);

    mockMvc
        .perform(
            put("/items/{id}", created.getId())
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("Shelf cannot contain more than 100 items")));
  }

  @Test
  void shouldNotAccessItemFromAnotherHouse() throws Exception {
    Integer primaryFreezerId = createFreezer("Primary house freezer");
    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference("55555555");
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription("Cross-house isolation item");
    createRequest.setFreezerId(primaryFreezerId);
    createRequest.setShelfNumber(1);

    String createResponse =
        mockMvc
            .perform(
                post("/items")
                    .header("X-API-Key", apiKey.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    ItemResponse created = objectMapper.readValue(createResponse, ItemResponse.class);

    UUID otherHouseApiKey = seedApiKey("other-house-item-tester@example.com", "Other item test house", false);

    mockMvc
        .perform(get("/items/{id}", created.getId()).header("X-API-Key", otherHouseApiKey.toString()))
        .andExpect(status().isNotFound());

    ItemUpdateRequest updateRequest = new ItemUpdateRequest();
    updateRequest.setReference(created.getReference());
    updateRequest.setFreezeDate(created.getFreezeDate());
    updateRequest.setBestBefore(created.getBestBefore());
    updateRequest.setDescription("Should not update");
    updateRequest.setFreezerId(primaryFreezerId);
    updateRequest.setShelfNumber(1);

    mockMvc
        .perform(
            put("/items/{id}", created.getId())
                .header("X-API-Key", otherHouseApiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(delete("/items/{id}", created.getId()).header("X-API-Key", otherHouseApiKey.toString()))
        .andExpect(status().isNotFound());
  }

  private void createItem(String reference, String description, Integer freezerId) throws Exception {
    ItemCreateRequest createRequest = new ItemCreateRequest();
    createRequest.setReference(reference);
    createRequest.setFreezeDate(LocalDate.of(2024, 1, 10));
    createRequest.setBestBefore(LocalDate.of(2025, 1, 10));
    createRequest.setDescription(description);
    createRequest.setFreezerId(freezerId);
    createRequest.setShelfNumber(2);

    mockMvc
        .perform(
            post("/items")
                .header("X-API-Key", apiKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());
  }

  private Integer createFreezer(String name) throws Exception {
    io.learnsharegrow.freezertracker.api.freezers.FreezerCreateRequest createRequest =
        new io.learnsharegrow.freezertracker.api.freezers.FreezerCreateRequest();
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
    io.learnsharegrow.freezertracker.api.freezers.FreezerResponse created =
        objectMapper.readValue(response, io.learnsharegrow.freezertracker.api.freezers.FreezerResponse.class);
    return created.getId();
  }

  private UUID seedApiKey() {
    return seedApiKey("tester@example.com", "Test house", true);
  }

  private UUID seedApiKey(String username, String houseName, boolean primaryHouse) {
    House house = new House();
    house.setId(UUID.randomUUID());
    house.setName(houseName);
    houseRepository.save(house);
    if (primaryHouse) {
      houseId = house.getId();
    }

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

  private void seedShelfItems(Integer freezerId, Integer shelfNumber, int count, int referenceStart) {
    for (int i = 0; i < count; i++) {
      Item item = new Item();
      item.setId(UUID.randomUUID());
      item.setReference(String.format("%08d", referenceStart + i));
      item.setFreezeDate(LocalDate.of(2024, 1, 1));
      item.setBestBefore(LocalDate.of(2025, 1, 1));
      item.setDescription("Seed item " + i);
      item.setFreezerId(freezerId);
      item.setShelfNumber(shelfNumber);
      item.setHouseId(houseId);
      itemRepository.save(item);
    }
  }
}
