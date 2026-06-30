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
    void leavesMissingFieldsPendingInsteadOfInventingValues() {
        String text = "Synthetic Santander Visa statement without totals or dates";

        var parsed = santanderVisaParser.parse(text);

        assertThat(parsed.dueDate()).isNull();
        assertThat(parsed.closingDate()).isNull();
        assertThat(parsed.totalPesos()).isNull();
        assertThat(parsed.totalUsd()).isNull();
        assertThat(parsed.transactions()).isEmpty();
        assertThat(parsed.warnings()).contains(
                "Due date was not detected",
                "Closing date was not detected",
                "Statement totals were not detected",
                "No reliable transaction rows were detected"
        );
    }
}
