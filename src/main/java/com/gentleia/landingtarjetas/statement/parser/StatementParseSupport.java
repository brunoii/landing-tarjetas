package com.gentleia.landingtarjetas.statement.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.TransactionType;

final class StatementParseSupport {

    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern DUE_DATE = Pattern.compile(
            "(?ium)^\\s*(?:due date|payment due date|vencimiento|fecha de vencimiento)\\s*[:\\-]?\\s*(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\s*$");
    private static final Pattern CLOSING_DATE = Pattern.compile(
            "(?ium)^\\s*(?:closing date|statement closing date|cierre|fecha de cierre)\\s*[:\\-]?\\s*(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\s*$");
    private static final Pattern TOTAL_PESOS = Pattern.compile(
            "(?ium)^\\s*(?:total pesos|total ars|total en pesos|total \\$)\\s*[:\\-]?\\s*(?:ARS|\\$)?\\s*([0-9][0-9.,]*)\\s*$");
    private static final Pattern TOTAL_USD = Pattern.compile(
            "(?ium)^\\s*(?:total usd|total us\\$|total dollars|total dolares)\\s*[:\\-]?\\s*(?:USD|US\\$)?\\s*([0-9][0-9.,]*)\\s*$");
    private static final Pattern MINIMUM_PAYMENT = Pattern.compile(
            "(?ium)^\\s*(?:minimum payment|pago minimo|pago mínimo)\\s*[:\\-]?\\s*(?:ARS|\\$)?\\s*([0-9][0-9.,]*)\\s*$");
    private static final Pattern INSTALLMENT = Pattern.compile(
            "(?iu)(?:installment|cuota)\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})|\\b(\\d{1,2})\\s*/\\s*(\\d{1,2})\\b");
    private static final Pattern PLAN_Z = Pattern.compile("(?iu)\\b(?:plan\\s*z|plan\\s*zeta|zeta)\\b");
    private static final Pattern PLAN_Z_CURRENT = Pattern.compile("(?iu)(?:installment|cuota)\\s*(\\d{1,2})(?!\\s*/)");

    private StatementParseSupport() {
    }

    static ParsedStatement parseCommon(String extractedText, Provider provider, CardBrand cardBrand, String cardAlias) {
        LocalDate dueDate = findDate(DUE_DATE, extractedText).orElse(null);
        LocalDate closingDate = findDate(CLOSING_DATE, extractedText).orElse(null);
        BigDecimal totalPesos = findAmount(TOTAL_PESOS, extractedText).orElse(null);
        BigDecimal totalUsd = findAmount(TOTAL_USD, extractedText).orElse(null);
        BigDecimal minimumPaymentPesos = findAmount(MINIMUM_PAYMENT, extractedText).orElse(null);
        List<ParsedTransaction> transactions = parseTransactions(extractedText);
        List<String> warnings = warnings(dueDate, closingDate, totalPesos, totalUsd, transactions);

        return new ParsedStatement(
                provider,
                cardBrand,
                cardAlias,
                closingDate,
                dueDate,
                dueDate == null ? null : dueDate.withDayOfMonth(1),
                totalPesos,
                totalUsd,
                minimumPaymentPesos,
                transactions,
                warnings
        );
    }

    static String normalize(String extractedText) {
        if (extractedText == null) {
            return "";
        }
        return extractedText.toLowerCase(Locale.ROOT);
    }

    private static Optional<LocalDate> findDate(Pattern pattern, String extractedText) {
        Matcher matcher = pattern.matcher(extractedText == null ? "" : extractedText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseDate(matcher.group(1));
    }

    private static Optional<BigDecimal> findAmount(Pattern pattern, String extractedText) {
        Matcher matcher = pattern.matcher(extractedText == null ? "" : extractedText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseAmount(matcher.group(1));
    }

    private static Optional<LocalDate> parseDate(String value) {
        try {
            if (value.contains("/")) {
                return Optional.of(LocalDate.parse(value, SLASH_DATE));
            }
            return Optional.of(LocalDate.parse(value));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private static Optional<BigDecimal> parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String compact = value.trim().replace(" ", "");
        int lastComma = compact.lastIndexOf(',');
        int lastDot = compact.lastIndexOf('.');
        String normalized;
        if (lastComma > lastDot) {
            normalized = compact.replace(".", "").replace(',', '.');
        } else if (lastDot > lastComma) {
            normalized = compact.replace(",", "");
        } else {
            normalized = compact;
        }
        try {
            return Optional.of(new BigDecimal(normalized));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static List<ParsedTransaction> parseTransactions(String extractedText) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        String[] lines = (extractedText == null ? "" : extractedText).split("\\R");
        for (String line : lines) {
            parseTransaction(line).ifPresent(transactions::add);
        }
        return transactions;
    }

    private static Optional<ParsedTransaction> parseTransaction(String line) {
        if (line == null || !line.trim().toLowerCase(Locale.ROOT).matches("^(tx|transaction)\\b.*")) {
            return Optional.empty();
        }
        String normalized = line.trim().replaceFirst("(?iu)^(tx|transaction)\\s*[:|]\\s*", "");
        String[] parts = normalized.split("\\|");
        if (parts.length < 3) {
            return Optional.empty();
        }
        Optional<LocalDate> transactionDate = parseDate(parts[0].trim());
        String description = parts[1].trim();
        Optional<Money> money = parseMoney(parts[2].trim());
        if (transactionDate.isEmpty() || description.isBlank() || description.length() > 240 || money.isEmpty()) {
            return Optional.empty();
        }
        String notes = parts.length >= 4 ? parts[3].trim() : null;
        String installmentSource = notes == null ? description : description + " " + notes;
        InstallmentMarker installment = findInstallment(installmentSource);
        String normalizedNotes = notes == null || notes.isBlank() ? null : notes;
        boolean planZDetected = isPlanZ(installmentSource);
        if (installment.current() == null && planZDetected) {
            Integer current = findPlanZCurrent(installmentSource).orElse(null);
            installment = new InstallmentMarker(current, current == null ? null : 3);
            if (current == null) {
                normalizedNotes = appendNote(normalizedNotes, "Plan Z detected; review current installment before confirmation");
            }
        }
        TransactionType type = installment.current() == null && !planZDetected ? TransactionType.PURCHASE : TransactionType.INSTALLMENT;

        return Optional.of(new ParsedTransaction(
                transactionDate.get(),
                description,
                type,
                money.get().pesos(),
                money.get().usd(),
                installment.current(),
                installment.total(),
                normalizedNotes
        ));
    }

    private static Optional<Money> parseMoney(String value) {
        Matcher matcher = Pattern.compile("(?iu)^(ARS|USD|US\\$|\\$)\\s*([0-9][0-9.,]*)$").matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<BigDecimal> amount = parseAmount(matcher.group(2));
        if (amount.isEmpty()) {
            return Optional.empty();
        }
        String currency = matcher.group(1).toUpperCase(Locale.ROOT);
        if ("USD".equals(currency) || "US$".equals(currency)) {
            return Optional.of(new Money(null, amount.get()));
        }
        return Optional.of(new Money(amount.get(), null));
    }

    private static InstallmentMarker findInstallment(String value) {
        Matcher matcher = INSTALLMENT.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return new InstallmentMarker(null, null);
        }
        String current = matcher.group(1) == null ? matcher.group(3) : matcher.group(1);
        String total = matcher.group(2) == null ? matcher.group(4) : matcher.group(2);
        return new InstallmentMarker(Integer.valueOf(current), Integer.valueOf(total));
    }

    private static boolean isPlanZ(String value) {
        return PLAN_Z.matcher(value == null ? "" : value).find();
    }

    private static Optional<Integer> findPlanZCurrent(String value) {
        Matcher matcher = PLAN_Z_CURRENT.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.valueOf(matcher.group(1)));
    }

    private static String appendNote(String notes, String note) {
        if (notes == null || notes.isBlank()) {
            return note;
        }
        return notes + "; " + note;
    }

    private static List<String> warnings(LocalDate dueDate, LocalDate closingDate, BigDecimal totalPesos,
                                         BigDecimal totalUsd, List<ParsedTransaction> transactions) {
        List<String> warnings = new ArrayList<>();
        if (dueDate == null) {
            warnings.add("Due date was not detected");
        }
        if (closingDate == null) {
            warnings.add("Closing date was not detected");
        }
        if (totalPesos == null && totalUsd == null) {
            warnings.add("Statement totals were not detected");
        }
        if (transactions.isEmpty()) {
            warnings.add("No reliable transaction rows were detected");
        }
        return warnings;
    }

    private record Money(BigDecimal pesos, BigDecimal usd) {
    }

    private record InstallmentMarker(Integer current, Integer total) {
    }
}
