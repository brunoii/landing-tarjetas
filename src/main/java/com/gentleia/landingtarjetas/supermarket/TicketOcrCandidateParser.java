package com.gentleia.landingtarjetas.supermarket;

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

import org.springframework.stereotype.Component;

@Component
public class TicketOcrCandidateParser {

    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = new BigDecimal("0.50");
    private static final BigDecimal DATE_CONFIDENCE = new BigDecimal("0.90");
    private static final BigDecimal SOURCE_CONFIDENCE = new BigDecimal("0.70");
    private static final BigDecimal LINE_CONFIDENCE = new BigDecimal("0.75");
    private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern ARGENTINE_DATE = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b");
    private static final Pattern PRICE_AT_END = Pattern.compile("^(?<description>.+?)\\s+\\$?(?<price>\\d{1,3}(?:[.]\\d{3})*(?:,\\d{2})?|\\d+(?:[.,]\\d{2})?)$");
    private static final List<String> NON_PRODUCT_PREFIXES = List.of("fecha", "total", "subtotal", "iva", "cuit", "ticket", "factura");

    public TicketOcrEngineResult parse(String rawText, BigDecimal ocrConfidence) {
        List<TicketOcrDateCandidateResponse> dateCandidates = new ArrayList<>();
        List<TicketOcrSourceCandidateResponse> sourceCandidates = new ArrayList<>();
        List<TicketOcrLineCandidateResponse> lineCandidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (ocrConfidence != null && ocrConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0) {
            warnings.add("Low OCR confidence; review candidates manually");
        }

        for (String line : safeLines(rawText)) {
            Optional<LocalDate> date = parseDate(line);
            if (date.isPresent()) {
                dateCandidates.add(new TicketOcrDateCandidateResponse(date.get(), DATE_CONFIDENCE, List.of()));
                continue;
            }
            if (sourceCandidates.isEmpty() && looksLikeSource(line)) {
                sourceCandidates.add(new TicketOcrSourceCandidateResponse(line, SOURCE_CONFIDENCE, List.of()));
                continue;
            }
            if (isIgnoredNonProductLine(line)) {
                continue;
            }
            lineCandidates.add(parseLine(line, ocrConfidence));
        }

        warnings.add("Review OCR output before saving");
        return new TicketOcrEngineResult(
                ocrConfidence,
                List.copyOf(dateCandidates),
                List.copyOf(sourceCandidates),
                List.copyOf(lineCandidates),
                List.copyOf(warnings)
        );
    }

    private List<String> safeLines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        return rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private Optional<LocalDate> parseDate(String line) {
        Matcher isoMatcher = ISO_DATE.matcher(line);
        if (isoMatcher.find()) {
            return parseDate(isoMatcher.group(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        Matcher argentineMatcher = ARGENTINE_DATE.matcher(line);
        if (argentineMatcher.find()) {
            return parseDate(argentineMatcher.group(), DateTimeFormatter.ofPattern("d/M/uuuu"));
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseDate(String value, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDate.parse(value, formatter));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private boolean looksLikeSource(String line) {
        String lowerLine = line.toLowerCase(Locale.ROOT);
        return !containsDigit(line) && (lowerLine.contains("super") || lowerLine.contains("mercado") || lowerLine.contains("almacen"));
    }

    private boolean containsDigit(String line) {
        return line.chars().anyMatch(Character::isDigit);
    }

    private boolean isIgnoredNonProductLine(String line) {
        String lowerLine = line.toLowerCase(Locale.ROOT);
        return NON_PRODUCT_PREFIXES.stream().anyMatch(lowerLine::startsWith);
    }

    private TicketOcrLineCandidateResponse parseLine(String line, BigDecimal ocrConfidence) {
        List<String> warnings = new ArrayList<>();
        if (ocrConfidence != null && ocrConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0) {
            warnings.add("Low OCR confidence; review this line manually");
        }
        Matcher matcher = PRICE_AT_END.matcher(line);
        if (!matcher.matches()) {
            warnings.add("Line could not be parsed into a price candidate");
            return new TicketOcrLineCandidateResponse(line, line, null, confidenceForLine(ocrConfidence), List.copyOf(warnings), null, null);
        }
        String description = matcher.group("description").trim();
        BigDecimal price = parsePrice(matcher.group("price"));
        return new TicketOcrLineCandidateResponse(line, description, price, confidenceForLine(ocrConfidence), List.copyOf(warnings), null, null);
    }

    private BigDecimal confidenceForLine(BigDecimal ocrConfidence) {
        return ocrConfidence == null ? LINE_CONFIDENCE : ocrConfidence;
    }

    private BigDecimal parsePrice(String price) {
        String normalized = price.contains(",")
                ? price.replace(".", "").replace(',', '.')
                : price.matches("\\d{1,3}(?:\\.\\d{3})+") ? price.replace(".", "") : price;
        return new BigDecimal(normalized);
    }
}
