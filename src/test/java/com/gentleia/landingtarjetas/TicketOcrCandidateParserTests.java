package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.supermarket.TicketOcrCandidateParser;
import com.gentleia.landingtarjetas.supermarket.TicketOcrEngineResult;

import org.junit.jupiter.api.Test;

class TicketOcrCandidateParserTests {

    private final TicketOcrCandidateParser parser = new TicketOcrCandidateParser();

    @Test
    void extractsDateSourceAndLineCandidatesWithoutProductAutoConfirmation() {
        TicketOcrEngineResult result = parser.parse("""
                Supermercado Central
                Fecha 21/07/2026
                YERBA 1KG 2500,50
                PAN LACTAL $1.999,99
                """, new BigDecimal("0.86"));

        assertThat(result.ocrConfidence()).isEqualByComparingTo("0.86");
        assertThat(result.dateCandidates()).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.value()).isEqualTo(LocalDate.of(2026, 7, 21));
                    assertThat(candidate.confidence()).isEqualByComparingTo("0.90");
                    assertThat(candidate.warnings()).isEmpty();
                });
        assertThat(result.sourceCandidates()).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.label()).isEqualTo("Supermercado Central");
                    assertThat(candidate.confidence()).isEqualByComparingTo("0.70");
                });
        assertThat(result.lineCandidates()).hasSize(2)
                .allSatisfy(candidate -> {
                    assertThat(candidate.rawText()).isNotBlank();
                    assertThat(candidate.descriptionCandidate()).isNotBlank();
                    assertThat(candidate.pricePesos()).isPositive();
                    assertThat(candidate.productCandidateId()).isNull();
                    assertThat(candidate.productCandidateName()).isNull();
                });
        assertThat(result.lineCandidates().get(0).descriptionCandidate()).isEqualTo("YERBA 1KG");
        assertThat(result.lineCandidates().get(0).pricePesos()).isEqualByComparingTo("2500.50");
        assertThat(result.lineCandidates().get(1).descriptionCandidate()).isEqualTo("PAN LACTAL");
        assertThat(result.lineCandidates().get(1).pricePesos()).isEqualByComparingTo("1999.99");
        assertThat(result.warnings()).contains("Review OCR output before saving");
    }

    @Test
    void marksAmbiguousAndLowConfidenceLinesWithWarnings() {
        TicketOcrEngineResult result = parser.parse("""
                Ticket borroso
                Fecha 2026-07-21
                TOTAL 4500,00
                PRODUCTO SIN PRECIO
                GASEOSA 1500
                """, new BigDecimal("0.41"));

        assertThat(result.dateCandidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.value()).isEqualTo(LocalDate.of(2026, 7, 21)));
        assertThat(result.lineCandidates()).hasSize(2);
        assertThat(result.lineCandidates().get(0).rawText()).isEqualTo("PRODUCTO SIN PRECIO");
        assertThat(result.lineCandidates().get(0).pricePesos()).isNull();
        assertThat(result.lineCandidates().get(0).warnings()).contains("Line could not be parsed into a price candidate");
        assertThat(result.lineCandidates().get(1).descriptionCandidate()).isEqualTo("GASEOSA");
        assertThat(result.lineCandidates().get(1).warnings()).contains("Low OCR confidence; review this line manually");
        assertThat(result.warnings()).contains("Low OCR confidence; review candidates manually");
    }

    @Test
    void preservesDotDecimalAndDotThousandsPrices() {
        TicketOcrEngineResult result = parser.parse("""
                ACEITE 2500.50
                AZUCAR 1.999
                """, new BigDecimal("0.86"));

        assertThat(result.lineCandidates()).hasSize(2);
        assertThat(result.lineCandidates().get(0).pricePesos()).isEqualByComparingTo("2500.50");
        assertThat(result.lineCandidates().get(1).pricePesos()).isEqualByComparingTo("1999");
    }
}
