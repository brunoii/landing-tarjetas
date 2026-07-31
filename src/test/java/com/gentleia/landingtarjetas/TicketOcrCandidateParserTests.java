package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.supermarket.TicketOcrCandidateParser;
import com.gentleia.landingtarjetas.supermarket.TicketOcrDebugLineResponse;
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

    @Test
    void groupsSupportedTicketBlocksIntoUsefulCandidatesAndSeparatesDebugNoise() {
        record ParserCase(String name, String rawText, String expectedCode, String expectedDescription,
                          String expectedQuantity, String expectedUnitPrice, String expectedLineTotal,
                          String expectedTax, String expectedDebugLine) {
        }

        List<ParserCase> cases = List.of(
                new ParserCase(
                        "vea barcode block",
                        """
                        VEA HIPERMERCADO
                        Fecha 21/07/2026
                        7791234567890  DULCE   DE   LECHE CLASICO
                        2 x 1.250,50 2.501,00
                        IVA 0,00
                        TOTAL 2.501,00
                        """,
                        "7791234567890",
                        "DULCE DE LECHE CLASICO",
                        "2",
                        "1250.50",
                        "2501.00",
                        "0.00",
                        "TOTAL 2.501,00"
                ),
                new ParserCase(
                        "gomez pardo store code block",
                        """
                        GOMEZ PARDO
                        Fecha 2026-07-21
                        12345  ARROZ LARGO FINO
                        1,000 x 1.999,00 1.999,00
                        IVA 209,90
                        --------
                        """,
                        "12345",
                        "ARROZ LARGO FINO",
                        "1.000",
                        "1999.00",
                        "1999.00",
                        "209.90",
                        "--------"
                )
        );

        cases.forEach(testCase -> {
            TicketOcrEngineResult result = parser.parse(testCase.rawText(), new BigDecimal("0.86"));

            assertThat(result.lineCandidates()).singleElement().satisfies(candidate -> {
                assertThat(candidate.barcodeOrStoreCode()).isEqualTo(testCase.expectedCode());
                assertThat(candidate.descriptionCandidate()).isEqualTo(testCase.expectedDescription());
                assertThat(candidate.quantity()).isEqualByComparingTo(testCase.expectedQuantity());
                assertThat(candidate.unitPricePesos()).isEqualByComparingTo(testCase.expectedUnitPrice());
                assertThat(candidate.lineTotalPesos()).isEqualByComparingTo(testCase.expectedLineTotal());
                assertThat(candidate.taxPesos()).isEqualByComparingTo(testCase.expectedTax());
                assertThat(candidate.pricePesos()).isEqualByComparingTo(testCase.expectedLineTotal());
                assertThat(candidate.productCandidateId()).isNull();
                assertThat(candidate.productCandidateName()).isNull();
            });
            assertThat(result.debugLines()).extracting(TicketOcrDebugLineResponse::normalizedText)
                    .contains(testCase.expectedDebugLine());
        });
    }

    @Test
    void keepsMalformedPartialBlocksOutOfUsefulCandidatesUnlessTheyRemainReviewable() {
        TicketOcrEngineResult result = parser.parse("""
                VEA
                7791234567890 QUESO CREMA
                IVA 315,00
                TEXTO SUELTO ###
                """, new BigDecimal("0.86"));

        assertThat(result.lineCandidates()).isEmpty();
        assertThat(result.debugLines()).extracting(TicketOcrDebugLineResponse::classification)
                .contains("incomplete-block", "noise");
        assertThat(result.debugLines()).extracting(TicketOcrDebugLineResponse::normalizedText)
                .contains("7791234567890 QUESO CREMA", "TEXTO SUELTO ###");
    }

    @Test
    void doesNotConsumeFollowingValidSupportedBlockHeaderWhenPreviousBlockIsMalformed() {
        TicketOcrEngineResult result = parser.parse("""
                VEA
                7791234567890 QUESO CREMA
                7791234567891 YOGUR NATURAL
                2 x 1.250,50 2.501,00
                IVA 0,00
                """, new BigDecimal("0.86"));

        assertThat(result.lineCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.barcodeOrStoreCode()).isEqualTo("7791234567891");
            assertThat(candidate.descriptionCandidate()).isEqualTo("YOGUR NATURAL");
            assertThat(candidate.quantity()).isEqualByComparingTo("2");
            assertThat(candidate.unitPricePesos()).isEqualByComparingTo("1250.50");
            assertThat(candidate.lineTotalPesos()).isEqualByComparingTo("2501.00");
            assertThat(candidate.taxPesos()).isEqualByComparingTo("0.00");
        });
        assertThat(result.debugLines()).extracting(TicketOcrDebugLineResponse::normalizedText)
                .contains("7791234567890 QUESO CREMA")
                .doesNotContain("7791234567891 YOGUR NATURAL");
    }

    @Test
    void sendsArbitraryNonProductGarbageToDebugInsteadOfUsefulCandidates() {
        TicketOcrEngineResult result = parser.parse("""
                Supermercado Central
                Fecha 21/07/2026
                A1 B2 C3 ???
                PRODUCTO SIN PRECIO
                """, new BigDecimal("0.86"));

        assertThat(result.lineCandidates()).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.rawText()).isEqualTo("PRODUCTO SIN PRECIO");
                    assertThat(candidate.pricePesos()).isNull();
                    assertThat(candidate.warnings()).contains("Line could not be parsed into a price candidate");
                });
        assertThat(result.debugLines()).extracting(TicketOcrDebugLineResponse::normalizedText)
                .contains("A1 B2 C3 ???");
    }
}
