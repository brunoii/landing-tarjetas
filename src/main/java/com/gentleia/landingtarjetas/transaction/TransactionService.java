package com.gentleia.landingtarjetas.transaction;

import java.util.List;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryService;
import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.TransactionType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    private final StatementTransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(StatementTransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(String month, CardBrand cardBrand, Long categoryId, TransactionType type) {
        return transactionRepository.findConfirmedWithFilters(DateParsers.parseYearMonth(month), cardBrand, categoryId, type).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionUpdateRequest request) {
        StatementTransaction transaction = getTransaction(id);
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
        validate(transaction);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        transactionRepository.delete(getTransaction(id));
    }

    public StatementTransaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public void validate(StatementTransaction transaction) {
        if (transaction.getAmountPesos() == null && transaction.getAmountUsd() == null) {
            throw new IllegalArgumentException("Transaction requires an amount in pesos or USD");
        }
        if (transaction.getType() == TransactionType.INSTALLMENT) {
            if (transaction.getCurrentInstallment() == null || transaction.getTotalInstallments() == null) {
                throw new IllegalArgumentException("Installment transactions require current and total installments");
            }
            if (transaction.getCurrentInstallment() > transaction.getTotalInstallments()) {
                throw new IllegalArgumentException("Current installment cannot exceed total installments");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
