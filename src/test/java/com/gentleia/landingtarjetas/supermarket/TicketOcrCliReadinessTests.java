package com.gentleia.landingtarjetas.supermarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TicketOcrCliReadinessTests {

    @Test
    void probesConfiguredExecutableAndLanguagesBeforeOcr() {
        CapturingRunner runner = new CapturingRunner(
                success("tesseract 5.4"),
                success("List of available languages (2):\nspa\neng"));

        TicketOcrRuntimeWarning result = readiness("/usr/bin/tesseract", "spa+eng", "/srv/tessdata", runner, files(true, true, true)).check();

        assertThat(result).isNull();
        assertThat(runner.commands()).containsExactly(
                List.of("/usr/bin/tesseract", "--version"),
                List.of("/usr/bin/tesseract", "--list-langs", "--tessdata-dir", "/srv/tessdata"));
    }

    @Test
    void mapsConfigurationAndProbeFailuresToFixedCategories() {
        assertThat(readiness("", "spa", "/data", runner(), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.EXECUTABLE_EMPTY);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(), files(false, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.EXECUTABLE_NOT_FOUND);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(), files(true, false, true)).check()).isEqualTo(TicketOcrRuntimeWarning.EXECUTABLE_NOT_EXECUTABLE);
        assertThat(readiness("/bin/tesseract", "spa", "", runner(), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.TESSDATA_PATH_EMPTY);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(), files(true, true, false)).check()).isEqualTo(TicketOcrRuntimeWarning.TESSDATA_PATH_NOT_FOUND);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(nonZero(), success("spa")), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.VERSION_CHECK_FAILED);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(timeout(), success("spa")), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.PROCESS_TIMEOUT);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(startFailure(), success("spa")), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.PROCESS_START_FAILED);
        assertThat(readiness("/bin/tesseract", "spa", "/data", runner(success("version"), nonZero()), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.LANGUAGES_NOT_AVAILABLE);
        assertThat(readiness("/bin/tesseract", "spa+eng", "/data", runner(success("version"), success("spa")), files(true, true, true)).check()).isEqualTo(TicketOcrRuntimeWarning.LANGUAGES_NOT_AVAILABLE);
    }

    @Test
    void truncatesAllowedProbeDiagnosticsWithoutAcceptingTicketContent() {
        String diagnostic = "x".repeat(1_001) + "PRIVATE OCR TEXT";

        assertThat(TicketOcrCliReadiness.truncate(diagnostic)).hasSize(1_000).doesNotContain("PRIVATE OCR TEXT");
        assertThat(TicketOcrRuntimeWarning.CLI_EXIT_NONZERO.publicWarning()).doesNotContain("PRIVATE OCR TEXT", "ticket.png", "/tmp/");
    }

    private TicketOcrCliReadiness readiness(String executable, String languages, String datapath, CapturingRunner runner, TicketOcrCliReadiness.FileSystemChecks files) {
        TicketOcrUploadProperties properties = new TicketOcrUploadProperties();
        properties.setExecutable(executable);
        properties.setLanguages(languages);
        properties.setDatapath(datapath);
        properties.setTimeout(Duration.ofSeconds(3));
        return new TicketOcrCliReadiness(properties, runner, files);
    }

    private CapturingRunner runner(TicketOcrProcessResult... results) {
        return new CapturingRunner(results);
    }

    private TicketOcrCliReadiness.FileSystemChecks files(boolean exists, boolean executable, boolean directory) {
        return new TicketOcrCliReadiness.FileSystemChecks() {
            @Override public boolean exists(String path) { return exists; }
            @Override public boolean executable(String path) { return executable; }
            @Override public boolean directory(String path) { return directory; }
        };
    }

    private TicketOcrProcessResult success(String stdout) { return new TicketOcrProcessResult(TicketOcrProcessResult.Status.SUCCESS, stdout, ""); }
    private TicketOcrProcessResult nonZero() { return new TicketOcrProcessResult(TicketOcrProcessResult.Status.NON_ZERO_EXIT, "", ""); }
    private TicketOcrProcessResult timeout() { return new TicketOcrProcessResult(TicketOcrProcessResult.Status.TIMED_OUT, "", ""); }
    private TicketOcrProcessResult startFailure() { return new TicketOcrProcessResult(TicketOcrProcessResult.Status.FAILED_TO_START, "", ""); }

    private static final class CapturingRunner implements TicketOcrProcessRunner {
        private final List<TicketOcrProcessResult> results;
        private final List<List<String>> commands = new ArrayList<>();

        private CapturingRunner(TicketOcrProcessResult... results) { this.results = List.of(results); }

        @Override public TicketOcrProcessResult run(List<String> command, Duration timeout) {
            commands.add(List.copyOf(command));
            return results.get(commands.size() - 1);
        }

        private List<List<String>> commands() { return commands; }
    }
}
