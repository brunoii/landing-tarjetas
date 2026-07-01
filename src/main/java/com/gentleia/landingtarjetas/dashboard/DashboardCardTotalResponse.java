package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;

import com.gentleia.landingtarjetas.shared.CardBrand;

public record DashboardCardTotalResponse(
        CardBrand cardBrand,
        String cardAlias,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        long rowCount
) {
}
