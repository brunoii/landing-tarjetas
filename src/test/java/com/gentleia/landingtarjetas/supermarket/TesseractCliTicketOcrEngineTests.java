package com.gentleia.landingtarjetas.supermarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class TesseractCliTicketOcrEngineTests {

    @Test
    void writesGeneratedPngInvokesCliAndParsesStdout() throws Exception {
        Path tempDirectory = Files.createTempDirectory("ticket-ocr-cli-engine");
        CapturingRunner runner = new CapturingRunner(new TicketOcrProcessResult(
                TicketOcrProcessResult.Status.SUCCESS,
                "Supermercado Central\nFecha 21/07/2026\nYERBA 1KG 2500,50",
                ""));
        TesseractCliTicketOcrEngine engine = new TesseractCliTicketOcrEngine(
                properties("custom-tesseract", "spa+eng", "C:/ocr/tessdata", Duration.ofSeconds(7)),
                new TicketOcrCandidateParser(),
                runner,
                new TesseractCliTicketOcrEngine.PngTempFileStore(tempDirectory));

        TicketOcrEngineResult result = engine.extractCandidates(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB));

        assertThat(runner.timeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(runner.command()).containsExactly(
                "custom-tesseract",
                runner.command().get(1),
                "stdout",
                "-l",
                "spa+eng",
                "--tessdata-dir",
                "C:/ocr/tessdata");
        assertThat(Path.of(runner.command().get(1)).getFileName().toString()).startsWith("ticket-ocr-").endsWith(".png");
        assertThat(runner.inputExistedDuringRun()).isTrue();
        assertThat(runner.inputStartedAsPng()).isTrue();
        assertThat(tempDirectory).isEmptyDirectory();
        assertThat(result.sourceCandidates()).singleElement().satisfies(candidate -> assertThat(candidate.label()).isEqualTo("Supermercado Central"));
        assertThat(result.dateCandidates()).singleElement().satisfies(candidate -> assertThat(candidate.value().toString()).isEqualTo("2026-07-21"));
        assertThat(result.lineCandidates()).singleElement().satisfies(candidate -> assertThat(candidate.pricePesos()).isEqualByComparingTo("2500.50"));
    }

    @Test
    void returnsRuntimeUnavailableAndCleansUpTempPngWhenCliFails() throws Exception {
        Path tempDirectory = Files.createTempDirectory("ticket-ocr-cli-engine");
        TesseractCliTicketOcrEngine engine = new TesseractCliTicketOcrEngine(
                properties("tesseract", "spa+eng", "", Duration.ofSeconds(5)),
                new TicketOcrCandidateParser(),
                new CapturingRunner(new TicketOcrProcessResult(
                        TicketOcrProcessResult.Status.NON_ZERO_EXIT,
                        "",
                        ProcessBuilderTicketOcrProcessRunner.SAFE_DIAGNOSTIC)),
                new TesseractCliTicketOcrEngine.PngTempFileStore(tempDirectory));

        TicketOcrEngineResult result = engine.extractCandidates(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        assertThat(result.warnings()).containsExactly(ProcessBuilderTicketOcrProcessRunner.SAFE_DIAGNOSTIC);
        assertThat(result.sourceCandidates()).isEmpty();
        assertThat(result.dateCandidates()).isEmpty();
        assertThat(result.lineCandidates()).isEmpty();
        assertThat(tempDirectory).isEmptyDirectory();
    }

    @Test
    void returnsRuntimeUnavailableWhenCleanupFailsAfterSuccessfulCliRun() {
        TesseractCliTicketOcrEngine engine = new TesseractCliTicketOcrEngine(
                properties("tesseract", "spa+eng", "", Duration.ofSeconds(5)),
                new TicketOcrCandidateParser(),
                new CapturingRunner(new TicketOcrProcessResult(
                        TicketOcrProcessResult.Status.SUCCESS,
                        "Supermercado Central",
                        "")),
                new FailingDeleteTempFileStore());

        TicketOcrEngineResult result = engine.extractCandidates(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        assertThat(result.warnings()).containsExactly(ProcessBuilderTicketOcrProcessRunner.SAFE_DIAGNOSTIC);
        assertThat(result.sourceCandidates()).isEmpty();
        assertThat(result.dateCandidates()).isEmpty();
        assertThat(result.lineCandidates()).isEmpty();
    }

    private TicketOcrUploadProperties properties(String executable, String languages, String datapath, Duration timeout) {
        TicketOcrUploadProperties properties = new TicketOcrUploadProperties();
        properties.setExecutable(executable);
        properties.setLanguages(languages);
        properties.setDatapath(datapath);
        properties.setTimeout(timeout);
        return properties;
    }

    private static final class CapturingRunner implements TicketOcrProcessRunner {

        private final TicketOcrProcessResult result;
        private List<String> command = List.of();
        private Duration timeout = Duration.ZERO;
        private boolean inputExistedDuringRun;
        private boolean inputStartedAsPng;

        private CapturingRunner(TicketOcrProcessResult result) {
            this.result = result;
        }

        @Override
        public TicketOcrProcessResult run(List<String> command, Duration timeout) {
            this.command = List.copyOf(command);
            this.timeout = timeout;
            Path input = Path.of(command.get(1));
            inputExistedDuringRun = Files.exists(input);
            try {
                byte[] bytes = Files.readAllBytes(input);
                inputStartedAsPng = bytes.length >= 8
                        && bytes[0] == (byte) 0x89
                        && bytes[1] == 0x50
                        && bytes[2] == 0x4e
                        && bytes[3] == 0x47;
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            return result;
        }

        private List<String> command() {
            return command;
        }

        private Duration timeout() {
            return timeout;
        }

        private boolean inputExistedDuringRun() {
            return inputExistedDuringRun;
        }

        private boolean inputStartedAsPng() {
            return inputStartedAsPng;
        }
    }

    private static final class FailingDeleteTempFileStore implements TesseractCliTicketOcrEngine.TempFileStore {

        @Override
        public Path write(BufferedImage image) throws IOException {
            return Files.createTempFile("ticket-ocr-", ".png");
        }

        @Override
        public void delete(Path path) throws IOException {
            throw new IOException("cannot delete temp file");
        }
    }
}
