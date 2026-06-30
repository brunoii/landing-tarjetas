package com.gentleia.landingtarjetas.statement.parser;

public interface StatementParser {

    String name();

    boolean supports(String extractedText);

    ParsedStatement parse(String extractedText);
}
