package com.gentleia.landingtarjetas.statement.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.TransactionType;

final class StatementParseSupport {

    private static final String NUMERIC_DATE_VALUE = "\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[-/.]\\d{1,2}(?:[-/.]\\d{2,4})?";
    private static final String MONTH_NAME = "(?:ene(?:ro)?|feb(?:rero)?|mar(?:zo)?|abr(?:il)?|may(?:o)?|jun(?:io)?|jul(?:io)?|ago(?:sto)?|sep(?:tiembre)?|set(?:iembre)?|oct(?:ubre)?|nov(?:iembre)?|dic(?:iem(?:bre)?)?)\\.?";
    private static final String MONTH_NAME_DATE_VALUE = "\\d{1,2}\\s+" + MONTH_NAME + "\\s+\\d{2,4}";
    private static final Pattern DATE_VALUE = Pattern.compile("(?iu)\\b(" + NUMERIC_DATE_VALUE + "|" + MONTH_NAME_DATE_VALUE + ")\\b");
    private static final String CURRENCY_MARKER = "(?:ARS|USD|US\\$|U\\$S|\\$)";
    private static final String AMOUNT_NUMBER = "(?:\\d{1,3}(?:[. ]\\d{3})+|\\d+)(?:[,.]\\d{2})?";
    private static final String DECIMAL_AMOUNT_NUMBER = "(?:\\d{1,3}(?:[. ]\\d{3})+|\\d+)(?:[,.]\\d{2})";
    private static final String STRICT_AMOUNT_CORE = "[+-]?\\s*(?:" + CURRENCY_MARKER + ")\\s*[+-]?\\s*" + AMOUNT_NUMBER
            + "(?:\\s*(?:" + CURRENCY_MARKER + "))?\\s*[+-]?"
            + "|[+-]?\\s*" + DECIMAL_AMOUNT_NUMBER + "(?:\\s*(?:" + CURRENCY_MARKER + "))?\\s*[+-]?";
    private static final Pattern STRICT_AMOUNT_VALUE = Pattern.compile(
            "(?iu)(?:\\(\\s*(?:" + STRICT_AMOUNT_CORE + ")\\s*\\)|(?:" + STRICT_AMOUNT_CORE + "))");
    private static final Pattern LINE_DATE = Pattern.compile("(?iu)^\\s*(" + NUMERIC_DATE_VALUE + "|" + MONTH_NAME_DATE_VALUE + ")\\s+(.+)$");
    private static final Pattern DATE_PREFIX = Pattern.compile("(?iu)^\\s*(?:" + NUMERIC_DATE_VALUE + "|" + MONTH_NAME_DATE_VALUE + ")\\b.*$");
    private static final Pattern LEADING_DATE_PREFIX = Pattern.compile("(?iu)^\\s*(?:" + NUMERIC_DATE_VALUE + "|" + MONTH_NAME_DATE_VALUE + ")\\s+");
    private static final Pattern MONTH_NAME_DATE_PARTS = Pattern.compile("(?iu)^(\\d{1,2})\\s+(" + MONTH_NAME + ")\\s+(\\d{2,4})$");
    private static final Pattern SANTANDER_VISA_FULL_LINE = Pattern.compile(
            "(?iu)^\\s*(\\d{2})\\s+(" + MONTH_NAME + ")\\s+(\\d{1,2})\\s+(.+)$");
    private static final Pattern SANTANDER_VISA_CONTINUATION_LINE = Pattern.compile(
            "(?iu)^\\s*(\\d{1,2})\\s+(\\d{4,6})(?:\\s+([*K]))?\\s+(.+)$");
    private static final Pattern SANTANDER_VISA_ROW_BODY = Pattern.compile(
            "(?iu)^\\s*(\\d{4,6})(?:\\s+([*K]))?\\s+(.+)$");
    private static final Pattern SANTANDER_VISA_TOTAL_CONSUMOS = Pattern.compile(
            "(?iu).*\\btarjeta\\b.*\\btotal\\s+consumos\\b.*");
    private static final Pattern SANTANDER_VISA_POST_TOTAL_CHARGE = Pattern.compile(
            "(?iu)^\\s*((?:IMPUESTO\\s+DE\\s+SELLOS(?:\\s+P)?|IVA\\s+RG\\s+\\d+|DB\\.RG\\s+\\d+)\\b.*)$");
    private static final Pattern SANTANDER_VISA_INSTALLMENT_TOKEN = Pattern.compile(
            "(?iu)\\bC\\.\\s*\\d{1,2}\\s*/\\s*\\d{1,2}\\b");
    private static final List<Pattern> DUE_DATE_LABELS = List.of(
            label("payment due date"),
            label("due date"),
            label("fecha\\s+de\\s+vencimiento(?:\\s+actual)?"),
            label("fecha\\s+vencimiento(?:\\s+actual)?"),
            label("fecha\\s+limite\\s+de\\s+pago"),
            label("proximo\\s+vencimiento"),
            label("vencimiento(?:\\s+actual)?"),
            label("vence(?:\\s+el)?"),
            label("vto\\.?")
    );
    private static final List<Pattern> CLOSING_DATE_LABELS = List.of(
            label("statement closing date"),
            label("closing date"),
            label("fecha\\s+de\\s+cierre(?:\\s+actual)?"),
            label("fecha\\s+cierre(?:\\s+actual)?"),
            label("proximo\\s+cierre"),
            label("cierre(?:\\s+actual)?"),
            label("cierra(?:\\s+el)?")
    );
    private static final List<Pattern> TOTAL_PESOS_LABELS = List.of(
            label("total\\s+pesos"),
            label("total\\s+ars"),
            label("total\\s+en\\s+pesos"),
            label("total\\s+a\\s+pagar(?:\\s+en\\s+pesos)?(?!\\s+en\\s+dolares)"),
            label("saldo\\s+actual\\s+en\\s+pesos"),
            label("saldo\\s+en\\s+pesos"),
            label("saldo\\s+actual"),
            label("total\\s+actual"),
            label("importe\\s+total\\s+en\\s+pesos"),
            label("total\\s+\\$")
    );
    private static final List<Pattern> TOTAL_USD_LABELS = List.of(
            label("total\\s+usd"),
            label("total\\s+us\\$"),
            label("total\\s+dollars"),
            label("total\\s+dolares"),
            label("total\\s+en\\s+dolares"),
            label("total\\s+a\\s+pagar\\s+en\\s+dolares"),
            label("saldo\\s+actual\\s+en\\s+dolares"),
            label("saldo\\s+actual\\s+usd"),
            label("saldo\\s+en\\s+dolares"),
            label("importe\\s+total\\s+en\\s+dolares")
    );
    private static final List<Pattern> MINIMUM_PAYMENT_LABELS = List.of(
            label("minimum\\s+payment"),
            label("pago\\s+minimo"),
            label("minimo\\s+a\\s+pagar")
    );
    private static final List<List<Pattern>> FIELD_LABEL_GROUPS = List.of(
            DUE_DATE_LABELS,
            CLOSING_DATE_LABELS,
            TOTAL_PESOS_LABELS,
            TOTAL_USD_LABELS,
            MINIMUM_PAYMENT_LABELS
    );
    private static final Pattern INSTALLMENT = Pattern.compile(
            "(?iu)(?:installment|cuota)\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})|\\b(\\d{1,2})\\s*/\\s*(\\d{1,2})\\b");
    private static final Pattern PLAN_Z = Pattern.compile("(?iu)\\b(?:plan\\s*z|plan\\s*zeta|zeta)\\b");
    private static final Pattern PLAN_Z_BARE_MARKER = Pattern.compile(
            "(?iu)(?:^|\\s)[+-]?\\s*" + AMOUNT_NUMBER + "\\s+z\\b\\s*$");
    private static final Pattern PLAN_Z_BARE_AMOUNT = Pattern.compile(
            "(?iu)(?:^|\\s)([+-]?\\s*" + AMOUNT_NUMBER + ")(?:\\s+(?:plan\\s*z|plan\\s*zeta|zeta|z)\\b)\\s*$");
    private static final Pattern PLAN_Z_CURRENT = Pattern.compile("(?iu)(?:installment|cuota)\\s*(\\d{1,2})(?!\\s*/)");
    private static final int LABEL_LOOKAHEAD_LINES = 3;
    private static final int MULTILINE_TRANSACTION_LOOKAHEAD_LINES = 2;
    private static final int PLAN_Z_TOTAL_INSTALLMENTS = 3;

    private StatementParseSupport() {
    }

    static ParsedStatement parseCommon(String extractedText, Provider provider, CardBrand cardBrand, String cardAlias) {
        Optional<String> dueDateValue = findValueNearLabels(DUE_DATE_LABELS, extractedText, DATE_VALUE);
        Optional<String> closingDateValue = findValueNearLabels(CLOSING_DATE_LABELS, extractedText, DATE_VALUE);
        Integer statementYear = inferStatementYear(dueDateValue, closingDateValue).orElse(null);
        LocalDate dueDateWithYear = dueDateValue.flatMap(value -> parseDate(value, null, DateRole.DUE, null)).orElse(null);
        LocalDate closingDateWithYear = closingDateValue.flatMap(value -> parseDate(value, null, DateRole.CLOSING, null)).orElse(null);
        LocalDate closingDate = closingDateValue
                .flatMap(value -> parseDate(value, dueDateWithYear, DateRole.CLOSING, statementYear))
                .orElse(null);
        LocalDate dueDate = dueDateValue
                .flatMap(value -> parseDate(value, closingDate == null ? closingDateWithYear : closingDate, DateRole.DUE, statementYear))
                .orElse(null);
        BigDecimal totalPesos = findAmount(TOTAL_PESOS_LABELS, extractedText).orElse(null);
        BigDecimal totalUsd = findAmount(TOTAL_USD_LABELS, extractedText).orElse(null);
        BigDecimal minimumPaymentPesos = findAmount(MINIMUM_PAYMENT_LABELS, extractedText).orElse(null);
        List<ParsedTransaction> transactions = parseTransactions(extractedText, closingDate, dueDate, statementYear,
                provider, cardBrand);
        if (provider == Provider.NARANJA_X && !transactions.isEmpty()) {
            Money transactionTotals = sumTransactionAmounts(transactions);
            totalPesos = transactionTotals.pesos();
            totalUsd = transactionTotals.usd();
        }
        List<String> warnings = warnings(dueDate, closingDate, totalPesos, totalUsd, minimumPaymentPesos, transactions);

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

    static Optional<CardBrand> detectNaranjaCardBrand(String extractedText) {
        String[] lines = (extractedText == null ? "" : extractedText).split("\\R");
        for (String line : lines) {
            String normalizedLine = fold(line);
            if (normalizedLine.contains("naranja") && normalizedLine.contains("visa")) {
                return Optional.of(CardBrand.VISA);
            }
            if (normalizedLine.contains("naranja") && normalizedLine.contains("mastercard")) {
                return Optional.of(CardBrand.MASTERCARD);
            }
            if (normalizedLine.contains("naranja")
                    && (normalizedLine.contains("american express") || normalizedLine.contains("amex"))) {
                return Optional.of(CardBrand.AMERICAN_EXPRESS);
            }
        }
        return Optional.empty();
    }

    static String normalize(String extractedText) {
        if (extractedText == null) {
            return "";
        }
        return fold(extractedText);
    }

    private static Pattern label(String pattern) {
        return Pattern.compile("(?iu)(?:" + pattern + ")\\s*[:\\-]?");
    }

    private static String fold(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }

    private static Optional<String> findValueNearLabels(List<Pattern> labels, String extractedText, Pattern valuePattern) {
        String[] lines = (extractedText == null ? "" : extractedText).split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String normalizedLine = fold(lines[index]);
            for (Pattern label : labels) {
                Matcher labelMatcher = label.matcher(normalizedLine);
                if (labelMatcher.find()) {
                    String sameLineSource = sameLineValueSource(lines[index], normalizedLine, labelMatcher.end(), labels);
                    Optional<String> sameLineValue = firstValue(sameLineSource, valuePattern);
                    if (sameLineValue.isPresent()) {
                        return sameLineValue;
                    }
                    for (int lookahead = 1; lookahead <= LABEL_LOOKAHEAD_LINES && index + lookahead < lines.length; lookahead++) {
                        String nearbyLine = lines[index + lookahead];
                        if (containsFieldLabel(nearbyLine)) {
                            break;
                        }
                        if (valuePattern == DATE_VALUE && hasMultipleDateLabels(normalizedLine)) {
                            Optional<String> alignedValue = dateValueByLabelOrdinal(
                                    normalizedLine,
                                    labelMatcher.start(),
                                    nearbyLine
                            );
                            if (alignedValue.isPresent()) {
                                return alignedValue;
                            }
                            continue;
                        }
                        if (valuePattern == STRICT_AMOUNT_VALUE && hasMultipleAmountLabels(normalizedLine)) {
                            Optional<String> alignedValue = amountValueByLabelOrdinal(
                                    normalizedLine,
                                    labelMatcher.start(),
                                    nearbyLine
                            );
                            if (alignedValue.isPresent()) {
                                return alignedValue;
                            }
                            continue;
                        }
                        Optional<String> nearbyValue = firstValue(nearbyLine, valuePattern);
                        if (nearbyValue.isPresent()) {
                            return nearbyValue;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean hasMultipleDateLabels(String normalizedLine) {
        return dateLabelStarts(normalizedLine).size() > 1;
    }

    private static boolean hasMultipleAmountLabels(String normalizedLine) {
        return amountLabelStarts(normalizedLine).size() > 1;
    }

    private static Optional<String> dateValueByLabelOrdinal(String normalizedLine, int labelStart, String valueLine) {
        List<Integer> labelStarts = dateLabelStarts(normalizedLine);
        List<String> values = allValues(valueLine, DATE_VALUE);
        int ordinal = 0;
        for (int start : labelStarts) {
            if (start < labelStart) {
                ordinal++;
            }
        }
        if (ordinal >= values.size()) {
            return Optional.empty();
        }
        return Optional.of(values.get(ordinal));
    }

    private static Optional<String> amountValueByLabelOrdinal(String normalizedLine, int labelStart, String valueLine) {
        List<Integer> labelStarts = amountLabelStarts(normalizedLine);
        List<String> values = allValues(valueLine, STRICT_AMOUNT_VALUE);
        int ordinal = 0;
        for (int start : labelStarts) {
            if (start < labelStart) {
                ordinal++;
            }
        }
        if (ordinal >= values.size()) {
            return Optional.empty();
        }
        return Optional.of(values.get(ordinal));
    }

    private static List<Integer> dateLabelStarts(String normalizedLine) {
        TreeSet<Integer> starts = new TreeSet<>();
        firstLabelStart(DUE_DATE_LABELS, normalizedLine).ifPresent(starts::add);
        firstLabelStart(CLOSING_DATE_LABELS, normalizedLine).ifPresent(starts::add);
        return new ArrayList<>(starts);
    }

    private static List<Integer> amountLabelStarts(String normalizedLine) {
        TreeSet<Integer> starts = new TreeSet<>();
        labelStarts(TOTAL_PESOS_LABELS, normalizedLine).forEach(starts::add);
        labelStarts(TOTAL_USD_LABELS, normalizedLine).forEach(starts::add);
        labelStarts(MINIMUM_PAYMENT_LABELS, normalizedLine).forEach(starts::add);
        return new ArrayList<>(starts);
    }

    private static List<Integer> labelStarts(List<Pattern> labels, String normalizedLine) {
        List<Integer> starts = new ArrayList<>();
        for (Pattern label : labels) {
            Matcher matcher = label.matcher(normalizedLine);
            while (matcher.find()) {
                starts.add(matcher.start());
            }
        }
        return starts;
    }

    private static Optional<Integer> firstLabelStart(List<Pattern> labels, String normalizedLine) {
        Integer firstStart = null;
        for (Pattern label : labels) {
            Matcher matcher = label.matcher(normalizedLine);
            if (matcher.find() && (firstStart == null || matcher.start() < firstStart)) {
                firstStart = matcher.start();
            }
        }
        return Optional.ofNullable(firstStart);
    }

    private static String sameLineValueSource(String line, String normalizedLine, int start, List<Pattern> targetLabels) {
        int safeStart = Math.min(start, line.length());
        int nextLabelStart = nextUnrelatedFieldLabelStart(normalizedLine, safeStart, targetLabels);
        int safeEnd = nextLabelStart < 0 ? line.length() : Math.min(nextLabelStart, line.length());
        return line.substring(safeStart, safeEnd);
    }

    private static int nextUnrelatedFieldLabelStart(String normalizedLine, int start, List<Pattern> targetLabels) {
        int nextStart = -1;
        for (List<Pattern> fieldLabels : FIELD_LABEL_GROUPS) {
            if (fieldLabels == targetLabels) {
                continue;
            }
            for (Pattern fieldLabel : fieldLabels) {
                Matcher matcher = fieldLabel.matcher(normalizedLine);
                if (matcher.find(start) && (nextStart < 0 || matcher.start() < nextStart)) {
                    nextStart = matcher.start();
                }
            }
        }
        return nextStart;
    }

    private static boolean containsFieldLabel(String line) {
        for (List<Pattern> fieldLabels : FIELD_LABEL_GROUPS) {
            if (containsAnyLabel(fieldLabels, line)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyLabel(List<Pattern> labels, String line) {
        String normalizedLine = fold(line);
        for (Pattern label : labels) {
            if (label.matcher(normalizedLine).find()) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> firstValue(String value, Pattern valuePattern) {
        Matcher matcher = valuePattern.matcher(value == null ? "" : value);
        while (matcher.find()) {
            String candidate = matcher.group().trim();
            if (!candidate.isBlank()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static List<String> allValues(String value, Pattern valuePattern) {
        List<String> values = new ArrayList<>();
        Matcher matcher = valuePattern.matcher(value == null ? "" : value);
        while (matcher.find()) {
            String candidate = matcher.group().trim();
            if (!candidate.isBlank()) {
                values.add(candidate);
            }
        }
        return values;
    }

    private static Optional<BigDecimal> findAmount(List<Pattern> labels, String extractedText) {
        return findValueNearLabels(labels, extractedText, STRICT_AMOUNT_VALUE).flatMap(StatementParseSupport::parseAmount);
    }

    private static Optional<Integer> inferStatementYear(Optional<String> dueDateValue, Optional<String> closingDateValue) {
        Integer year = null;
        for (Optional<String> dateValue : List.of(dueDateValue, closingDateValue)) {
            Optional<Integer> explicitYear = dateValue.flatMap(StatementParseSupport::explicitYear);
            if (explicitYear.isEmpty()) {
                continue;
            }
            Integer candidate = explicitYear.get();
            if (year == null) {
                year = candidate;
            } else if (!year.equals(candidate)) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(year);
    }

    private static Optional<Integer> explicitYear(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return Optional.of(Integer.valueOf(trimmed.substring(0, 4)));
        }
        Matcher monthNameMatcher = MONTH_NAME_DATE_PARTS.matcher(trimmed);
        if (monthNameMatcher.matches()) {
            int parsedYear = Integer.parseInt(monthNameMatcher.group(3));
            return Optional.of(parsedYear < 100 ? 2000 + parsedYear : parsedYear);
        }
        String[] parts = trimmed.split("[-/.]");
        if (parts.length != 3) {
            return Optional.empty();
        }
        int parsedYear = Integer.parseInt(parts[2]);
        return Optional.of(parsedYear < 100 ? 2000 + parsedYear : parsedYear);
    }

    private static Optional<LocalDate> parseDate(String value, LocalDate referenceDate, DateRole role, Integer fallbackYear) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        try {
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return Optional.of(LocalDate.parse(trimmed));
            }
            Matcher monthNameMatcher = MONTH_NAME_DATE_PARTS.matcher(trimmed);
            if (monthNameMatcher.matches()) {
                int day = Integer.parseInt(monthNameMatcher.group(1));
                Optional<Integer> month = monthNumber(monthNameMatcher.group(2));
                if (month.isEmpty()) {
                    return Optional.empty();
                }
                int parsedYear = Integer.parseInt(monthNameMatcher.group(3));
                int year = parsedYear < 100 ? 2000 + parsedYear : parsedYear;
                return Optional.of(LocalDate.of(year, month.get(), day));
            }
            String[] parts = trimmed.split("[-/.]");
            if (parts.length < 2 || parts.length > 3) {
                return Optional.empty();
            }
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            if (parts.length == 3) {
                int parsedYear = Integer.parseInt(parts[2]);
                int year = parsedYear < 100 ? 2000 + parsedYear : parsedYear;
                return Optional.of(LocalDate.of(year, month, day));
            }
            if (referenceDate == null) {
                if (fallbackYear == null) {
                    return Optional.empty();
                }
                return Optional.of(LocalDate.of(fallbackYear, month, day));
            }
            int year = inferYear(month, referenceDate, role);
            return Optional.of(LocalDate.of(year, month, day));
        } catch (DateTimeException | NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> monthNumber(String value) {
        String normalized = fold(value).replace(".", "").trim();
        if (normalized.startsWith("ene")) {
            return Optional.of(1);
        }
        if (normalized.startsWith("feb")) {
            return Optional.of(2);
        }
        if (normalized.startsWith("mar")) {
            return Optional.of(3);
        }
        if (normalized.startsWith("abr")) {
            return Optional.of(4);
        }
        if (normalized.startsWith("may")) {
            return Optional.of(5);
        }
        if (normalized.startsWith("jun")) {
            return Optional.of(6);
        }
        if (normalized.startsWith("jul")) {
            return Optional.of(7);
        }
        if (normalized.startsWith("ago")) {
            return Optional.of(8);
        }
        if (normalized.startsWith("sep") || normalized.startsWith("set")) {
            return Optional.of(9);
        }
        if (normalized.startsWith("oct")) {
            return Optional.of(10);
        }
        if (normalized.startsWith("nov")) {
            return Optional.of(11);
        }
        if (normalized.startsWith("dic")) {
            return Optional.of(12);
        }
        return Optional.empty();
    }

    private static int inferYear(int month, LocalDate referenceDate, DateRole role) {
        int year = referenceDate.getYear();
        if (role == DateRole.DUE && month < referenceDate.getMonthValue() && referenceDate.getMonthValue() >= 11) {
            return year + 1;
        }
        if ((role == DateRole.CLOSING || role == DateRole.TRANSACTION)
                && month > referenceDate.getMonthValue() && referenceDate.getMonthValue() <= 2) {
            return year - 1;
        }
        return year;
    }

    private static Optional<BigDecimal> parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String signedValue = value.replace('\u2212', '-');
        boolean negative = signedValue.contains("(") && signedValue.contains(")");
        String compact = signedValue.trim()
                .replace('\u00A0', ' ')
                .replaceAll("(?iu)ARS|USD|US\\$|U\\$S|\\$", "")
                .replace(" ", "")
                .replace("+", "");
        if (compact.startsWith("-")) {
            negative = true;
            compact = compact.substring(1);
        }
        if (compact.endsWith("-")) {
            negative = true;
            compact = compact.substring(0, compact.length() - 1);
        }
        compact = compact.replaceAll("[^0-9,.]", "");
        if (compact.isBlank()) {
            return Optional.empty();
        }
        int lastComma = compact.lastIndexOf(',');
        int lastDot = compact.lastIndexOf('.');
        String normalized;
        if (lastComma > lastDot) {
            normalized = compact.replace(".", "").replace(',', '.');
        } else if (lastDot > lastComma) {
            int decimalDigits = compact.length() - lastDot - 1;
            if (decimalDigits == 3 && compact.indexOf('.') == lastDot && !compact.contains(",")) {
                normalized = compact.replace(".", "");
            } else {
                normalized = compact.replace(",", "");
            }
        } else {
            normalized = compact;
        }
        try {
            BigDecimal amount = new BigDecimal(normalized);
            return Optional.of(negative ? amount.negate() : amount);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static List<ParsedTransaction> parseTransactions(String extractedText, LocalDate closingDate, LocalDate dueDate,
                                                            Integer statementYear, Provider provider,
                                                            CardBrand cardBrand) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        String[] lines = (extractedText == null ? "" : extractedText).split("\\R");
        LocalDate referenceDate = closingDate == null ? dueDate : closingDate;
        if (provider == Provider.SANTANDER && cardBrand == CardBrand.VISA) {
            return parseSantanderVisaTransactions(lines, referenceDate, statementYear);
        }
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            Optional<ParsedTransaction> transaction = parseSyntheticTransaction(line, referenceDate, statementYear)
                    .or(() -> parseTableTransaction(line, referenceDate, statementYear));
            if (transaction.isPresent()) {
                transactions.add(transaction.get());
                continue;
            }
            if (startsWithDate(line)) {
                String combined = line;
                for (int lookahead = 1; lookahead <= MULTILINE_TRANSACTION_LOOKAHEAD_LINES && index + lookahead < lines.length; lookahead++) {
                    combined = combined + " " + lines[index + lookahead];
                    Optional<ParsedTransaction> combinedTransaction = parseTableTransaction(combined, referenceDate, statementYear);
                    if (combinedTransaction.isPresent()) {
                        transactions.add(combinedTransaction.get());
                        index += lookahead;
                        break;
                    }
                }
            }
        }
        return transactions;
    }

    private static List<ParsedTransaction> parseSantanderVisaTransactions(String[] lines, LocalDate referenceDate,
                                                                          Integer statementYear) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        Integer currentYear = null;
        Integer currentMonth = null;
        boolean afterTotalConsumos = false;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (isSantanderVisaTotalConsumos(line)) {
                afterTotalConsumos = true;
                continue;
            }
            if (afterTotalConsumos) {
                parseSantanderVisaPostTotalCharge(line).ifPresent(transactions::add);
                continue;
            }

            Matcher fullLine = SANTANDER_VISA_FULL_LINE.matcher(line == null ? "" : line.trim());
            if (fullLine.matches()) {
                Optional<LocalDate> transactionDate = santanderVisaFullDate(fullLine, statementYear);
                if (transactionDate.isPresent()) {
                    currentYear = transactionDate.get().getYear();
                    currentMonth = transactionDate.get().getMonthValue();
                    Optional<ParsedTransaction> transaction = parseSantanderVisaFullDateTransaction(
                            transactionDate.get(), fullLine.group(4));
                    if (transaction.isPresent()) {
                        transactions.add(transaction.get());
                    }
                    continue;
                }
                continue;
            }

            Matcher continuation = SANTANDER_VISA_CONTINUATION_LINE.matcher(line == null ? "" : line.trim());
            if (continuation.matches() && currentYear != null && currentMonth != null) {
                Optional<ParsedTransaction> transaction = parseSantanderVisaContinuationTransaction(
                        currentYear,
                        currentMonth,
                        continuation
                );
                if (transaction.isPresent()) {
                    transactions.add(transaction.get());
                    continue;
                }
            }

            Optional<ParsedTransaction> transaction = parseSyntheticTransaction(line, referenceDate, statementYear)
                    .or(() -> parseTableTransaction(line, referenceDate, statementYear));
            if (transaction.isPresent()) {
                transactions.add(transaction.get());
                continue;
            }
            if (startsWithDate(line)) {
                String combined = line;
                for (int lookahead = 1; lookahead <= MULTILINE_TRANSACTION_LOOKAHEAD_LINES && index + lookahead < lines.length; lookahead++) {
                    combined = combined + " " + lines[index + lookahead];
                    Optional<ParsedTransaction> combinedTransaction = parseTableTransaction(combined, referenceDate, statementYear);
                    if (combinedTransaction.isPresent()) {
                        transactions.add(combinedTransaction.get());
                        index += lookahead;
                        break;
                    }
                }
            }
        }
        return transactions;
    }

    private static boolean isSantanderVisaTotalConsumos(String line) {
        return SANTANDER_VISA_TOTAL_CONSUMOS.matcher(line == null ? "" : line).matches();
    }

    private static Optional<LocalDate> santanderVisaFullDate(Matcher fullLine, Integer statementYear) {
        Optional<Integer> month = monthNumber(fullLine.group(2));
        if (month.isEmpty()) {
            return Optional.empty();
        }

        int firstNumber = Integer.parseInt(fullLine.group(1));
        int lastNumber = Integer.parseInt(fullLine.group(3));
        int parsedYear = 2000 + firstNumber;
        if (parsedYear >= 2020 && (statementYear == null || parsedYear <= statementYear)) {
            try {
                return Optional.of(LocalDate.of(parsedYear, month.get(), lastNumber));
            } catch (DateTimeException exception) {
                // Keep evaluating older supported DD month YY rows below.
            }
        }

        int legacyYear = 2000 + lastNumber;
        if (statementYear != null && legacyYear == statementYear) {
            try {
                return Optional.of(LocalDate.of(legacyYear, month.get(), firstNumber));
            } catch (DateTimeException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<ParsedTransaction> parseSantanderVisaFullDateTransaction(LocalDate transactionDate,
                                                                                    String row) {
        Matcher body = SANTANDER_VISA_ROW_BODY.matcher(row == null ? "" : row.trim());
        if (!body.matches()) {
            return Optional.empty();
        }
        return parseSantanderVisaDatedTransaction(transactionDate, body.group(3), body.group(1));
    }

    private static Optional<ParsedTransaction> parseSantanderVisaContinuationTransaction(int year, int month,
                                                                                        Matcher continuation) {
        int day = Integer.parseInt(continuation.group(1));
        try {
            return parseSantanderVisaDatedTransaction(LocalDate.of(year, month, day), continuation.group(4), continuation.group(2));
        } catch (DateTimeException exception) {
            return Optional.empty();
        }
    }

    private static Optional<ParsedTransaction> parseSantanderVisaDatedTransaction(LocalDate transactionDate,
                                                                                 String transactionSource,
                                                                                 String operationNumber) {
        if (looksLikeNonTransactionLine(transactionSource)) {
            return Optional.empty();
        }
        List<AmountMatch> amountMatches = amountMatches(transactionSource);
        if (amountMatches.isEmpty()) {
            return Optional.empty();
        }
        String installmentSource = transactionSource.substring(0, amountMatches.get(0).start()).trim();
        String description = stripSantanderVisaInstallmentToken(installmentSource).replaceAll("\\s+", " ").trim();
        if (description.isBlank() || description.length() > 240 || looksLikeNonTransactionLine(description)) {
            return Optional.empty();
        }
        Optional<Money> money = mergeMoney(amountMatches);
        if (money.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(createTransaction(transactionDate, description, money.get(), transactionSource, operationNumber, null));
    }

    private static Optional<ParsedTransaction> parseSantanderVisaPostTotalCharge(String line) {
        Matcher charge = SANTANDER_VISA_POST_TOTAL_CHARGE.matcher(line == null ? "" : line.trim());
        if (!charge.matches() || looksLikeNonTransactionLine(line)) {
            return Optional.empty();
        }
        String source = charge.group(1);
        List<AmountMatch> amountMatches = amountMatches(source);
        if (amountMatches.isEmpty()) {
            return Optional.empty();
        }
        String description = source.substring(0, amountMatches.get(0).start()).trim().replaceAll("\\s+", " ");
        if (description.isBlank() || description.length() > 240) {
            return Optional.empty();
        }
        Optional<Money> money = mergeMoney(amountMatches);
        if (money.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(createTransaction(null, description, money.get(), description, null, null));
    }

    private static String stripSantanderVisaInstallmentToken(String value) {
        return SANTANDER_VISA_INSTALLMENT_TOKEN.matcher(value == null ? "" : value).replaceAll("");
    }

    private static Optional<ParsedTransaction> parseSyntheticTransaction(String line, LocalDate referenceDate, Integer statementYear) {
        if (line == null || !line.trim().toLowerCase(Locale.ROOT).matches("^(tx|transaction)\\b.*")) {
            return Optional.empty();
        }
        String normalized = line.trim().replaceFirst("(?iu)^(tx|transaction)\\s*[:|]\\s*", "");
        String[] parts = normalized.split("\\|");
        if (parts.length < 3) {
            return Optional.empty();
        }
        Optional<LocalDate> transactionDate = parseDate(parts[0].trim(), referenceDate, DateRole.TRANSACTION, statementYear);
        String description = parts[1].trim();
        Optional<Money> money = parseMoney(parts[2].trim(), Currency.PESOS);
        if (transactionDate.isEmpty() || description.isBlank() || description.length() > 240 || money.isEmpty()) {
            return Optional.empty();
        }
        String operationNumber = null;
        String notes = null;
        if (parts.length >= 4) {
            String optional = parts[3].trim();
            if (optional.toLowerCase(Locale.ROOT).startsWith("op:")) {
                operationNumber = optional.substring(3).trim();
                notes = parts.length >= 5 ? parts[4].trim() : null;
            } else {
                notes = optional;
            }
        }
        String installmentSource = notes == null ? description : description + " " + notes;
        return Optional.of(createTransaction(transactionDate.get(), description, money.get(), installmentSource, operationNumber, notes));
    }

    private static Optional<ParsedTransaction> parseTableTransaction(String line, LocalDate referenceDate, Integer statementYear) {
        Matcher matcher = LINE_DATE.matcher(line == null ? "" : line.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<LocalDate> transactionDate = parseDate(matcher.group(1), referenceDate, DateRole.TRANSACTION, statementYear);
        if (transactionDate.isEmpty() || looksLikeNonTransactionLine(line)) {
            return Optional.empty();
        }
        OperationScopedRow scopedRow = extractLeadingOperationNumber(matcher.group(2).trim());
        String row = scopedRow.row();
        List<AmountMatch> amountMatches = amountMatches(row);
        if (amountMatches.isEmpty() && isPlanZ(row)) {
            amountMatches = planZBareAmountMatches(row);
        }
        if (amountMatches.isEmpty()) {
            return Optional.empty();
        }
        String description = row.substring(0, amountMatches.get(0).start()).trim().replaceAll("\\s+", " ");
        if (description.isBlank() || description.length() > 240 || looksLikeNonTransactionLine(description)) {
            return Optional.empty();
        }
        Optional<Money> money = mergeMoney(amountMatches);
        if (money.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(createTransaction(transactionDate.get(), description, money.get(), row, scopedRow.operationNumber(), null));
    }

    private static ParsedTransaction createTransaction(LocalDate transactionDate, String description, Money money,
                                                       String installmentSource, String operationNumber, String notes) {
        InstallmentMarker installment = findInstallment(installmentSource);
        String normalizedNotes = notes == null || notes.isBlank() ? null : notes;
        boolean planZDetected = isPlanZ(installmentSource);
        if (planZDetected) {
            Integer current = installment.current() == null ? findPlanZCurrent(installmentSource).orElse(1) : installment.current();
            installment = new InstallmentMarker(current, PLAN_Z_TOTAL_INSTALLMENTS);
            money = divideMoney(money, PLAN_Z_TOTAL_INSTALLMENTS);
            if (normalizedNotes == null) {
                normalizedNotes = "Plan Z detectado: se cargó como " + current + " de 3 cuotas.";
            }
        }
        TransactionType type = installment.current() == null && !planZDetected ? TransactionType.PURCHASE : TransactionType.INSTALLMENT;
        return new ParsedTransaction(
                transactionDate,
                description,
                type,
                money.pesos(),
                money.usd(),
                installment.current(),
                installment.total(),
                normalizeOperationNumber(operationNumber),
                normalizedNotes
        );
    }

    private static OperationScopedRow extractLeadingOperationNumber(String row) {
        Matcher matcher = Pattern.compile("^\\s*([A-Z0-9-]{4,32})\\s+(.+)$", Pattern.CASE_INSENSITIVE).matcher(row == null ? "" : row.trim());
        if (!matcher.matches()) {
            return new OperationScopedRow(null, row);
        }
        String candidate = matcher.group(1);
        if (!candidate.matches(".*\\d.*")) {
            return new OperationScopedRow(null, row);
        }
        return new OperationScopedRow(candidate, row);
    }

    private static String normalizeOperationNumber(String operationNumber) {
        if (operationNumber == null || operationNumber.isBlank()) {
            return null;
        }
        return operationNumber.trim();
    }

    private static Money divideMoney(Money money, int divisor) {
        return new Money(
                money.pesos() == null ? null : money.pesos().divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP),
                money.usd() == null ? null : money.usd().divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
        );
    }

    private static boolean startsWithDate(String line) {
        return DATE_PREFIX.matcher(line == null ? "" : line.trim()).find();
    }

    private static boolean looksLikeNonTransactionLine(String line) {
        String normalized = fold(line);
        return normalized.contains("fecha de")
                || normalized.contains("vencimiento")
                || normalized.contains("cierre")
                || normalized.contains("total a pagar")
                || normalized.contains("total usd")
                || normalized.contains("total us$")
                || normalized.contains("total en dolares")
                || normalized.contains("total pesos")
                || normalized.contains("total ars")
                || normalized.contains("total actual")
                || normalized.contains("pago minimo")
                || normalized.matches(".*\\bplan\\s+v\\b.*")
                || normalized.contains("cuotas a vencer")
                || normalized.matches(".*\\b(tna|tea|cftea)\\b.*")
                || normalized.contains("legal")
                || normalized.contains("limite")
                || looksLikePaymentOrSummaryRow(normalized)
                || normalized.matches(".*\\b(fecha|descripcion|detalle|importe|consumos|movimientos)\\b.*");
    }

    private static boolean looksLikePaymentOrSummaryRow(String normalized) {
        String withoutLeadingDate = LEADING_DATE_PREFIX.matcher(normalized).replaceFirst("");
        boolean hasLeadingDate = !withoutLeadingDate.equals(normalized);
        return withoutLeadingDate.matches("^(?:su\\s+)?pago\\b.*")
                || (hasLeadingDate && hasShortNumericReferenceBeforePaymentKeyword(withoutLeadingDate))
                || withoutLeadingDate.matches("^payment\\b.*")
                || withoutLeadingDate.matches(".*\\b(pago\\s+recibido|payment\\s+received)\\b.*")
                || normalized.matches(".*\\b(saldo\\s+anterior|saldo\\s+actual|subtotal|total\\s+resumen)\\b.*");
    }

    private static boolean hasShortNumericReferenceBeforePaymentKeyword(String value) {
        return value.matches("^\\d{4,8}\\s+(?:su\\s+)?pago\\b(?:\\s+(?:ars|usd|us\\$|\\$|[+-]?\\d).*)?$")
                || value.matches("^\\d{4,8}\\s+(?:pago\\s+recibido|payment\\s+received)\\b.*");
    }

    private static Money sumTransactionAmounts(List<ParsedTransaction> transactions) {
        BigDecimal pesos = null;
        BigDecimal usd = null;
        for (ParsedTransaction transaction : transactions) {
            if (transaction.amountPesos() != null) {
                pesos = pesos == null ? transaction.amountPesos() : pesos.add(transaction.amountPesos());
            }
            if (transaction.amountUsd() != null) {
                usd = usd == null ? transaction.amountUsd() : usd.add(transaction.amountUsd());
            }
        }
        return new Money(pesos, usd);
    }

    private static List<AmountMatch> amountMatches(String value) {
        List<AmountMatch> matches = new ArrayList<>();
        Matcher matcher = STRICT_AMOUNT_VALUE.matcher(value == null ? "" : value);
        while (matcher.find()) {
            Optional<Money> money = parseMoney(matcher.group(), Currency.PESOS);
            money.ifPresent(parsed -> matches.add(new AmountMatch(matcher.start(), parsed)));
        }
        return matches;
    }

    private static List<AmountMatch> planZBareAmountMatches(String value) {
        Matcher matcher = PLAN_Z_BARE_AMOUNT.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return List.of();
        }
        return parseAmount(matcher.group(1))
                .map(amount -> List.of(new AmountMatch(matcher.start(1), new Money(amount, null))))
                .orElseGet(List::of);
    }

    private static Optional<Money> mergeMoney(List<AmountMatch> matches) {
        Map<Currency, BigDecimal> amounts = new EnumMap<>(Currency.class);
        for (AmountMatch match : matches) {
            if (match.money().pesos() != null) {
                amounts.put(Currency.PESOS, match.money().pesos());
            }
            if (match.money().usd() != null) {
                amounts.put(Currency.USD, match.money().usd());
            }
        }
        if (amounts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Money(amounts.get(Currency.PESOS), amounts.get(Currency.USD)));
    }

    private static Optional<Money> parseMoney(String value, Currency defaultCurrency) {
        Optional<BigDecimal> amount = parseAmount(value);
        if (amount.isEmpty()) {
            return Optional.empty();
        }
        String normalized = fold(value).replace(" ", "");
        if (normalized.contains("usd") || normalized.contains("us$") || normalized.contains("u$s")) {
            return Optional.of(new Money(null, amount.get()));
        }
        if (normalized.contains("ars") || normalized.contains("$") || defaultCurrency == Currency.PESOS) {
            return Optional.of(new Money(amount.get(), null));
        }
        if (defaultCurrency == Currency.USD) {
            return Optional.of(new Money(null, amount.get()));
        }
        return Optional.empty();
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
        String source = value == null ? "" : value;
        return PLAN_Z.matcher(source).find() || PLAN_Z_BARE_MARKER.matcher(source).find();
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
                                         BigDecimal totalUsd, BigDecimal minimumPaymentPesos,
                                         List<ParsedTransaction> transactions) {
        List<String> warnings = new ArrayList<>();
        if (dueDate == null && closingDate == null && totalPesos == null && totalUsd == null
                && minimumPaymentPesos == null && transactions.isEmpty()) {
            warnings.add("Se detectó un formato compatible, pero no se extrajeron campos útiles del resumen ni consumos");
        }
        if (dueDate == null) {
            warnings.add("No se detectó la fecha de vencimiento");
        }
        if (closingDate == null) {
            warnings.add("No se detectó la fecha de cierre");
        }
        if (totalPesos == null && totalUsd == null) {
            warnings.add("No se detectaron los totales del resumen");
        }
        if (transactions.isEmpty()) {
            warnings.add("No se detectaron consumos confiables");
        }
        return warnings;
    }

    private record Money(BigDecimal pesos, BigDecimal usd) {
    }

    private record InstallmentMarker(Integer current, Integer total) {
    }

    private record AmountMatch(int start, Money money) {
    }

    private record OperationScopedRow(String operationNumber, String row) {
    }

    private enum DateRole {
        DUE,
        CLOSING,
        TRANSACTION
    }

    private enum Currency {
        PESOS,
        USD
    }
}
