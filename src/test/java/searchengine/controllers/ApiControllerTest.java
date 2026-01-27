package searchengine.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testStatisticsEndpoint() throws Exception {
        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.statistics").exists());
    }
    
    @Test
    void testSearchWithEmptyQuery() throws Exception {
        mockMvc.perform(get("/api/search").param("query", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.error").value("Задан пустой поисковый запрос"));
    }
    
    @Test
    void testSearchWithValidQuery() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("query", "тест")
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void testStartIndexingEndpoint() throws Exception {
        mockMvc.perform(get("/api/startIndexing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists());
    }
    
    @Test
    void testStopIndexingEndpoint() throws Exception {
        mockMvc.perform(get("/api/stopIndexing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists());
    }
}
