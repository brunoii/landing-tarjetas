package com.gentleia.landingtarjetas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardMonthDetailResponse(
        LocalDate month,
        boolean currentReal,
        boolean projectionOnly,
        BigDecimal totalPesos,
        BigDecimal totalUsd,
        List<DashboardCardTotalResponse> totalsByCard,
        List<DashboardMonthDetailRowResponse> rows
) {
}
