package com.gentleia.landingtarjetas.supermarket;

enum TicketOcrRuntimeWarning {
    EXECUTABLE_EMPTY("executable-empty"),
    EXECUTABLE_NOT_FOUND("executable-not-found"),
    EXECUTABLE_NOT_EXECUTABLE("executable-not-executable"),
    VERSION_CHECK_FAILED("version-check-failed"),
    TESSDATA_PATH_EMPTY("tessdata-path-empty"),
    TESSDATA_PATH_NOT_FOUND("tessdata-path-not-found"),
    LANGUAGES_NOT_AVAILABLE("languages-not-available"),
    PROCESS_START_FAILED("process-start-failed"),
    PROCESS_TIMEOUT("process-timeout"),
    CLI_EXIT_NONZERO("cli-exit-nonzero");

    private static final String PREFIX = "ticket-ocr-runtime-";
    private static final String FALLBACK = "Ticket OCR runtime is unavailable; review image manually";

    private final String category;

    TicketOcrRuntimeWarning(String category) {
        this.category = category;
    }

    String category() {
        return category;
    }

    String publicWarning() {
        return PREFIX + category + ": " + FALLBACK;
    }

    static TicketOcrRuntimeWarning fromProcessStatus(TicketOcrProcessResult.Status status) {
        return switch (status) {
            case FAILED_TO_START, INTERRUPTED -> PROCESS_START_FAILED;
            case TIMED_OUT -> PROCESS_TIMEOUT;
            case NON_ZERO_EXIT -> CLI_EXIT_NONZERO;
            case SUCCESS -> throw new IllegalArgumentException("Successful process has no failure category");
        };
    }
}
