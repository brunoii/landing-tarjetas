package com.gentleia.landingtarjetas.dashboard;

import java.time.LocalDate;

public record DashboardMonthResponse(
        String yearMonth,
        LocalDate month,
        boolean currentReal,
        boolean projectionOnly,
        boolean hasConfirmedData,
        boolean hasProjectedData
) {
    public static DashboardMonthResponse of(LocalDate month, boolean hasConfirmedData, boolean hasProjectedData) {
        return new DashboardMonthResponse(
                month.toString().substring(0, 7),
                month,
                hasConfirmedData,
                !hasConfirmedData && hasProjectedData,
                hasConfirmedData,
                hasProjectedData
        );
    }
}
