package com.gentleia.landingtarjetas.statement.parser;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SantanderVisaParser implements StatementParser {

    @Override
    public String name() {
        return "SantanderVisaParser";
    }

    @Override
    public boolean supports(String extractedText) {
        String normalized = StatementParseSupport.normalize(extractedText);
        return normalized.contains("santander") && normalized.contains("visa");
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        return StatementParseSupport.parseCommon(extractedText, Provider.SANTANDER, CardBrand.VISA, "Santander Visa");
    }
}
