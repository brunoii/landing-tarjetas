package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryRepository;
import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.shared.TransactionType;
import com.gentleia.landingtarjetas.statement.CardStatement;
import com.gentleia.landingtarjetas.statement.CardStatementRepository;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;
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
class TransactionControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CardStatementRepository statementRepository;
    @Autowired
    private StatementTransactionRepository transactionRepository;
    @Autowired
    private InstallmentProjectionRepository projectionRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        projectionRepository.deleteAll();
        transactionRepository.deleteAll();
        statementRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void postCreatesDraftStatementTransaction() throws Exception {
        CardStatement statement = saveStatement(StatementStatus.DRAFT);
        Category category = categoryRepository.save(new Category("Fixture manual category", "#123456"));

        mockMvc.perform(post("/api/statements/{id}/transactions", statement.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statementId").value(statement.getId()))
                .andExpect(jsonPath("$.paymentMonth").value("2026-06-01"))
                .andExpect(jsonPath("$.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.description").value("Fixture added purchase"))
                .andExpect(jsonPath("$.category.id").value(category.getId()))
                .andExpect(jsonPath("$.amountPesos").value(66.25));

        assertThat(transactionRepository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getStatement().getId()).isEqualTo(statement.getId());
            assertThat(saved.getDescription()).isEqualTo("Fixture added purchase");
            assertThat(saved.getAmountPesos()).isEqualByComparingTo("66.25");
            assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        });
    }

    @Test
    void postRejectsConfirmedStatementTransactionCreate() throws Exception {
        CardStatement statement = saveStatement(StatementStatus.CONFIRMED);

        mockMvc.perform(post("/api/statements/{id}/transactions", statement.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Only draft statement transactions can be created"));

        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void putRejectsConfirmedStatementTransaction() throws Exception {
        StatementTransaction transaction = saveTransaction(saveStatement(StatementStatus.CONFIRMED),
                "Fixture confirmed purchase", new BigDecimal("50.00"));

        mockMvc.perform(put("/api/transactions/{id}", transaction.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Only draft statement transactions can be modified"));

        assertThat(transactionRepository.findById(transaction.getId()))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getDescription()).isEqualTo("Fixture confirmed purchase");
                    assertThat(saved.getAmountPesos()).isEqualByComparingTo("50.00");
                    assertThat(saved.getNotes()).isNull();
                });
    }

    @Test
    void deleteRejectsConfirmedStatementTransaction() throws Exception {
        StatementTransaction transaction = saveTransaction(saveStatement(StatementStatus.CONFIRMED),
                "Fixture confirmed purchase", new BigDecimal("50.00"));

        mockMvc.perform(delete("/api/transactions/{id}", transaction.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Only draft statement transactions can be modified"));

        assertThat(transactionRepository.existsById(transaction.getId())).isTrue();
    }

    @Test
    void putAllowsDraftStatementTransaction() throws Exception {
        StatementTransaction transaction = saveTransaction(saveStatement(StatementStatus.DRAFT),
                "Fixture draft purchase", new BigDecimal("50.00"));

        mockMvc.perform(put("/api/transactions/{id}", transaction.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Fixture reviewed purchase"))
                .andExpect(jsonPath("$.amountPesos").value(75.00))
                .andExpect(jsonPath("$.notes").value("Reviewed note"));

        assertThat(transactionRepository.findById(transaction.getId()))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getDescription()).isEqualTo("Fixture reviewed purchase");
                    assertThat(saved.getAmountPesos()).isEqualByComparingTo("75.00");
                    assertThat(saved.getNotes()).isEqualTo("Reviewed note");
                });
    }

    @Test
    void deleteAllowsDraftStatementTransaction() throws Exception {
        StatementTransaction transaction = saveTransaction(saveStatement(StatementStatus.DRAFT),
                "Fixture draft purchase", new BigDecimal("50.00"));

        mockMvc.perform(delete("/api/transactions/{id}", transaction.getId()))
                .andExpect(status().isNoContent());

        assertThat(transactionRepository.existsById(transaction.getId())).isFalse();
    }

    private CardStatement saveStatement(StatementStatus status) {
        CardStatement statement = new CardStatement(Provider.MANUAL, CardBrand.VISA);
        statement.setPaymentMonth(LocalDate.of(2026, 6, 1));
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement.setStatus(status);
        return statementRepository.save(statement);
    }

    private StatementTransaction saveTransaction(CardStatement statement, String description, BigDecimal amountPesos) {
        StatementTransaction transaction = new StatementTransaction(statement, description, TransactionType.PURCHASE);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 10));
        transaction.setAmountPesos(amountPesos);
        return transactionRepository.save(transaction);
    }

    private String updatePayload() {
        return """
                {
                  "transactionDate": "2026-06-11",
                  "description": "Fixture reviewed purchase",
                  "type": "PURCHASE",
                  "categoryId": null,
                  "amountPesos": 75.00,
                  "amountUsd": null,
                  "currentInstallment": null,
                  "totalInstallments": null,
                  "notes": "Reviewed note"
                }
                """;
    }

    private String createPayload(Long categoryId) {
        String categoryValue = categoryId == null ? "null" : categoryId.toString();
        return """
                {
                  "transactionDate": "2026-06-12",
                  "description": "Fixture added purchase",
                  "type": "PURCHASE",
                  "categoryId": %s,
                  "amountPesos": 66.25,
                  "amountUsd": null,
                  "currentInstallment": null,
                  "totalInstallments": null,
                  "notes": "Added manually"
                }
                """.formatted(categoryValue);
    }
}
