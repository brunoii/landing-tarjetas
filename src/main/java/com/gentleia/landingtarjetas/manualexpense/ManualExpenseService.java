package com.gentleia.landingtarjetas.manualexpense;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.category.CategoryService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManualExpenseService {

    private final ManualExpenseRepository manualExpenseRepository;
    private final CategoryService categoryService;

    public ManualExpenseService(ManualExpenseRepository manualExpenseRepository, CategoryService categoryService) {
        this.manualExpenseRepository = manualExpenseRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<ManualExpenseResponse> list(String month) {
        if (month == null || month.isBlank()) {
            return manualExpenseRepository.findAllByOrderByStartMonthAscIdAsc().stream()
                    .map(expense -> ManualExpenseResponse.from(expense, false, effectiveCurrentInstallment(expense)))
                    .toList();
        }

        LocalDate targetMonth = parseMonth(month);
        return manualExpenseRepository.findAllByOrderByStartMonthAscIdAsc().stream()
                .filter(expense -> appliesToMonth(expense, targetMonth))
                .map(expense -> ManualExpenseResponse.from(
                        expense,
                        isProjectedForMonth(expense, targetMonth),
                        installmentNumberForMonth(expense, targetMonth)))
                .toList();
    }

    @Transactional
    public ManualExpenseResponse create(ManualExpenseRequest request) {
        ManualExpense expense = new ManualExpense(
                request.description().trim(),
                request.type(),
                request.amountPesos(),
                parseMonth(request.startMonth())
        );
        applyOptionalFields(expense, request);
        validate(expense);
        return ManualExpenseResponse.from(manualExpenseRepository.save(expense), false, effectiveCurrentInstallment(expense));
    }

    @Transactional
    public ManualExpenseResponse update(Long id, ManualExpenseRequest request) {
        ManualExpense expense = getManualExpense(id);
        expense.setDescription(request.description().trim());
        expense.setType(request.type());
        expense.setAmountPesos(request.amountPesos());
        expense.setStartMonth(parseMonth(request.startMonth()));
        applyOptionalFields(expense, request);
        validate(expense);
        return ManualExpenseResponse.from(expense, false, effectiveCurrentInstallment(expense));
    }

    @Transactional
    public void delete(Long id) {
        manualExpenseRepository.delete(getManualExpense(id));
    }

    public boolean appliesToMonth(ManualExpense expense, LocalDate targetMonth) {
        if (!isInstallmentLike(expense)) {
            return expense.getStartMonth().equals(targetMonth);
        }
        long monthOffset = ChronoUnit.MONTHS.between(expense.getStartMonth(), targetMonth);
        if (monthOffset < 0) {
            return false;
        }
        Integer installmentNumber = installmentNumberForMonth(expense, targetMonth);
        return installmentNumber != null && installmentNumber <= expense.getTotalInstallments();
    }

    public boolean isProjectedForMonth(ManualExpense expense, LocalDate targetMonth) {
        return isInstallmentLike(expense) && targetMonth.isAfter(expense.getStartMonth());
    }

    public Integer installmentNumberForMonth(ManualExpense expense, LocalDate targetMonth) {
        if (!isInstallmentLike(expense)) {
            return null;
        }
        long monthOffset = ChronoUnit.MONTHS.between(expense.getStartMonth(), targetMonth);
        if (monthOffset < 0) {
            return null;
        }
        return Math.toIntExact(effectiveCurrentInstallment(expense) + monthOffset);
    }

    public Integer effectiveCurrentInstallment(ManualExpense expense) {
        return isInstallmentLike(expense) ? expense.getCurrentInstallment() == null ? 1 : expense.getCurrentInstallment() : null;
    }

    private ManualExpense getManualExpense(Long id) {
        return manualExpenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el gasto manual"));
    }

    private void applyOptionalFields(ManualExpense expense, ManualExpenseRequest request) {
        expense.setAmountUsd(request.amountUsd());
        if (isInstallmentLike(request.type())) {
            expense.setCurrentInstallment(request.currentInstallment());
            expense.setTotalInstallments(request.totalInstallments());
        } else {
            expense.setCurrentInstallment(null);
            expense.setTotalInstallments(null);
        }

        Category category = request.categoryId() == null ? null : categoryService.getCategory(request.categoryId());
        if (category != null && !category.isActive()) {
            throw new IllegalArgumentException("No se puede asignar una categoría inactiva al gasto manual");
        }
        expense.setCategory(category);
        expense.setNotes(trimToNull(request.notes()));
    }

    private void validate(ManualExpense expense) {
        if (!isInstallmentLike(expense)) {
            return;
        }
        if (expense.getTotalInstallments() == null) {
            throw new IllegalArgumentException("Los gastos en cuotas y préstamos requieren la cantidad total de cuotas");
        }
        Integer currentInstallment = effectiveCurrentInstallment(expense);
        if (currentInstallment > expense.getTotalInstallments()) {
            throw new IllegalArgumentException("La cuota actual no puede superar el total de cuotas");
        }
    }

    private boolean isInstallmentLike(ManualExpense expense) {
        return isInstallmentLike(expense.getType());
    }

    private boolean isInstallmentLike(ManualExpenseType type) {
        return type == ManualExpenseType.INSTALLMENT || type == ManualExpenseType.LOAN;
    }

    private LocalDate parseMonth(String value) {
        return YearMonth.parse(value.trim()).atDay(1);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
