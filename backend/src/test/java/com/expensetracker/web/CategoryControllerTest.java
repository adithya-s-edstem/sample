package com.expensetracker.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies {@code GET /api/categories} returns the fixed enum set, in
 * declaration order, as a JSON string array — matching {@code docs/api-contracts.md}.
 */
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAllCategoriesInContractOrder() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(9)))
                .andExpect(jsonPath("$[0]").value("FOOD"))
                .andExpect(jsonPath("$[1]").value("TRANSPORT"))
                .andExpect(jsonPath("$[2]").value("RENT"))
                .andExpect(jsonPath("$[3]").value("UTILITIES"))
                .andExpect(jsonPath("$[4]").value("GROCERIES"))
                .andExpect(jsonPath("$[5]").value("ENTERTAINMENT"))
                .andExpect(jsonPath("$[6]").value("HEALTH"))
                .andExpect(jsonPath("$[7]").value("SHOPPING"))
                .andExpect(jsonPath("$[8]").value("OTHER"));
    }
}
