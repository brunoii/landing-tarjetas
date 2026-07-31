package com.gentleia.landingtarjetas.supermarket;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketOcrService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg");
    private static final List<String> ALLOWED_FORMAT_NAMES = List.of("png", "jpeg");
    private static final String RUNTIME_WARNING_PREFIX = "ticket-ocr-runtime-";

    private final TicketOcrUploadProperties properties;
    private final TicketOcrEngine ticketOcrEngine;

    public TicketOcrService(TicketOcrUploadProperties properties, TicketOcrEngine ticketOcrEngine) {
        this.properties = properties;
        this.ticketOcrEngine = ticketOcrEngine;
    }

    public TicketOcrResponse extractCandidates(MultipartFile[] files) {
        MultipartFile file = requireExactlyOneFile(files);
        validateFile(file);
        byte[] bytes = readBytes(file);
        BufferedImage image = decodeImage(bytes);
        TicketOcrEngineResult result = ticketOcrEngine.extractCandidates(image);
        TicketOcrOutcome outcome = classifyOutcome(result);
        return new TicketOcrResponse(
                outcome,
                sha256(bytes),
                safeFilename(file),
                file.getContentType(),
                file.getSize(),
                result.ocrConfidence(),
                safeList(result.dateCandidates()),
                safeList(result.sourceCandidates()),
                safeList(result.lineCandidates()),
                safeList(result.debugLines()),
                safeList(result.warnings())
        );
    }

    private MultipartFile requireExactlyOneFile(MultipartFile[] files) {
        if (files == null || files.length != 1) {
            throw invalidFile("Upload exactly one ticket image");
        }
        return files[0];
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("Upload exactly one ticket image");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw invalidFile("Ticket image exceeds the allowed size limit");
        }
        if (file.getContentType() != null && !hasAllowedContentType(file)) {
            throw invalidFile("Only PNG or JPEG ticket images are accepted");
        }
        if (file.getContentType() == null && !hasAllowedExtension(file)) {
            throw invalidFile("Only PNG or JPEG ticket images are accepted");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw decodeFailure("Ticket image could not be read");
        }
    }

    private BufferedImage decodeImage(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw decodeFailure("Ticket image could not be decoded");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw decodeFailure("Ticket image could not be decoded");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                validateDecodedFormat(reader);
                validateDecodedDimensions(reader.getWidth(0), reader.getHeight(0));
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw decodeFailure("Ticket image could not be decoded");
        }
    }

    private void validateDecodedFormat(ImageReader reader) throws IOException {
        String format = reader.getFormatName();
        if (format == null || !ALLOWED_FORMAT_NAMES.contains(format.trim().toLowerCase(Locale.ROOT))) {
            throw invalidFile("Only PNG or JPEG ticket images are accepted");
        }
    }

    private void validateDecodedDimensions(int width, int height) {
        if (width > properties.getMaxDecodedDimension() || height > properties.getMaxDecodedDimension()) {
            throw invalidFile("Ticket image dimensions exceed the allowed limit");
        }
    }

    private TicketOcrOutcome classifyOutcome(TicketOcrEngineResult result) {
        if (hasRuntimeWarning(result.warnings())) {
            return TicketOcrOutcome.RUNTIME_UNAVAILABLE;
        }
        if (hasUsableCandidates(result)) {
            return TicketOcrOutcome.READY;
        }
        return TicketOcrOutcome.EMPTY_EXTRACTION;
    }

    private boolean hasRuntimeWarning(List<String> warnings) {
        return warnings != null && warnings.stream()
                .filter(warning -> warning != null)
                .map(String::trim)
                .anyMatch(warning -> warning.startsWith(RUNTIME_WARNING_PREFIX));
    }

    private boolean hasUsableCandidates(TicketOcrEngineResult result) {
        return !safeList(result.lineCandidates()).isEmpty()
                || !safeList(result.dateCandidates()).isEmpty()
                || !safeList(result.sourceCandidates()).isEmpty();
    }

    private TicketOcrException invalidFile(String message) {
        return new TicketOcrException(TicketOcrOutcome.INVALID_FILE, message);
    }

    private TicketOcrException decodeFailure(String message) {
        return new TicketOcrException(TicketOcrOutcome.DECODE_FAILED, message);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean hasAllowedContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT));
    }

    private boolean hasAllowedExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
    }

    private String safeFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename == null ? "ticket-image" : filename;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
