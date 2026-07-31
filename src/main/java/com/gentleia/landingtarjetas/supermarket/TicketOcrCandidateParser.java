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
    private static final Pattern BLOCK_HEADER = Pattern.compile("^(?<code>\\d{5,14})\\s+(?<description>.+)$");
    private static final Pattern BLOCK_PRICE = Pattern.compile("^(?<quantity>\\d+(?:[.,]\\d{1,3})?)\\s*[xX]\\s*\\$?(?<unit>\\d{1,3}(?:[.]\\d{3})*(?:,\\d{2})?|\\d+(?:[.,]\\d{2})?)\\s+\\$?(?<total>\\d{1,3}(?:[.]\\d{3})*(?:,\\d{2})?|\\d+(?:[.,]\\d{2})?)$");
    private static final Pattern TAX_LINE = Pattern.compile("^IVA\\s+\\$?(?<tax>\\d{1,3}(?:[.]\\d{3})*(?:,\\d{2})?|\\d+(?:[.,]\\d{2})?)$", Pattern.CASE_INSENSITIVE);
    private static final List<String> NON_PRODUCT_PREFIXES = List.of("fecha", "total", "subtotal", "iva", "cuit", "ticket", "factura");

    public TicketOcrEngineResult parse(String rawText, BigDecimal ocrConfidence) {
        List<TicketOcrDateCandidateResponse> dateCandidates = new ArrayList<>();
        List<TicketOcrSourceCandidateResponse> sourceCandidates = new ArrayList<>();
        List<TicketOcrLineCandidateResponse> lineCandidates = new ArrayList<>();
        List<TicketOcrDebugLineResponse> debugLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (ocrConfidence != null && ocrConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0) {
            warnings.add("Low OCR confidence; review candidates manually");
        }

        List<String> lines = safeLines(rawText);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Optional<LocalDate> date = parseDate(line);
            if (date.isPresent()) {
                dateCandidates.add(new TicketOcrDateCandidateResponse(date.get(), DATE_CONFIDENCE, List.of()));
                continue;
            }
            if (sourceCandidates.isEmpty() && looksLikeSource(line)) {
                sourceCandidates.add(new TicketOcrSourceCandidateResponse(line, SOURCE_CONFIDENCE, List.of()));
                continue;
            }
            ParsedBlock parsedBlock = parseSupportedBlock(lines, index, ocrConfidence);
            if (parsedBlock != null) {
                if (parsedBlock.candidate() != null) {
                    lineCandidates.add(parsedBlock.candidate());
                }
                debugLines.addAll(parsedBlock.debugLines());
                index += parsedBlock.consumedLines() - 1;
                continue;
            }
            if (isIgnoredNonProductLine(line) || isSeparator(line) || looksLikeNoise(line)) {
                debugLines.add(debugLine(line, "noise", "Ignored non-product OCR line"));
                continue;
            }
            Optional<TicketOcrLineCandidateResponse> parsedLineCandidate = parseLineCandidate(line, ocrConfidence);
            if (parsedLineCandidate.isPresent()) {
                lineCandidates.add(parsedLineCandidate.get());
                continue;
            }
            debugLines.add(debugLine(line, "noise", "Ignored non-reviewable OCR line"));
        }

        warnings.add("Review OCR output before saving");
        return new TicketOcrEngineResult(
                ocrConfidence,
                List.copyOf(dateCandidates),
                List.copyOf(sourceCandidates),
                List.copyOf(lineCandidates),
                List.copyOf(debugLines),
                List.copyOf(warnings)
        );
    }

    private List<String> safeLines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        return rawText.lines()
                .map(this::normalizeLine)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String normalizeLine(String line) {
        return line == null ? "" : line.trim().replaceAll("\\s+", " ");
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
        return !containsDigit(line) && (lowerLine.contains("super")
                || lowerLine.contains("mercado")
                || lowerLine.contains("almacen")
                || lowerLine.contains("vea")
                || lowerLine.contains("gomez pardo"));
    }

    private boolean containsDigit(String line) {
        return line.chars().anyMatch(Character::isDigit);
    }

    private boolean isIgnoredNonProductLine(String line) {
        String lowerLine = line.toLowerCase(Locale.ROOT);
        return NON_PRODUCT_PREFIXES.stream().anyMatch(lowerLine::startsWith);
    }

    private boolean isSeparator(String line) {
        return line.matches("^[\\-=_]{3,}$");
    }

    private boolean looksLikeNoise(String line) {
        return line.contains("###") || line.contains("***");
    }

    private ParsedBlock parseSupportedBlock(List<String> lines, int index, BigDecimal ocrConfidence) {
        String headerLine = lines.get(index);
        Matcher headerMatcher = BLOCK_HEADER.matcher(headerLine);
        if (!headerMatcher.matches()) {
            return null;
        }
        if (index + 1 >= lines.size()) {
            return new ParsedBlock(null, List.of(debugLine(headerLine, "incomplete-block", "Missing quantity/price line")), 1);
        }
        String priceLine = lines.get(index + 1);
        Matcher priceMatcher = BLOCK_PRICE.matcher(priceLine);
        if (!priceMatcher.matches()) {
            if (BLOCK_HEADER.matcher(priceLine).matches()) {
                return new ParsedBlock(
                        null,
                        List.of(debugLine(headerLine, "incomplete-block", "Missing quantity/price line")),
                        1
                );
            }
            return new ParsedBlock(
                    null,
                    List.of(
                            debugLine(headerLine, "incomplete-block", "Missing quantity/price line"),
                            debugLine(priceLine, "incomplete-block", "Unusable quantity/price line")
                    ),
                    2
            );
        }
        int consumedLines = 2;
        BigDecimal tax = null;
        String taxLine = null;
        if (index + 2 < lines.size()) {
            Matcher taxMatcher = TAX_LINE.matcher(lines.get(index + 2));
            if (taxMatcher.matches()) {
                taxLine = lines.get(index + 2);
                tax = parsePrice(taxMatcher.group("tax"));
                consumedLines = 3;
            }
        }
        List<String> candidateWarnings = new ArrayList<>();
        if (ocrConfidence != null && ocrConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0) {
            candidateWarnings.add("Low OCR confidence; review this line manually");
        }
        BigDecimal quantity = parseQuantity(priceMatcher.group("quantity"));
        BigDecimal unitPrice = parsePrice(priceMatcher.group("unit"));
        BigDecimal total = parsePrice(priceMatcher.group("total"));
        String description = headerMatcher.group("description").trim();
        String rawText = taxLine == null ? String.join("\n", headerLine, priceLine) : String.join("\n", headerLine, priceLine, taxLine);
        TicketOcrLineCandidateResponse candidate = new TicketOcrLineCandidateResponse(
                rawText,
                description,
                total,
                confidenceForLine(ocrConfidence),
                List.copyOf(candidateWarnings),
                headerMatcher.group("code"),
                quantity,
                unitPrice,
                total,
                tax,
                null,
                null
        );
        return new ParsedBlock(candidate, List.of(), consumedLines);
    }

    private Optional<TicketOcrLineCandidateResponse> parseLineCandidate(String line, BigDecimal ocrConfidence) {
        List<String> warnings = new ArrayList<>();
        if (ocrConfidence != null && ocrConfidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0) {
            warnings.add("Low OCR confidence; review this line manually");
        }
        Matcher matcher = PRICE_AT_END.matcher(line);
        if (!matcher.matches()) {
            if (!looksLikeReviewableTextCandidate(line)) {
                return Optional.empty();
            }
            warnings.add("Line could not be parsed into a price candidate");
            return Optional.of(new TicketOcrLineCandidateResponse(
                    line,
                    line,
                    null,
                    confidenceForLine(ocrConfidence),
                    List.copyOf(warnings),
                    null,
                    null
            ));
        }
        String description = matcher.group("description").trim();
        BigDecimal price = parsePrice(matcher.group("price"));
        return Optional.of(new TicketOcrLineCandidateResponse(
                line,
                description,
                price,
                confidenceForLine(ocrConfidence),
                List.copyOf(warnings),
                null,
                null
        ));
    }

    private boolean looksLikeReviewableTextCandidate(String line) {
        long letterCount = line.chars().filter(Character::isLetter).count();
        long digitCount = line.chars().filter(Character::isDigit).count();
        long suspiciousCharacterCount = line.chars()
                .filter(character -> !Character.isLetterOrDigit(character)
                        && !Character.isWhitespace(character)
                        && character != '$'
                        && character != ','
                        && character != '.')
                .count();
        long descriptiveTokenCount = java.util.Arrays.stream(line.split("\\s+"))
                .filter(token -> token.chars().anyMatch(Character::isLetter))
                .count();
        return letterCount >= 5
                && descriptiveTokenCount >= 2
                && suspiciousCharacterCount == 0
                && letterCount >= digitCount;
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

    private BigDecimal parseQuantity(String quantity) {
        return new BigDecimal(quantity.replace(',', '.'));
    }

    private TicketOcrDebugLineResponse debugLine(String normalizedText, String classification, String warning) {
        return new TicketOcrDebugLineResponse(normalizedText, classification, warning);
    }

    private record ParsedBlock(TicketOcrLineCandidateResponse candidate, List<TicketOcrDebugLineResponse> debugLines,
                               int consumedLines) {
    }
}
