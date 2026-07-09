package com.gentleia.landingtarjetas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.gentleia.landingtarjetas.income.Income;
import com.gentleia.landingtarjetas.income.IncomeRepository;
import com.gentleia.landingtarjetas.income.IncomeType;
import com.gentleia.landingtarjetas.manualexpense.ManualExpenseRepository;
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

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

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
    @Autowired
    private IncomeRepository incomeRepository;
    @Autowired
    private ManualExpenseRepository manualExpenseRepository;

    @BeforeEach
    void cleanDatabase() {
        manualExpenseRepository.deleteAll();
        incomeRepository.deleteAll();
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
    void dashboardSummaryIncludesIncomeExpenseAndMonthlyBalanceForSelectedMonth() throws Exception {
        saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 6, 1), null, true);
        saveIncome("Fixture bonus", IncomeType.VARIABLE, "90000.00", LocalDate.of(2026, 7, 1), null, false);
        saveIncome("Fixture other month", IncomeType.VARIABLE, "1.00", LocalDate.of(2026, 8, 1), null, false);

        CardStatement statement = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        statement.setStatus(StatementStatus.DRAFT);
        statement.setCardAlias("Santander Visa");
        statement.setPaymentMonth(LocalDate.of(2026, 7, 1));
        statement.setTotalPesos(new BigDecimal("120000.00"));
        statement = statementRepository.save(statement);
        StatementTransaction purchase = new StatementTransaction(statement, "Fixture purchase", TransactionType.PURCHASE);
        purchase.setAmountPesos(new BigDecimal("120000.00"));
        transactionRepository.save(purchase);
        statementService.confirm(statement.getId());

        mockMvc.perform(get("/api/dashboard/summary").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeTotalPesos").value(590000.00))
                .andExpect(jsonPath("$.salaryIncomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.variableIncomeTotalPesos").value(90000.00))
                .andExpect(jsonPath("$.projectedIncomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.expenseTotalPesos").value(120000.00))
                .andExpect(jsonPath("$.totalPesos").value(120000.00))
                .andExpect(jsonPath("$.monthlyBalancePesos").value(470000.00))
                .andExpect(jsonPath("$.incomeCount").value(2))
                .andExpect(jsonPath("$.transactionCount").value(1));
    }

    @Test
    void dashboardSummaryIncludesProjectedObligationsForProjectionOnlyMonth() throws Exception {
        saveIncome("Fixture salary", IncomeType.SALARY, "500000.00", LocalDate.of(2026, 7, 1), null, true);

        CardStatement statement = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        statement.setStatus(StatementStatus.DRAFT);
        statement.setCardAlias("Santander Visa");
        statement.setPaymentMonth(LocalDate.of(2026, 7, 1));
        statement.setTotalPesos(new BigDecimal("120000.00"));
        statement = statementRepository.save(statement);
        StatementTransaction purchase = new StatementTransaction(statement, "Fixture projected installment", TransactionType.INSTALLMENT);
        purchase.setAmountPesos(new BigDecimal("120000.00"));
        purchase.setCurrentInstallment(1);
        purchase.setTotalInstallments(2);
        transactionRepository.save(purchase);
        statementService.confirm(statement.getId());

        mockMvc.perform(get("/api/dashboard/months/2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectionOnly").value(true))
                .andExpect(jsonPath("$.totalPesos").value(120000.00));

        mockMvc.perform(get("/api/dashboard/summary").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.salaryIncomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.variableIncomeTotalPesos").value(0))
                .andExpect(jsonPath("$.projectedIncomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.expenseTotalPesos").value(120000.00))
                .andExpect(jsonPath("$.totalPesos").value(120000.00))
                .andExpect(jsonPath("$.monthlyBalancePesos").value(380000.00))
                .andExpect(jsonPath("$.estimated").value(true))
                .andExpect(jsonPath("$.incomeCount").value(1))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    @Test
    void dashboardSummaryMarksFutureSelectedMonthAsEstimated() throws Exception {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate futureMonth = currentMonth.plusMonths(1);
        saveIncome("Fixture future salary", IncomeType.SALARY, "500000.00", currentMonth, null, true);

        CardStatement statement = new CardStatement(Provider.SANTANDER, CardBrand.VISA);
        statement.setStatus(StatementStatus.DRAFT);
        statement.setCardAlias("Santander Visa");
        statement.setPaymentMonth(currentMonth);
        statement.setTotalPesos(new BigDecimal("120000.00"));
        statement = statementRepository.save(statement);
        StatementTransaction purchase = new StatementTransaction(statement, "Fixture projected installment", TransactionType.INSTALLMENT);
        purchase.setAmountPesos(new BigDecimal("120000.00"));
        purchase.setCurrentInstallment(1);
        purchase.setTotalInstallments(2);
        transactionRepository.save(purchase);
        statementService.confirm(statement.getId());

        mockMvc.perform(get("/api/dashboard/summary").param("month", MONTH_FORMATTER.format(futureMonth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimated").value(true))
                .andExpect(jsonPath("$.incomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.projectedIncomeTotalPesos").value(500000.00))
                .andExpect(jsonPath("$.monthlyBalancePesos").value(380000.00));
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

    private Income saveIncome(String description, IncomeType incomeType, String amountPesos, LocalDate startMonth,
                              LocalDate endMonth, boolean recurringMonthly) {
        Income income = new Income(description, incomeType, new BigDecimal(amountPesos), startMonth, recurringMonthly);
        income.setEndMonth(endMonth);
        return incomeRepository.save(income);
    }
}
