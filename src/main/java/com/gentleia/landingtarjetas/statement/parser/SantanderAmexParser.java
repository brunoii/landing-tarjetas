package com.gentleia.landingtarjetas.statement.parser;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class SantanderAmexParser implements StatementParser {

    @Override
    public String name() {
        return "Santander American Express";
    }

    @Override
    public boolean supports(String extractedText) {
        String normalized = StatementParseSupport.normalize(extractedText);
        return normalized.contains("santander")
                && (normalized.contains("american express") || normalized.contains("amex"));
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        return StatementParseSupport.parseCommon(extractedText, Provider.SANTANDER, CardBrand.AMERICAN_EXPRESS,
                "Santander American Express");
    }
}
