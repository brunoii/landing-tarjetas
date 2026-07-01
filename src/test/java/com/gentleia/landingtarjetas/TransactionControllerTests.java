package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @BeforeEach
    void cleanDatabase() {
        projectionRepository.deleteAll();
        transactionRepository.deleteAll();
        statementRepository.deleteAll();
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
}
