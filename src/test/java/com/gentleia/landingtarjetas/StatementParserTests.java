package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.TransactionType;
import com.gentleia.landingtarjetas.statement.parser.NaranjaXParser;
import com.gentleia.landingtarjetas.statement.parser.SantanderAmexParser;
import com.gentleia.landingtarjetas.statement.parser.SantanderVisaParser;
import com.gentleia.landingtarjetas.statement.parser.StatementParserRegistry;

import org.junit.jupiter.api.Test;

class StatementParserTests {

    private final SantanderVisaParser santanderVisaParser = new SantanderVisaParser();
    private final SantanderAmexParser santanderAmexParser = new SantanderAmexParser();
    private final NaranjaXParser naranjaXParser = new NaranjaXParser();
    private final StatementParserRegistry registry = new StatementParserRegistry(
            List.of(santanderVisaParser, santanderAmexParser, naranjaXParser)
    );

    @Test
    void detectsSantanderVisaSyntheticText() {
        var parser = registry.detect("Synthetic Santander statement for Visa account");

        assertThat(parser).containsSame(santanderVisaParser);
    }

    @Test
    void detectsSantanderAmexSyntheticText() {
        var parser = registry.detect("Synthetic Santander statement for American Express account");

        assertThat(parser).containsSame(santanderAmexParser);
    }

    @Test
    void detectsNaranjaXSyntheticText() {
        var parser = registry.detect("Synthetic Naranja X account summary");

        assertThat(parser).containsSame(naranjaXParser);
    }

    @Test
    void parsesMinimalFieldsAndReliableSyntheticTransactions() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Total pesos: ARS 1234.56
                Total USD: USD 45.67
                Minimum payment: ARS 100.00
                TX: 2026-06-01 | Fictional bookstore | ARS 120.50 | installment 2/6
                TX: 2026-06-02 | Fictional cloud service | USD 10.00
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.provider()).isEqualTo(Provider.SANTANDER);
        assertThat(parsed.cardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.paymentMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(parsed.totalPesos()).isEqualByComparingTo("1234.56");
        assertThat(parsed.totalUsd()).isEqualByComparingTo("45.67");
        assertThat(parsed.minimumPaymentPesos()).isEqualByComparingTo("100.00");
        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0).type()).isEqualTo(TransactionType.INSTALLMENT);
        assertThat(parsed.transactions().get(0).currentInstallment()).isEqualTo(2);
        assertThat(parsed.transactions().get(0).totalInstallments()).isEqualTo(6);
        assertThat(parsed.transactions().get(1).amountUsd()).isEqualByComparingTo("10.00");
    }

    @Test
    void parsesSpanishSplitLabelsArgentineAmountsAndShortDates() {
        String text = """
                Synthetic Santander Visa statement
                Fecha de cierre actual
                25/06/2026
                Fecha de vencimiento actual
                10/07
                Total a pagar en pesos
                $ 1.234,56
                Total en dólares
                USD 45,67
                Pago mínimo
                $ 100,00
                01/06 Synthetic market cuota 2/6 $ 120,50
                02/06 Synthetic streaming USD 10,00
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.totalPesos()).isEqualByComparingTo("1234.56");
        assertThat(parsed.totalUsd()).isEqualByComparingTo("45.67");
        assertThat(parsed.minimumPaymentPesos()).isEqualByComparingTo("100.00");
        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(parsed.transactions().get(0).type()).isEqualTo(TransactionType.INSTALLMENT);
        assertThat(parsed.transactions().get(0).currentInstallment()).isEqualTo(2);
        assertThat(parsed.transactions().get(0).totalInstallments()).isEqualTo(6);
        assertThat(parsed.transactions().get(1).amountUsd()).isEqualByComparingTo("10.00");
    }

    @Test
    void doesNotBorrowValuesFromUnrelatedFollowingLabels() {
        String text = """
                Synthetic Santander Visa statement
                Closing date
                Due date: 2026-07-10
                Total pesos
                Total USD
                USD 45,67
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.closingDate()).isNull();
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.totalPesos()).isNull();
        assertThat(parsed.totalUsd()).isEqualByComparingTo("45.67");
        assertThat(parsed.warnings()).contains("No se detectó la fecha de cierre");
    }

    @Test
    void doesNotBorrowSameLineDateValueFromNextUnrelatedLabel() {
        String text = """
                Synthetic Santander Visa statement
                Closing date Due date: 2026-07-10
                Total pesos: ARS 1234.56
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.closingDate()).isNull();
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.warnings()).contains("No se detectó la fecha de cierre");
    }

    @Test
    void doesNotBorrowSameLineUsdTotalFromPesosLabel() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Total pesos Total USD USD 45,67
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.totalPesos()).isNull();
        assertThat(parsed.totalUsd()).isEqualByComparingTo("45.67");
    }

    @Test
    void alignsNextLineAmountsWhenMinimumPaymentPrecedesSantanderVisaTotal() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Pago mínimo Total a pagar en pesos
                $ 2.012,38 $ 45.678,90
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.minimumPaymentPesos()).isEqualByComparingTo("2012.38");
        assertThat(parsed.totalPesos()).isEqualByComparingTo("45678.90");
    }

    @Test
    void alignsSantanderVisaMinimumPesosAndDollarTotalsFromSharedSummaryRows() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Pago mínimo Total a pagar en pesos Total a pagar en dólares
                $ 2.012,38 $ 45.678,90 U$S 12,34
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.minimumPaymentPesos()).isEqualByComparingTo("2012.38");
        assertThat(parsed.totalPesos()).isEqualByComparingTo("45678.90");
        assertThat(parsed.totalUsd()).isEqualByComparingTo("12.34");
    }

    @Test
    void alignsNextLineAmountsWhenSantanderVisaTotalPrecedesMinimumPayment() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Total a pagar en pesos Pago mínimo
                $ 45.678,90 $ 2.012,38
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.totalPesos()).isEqualByComparingTo("45678.90");
        assertThat(parsed.minimumPaymentPesos()).isEqualByComparingTo("2012.38");
    }

    @Test
    void parsesTrailingUsdMarkersAsUsdTransactions() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                01/06 Synthetic purchase 10,00 US$
                02/06 Synthetic service 10,00 USD
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).hasSize(2).allSatisfy(transaction -> {
            assertThat(transaction.amountPesos()).isNull();
            assertThat(transaction.amountUsd()).isEqualByComparingTo("10.00");
        });
    }

    @Test
    void ignoresFooterYearForAmbiguousShortDateInference() {
        String text = """
                Synthetic Santander Visa statement
                Due date
                10/07
                Synthetic footer year 2026
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.dueDate()).isNull();
        assertThat(parsed.warnings()).contains("No se detectó la fecha de vencimiento");
    }

    @Test
    void keepsNegativeTransactionAmountsNegative() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                01/06 Synthetic refund -$ 100,00
                02/06 Synthetic adjustment $ 25,00-
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0).amountPesos()).isEqualByComparingTo("-100.00");
        assertThat(parsed.transactions().get(1).amountPesos()).isEqualByComparingTo("-25.00");
    }

    @Test
    void keepsParenthesizedCreditAmountsNegative() {
        String text = """
                Synthetic Naranja X account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/06/2026 10/07/2026
                Total a pagar $ 9.999,99
                01/06 Synthetic local purchase $ 120,40
                02/06 Synthetic refund ($ 47,30)
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(1).amountPesos()).isEqualByComparingTo("-47.30");
        assertThat(parsed.totalPesos()).isEqualByComparingTo("73.10");
    }

    @Test
    void parsesSantanderAmexTwoDigitDatesAndMultilineTableRows() {
        String text = """
                Synthetic Santander American Express statement
                Cierre actual 25/06/26
                Vencimiento actual 10/07/26
                Total USD US$ 88,90
                03/06
                Synthetic travel
                US$ 25,30
                """;

        var parsed = santanderAmexParser.parse(text);

        assertThat(parsed.cardBrand()).isEqualTo(CardBrand.AMERICAN_EXPRESS);
        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.totalUsd()).isEqualByComparingTo("88.90");
        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 6, 3));
            assertThat(transaction.description()).isEqualTo("Synthetic travel");
            assertThat(transaction.amountUsd()).isEqualByComparingTo("25.30");
        });
    }

    @Test
    void detectsReliableNaranjaXVisaBrandFromProviderLine() {
        String text = """
                Synthetic Tarjeta Naranja Visa account summary
                Fecha de cierre 25/06/2026
                Vencimiento 10/07
                Total a pagar $ 2.500,00
                01/06 Synthetic local purchase $ 200,00
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.provider()).isEqualTo(Provider.NARANJA_X);
        assertThat(parsed.cardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.totalPesos()).isEqualByComparingTo("200.00");
        assertThat(parsed.transactions()).singleElement()
                .satisfies(transaction -> assertThat(transaction.amountPesos()).isEqualByComparingTo("200.00"));
    }

    @Test
    void derivesNaranjaXTotalsFromParsedConsumptionRowsWhenAvailable() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/06/2026 10/07/2026
                Total a pagar $ 9.999,99
                01/06 Synthetic local purchase $ 200,00
                02/06 Synthetic installment cuota 1/3 $ 300,00
                03/06 Synthetic refund -$ 50,00
                04/06 Pago recibido $ 150,00
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.totalPesos()).isEqualByComparingTo("450.00");
        assertThat(parsed.totalUsd()).isNull();
        assertThat(parsed.transactions()).hasSize(3);
        assertThat(parsed.transactions())
                .noneSatisfy(transaction -> assertThat(transaction.description()).containsIgnoringCase("pago"));
    }

    @Test
    void excludesMonthNamePaymentRowsFromNaranjaXDerivedTotals() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25 jun 26 10 jul 26
                Total a pagar $ 9.999,99
                01 jun 26 Synthetic local purchase $ 120,40
                04 jun 26 Su Pago $ 33,20
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.totalPesos()).isEqualByComparingTo("120.40");
        assertThat(parsed.transactions()).singleElement()
                .satisfies(transaction -> assertThat(transaction.description()).isEqualTo("Synthetic local purchase"));
    }

    @Test
    void excludesNumericDatePaymentRowsWithShortNumericReferenceBeforeSuPagoFromNaranjaXDerivedTotals() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/06/2026 10/07/2026
                Total a pagar $ 9.999,99
                01/06 Synthetic local purchase $ 120,40
                04/06 000004 Su Pago $ 33,20
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.totalPesos()).isEqualByComparingTo("120.40");
        assertThat(parsed.transactions()).singleElement()
                .satisfies(transaction -> assertThat(transaction.description()).isEqualTo("Synthetic local purchase"));
    }

    @Test
    void excludesMonthNameDatePaymentRowsWithShortNumericReferenceBeforeSuPagoFromNaranjaXDerivedTotals() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25 jun 26 10 jul 26
                Total a pagar $ 9.999,99
                01 jun 26 Synthetic local purchase $ 120,40
                04 jun 26 000004 Su Pago $ 33,20
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.totalPesos()).isEqualByComparingTo("120.40");
        assertThat(parsed.transactions()).singleElement()
                .satisfies(transaction -> assertThat(transaction.description()).isEqualTo("Synthetic local purchase"));
    }

    @Test
    void keepsReferenceNumberMerchantRowsWhenPagoIsNotAStatementPaymentKeyword() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/06/2026 10/07/2026
                Total a pagar $ 9.999,99
                04/06 000004 Pago Market $ 33,20
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.totalPesos()).isEqualByComparingTo("33.20");
        assertThat(parsed.transactions()).singleElement()
                .satisfies(transaction -> assertThat(transaction.description()).isEqualTo("000004 Pago Market"));
    }

    @Test
    void parsesNaranjaXPlanZRowsAsFirstOfThreeInstallmentsWithDividedAmount() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/07/2026 10/08/2026
                Total a pagar $ 30.000,00
                07/07/2026 compra gomez pardo 30000 z
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 7, 7));
            assertThat(transaction.description()).isEqualTo("compra gomez pardo");
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(1);
            assertThat(transaction.totalInstallments()).isEqualTo(3);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("10000.00");
            assertThat(transaction.notes()).contains("Plan Z detectado", "1 de 3 cuotas").doesNotContain("detected");
        });
        assertThat(parsed.totalPesos()).isEqualByComparingTo("10000.00");
    }

    @Test
    void doesNotTreatStandaloneZInsideNaranjaXMerchantDescriptionAsPlanZ() {
        String text = """
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/07/2026 10/08/2026
                Total a pagar $ 300,00
                07/07/2026 comercio z normal $ 300,00
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.description()).isEqualTo("comercio z normal");
            assertThat(transaction.type()).isEqualTo(TransactionType.PURCHASE);
            assertThat(transaction.currentInstallment()).isNull();
            assertThat(transaction.totalInstallments()).isNull();
            assertThat(transaction.amountPesos()).isEqualByComparingTo("300.00");
            assertThat(transaction.notes()).isNull();
        });
        assertThat(parsed.totalPesos()).isEqualByComparingTo("300.00");
    }

    @Test
    void parsesNaranjaXRealLikeColumnDatesAndCurrencySeparatedRows() {
        String text = """
                Synthetic Naranja X account summary
                Cierre actual Vencimiento actual
                25/06/2026 10/07/2026
                Total a pagar $ 8.000,00
                Total USD USD 80,00
                01/06 Synthetic local purchase $ 500,00
                02/06 Synthetic international purchase USD 20,00
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.totalPesos()).isEqualByComparingTo("500.00");
        assertThat(parsed.totalUsd()).isEqualByComparingTo("20.00");
        assertThat(parsed.transactions()).hasSize(2);
    }

    @Test
    void parsesSantanderVisaRealLikeColumnDatesAndRepresentativeRows() {
        String text = """
                Synthetic Santander Visa statement
                Fecha de cierre actual Fecha de vencimiento actual
                25/06/2026 10/07/2026
                Total a pagar en pesos $ 1.234,56
                Total en dólares USD 45,67
                01/06 000001 Synthetic local purchase 120,50
                02/06 000002 Synthetic foreign service 10,00 US$
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0).amountPesos()).isEqualByComparingTo("120.50");
        assertThat(parsed.transactions().get(1).amountUsd()).isEqualByComparingTo("10.00");
    }

    @Test
    void parsesSantanderMonthNameDatesFromRealLikeRows() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25 jun 26 Vto. act.: 10 jul 26
                Total a pagar en pesos $ 1.234,56
                01 junio 26 000001 Synthetic local purchase 120,50
                02 jun 26 000002 Synthetic foreign service 10,00 US$
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(parsed.transactions().get(0).amountPesos()).isEqualByComparingTo("120.50");
        assertThat(parsed.transactions().get(1).amountUsd()).isEqualByComparingTo("10.00");
    }

    @Test
    void parsesSantanderVisaYearMonthDayFullLineWithInstallments() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/01/26 Vto. act.: 10/02/26
                Total a pagar en pesos $ 12.024,39
                26 Enero 20 031415 * SYNTHETIC*DESIGNSTORE C.05/06 12.024,39
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 1, 20));
            assertThat(transaction.transactionDate()).isNotEqualTo(LocalDate.of(2020, 1, 26));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*DESIGNSTORE");
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(5);
            assertThat(transaction.totalInstallments()).isEqualTo(6);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("12024.39");
            assertThat(transaction.amountUsd()).isNull();
        });
    }

    @Test
    void parsesSantanderVisaPreviousDecemberFullLineAsAbbreviatedYearMonthDay() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/01/26 Vto. act.: 10/02/26
                Total a pagar en pesos $ 9.876,54
                25 Diciembre 31 031415 * SYNTHETIC*CROSSYEAR C.02/03 9.876,54
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2025, 12, 31));
            assertThat(transaction.transactionDate()).isNotEqualTo(LocalDate.of(2031, 12, 25));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*CROSSYEAR");
            assertThat(transaction.description()).doesNotContain("031415").doesNotContain("C.02/03");
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(2);
            assertThat(transaction.totalInstallments()).isEqualTo(3);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("9876.54");
        });
    }

    @Test
    void parsesSantanderVisaYearMonthDayFullLineFromPriorInstallmentYear() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/07/26 Vto. act.: 10/08/26
                Total a pagar en pesos $ 13.387,50
                25 Julio   25 003476 *  SYNTHETIC JULYSHOP        C.12/12                       13.387,50
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2025, 7, 25));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC JULYSHOP");
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(12);
            assertThat(transaction.totalInstallments()).isEqualTo(12);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("13387.50");
            assertThat(transaction.amountUsd()).isNull();
        });
    }

    @Test
    void parsesSantanderVisaDecemberAbbreviationFullLineAndSameDayContinuation() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/01/26 Vto. act.: 10/02/26
                Total a pagar en pesos $ 33.662,99
                25 Diciem. 19 099107 * SYNTHETIC*BAKERY C.07/09 24.774,44
                19 004533 * SYNTHETIC*SPORTING C.07/09 8.888,55
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0)).satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2025, 12, 19));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*BAKERY");
            assertThat(transaction.currentInstallment()).isEqualTo(7);
            assertThat(transaction.totalInstallments()).isEqualTo(9);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("24774.44");
        });
        assertThat(parsed.transactions().get(1)).satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2025, 12, 19));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*SPORTING");
            assertThat(transaction.currentInstallment()).isEqualTo(7);
            assertThat(transaction.totalInstallments()).isEqualTo(9);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("8888.55");
        });
    }

    @Test
    void parsesSantanderVisaAbbreviatedRowsUnderAprilYearMonthBlock() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/04/26 Vto. act.: 10/05/26
                Total a pagar en pesos $ 23.568,01
                26 Abril 07 170528 * SYNTHETIC*ANCHOR 1.000,00
                08 170529 * SYNTHETIC*HOME C.03/06 22.568,01
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(1)).satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 4, 8));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*HOME");
            assertThat(transaction.currentInstallment()).isEqualTo(3);
            assertThat(transaction.totalInstallments()).isEqualTo(6);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("22568.01");
        });
    }

    @Test
    void skipsInvalidSantanderVisaFullLineInsteadOfGenericDateFallback() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/01/26 Vto. act.: 10/02/26
                Total a pagar en pesos $ 1.234,56
                25 Febrero 31 031415 * SYNTHETIC*INVALIDDATE 1.234,56
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).isEmpty();
        assertThat(parsed.warnings()).contains("No se detectaron consumos confiables");
    }

    @Test
    void parsesSantanderVisaContinuationRowsAcrossPageLikeLines() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/05/26 Vto. act.: 10/06/26
                Total a pagar en pesos $ 23.144,86
                26 Mayo 24 031416 K SYNTHETIC*ANCHOR 1.000,00
                Synthetic page break separator
                27 144657 * SYNTHETIC*FOLLOWUP C.01/03 22.144,86
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).hasSize(2);
        assertThat(parsed.transactions().get(0)).satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 5, 24));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*ANCHOR");
            assertThat(transaction.type()).isEqualTo(TransactionType.PURCHASE);
            assertThat(transaction.currentInstallment()).isNull();
            assertThat(transaction.totalInstallments()).isNull();
            assertThat(transaction.amountPesos()).isEqualByComparingTo("1000.00");
        });
        assertThat(parsed.transactions().get(1)).satisfies(transaction -> {
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 5, 27));
            assertThat(transaction.description()).isEqualTo("SYNTHETIC*FOLLOWUP");
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(1);
            assertThat(transaction.totalInstallments()).isEqualTo(3);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("22144.86");
        });
    }

    @Test
    void stopsSantanderVisaNormalParsingAtTotalConsumosAndKeepsPostMarkerCharges() {
        String text = """
                Synthetic Santander Visa statement
                Cierre act.: 25/05/26 Vto. act.: 10/06/26
                Total a pagar en pesos $ 1.100,00
                26 Mayo 24 031416 * SYNTHETIC*BEFORETOTAL 1.000,00
                25 111111 * Plan V 2.000,00
                25 111112 * Cuotas a vencer 3.000,00
                25 111113 * TNA 4.000,00
                25 111114 * TEA 5.000,00
                25 111115 * CFTEA 6.000,00
                25 111116 * Legal message 7.000,00
                25 111117 * Limites disponibles 8.000,00
                25 111118 * Saldo anterior 9.000,00
                25 111119 * Su Pago en Pesos 10.000,00
                Tarjeta synthetic Total Consumos 1.000,00
                27 222222 * SYNTHETIC*AFTERTOTAL 2.000,00
                IMPUESTO DE SELLOS 10,00
                IMPUESTO DE SELLOS P 20,00
                IVA RG 4240 30,00
                DB.RG 5617 40,00
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).hasSize(5);
        assertThat(parsed.transactions()).extracting(transaction -> transaction.description()).containsExactly(
                "SYNTHETIC*BEFORETOTAL",
                "IMPUESTO DE SELLOS",
                "IMPUESTO DE SELLOS P",
                "IVA RG 4240",
                "DB.RG 5617"
        );
        assertThat(parsed.transactions())
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("AFTERTOTAL"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("Plan V"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("Cuotas a vencer"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("TNA"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("TEA"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("CFTEA"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("Legal"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("Limites"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("Saldo anterior"))
                .noneSatisfy(transaction -> assertThat(transaction.description()).contains("Su Pago"));
        assertThat(parsed.transactions().subList(1, parsed.transactions().size())).allSatisfy(transaction -> {
            assertThat(transaction.type()).isEqualTo(TransactionType.PURCHASE);
            assertThat(transaction.currentInstallment()).isNull();
            assertThat(transaction.totalInstallments()).isNull();
        });
    }

    @Test
    void parsesSantanderAmexRealLikeColumnDatesAndRepresentativeRows() {
        String text = """
                Synthetic Santander American Express statement
                Cierre actual Vto. actual
                25/06/2026 10/07/2026
                Total USD US$ 88,90
                03/06 000003 Synthetic travel service US$ 25,30
                """;

        var parsed = santanderAmexParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.description()).isEqualTo("000003 Synthetic travel service");
            assertThat(transaction.amountUsd()).isEqualByComparingTo("25.30");
        });
    }

    @Test
    void doesNotBorrowColumnDateWhenOnlyOneValueIsPresentForMultipleDateLabels() {
        String text = """
                Synthetic Santander Visa statement
                Fecha de cierre actual Fecha de vencimiento actual
                25/06/2026
                Total pesos ARS 100,00
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.closingDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(parsed.dueDate()).isNull();
        assertThat(parsed.warnings()).contains("No se detectó la fecha de vencimiento");
    }

    @Test
    void leavesAmbiguousShortDatesPendingWithoutReferenceYear() {
        String text = """
                Synthetic Naranja X account summary
                Vencimiento
                10/07
                Total a pagar $ 2.500,00
                01/06 Synthetic local purchase $ 200,00
                """;

        var parsed = naranjaXParser.parse(text);

        assertThat(parsed.dueDate()).isNull();
        assertThat(parsed.transactions()).isEmpty();
        assertThat(parsed.warnings()).contains("No se detectó la fecha de vencimiento");
    }

    @Test
    void detectsPlanZAsThreeInstallmentsWhenCurrentInstallmentIsPresent() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Total pesos: ARS 1234.56
                TX: 2026-06-01 | Fictional appliance Plan Z cuota 2 | ARS 120.50
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(2);
            assertThat(transaction.totalInstallments()).isEqualTo(3);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("40.17");
            assertThat(transaction.notes()).contains("Plan Z detectado", "2 de 3 cuotas")
                    .doesNotContain("1 de 3 cuotas")
                    .doesNotContain("detected");
        });
    }

    @Test
    void defaultsPlanZCurrentInstallmentWhenMissing() {
        String text = """
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Total pesos: ARS 1234.56
                TX: 2026-06-01 | Fictional appliance Plan Z | ARS 120.50
                """;

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.type()).isEqualTo(TransactionType.INSTALLMENT);
            assertThat(transaction.currentInstallment()).isEqualTo(1);
            assertThat(transaction.totalInstallments()).isEqualTo(3);
            assertThat(transaction.amountPesos()).isEqualByComparingTo("40.17");
            assertThat(transaction.notes()).contains("Plan Z detectado").doesNotContain("detected");
        });
    }

    @Test
    void leavesMissingFieldsPendingInsteadOfInventingValues() {
        String text = "Synthetic Santander Visa statement without totals or dates";

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.dueDate()).isNull();
        assertThat(parsed.closingDate()).isNull();
        assertThat(parsed.totalPesos()).isNull();
        assertThat(parsed.totalUsd()).isNull();
        assertThat(parsed.transactions()).isEmpty();
        assertThat(parsed.warnings()).contains(
                "Se detectó un formato compatible, pero no se extrajeron campos útiles del resumen ni consumos",
                "No se detectó la fecha de vencimiento",
                "No se detectó la fecha de cierre",
                "No se detectaron los totales del resumen",
                "No se detectaron consumos confiables"
        );
    }
}
