package com.gentleia.landingtarjetas.supermarket;

public record TicketOcrProcessResult(Status status, String stdout, String diagnostic) {

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
