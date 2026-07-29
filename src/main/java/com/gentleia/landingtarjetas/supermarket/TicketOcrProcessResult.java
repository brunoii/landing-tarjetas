package com.gentleia.landingtarjetas.supermarket;

public record TicketOcrProcessResult(Status status, String stdout, String stderr, Integer exitCode, String diagnostic) {

    public TicketOcrProcessResult(Status status, String stdout, String diagnostic) {
        this(status, stdout, "", null, diagnostic);
    }

    public enum Status {
        SUCCESS,
        NON_ZERO_EXIT,
        TIMED_OUT,
        FAILED_TO_START,
        INTERRUPTED
    }

    public boolean succeeded() {
        return status == Status.SUCCESS;
    }
}
