package com.gentleia.landingtarjetas.supermarket;

import java.time.Duration;
import java.util.List;

public interface TicketOcrProcessRunner {

    TicketOcrProcessResult run(List<String> command, Duration timeout);
}
