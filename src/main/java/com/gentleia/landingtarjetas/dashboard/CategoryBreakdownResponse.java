package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;

public record CategoryBreakdownResponse(
        Long categoryId,
        String categoryName,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        long transactionCount
) {
}
