package com.interviewprep.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    @BeforeEach
    void cleanDatabase() {
        expenseRepository.deleteAll();
    }

    @Test
    void summaryIncludesFirstAndLastDayOfMonthAndExcludesNeighbours() throws Exception {
        create("99.00", "FOOD", "2028-01-31");   // day before: excluded
        create("0.10", "FOOD", "2028-02-01");    // first day
        create("0.20", "FOOD", "2028-02-29");    // last day (leap year)
        create("10.00", "TRAVEL", "2028-02-15");
        create("99.00", "FOOD", "2028-03-01");   // day after: excluded

        String body = mockMvc.perform(get("/api/expenses/summary").param("month", "2028-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2028-02"))
                .andReturn().getResponse().getContentAsString();

        // Compare the raw JSON text so the check is exact, not via a double.
        assertThat(body)
                .contains("\"FOOD\":0.30")
                .contains("\"TRAVEL\":10.00")
                .contains("\"BILLS\":0.00")
                .contains("\"OTHER\":0.00")
                .contains("\"total\":10.30");
    }

    @Test
    void summaryCoversThirtyFirstDay() throws Exception {
        create("1.00", "BILLS", "2026-01-01");
        create("2.00", "BILLS", "2026-01-31");

        String body = mockMvc.perform(get("/api/expenses/summary").param("month", "2026-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"BILLS\":3.00").contains("\"total\":3.00");
    }

    @Test
    void summaryWithInvalidMonthReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/expenses/summary").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void amountWithMoreThanTwoDecimalsIsRejected() throws Exception {
        postExpense("0.001", "FOOD", "2026-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void zeroOrNegativeAmountIsRejected() throws Exception {
        postExpense("0", "FOOD", "2026-01-01").andExpect(status().isBadRequest());
        postExpense("-5.00", "FOOD", "2026-01-01").andExpect(status().isBadRequest());
    }

    @Test
    void missingFieldsAreReported() throws Exception {
        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.date").exists());
    }

    @Test
    void unknownCategoryIsRejectedWithAllowedValues() throws Exception {
        postExpense("5.00", "PIZZA", "2026-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value for 'category'. Allowed values: FOOD, TRAVEL, BILLS, OTHER"));
    }

    @Test
    void listFiltersByDateRangeAndCategory() throws Exception {
        create("1.00", "FOOD", "2026-01-10");
        create("2.00", "TRAVEL", "2026-01-20");
        create("3.00", "FOOD", "2026-02-05");

        mockMvc.perform(get("/api/expenses").param("from", "2026-01-10").param("to", "2026-01-20"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/expenses").param("category", "FOOD"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01").param("to", "2026-01-31").param("category", "FOOD"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date").value("2026-01-10"));
    }

    @Test
    void listRejectsFromAfterTo() throws Exception {
        mockMvc.perform(get("/api/expenses").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'from' must not be after 'to'"));
    }

    @Test
    void updateAndDeleteExpense() throws Exception {
        long id = create("5.00", "FOOD", "2026-01-01");

        mockMvc.perform(put("/api/expenses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("7.50", "OTHER", "2026-01-02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(7.50))
                .andExpect(jsonPath("$.category").value("OTHER"));

        mockMvc.perform(delete("/api/expenses/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/expenses/{id}", id)).andExpect(status().isNotFound());
    }

    private long create(String amount, String category, String date) throws Exception {
        String body = postExpense(amount, category, date)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions postExpense(String amount, String category, String date) throws Exception {
        return mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(amount, category, date)));
    }

    private static String json(String amount, String category, String date) {
        return "{\"amount\":" + amount + ",\"category\":\"" + category + "\",\"date\":\"" + date + "\"}";
    }
}
