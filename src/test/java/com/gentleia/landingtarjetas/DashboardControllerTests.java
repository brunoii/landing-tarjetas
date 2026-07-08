package com.gentleia.landingtarjetas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.gentleia.landingtarjetas.statement.StatementService;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StatementService statementService;
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
    void dashboardMonthEndpointsExposeRealAndProjectedMonths() throws Exception {
        CardStatement statement = new CardStatement(Provider.MANUAL, CardBrand.VISA);
        statement.setStatus(StatementStatus.DRAFT);
        statement.setPaymentMonth(LocalDate.of(2026, 7, 1));
        statement.setTotalPesos(new BigDecimal("100.00"));
        statement = statementRepository.save(statement);
        StatementTransaction transaction = new StatementTransaction(statement, "Fixture projected API row", TransactionType.INSTALLMENT);
        transaction.setAmountPesos(new BigDecimal("33.00"));
        transaction.setCurrentInstallment(1);
        transaction.setTotalInstallments(2);
        transactionRepository.save(transaction);
        statementService.confirm(statement.getId());

        mockMvc.perform(get("/api/dashboard/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.yearMonth == '2026-07' && @.currentReal == true)]").exists())
                .andExpect(jsonPath("$[?(@.yearMonth == '2026-08' && @.projectionOnly == true)]").exists());

        mockMvc.perform(get("/api/dashboard/months/2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectionOnly").value(true))
                .andExpect(jsonPath("$.totalPesos").value(33.00))
                .andExpect(jsonPath("$.rows[0].kind").value("PROJECTION"))
                .andExpect(jsonPath("$.rows[0].installmentNumber").value(2))
                .andExpect(jsonPath("$.rows[0].totalInstallments").value(2));
    }

    @Test
    void dashboardSummaryRejectsInvalidMonthWithSpanishError() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("month", "2026/07"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Use el formato de mes YYYY-MM"));
    }

    @Test
    void dashboardMonthDetailUsesTransactionSumsForSantanderVisaCardTotals() throws Exception {
        CardStatement statement = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        statement.setStatus(StatementStatus.DRAFT);
        statement.setCardAlias("Santander Visa");
        statement.setPaymentMonth(LocalDate.of(2026, 7, 1));
        statement.setTotalPesos(new BigDecimal("2012.38"));
        statement.setTotalUsd(BigDecimal.ZERO);
        statement.setMinimumPaymentPesos(new BigDecimal("2012.38"));
        statement = statementRepository.save(statement);
        StatementTransaction pesos = new StatementTransaction(statement, "Fixture local purchase", TransactionType.PURCHASE);
        pesos.setTransactionDate(LocalDate.of(2026, 6, 10));
        pesos.setAmountPesos(new BigDecimal("2012382.98"));
        StatementTransaction usd = new StatementTransaction(statement, "Fixture USD purchase", TransactionType.PURCHASE);
        usd.setTransactionDate(LocalDate.of(2026, 6, 11));
        usd.setAmountUsd(new BigDecimal("25.50"));
        transactionRepository.save(pesos);
        transactionRepository.save(usd);
        statementService.confirm(statement.getId());

        mockMvc.perform(get("/api/dashboard/months/2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPesos").value(2012382.98))
                .andExpect(jsonPath("$.totalUsd").value(25.50))
                .andExpect(jsonPath("$.totalsByCard[?(@.provider == 'SANTANDER' && @.cardBrand == 'VISA' && @.cardAlias == 'Santander Visa' && @.totalPesos == 2012382.98 && @.totalUsd == 25.50)]").exists());
    }

    @Test
    void dashboardMonthStatusIncludesConfirmedNaranjaStatementForSelectedMonth() throws Exception {
        CardStatement statement = new CardStatement(Provider.NARANJA_X, CardBrand.VISA);
        statement.setStatus(StatementStatus.DRAFT);
        statement.setCardAlias("Visa");
        statement.setPaymentMonth(LocalDate.of(2026, 7, 1));
        statement.setTotalPesos(new BigDecimal("300.00"));
        statement = statementRepository.save(statement);
        StatementTransaction transaction = new StatementTransaction(statement, "Fixture Naranja purchase", TransactionType.PURCHASE);
        transaction.setTransactionDate(LocalDate.of(2026, 7, 7));
        transaction.setAmountPesos(new BigDecimal("300.00"));
        transactionRepository.save(transaction);
        statementService.confirm(statement.getId());

        mockMvc.perform(get("/api/statements").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("NARANJA_X"))
                .andExpect(jsonPath("$[0].cardBrand").value("VISA"))
                .andExpect(jsonPath("$[0].cardAlias").value("Visa"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        mockMvc.perform(get("/api/dashboard/months/2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentReal").value(true))
                .andExpect(jsonPath("$.rows[0].provider").value("NARANJA_X"))
                .andExpect(jsonPath("$.rows[0].cardAlias").value("Visa"))
                .andExpect(jsonPath("$.totalsByCard[?(@.provider == 'NARANJA_X' && @.cardBrand == 'VISA' && @.cardAlias == 'Visa' && @.totalPesos == 300.00)]").exists());
    }
}
