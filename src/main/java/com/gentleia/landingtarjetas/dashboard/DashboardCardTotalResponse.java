package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;

public record DashboardCardTotalResponse(
        Provider provider,
        CardBrand cardBrand,
        String cardAlias,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        long rowCount
) {
}
