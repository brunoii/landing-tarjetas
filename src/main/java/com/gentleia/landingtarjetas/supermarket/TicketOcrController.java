package com.gentleia.landingtarjetas.supermarket;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/super/ticket-ocr")
public class TicketOcrController {

    private final TicketOcrService ticketOcrService;

    public TicketOcrController(TicketOcrService ticketOcrService) {
        this.ticketOcrService = ticketOcrService;
    }

    @PostMapping("/candidates")
    public TicketOcrResponse candidates(@RequestParam(name = "file", required = false) MultipartFile[] files) {
        return ticketOcrService.extractCandidates(files);
    }
}
