package com.gentleia.landingtarjetas.supermarket;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketOcrDebugLineResponse(
        String normalizedText,
        String classification,
        String warning
) {
}
