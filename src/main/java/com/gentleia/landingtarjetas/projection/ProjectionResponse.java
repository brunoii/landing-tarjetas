package com.gentleia.landingtarjetas.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectionResponse(
        Long id,
        Long sourceTransactionId,
        LocalDate projectedMonth,
        int installmentNumber,
        int totalInstallments,
        BigDecimal amountPesos,
        BigDecimal amountUsd,
        boolean active
) {
    public static ProjectionResponse from(InstallmentProjection projection) {
        return new ProjectionResponse(
                projection.getId(),
                projection.getSourceTransaction().getId(),
                projection.getProjectedMonth(),
                projection.getInstallmentNumber(),
                projection.getTotalInstallments(),
                projection.getAmountPesos(),
                projection.getAmountUsd(),
                projection.isActive()
        );
    }
}
