package com.gentleia.landingtarjetas.shared;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        List<String> details
) {

    public static ApiErrorResponse of(int status, String error, List<String> details) {
        return new ApiErrorResponse(Instant.now(), status, error, details);
    }
}
