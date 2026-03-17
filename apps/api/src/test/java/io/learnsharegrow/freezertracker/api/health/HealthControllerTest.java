package io.learnsharegrow.freezertracker.api.health;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnDatabaseOk() throws Exception {
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.databaseOk", is(true)))
        .andExpect(jsonPath("$.version", matchesPattern("^[0-9]+\\.[0-9]+\\.[0-9]+$")));
  }

  @Test
  void shouldReturnVersion() throws Exception {
    mockMvc
        .perform(get("/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version", matchesPattern("^[0-9]+\\.[0-9]+\\.[0-9]+$")));
  }
}
