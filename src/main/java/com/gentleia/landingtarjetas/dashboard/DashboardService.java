package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.StatementStatus;
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

    public DashboardService(CardStatementRepository statementRepository,
                            StatementTransactionRepository transactionRepository) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
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
