package com.gentleia.landingtarjetas.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record IncomeResponse(
        Long id,
        String description,
        IncomeType incomeType,
        BigDecimal amountPesos,
        String startMonth,
        String endMonth,
        boolean recurringMonthly,
        Long parentIncomeId,
        boolean projected,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public static IncomeResponse from(Income income, boolean projected) {
        return new IncomeResponse(
                income.getId(),
                income.getDescription(),
                income.getIncomeType(),
                income.getAmountPesos(),
                formatMonth(income.getStartMonth()),
                formatMonth(income.getEndMonth()),
                income.isRecurringMonthly(),
                income.getParentIncomeId(),
                projected,
                income.getNotes(),
                income.getCreatedAt(),
                income.getUpdatedAt()
        );
    }

    private static String formatMonth(LocalDate month) {
        return month == null ? null : MONTH_FORMATTER.format(month);
    }
}
