package com.gentleia.landingtarjetas.statement.parser;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class StatementParserRegistry {

    private final List<StatementParser> parsers;

    public StatementParserRegistry(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public Optional<StatementParser> detect(String extractedText) {
        return parsers.stream()
                .filter(parser -> parser.supports(extractedText))
                .findFirst();
    }
}
