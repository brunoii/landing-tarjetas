package com.gentleia.landingtarjetas.transaction;

import java.util.List;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryService;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.shared.TransactionType;
import com.gentleia.landingtarjetas.statement.CardStatement;
import com.gentleia.landingtarjetas.statement.CardStatementRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    private final StatementTransactionRepository transactionRepository;
    private final CardStatementRepository statementRepository;
    private final CategoryService categoryService;

    public TransactionService(StatementTransactionRepository transactionRepository,
                              CardStatementRepository statementRepository,
                              CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(String month, CardBrand cardBrand, Long categoryId, TransactionType type) {
        return transactionRepository.findConfirmedWithFilters(DateParsers.parseYearMonth(month), cardBrand, categoryId, type).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse createForDraftStatement(Long statementId, TransactionUpdateRequest request) {
        CardStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
        requireDraftStatement(statement, "Only draft statement transactions can be created");

        StatementTransaction transaction = new StatementTransaction(statement, request.description().trim(), request.type());
        applyRequest(transaction, request);
        validate(transaction);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionUpdateRequest request) {
        StatementTransaction transaction = getTransaction(id);
        requireDraftStatement(transaction, "Only draft statement transactions can be modified");
        applyRequest(transaction, request);
        validate(transaction);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        StatementTransaction transaction = getTransaction(id);
        requireDraftStatement(transaction, "Only draft statement transactions can be modified");
        transactionRepository.delete(transaction);
    }

    public StatementTransaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public void validate(StatementTransaction transaction) {
        if (transaction.getAmountPesos() == null && transaction.getAmountUsd() == null) {
            throw new IllegalArgumentException("La transacción requiere un importe en pesos o USD");
        }
        if (transaction.getType() == TransactionType.INSTALLMENT) {
            if (transaction.getCurrentInstallment() == null || transaction.getTotalInstallments() == null) {
                throw new IllegalArgumentException("Las transacciones a plazos requieren el pago actual y el total de las cuotas");
            }
            if (transaction.getCurrentInstallment() > transaction.getTotalInstallments()) {
                throw new IllegalArgumentException("La cuota actual no puede superar el total de cuotas");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void applyRequest(StatementTransaction transaction, TransactionUpdateRequest request) {
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(request.description().trim());
        transaction.setType(request.type());
        transaction.setAmountPesos(request.amountPesos());
        transaction.setAmountUsd(request.amountUsd());
        transaction.setCurrentInstallment(request.currentInstallment());
        transaction.setTotalInstallments(request.totalInstallments());
        transaction.setNotes(trimToNull(request.notes()));

        Category category = request.categoryId() == null ? null : categoryService.getCategory(request.categoryId());
        if (category != null && !category.isActive()) {
            throw new IllegalArgumentException("Cannot assign inactive category to transaction");
        }
        transaction.setCategory(category);
    }

    private void requireDraftStatement(StatementTransaction transaction, String message) {
        requireDraftStatement(transaction.getStatement(), message);
    }

    private void requireDraftStatement(CardStatement statement, String message) {
        if (statement.getStatus() != StatementStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }
}
