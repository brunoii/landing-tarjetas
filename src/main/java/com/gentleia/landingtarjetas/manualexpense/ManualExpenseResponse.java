package com.gentleia.landingtarjetas.manualexpense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.gentleia.landingtarjetas.category.CategoryResponse;

public record ManualExpenseResponse(
        Long id,
        String description,
        ManualExpenseType type,
        BigDecimal amountPesos,
        BigDecimal amountUsd,
        String startMonth,
        Integer totalInstallments,
        Integer currentInstallment,
        Integer installmentNumber,
        CategoryResponse category,
        boolean projected,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public static ManualExpenseResponse from(ManualExpense expense, boolean projected, Integer installmentNumber) {
        return new ManualExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getType(),
                expense.getAmountPesos(),
                expense.getAmountUsd(),
                formatMonth(expense.getStartMonth()),
                expense.getTotalInstallments(),
                expense.getCurrentInstallment(),
                installmentNumber,
                expense.getCategory() == null ? null : CategoryResponse.from(expense.getCategory()),
                projected,
                expense.getNotes(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }

    private static String formatMonth(LocalDate month) {
        return month == null ? null : MONTH_FORMATTER.format(month);
    }
}
