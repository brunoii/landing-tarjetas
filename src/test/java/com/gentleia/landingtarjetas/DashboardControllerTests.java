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
}
