package com.gentleia.landingtarjetas.statement.parser;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class NaranjaXParser implements StatementParser {

    @Override
    public String name() {
        return "NaranjaXParser";
    }

    @Override
    public boolean supports(String extractedText) {
        String normalized = StatementParseSupport.normalize(extractedText).replace(" ", "");
        return normalized.contains("naranjax") || normalized.contains("tarjetanaranja");
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        CardBrand cardBrand = StatementParseSupport.detectNaranjaCardBrand(extractedText).orElse(CardBrand.OTHER);
        String cardAlias = cardBrand == CardBrand.OTHER ? "Naranja X" : "Naranja X " + cardBrand.name().replace('_', ' ');
        return StatementParseSupport.parseCommon(extractedText, Provider.NARANJA_X, cardBrand, cardAlias);
    }
}
