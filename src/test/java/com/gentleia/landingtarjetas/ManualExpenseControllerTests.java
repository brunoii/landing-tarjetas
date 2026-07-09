package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryRepository;
import com.gentleia.landingtarjetas.manualexpense.ManualExpense;
import com.gentleia.landingtarjetas.manualexpense.ManualExpenseRepository;
import com.gentleia.landingtarjetas.manualexpense.ManualExpenseType;
import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.statement.CardStatementRepository;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ManualExpenseControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ManualExpenseRepository manualExpenseRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private InstallmentProjectionRepository projectionRepository;
    @Autowired
    private StatementTransactionRepository transactionRepository;
    @Autowired
    private CardStatementRepository statementRepository;

    @BeforeEach
    void cleanDatabase() {
        manualExpenseRepository.deleteAll();
        projectionRepository.deleteAll();
        transactionRepository.deleteAll();
        statementRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void postCreatesManualOnePaymentExpense() throws Exception {
        Category category = categoryRepository.save(new Category("Fixture manual expense category", "#123456"));

        mockMvc.perform(post("/api/manual-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Fixture manual purchase", "ONE_PAYMENT", "2026-07", null, null, category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Fixture manual purchase"))
                .andExpect(jsonPath("$.type").value("ONE_PAYMENT"))
                .andExpect(jsonPath("$.amountPesos").value(15000.00))
                .andExpect(jsonPath("$.amountUsd").value(12.50))
                .andExpect(jsonPath("$.startMonth").value("2026-07"))
                .andExpect(jsonPath("$.category.id").value(category.getId()))
                .andExpect(jsonPath("$.projected").value(false));

        assertThat(manualExpenseRepository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getDescription()).isEqualTo("Fixture manual purchase");
            assertThat(saved.getStartMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        });
    }

    @Test
    void postRejectsInstallmentWithoutTotalInstallments() throws Exception {
        mockMvc.perform(post("/api/manual-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Fixture invalid installment", "INSTALLMENT", "2026-07", null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los gastos en cuotas y préstamos requieren la cantidad total de cuotas"));
    }

    @Test
    void postRejectsMissingRequiredAmount() throws Exception {
        mockMvc.perform(post("/api/manual-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Fixture missing amount",
                                  "type": "ONE_PAYMENT",
                                  "startMonth": "2026-07"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Importe en pesos: es obligatorio')]").exists());
    }

    @Test
    void postRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/manual-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Fixture zero amount",
                                  "type": "ONE_PAYMENT",
                                  "amountPesos": 0,
                                  "startMonth": "2026-07"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La validación de la solicitud falló"))
                .andExpect(jsonPath("$.details[?(@ == 'Importe en pesos: debe ser mayor que cero')]").exists());
    }

    @Test
    void postRejectsCurrentInstallmentGreaterThanTotal() throws Exception {
        mockMvc.perform(post("/api/manual-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Fixture invalid loan", "LOAN", "2026-07", 4, 3, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cuota actual no puede superar el total de cuotas"));
    }

    @Test
    void listByMonthProjectsManualInstallmentsAndLoans() throws Exception {
        saveManualExpense("Fixture loan", ManualExpenseType.LOAN, LocalDate.of(2026, 7, 1), 1, 3);
        saveManualExpense("Fixture one payment", ManualExpenseType.ONE_PAYMENT, LocalDate.of(2026, 7, 1), null, null);

        mockMvc.perform(get("/api/manual-expenses").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.description == 'Fixture loan' && @.projected == false && @.installmentNumber == 1)]").exists())
                .andExpect(jsonPath("$[?(@.description == 'Fixture one payment' && @.projected == false)]").exists());

        mockMvc.perform(get("/api/manual-expenses").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Fixture loan"))
                .andExpect(jsonPath("$[0].projected").value(true))
                .andExpect(jsonPath("$[0].installmentNumber").value(2));
    }

    @Test
    void dashboardIncludesManualExpensesAndKeepsUsdSeparated() throws Exception {
        saveManualExpense("Fixture manual loan", ManualExpenseType.LOAN, LocalDate.of(2026, 7, 1), 1, 2);
        saveManualExpense("Fixture cash", ManualExpenseType.CASH, LocalDate.of(2026, 7, 1), null, null);

        mockMvc.perform(get("/api/dashboard/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.yearMonth == '2026-07' && @.currentReal == true)]").exists())
                .andExpect(jsonPath("$[?(@.yearMonth == '2026-08' && @.projectionOnly == true)]").exists());

        mockMvc.perform(get("/api/dashboard/months/2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentReal").value(true))
                .andExpect(jsonPath("$.totalPesos").value(30000.00))
                .andExpect(jsonPath("$.totalUsd").value(25.00))
                .andExpect(jsonPath("$.rows[?(@.description == 'Fixture manual loan' && @.kind == 'ACTUAL' && @.installmentNumber == 1)]").exists())
                .andExpect(jsonPath("$.rows[?(@.description == 'Fixture cash' && @.kind == 'ACTUAL')]").exists());

        mockMvc.perform(get("/api/dashboard/summary").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseTotalPesos").value(15000.00))
                .andExpect(jsonPath("$.totalUsd").value(12.50))
                .andExpect(jsonPath("$.estimated").value(true));
    }

    @Test
    void putAndDeleteUpdateManualExpenseLifecycle() throws Exception {
        ManualExpense expense = saveManualExpense("Fixture editable", ManualExpenseType.CASH, LocalDate.of(2026, 7, 1), null, null);

        mockMvc.perform(put("/api/manual-expenses/{id}", expense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("Fixture updated", "TAX", "2026-08", null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Fixture updated"))
                .andExpect(jsonPath("$.type").value("TAX"))
                .andExpect(jsonPath("$.startMonth").value("2026-08"));

        mockMvc.perform(delete("/api/manual-expenses/{id}", expense.getId()))
                .andExpect(status().isNoContent());

        assertThat(manualExpenseRepository.existsById(expense.getId())).isFalse();
    }

    private ManualExpense saveManualExpense(String description, ManualExpenseType type, LocalDate startMonth,
                                            Integer currentInstallment, Integer totalInstallments) {
        ManualExpense expense = new ManualExpense(description, type, new BigDecimal("15000.00"), startMonth);
        expense.setAmountUsd(new BigDecimal("12.50"));
        expense.setCurrentInstallment(currentInstallment);
        expense.setTotalInstallments(totalInstallments);
        return manualExpenseRepository.save(expense);
    }

    private String createPayload(String description, String type, String startMonth, Integer currentInstallment,
                                 Integer totalInstallments, Long categoryId) {
        String currentValue = currentInstallment == null ? "null" : currentInstallment.toString();
        String totalValue = totalInstallments == null ? "null" : totalInstallments.toString();
        String categoryValue = categoryId == null ? "null" : categoryId.toString();
        return """
                {
                  "description": "%s",
                  "type": "%s",
                  "amountPesos": 15000.00,
                  "amountUsd": 12.50,
                  "startMonth": "%s",
                  "currentInstallment": %s,
                  "totalInstallments": %s,
                  "categoryId": %s,
                  "notes": "Fixture notes"
                }
                """.formatted(description, type, startMonth, currentValue, totalValue, categoryValue);
    }
}
