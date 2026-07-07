package com.gentleia.landingtarjetas.statement;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentleia.landingtarjetas.projection.InstallmentProjectionService;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.transaction.TransactionService;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StatementService {

    private final CardStatementRepository statementRepository;
    private final StatementTransactionRepository transactionRepository;
    private final InstallmentProjectionService projectionService;
    private final TransactionService transactionService;

    public StatementService(CardStatementRepository statementRepository,
                            StatementTransactionRepository transactionRepository,
                            InstallmentProjectionService projectionService,
                            TransactionService transactionService) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
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
            throw new IllegalArgumentException("No se puede confirmar un resumen sin mes de pago");
        }
        validateTotals(statement.getTotalPesos(), statement.getTotalUsd());
        if (statement.getStatus() == StatementStatus.DRAFT) {
            removeDuplicateTransactions(statement);
        }
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el resumen"));
    }

    private void validateTotals(BigDecimal totalPesos, BigDecimal totalUsd) {
        if (totalPesos == null && totalUsd == null) {
            throw new IllegalArgumentException("El resumen requiere al menos un total en pesos o en dólares");
        }
    }

    private void ensureDraft(CardStatement statement) {
        if (statement.getStatus() != StatementStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden modificar resúmenes en borrador");
        }
    }

    private void removeDuplicateTransactions(CardStatement statement) {
        Set<String> existingKeys = new HashSet<>();
        for (StatementTransaction transaction : transactionRepository.findConfirmedWithFilters(
                statement.getPaymentMonth(), statement.getCardBrand(), null, null)) {
            if (StatementTransactionIdentity.sameStatementScope(statement, transaction.getStatement())) {
                existingKeys.add(StatementTransactionIdentity.key(transaction.getStatement(), transaction));
            }
        }
        Set<String> currentKeys = new HashSet<>();
        statement.getTransactions().removeIf(transaction -> {
            String key = StatementTransactionIdentity.key(statement, transaction);
            if (existingKeys.contains(key) || currentKeys.contains(key)) {
                return true;
            }
            currentKeys.add(key);
            return false;
        });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
