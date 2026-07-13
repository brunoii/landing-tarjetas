package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gentleia.landingtarjetas.income.Income;
import com.gentleia.landingtarjetas.income.IncomeRepository;
import com.gentleia.landingtarjetas.income.IncomeType;
import com.gentleia.landingtarjetas.manualexpense.ManualExpense;
import com.gentleia.landingtarjetas.manualexpense.ManualExpenseRepository;
import com.gentleia.landingtarjetas.manualexpense.ManualExpenseService;
import com.gentleia.landingtarjetas.projection.InstallmentProjection;
import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.statement.CardStatement;
import com.gentleia.landingtarjetas.statement.CardStatementRepository;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final String UNCATEGORIZED = "Uncategorized";

    private final CardStatementRepository statementRepository;
    private final StatementTransactionRepository transactionRepository;
    private final InstallmentProjectionRepository projectionRepository;
    private final IncomeRepository incomeRepository;
    private final ManualExpenseRepository manualExpenseRepository;
    private final ManualExpenseService manualExpenseService;

    public DashboardService(CardStatementRepository statementRepository,
                            StatementTransactionRepository transactionRepository,
                            InstallmentProjectionRepository projectionRepository,
                            IncomeRepository incomeRepository,
                            ManualExpenseRepository manualExpenseRepository,
                            ManualExpenseService manualExpenseService) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.projectionRepository = projectionRepository;
        this.incomeRepository = incomeRepository;
        this.manualExpenseRepository = manualExpenseRepository;
        this.manualExpenseService = manualExpenseService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(String month) {
        var paymentMonth = DateParsers.parseYearMonth(month);
        List<StatementTransaction> transactions = transactionRepository.findConfirmedWithFilters(paymentMonth, null, null, null);
        DashboardMonthDetailResponse monthDetail = monthDetail(month);
        BigDecimal expenseTotalPesos = monthDetail.totalPesos();
        BigDecimal expenseTotalUsd = monthDetail.totalUsd();
        List<Income> incomes = incomeRepository.findAllByOrderByStartMonthAscIdAsc().stream()
                .filter(income -> incomeAppliesToMonth(income, paymentMonth))
                .toList();
        BigDecimal incomeTotalPesos = sumIncomePesos(incomes);
        BigDecimal salaryIncomeTotalPesos = sumIncomePesosByType(incomes, IncomeType.SALARY);
        BigDecimal variableIncomeTotalPesos = sumIncomePesosByType(incomes, IncomeType.VARIABLE);
        BigDecimal projectedIncomeTotalPesos = incomes.stream()
                .filter(income -> isProjectedIncomeForMonth(income, paymentMonth))
                .map(Income::getAmountPesos)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean estimated = isFutureMonth(paymentMonth) || monthDetail.projectionOnly();
        long statementCount = statementRepository.findWithFiltersAndStatus(paymentMonth, null, StatementStatus.CONFIRMED).size();

        return new DashboardSummaryResponse(
                paymentMonth,
                expenseTotalPesos,
                expenseTotalUsd,
                incomeTotalPesos,
                salaryIncomeTotalPesos,
                variableIncomeTotalPesos,
                projectedIncomeTotalPesos,
                expenseTotalPesos,
                incomeTotalPesos.subtract(expenseTotalPesos),
                estimated,
                statementCount,
                transactions.size(),
                incomes.size()
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardMonthResponse> months() {
        Map<LocalDate, MonthPresence> months = new TreeMap<>();
        for (LocalDate month : statementRepository.findConfirmedPaymentMonths()) {
            months.computeIfAbsent(month, ignored -> new MonthPresence()).confirmed = true;
        }
        for (LocalDate month : projectionRepository.findActiveProjectedMonths()) {
            months.computeIfAbsent(month, ignored -> new MonthPresence()).projected = true;
        }
        for (ManualExpense expense : manualExpenseRepository.findAllByOrderByStartMonthAscIdAsc()) {
            months.computeIfAbsent(expense.getStartMonth(), ignored -> new MonthPresence()).confirmed = true;
            for (LocalDate projectedMonth : projectedMonths(expense)) {
                months.computeIfAbsent(projectedMonth, ignored -> new MonthPresence()).projected = true;
            }
        }
        return months.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, MonthPresence>comparingByKey().reversed())
                .map(entry -> DashboardMonthResponse.of(entry.getKey(), entry.getValue().confirmed, entry.getValue().projected))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardMonthDetailResponse monthDetail(String month) {
        LocalDate paymentMonth = DateParsers.parseYearMonth(month);
        List<StatementTransaction> transactions = transactionRepository.findConfirmedWithFilters(paymentMonth, null, null, null);
        boolean hasConfirmedStatementData = !transactions.isEmpty()
                || !statementRepository.findWithFiltersAndStatus(paymentMonth, null, StatementStatus.CONFIRMED).isEmpty();
        List<InstallmentProjection> projections = hasConfirmedStatementData
                ? List.of()
                : projectionRepository.findActiveDetailByProjectedMonth(paymentMonth);

        List<DashboardMonthDetailRowResponse> rows = new ArrayList<>();
        transactions.forEach(transaction -> rows.add(actualRow(transaction, paymentMonth)));
        projections.forEach(projection -> rows.add(projectionRow(projection)));
        rows.addAll(manualExpenseRows(paymentMonth));
        rows.sort(Comparator
                .comparing(DashboardMonthDetailRowResponse::cardBrand)
                .thenComparing(DashboardMonthDetailRowResponse::description, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(DashboardMonthDetailRowResponse::kind));

        BigDecimal totalPesos = rows.stream()
                .map(DashboardMonthDetailRowResponse::amountPesos)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUsd = rows.stream()
                .map(DashboardMonthDetailRowResponse::amountUsd)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasActualManualRows = rows.stream().anyMatch(row -> "ACTUAL".equals(row.kind()) && row.sourceStatementId() == null);
        boolean currentReal = hasConfirmedStatementData || hasActualManualRows;
        boolean hasProjection = rows.stream().anyMatch(row -> "PROJECTION".equals(row.kind()));
        return new DashboardMonthDetailResponse(
                paymentMonth,
                currentReal,
                !currentReal && hasProjection,
                totalPesos,
                totalUsd,
                cardTotals(rows),
                rows
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> categoryBreakdown(String month) {
        var paymentMonth = DateParsers.parseYearMonth(month);
        Map<String, MutableCategoryTotals> totals = new LinkedHashMap<>();
        for (StatementTransaction transaction : transactionRepository.findConfirmedWithFilters(paymentMonth, null, null, null)) {
            Long categoryId = transaction.getCategory() == null ? null : transaction.getCategory().getId();
            String categoryName = transaction.getCategory() == null ? UNCATEGORIZED : transaction.getCategory().getName();
            String key = categoryId == null ? UNCATEGORIZED : categoryId.toString();
            MutableCategoryTotals current = totals.computeIfAbsent(key, ignored -> new MutableCategoryTotals(categoryId, categoryName));
            current.add(transaction);
        }
        return totals.values().stream()
                .map(MutableCategoryTotals::toResponse)
                .toList();
    }

    private BigDecimal sumIncomePesos(List<Income> incomes) {
        return incomes.stream()
                .map(Income::getAmountPesos)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumIncomePesosByType(List<Income> incomes, IncomeType type) {
        return incomes.stream()
                .filter(income -> income.getIncomeType() == type)
                .map(Income::getAmountPesos)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean incomeAppliesToMonth(Income income, LocalDate paymentMonth) {
        if (paymentMonth == null) {
            return true;
        }
        if (!income.isRecurringMonthly()) {
            return income.getStartMonth().equals(paymentMonth);
        }
        return !income.getStartMonth().isAfter(paymentMonth)
                && (income.getEndMonth() == null || !income.getEndMonth().isBefore(paymentMonth));
    }

    private boolean isProjectedIncomeForMonth(Income income, LocalDate paymentMonth) {
        return paymentMonth != null
                && income.isRecurringMonthly()
                && paymentMonth.isAfter(income.getStartMonth());
    }

    private boolean isFutureMonth(LocalDate paymentMonth) {
        return paymentMonth != null && paymentMonth.isAfter(LocalDate.now().withDayOfMonth(1));
    }

    private DashboardMonthDetailRowResponse actualRow(StatementTransaction transaction, LocalDate month) {
        CardStatement statement = transaction.getStatement();
        return rowFrom(new MonthDetailRowDraft()
                .kind("ACTUAL")
                .sourceStatement(statement.getId(), statement.getPaymentMonth())
                .sourceTransaction(transaction.getId(), transaction.getTransactionDate())
                .month(month)
                .description(transaction.getDescription())
                .medium(statement.getProvider(), statement.getCardBrand(), statement.getCardAlias())
                .type(transaction.getType().name())
                .category(transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                        transaction.getCategory() == null ? null : transaction.getCategory().getName())
                .installments(transaction.getCurrentInstallment(), transaction.getTotalInstallments())
                .amounts(transaction.getAmountPesos(), transaction.getAmountUsd())
                .estimatedFinishMonth(finishMonth(statement.getPaymentMonth(), transaction.getCurrentInstallment(), transaction.getTotalInstallments()))
                .notes(transaction.getNotes())
                .source("STATEMENT"));
    }

    private DashboardMonthDetailRowResponse projectionRow(InstallmentProjection projection) {
        StatementTransaction transaction = projection.getSourceTransaction();
        CardStatement statement = transaction.getStatement();
        return rowFrom(new MonthDetailRowDraft()
                .kind("PROJECTION")
                .sourceStatement(statement.getId(), statement.getPaymentMonth())
                .sourceTransaction(transaction.getId(), transaction.getTransactionDate())
                .month(projection.getProjectedMonth())
                .description(transaction.getDescription())
                .medium(statement.getProvider(), statement.getCardBrand(), statement.getCardAlias())
                .type(transaction.getType().name())
                .category(transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                        transaction.getCategory() == null ? null : transaction.getCategory().getName())
                .installments(projection.getInstallmentNumber(), projection.getTotalInstallments())
                .amounts(projection.getAmountPesos(), projection.getAmountUsd())
                .estimatedFinishMonth(finishMonth(projection.getProjectedMonth(), projection.getInstallmentNumber(), projection.getTotalInstallments()))
                .notes(transaction.getNotes())
                .source("STATEMENT"));
    }

    private List<DashboardMonthDetailRowResponse> manualExpenseRows(LocalDate paymentMonth) {
        return manualExpenseRepository.findAllByOrderByStartMonthAscIdAsc().stream()
                .filter(expense -> manualExpenseService.appliesToMonth(expense, paymentMonth))
                .map(expense -> manualExpenseRow(expense, paymentMonth))
                .toList();
    }

    private DashboardMonthDetailRowResponse manualExpenseRow(ManualExpense expense, LocalDate paymentMonth) {
        boolean projected = manualExpenseService.isProjectedForMonth(expense, paymentMonth);
        Integer installmentNumber = manualExpenseService.installmentNumberForMonth(expense, paymentMonth);
        return rowFrom(new MonthDetailRowDraft()
                .kind(projected ? "PROJECTION" : "ACTUAL")
                .sourceStatement(null, expense.getStartMonth())
                .sourceTransaction(expense.getId(), null)
                .month(paymentMonth)
                .description(expense.getDescription())
                .medium(Provider.MANUAL, CardBrand.OTHER, manualSourceLabel(expense))
                .type(expense.getType().name())
                .category(expense.getCategory() == null ? null : expense.getCategory().getId(),
                        expense.getCategory() == null ? null : expense.getCategory().getName())
                .installments(installmentNumber, expense.getTotalInstallments())
                .amounts(expense.getAmountPesos(), expense.getAmountUsd())
                .estimatedFinishMonth(finishMonth(expense.getStartMonth(), manualExpenseService.effectiveCurrentInstallment(expense), expense.getTotalInstallments()))
                .notes(expense.getNotes())
                .source("MANUAL_EXPENSE"));
    }

    private DashboardMonthDetailRowResponse rowFrom(MonthDetailRowDraft row) {
        return new DashboardMonthDetailRowResponse(
                row.kind,
                row.sourceStatementId,
                row.sourceStatementMonth,
                row.sourceTransactionId,
                row.transactionDate,
                row.month,
                row.description,
                row.provider,
                row.cardBrand,
                row.cardAlias,
                row.type,
                row.categoryId,
                row.categoryName,
                row.installmentNumber,
                row.totalInstallments,
                row.amountPesos,
                row.amountUsd,
                row.estimatedFinishMonth,
                row.notes,
                row.source
        );
    }

    private String manualSourceLabel(ManualExpense expense) {
        return switch (expense.getType()) {
            case LOAN -> "Préstamo manual";
            case CASH -> "Efectivo";
            default -> "Gasto manual";
        };
    }

    private List<LocalDate> projectedMonths(ManualExpense expense) {
        Integer currentInstallment = manualExpenseService.effectiveCurrentInstallment(expense);
        if (currentInstallment == null || expense.getTotalInstallments() == null
                || currentInstallment >= expense.getTotalInstallments()) {
            return List.of();
        }
        List<LocalDate> months = new ArrayList<>();
        for (int installment = currentInstallment + 1; installment <= expense.getTotalInstallments(); installment++) {
            months.add(expense.getStartMonth().plusMonths(installment - currentInstallment));
        }
        return months;
    }

    private LocalDate finishMonth(LocalDate baseMonth, Integer installmentNumber, Integer totalInstallments) {
        if (baseMonth == null || installmentNumber == null || totalInstallments == null || installmentNumber > totalInstallments) {
            return null;
        }
        return baseMonth.plusMonths(totalInstallments - installmentNumber);
    }

    private List<DashboardCardTotalResponse> cardTotals(List<DashboardMonthDetailRowResponse> rows) {
        Map<String, MutableCardTotals> totals = new LinkedHashMap<>();
        for (DashboardMonthDetailRowResponse row : rows) {
            String key = row.provider() + "|" + row.cardBrand() + "|" + (row.cardAlias() == null ? "" : row.cardAlias());
            totals.computeIfAbsent(key, ignored -> new MutableCardTotals(row)).add(row);
        }
        return totals.values().stream()
                .map(MutableCardTotals::toResponse)
                .toList();
    }

    private static class MonthDetailRowDraft {
        private String kind;
        private Long sourceStatementId;
        private LocalDate sourceStatementMonth;
        private Long sourceTransactionId;
        private LocalDate transactionDate;
        private LocalDate month;
        private String description;
        private Provider provider;
        private CardBrand cardBrand;
        private String cardAlias;
        private String type;
        private Long categoryId;
        private String categoryName;
        private Integer installmentNumber;
        private Integer totalInstallments;
        private BigDecimal amountPesos;
        private BigDecimal amountUsd;
        private LocalDate estimatedFinishMonth;
        private String notes;
        private String source;

        MonthDetailRowDraft kind(String kind) {
            this.kind = kind;
            return this;
        }

        MonthDetailRowDraft sourceStatement(Long sourceStatementId, LocalDate sourceStatementMonth) {
            this.sourceStatementId = sourceStatementId;
            this.sourceStatementMonth = sourceStatementMonth;
            return this;
        }

        MonthDetailRowDraft sourceTransaction(Long sourceTransactionId, LocalDate transactionDate) {
            this.sourceTransactionId = sourceTransactionId;
            this.transactionDate = transactionDate;
            return this;
        }

        MonthDetailRowDraft month(LocalDate month) {
            this.month = month;
            return this;
        }

        MonthDetailRowDraft description(String description) {
            this.description = description;
            return this;
        }

        MonthDetailRowDraft medium(Provider provider, CardBrand cardBrand, String cardAlias) {
            this.provider = provider;
            this.cardBrand = cardBrand;
            this.cardAlias = cardAlias;
            return this;
        }

        MonthDetailRowDraft type(String type) {
            this.type = type;
            return this;
        }

        MonthDetailRowDraft category(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            return this;
        }

        MonthDetailRowDraft installments(Integer installmentNumber, Integer totalInstallments) {
            this.installmentNumber = installmentNumber;
            this.totalInstallments = totalInstallments;
            return this;
        }

        MonthDetailRowDraft amounts(BigDecimal amountPesos, BigDecimal amountUsd) {
            this.amountPesos = amountPesos;
            this.amountUsd = amountUsd;
            return this;
        }

        MonthDetailRowDraft estimatedFinishMonth(LocalDate estimatedFinishMonth) {
            this.estimatedFinishMonth = estimatedFinishMonth;
            return this;
        }

        MonthDetailRowDraft notes(String notes) {
            this.notes = notes;
            return this;
        }

        MonthDetailRowDraft source(String source) {
            this.source = source;
            return this;
        }
    }

    private static class MonthPresence {
        private boolean confirmed;
        private boolean projected;
    }

    private static class MutableCardTotals {
        private final com.gentleia.landingtarjetas.shared.CardBrand cardBrand;
        private final com.gentleia.landingtarjetas.shared.Provider provider;
        private final String cardAlias;
        private BigDecimal totalPesos = BigDecimal.ZERO;
        private BigDecimal totalUsd = BigDecimal.ZERO;
        private long rowCount;

        MutableCardTotals(DashboardMonthDetailRowResponse row) {
            this.provider = row.provider();
            this.cardBrand = row.cardBrand();
            this.cardAlias = row.cardAlias();
        }

        void add(DashboardMonthDetailRowResponse row) {
            if (row.amountPesos() != null) {
                totalPesos = totalPesos.add(row.amountPesos());
            }
            if (row.amountUsd() != null) {
                totalUsd = totalUsd.add(row.amountUsd());
            }
            rowCount++;
        }

        DashboardCardTotalResponse toResponse() {
            return new DashboardCardTotalResponse(provider, cardBrand, cardAlias, totalPesos, totalUsd, rowCount);
        }
    }

    private static class MutableCategoryTotals {
        private final Long categoryId;
        private final String categoryName;
        private BigDecimal totalPesos = BigDecimal.ZERO;
        private BigDecimal totalUsd = BigDecimal.ZERO;
        private long transactionCount;

        MutableCategoryTotals(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        void add(StatementTransaction transaction) {
            if (transaction.getAmountPesos() != null) {
                totalPesos = totalPesos.add(transaction.getAmountPesos());
            }
            if (transaction.getAmountUsd() != null) {
                totalUsd = totalUsd.add(transaction.getAmountUsd());
            }
            transactionCount++;
        }

        CategoryBreakdownResponse toResponse() {
            return new CategoryBreakdownResponse(categoryId, categoryName, totalPesos, totalUsd, transactionCount);
        }
    }
}
