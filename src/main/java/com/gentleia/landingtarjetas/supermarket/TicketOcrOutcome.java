package com.gentleia.landingtarjetas.supermarket;

public enum TicketOcrOutcome {
    READY,
    INVALID_FILE,
    DECODE_FAILED,
    RUNTIME_UNAVAILABLE,
    EMPTY_EXTRACTION
}
