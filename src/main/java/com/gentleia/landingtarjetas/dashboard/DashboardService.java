package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gentleia.landingtarjetas.projection.InstallmentProjection;
import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.shared.DateParsers;
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

    public DashboardService(CardStatementRepository statementRepository,
                            StatementTransactionRepository transactionRepository,
                            InstallmentProjectionRepository projectionRepository) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.projectionRepository = projectionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(String month) {
        var paymentMonth = DateParsers.parseYearMonth(month);
        List<StatementTransaction> transactions = transactionRepository.findConfirmedWithFilters(paymentMonth, null, null, null);
        BigDecimal totalPesos = sumPesos(transactions);
        BigDecimal totalUsd = sumUsd(transactions);
        long statementCount = statementRepository.findWithFiltersAndStatus(paymentMonth, null, StatementStatus.CONFIRMED).size();

        return new DashboardSummaryResponse(paymentMonth, totalPesos, totalUsd, statementCount, transactions.size());
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
        return months.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, MonthPresence>comparingByKey().reversed())
                .map(entry -> DashboardMonthResponse.of(entry.getKey(), entry.getValue().confirmed, entry.getValue().projected))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardMonthDetailResponse monthDetail(String month) {
        LocalDate paymentMonth = DateParsers.parseYearMonth(month);
        List<StatementTransaction> transactions = transactionRepository.findConfirmedWithFilters(paymentMonth, null, null, null);
        boolean currentReal = !transactions.isEmpty()
                || !statementRepository.findWithFiltersAndStatus(paymentMonth, null, StatementStatus.CONFIRMED).isEmpty();
        List<InstallmentProjection> projections = currentReal
                ? List.of()
                : projectionRepository.findActiveDetailByProjectedMonth(paymentMonth);

        List<DashboardMonthDetailRowResponse> rows = new ArrayList<>();
        transactions.forEach(transaction -> rows.add(actualRow(transaction, paymentMonth)));
        projections.forEach(projection -> rows.add(projectionRow(projection)));
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

        boolean hasProjection = !projections.isEmpty();
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

    private BigDecimal sumPesos(List<StatementTransaction> transactions) {
        return transactions.stream()
                .map(StatementTransaction::getAmountPesos)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumUsd(List<StatementTransaction> transactions) {
        return transactions.stream()
                .map(StatementTransaction::getAmountUsd)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DashboardMonthDetailRowResponse actualRow(StatementTransaction transaction, LocalDate month) {
        CardStatement statement = transaction.getStatement();
        return new DashboardMonthDetailRowResponse(
                "ACTUAL",
                statement.getId(),
                statement.getPaymentMonth(),
                transaction.getId(),
                month,
                transaction.getDescription(),
                statement.getProvider(),
                statement.getCardBrand(),
                statement.getCardAlias(),
                transaction.getType(),
                transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                transaction.getCategory() == null ? null : transaction.getCategory().getName(),
                transaction.getCurrentInstallment(),
                transaction.getTotalInstallments(),
                transaction.getAmountPesos(),
                transaction.getAmountUsd(),
                finishMonth(statement.getPaymentMonth(), transaction.getCurrentInstallment(), transaction.getTotalInstallments())
        );
    }

    private DashboardMonthDetailRowResponse projectionRow(InstallmentProjection projection) {
        StatementTransaction transaction = projection.getSourceTransaction();
        CardStatement statement = transaction.getStatement();
        return new DashboardMonthDetailRowResponse(
                "PROJECTION",
                statement.getId(),
                statement.getPaymentMonth(),
                transaction.getId(),
                projection.getProjectedMonth(),
                transaction.getDescription(),
                statement.getProvider(),
                statement.getCardBrand(),
                statement.getCardAlias(),
                transaction.getType(),
                transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                transaction.getCategory() == null ? null : transaction.getCategory().getName(),
                projection.getInstallmentNumber(),
                projection.getTotalInstallments(),
                projection.getAmountPesos(),
                projection.getAmountUsd(),
                finishMonth(projection.getProjectedMonth(), projection.getInstallmentNumber(), projection.getTotalInstallments())
        );
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
