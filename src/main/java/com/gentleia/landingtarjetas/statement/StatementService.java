package com.gentleia.landingtarjetas.statement;

import java.math.BigDecimal;
import java.util.List;

import com.gentleia.landingtarjetas.projection.InstallmentProjectionService;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.transaction.TransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StatementService {

    private final CardStatementRepository statementRepository;
    private final InstallmentProjectionService projectionService;
    private final TransactionService transactionService;

    public StatementService(CardStatementRepository statementRepository,
                            InstallmentProjectionService projectionService,
                            TransactionService transactionService) {
        this.statementRepository = statementRepository;
        this.projectionService = projectionService;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<StatementSummaryResponse> list(String month, CardBrand cardBrand) {
        return statementRepository.findWithFilters(DateParsers.parseYearMonth(month), cardBrand).stream()
                .map(StatementSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatementDetailResponse get(Long id) {
        return StatementDetailResponse.from(getStatement(id));
    }

    @Transactional
    public StatementDetailResponse update(Long id, StatementUpdateRequest request) {
        CardStatement statement = getStatement(id);
        ensureDraft(statement);
        statement.setProvider(request.provider());
        statement.setCardBrand(request.cardBrand());
        statement.setCardAlias(trimToNull(request.cardAlias()));
        statement.setPeriodStart(request.periodStart());
        statement.setPeriodEnd(request.periodEnd());
        statement.setClosingDate(request.closingDate());
        statement.setDueDate(request.dueDate());
        if (request.paymentMonth() != null) {
            statement.setPaymentMonth(DateParsers.normalizeMonth(request.paymentMonth()));
        }
        statement.setTotalPesos(request.totalPesos());
        statement.setTotalUsd(request.totalUsd());
        statement.setMinimumPaymentPesos(request.minimumPaymentPesos());
        return StatementDetailResponse.from(statement);
    }

    @Transactional
    public StatementDetailResponse confirm(Long id) {
        CardStatement statement = getStatement(id);
        if (statement.getPaymentMonth() == null) {
            throw new IllegalArgumentException("Cannot confirm a statement without a payment month");
        }
        validateTotals(statement.getTotalPesos(), statement.getTotalUsd());
        statement.getTransactions().forEach(transactionService::validate);
        statement.setStatus(StatementStatus.CONFIRMED);
        projectionService.replaceForStatement(statement);
        return StatementDetailResponse.from(statement);
    }

    @Transactional
    public void delete(Long id) {
        CardStatement statement = getStatement(id);
        ensureDraft(statement);
        statementRepository.delete(statement);
    }

    public CardStatement getStatement(Long id) {
        return statementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
    }

    private void validateTotals(BigDecimal totalPesos, BigDecimal totalUsd) {
        if (totalPesos == null && totalUsd == null) {
            throw new IllegalArgumentException("Statement requires at least one total amount in pesos or USD");
        }
    }

    private void ensureDraft(CardStatement statement) {
        if (statement.getStatus() != StatementStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft statements can be modified");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
