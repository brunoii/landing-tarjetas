package com.gentleia.landingtarjetas.shared;

import java.time.LocalDate;
import java.time.YearMonth;

public final class DateParsers {

    private DateParsers() {
    }

    public static LocalDate parseYearMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return YearMonth.parse(value).atDay(1);
    }

    public static LocalDate normalizeMonth(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.withDayOfMonth(1);
    }
}
