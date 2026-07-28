package com.gentleia.landingtarjetas.supermarket;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class TesseractCliTicketOcrEngine implements TicketOcrEngine {

    private static final String DEFAULT_EXECUTABLE = "tesseract";
    private static final String DEFAULT_LANGUAGES = "spa+eng";

    private final TicketOcrUploadProperties properties;
    private final TicketOcrCandidateParser parser;
    private final TicketOcrProcessRunner processRunner;
    private final TempFileStore tempFileStore;

    @Autowired
    public TesseractCliTicketOcrEngine(
            TicketOcrUploadProperties properties,
            TicketOcrCandidateParser parser,
            TicketOcrProcessRunner processRunner) {
        this(properties, parser, processRunner, new PngTempFileStore(null));
    }

    TesseractCliTicketOcrEngine(
            TicketOcrUploadProperties properties,
            TicketOcrCandidateParser parser,
            TicketOcrProcessRunner processRunner,
            TempFileStore tempFileStore) {
        this.properties = properties;
        this.parser = parser;
        this.processRunner = processRunner;
        this.tempFileStore = tempFileStore;
    }

    @Override
    public TicketOcrEngineResult extractCandidates(BufferedImage image) {
        Path tempFile = null;
        TicketOcrEngineResult result;
        boolean cleanupFailed = false;
        try {
            tempFile = tempFileStore.write(image);
            TicketOcrProcessResult processResult = processRunner.run(buildCommand(tempFile), timeout());
            result = processResult.succeeded()
                    ? parser.parse(processResult.stdout(), null)
                    : safeFailure(processResult.diagnostic());
        } catch (IOException | RuntimeException exception) {
            result = safeFailure(ProcessBuilderTicketOcrProcessRunner.SAFE_DIAGNOSTIC);
        } finally {
            if (tempFile != null) {
                try {
                    tempFileStore.delete(tempFile);
                } catch (IOException exception) {
                    cleanupFailed = true;
                }
            }
        }
        return cleanupFailed ? safeFailure(ProcessBuilderTicketOcrProcessRunner.SAFE_DIAGNOSTIC) : result;
    }

    private List<String> buildCommand(Path tempFile) {
        List<String> command = new ArrayList<>();
        command.add(normalizedExecutable());
        command.add(tempFile.toString());
        command.add("stdout");
        command.add("-l");
        command.add(normalizedLanguages());
        if (!isBlank(properties.getDatapath())) {
            command.add("--tessdata-dir");
            command.add(properties.getDatapath().trim());
        }
        return List.copyOf(command);
    }

    private Duration timeout() {
        Duration timeout = properties.getTimeout();
        return timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofSeconds(15) : timeout;
    }

    private String normalizedExecutable() {
        return isBlank(properties.getExecutable()) ? DEFAULT_EXECUTABLE : properties.getExecutable().trim();
    }

    private String normalizedLanguages() {
        return isBlank(properties.getLanguages()) ? DEFAULT_LANGUAGES : properties.getLanguages().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private TicketOcrEngineResult safeFailure(String warning) {
        return new TicketOcrEngineResult(null, List.of(), List.of(), List.of(), List.of(
                isBlank(warning) ? ProcessBuilderTicketOcrProcessRunner.SAFE_DIAGNOSTIC : warning.trim()));
    }

    interface TempFileStore {
        Path write(BufferedImage image) throws IOException;

        void delete(Path path) throws IOException;
    }

    static final class PngTempFileStore implements TempFileStore {

        private final Path directory;

        PngTempFileStore(Path directory) {
            this.directory = directory;
        }

        @Override
        public Path write(BufferedImage image) throws IOException {
            Path tempFile = directory == null
                    ? Files.createTempFile("ticket-ocr-", ".png")
                    : Files.createTempFile(directory, "ticket-ocr-", ".png");
            if (!ImageIO.write(image, "png", tempFile.toFile())) {
                Files.deleteIfExists(tempFile);
                throw new IOException("PNG writer unavailable");
            }
            return tempFile;
        }

        @Override
        public void delete(Path path) throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
