package com.gentleia.landingtarjetas.supermarket;

public class TicketOcrException extends IllegalArgumentException {

    private final TicketOcrOutcome outcome;

    public TicketOcrException(TicketOcrOutcome outcome, String message) {
        super(message);
        this.outcome = outcome;
    }

    public TicketOcrOutcome getOutcome() {
        return outcome;
    }
}
