package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.gentleia.landingtarjetas.supermarket.ProcessBuilderTicketOcrProcessRunner;
import com.gentleia.landingtarjetas.supermarket.TicketOcrProcessResult;

import org.junit.jupiter.api.Test;

class ProcessBuilderTicketOcrProcessRunnerTests {

    @Test
    void returnsCapturedStdoutWhenProcessCompletesSuccessfully() throws Exception {
        FakeProcessStarter starter = new FakeProcessStarter(new FakeRunningProcess(true, 0, "OCR READY", ""));
        ProcessBuilderTicketOcrProcessRunner runner = new ProcessBuilderTicketOcrProcessRunner(starter);

        TicketOcrProcessResult result = runner.run(List.of("tesseract", "generated-input.png", "stdout"), Duration.ofSeconds(5));

        assertThat(starter.lastCommand()).containsExactly("tesseract", "generated-input.png", "stdout");
        assertThat(result.status()).isEqualTo(TicketOcrProcessResult.Status.SUCCESS);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout()).isEqualTo("OCR READY");
        assertThat(result.diagnostic()).isEmpty();
    }

    @Test
    void returnsSafeDiagnosticWhenProcessExitsNonZeroWithoutLeakingSensitiveDetails() throws Exception {
        FakeProcessStarter starter = new FakeProcessStarter(new FakeRunningProcess(
                true,
                2,
                "PRIVATE OCR LINE 2500,50",
                "failed at C:/secret/runtime/ticket.png with text PRIVATE OCR LINE 2500,50"));
        ProcessBuilderTicketOcrProcessRunner runner = new ProcessBuilderTicketOcrProcessRunner(starter);

        TicketOcrProcessResult result = runner.run(List.of("tesseract", "generated-input.png", "stdout"), Duration.ofSeconds(5));

        assertThat(result.status()).isEqualTo(TicketOcrProcessResult.Status.NON_ZERO_EXIT);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("PRIVATE OCR LINE");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.diagnostic()).isEqualTo("ticket-ocr-runtime-unavailable: Ticket OCR runtime is unavailable; review image manually");
        assertThat(result.diagnostic()).doesNotContain("C:/secret", "PRIVATE OCR LINE", "ticket.png");
    }

    @Test
    void destroysTimedOutProcessesAndReturnsTimedOutStatus() throws Exception {
        FakeRunningProcess process = new FakeRunningProcess(false, 0, "", "");
        ProcessBuilderTicketOcrProcessRunner runner = new ProcessBuilderTicketOcrProcessRunner(new FakeProcessStarter(process));

        TicketOcrProcessResult result = runner.run(List.of("tesseract", "generated-input.png", "stdout"), Duration.ofSeconds(5));

        assertThat(result.status()).isEqualTo(TicketOcrProcessResult.Status.TIMED_OUT);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.diagnostic()).isEqualTo("ticket-ocr-runtime-unavailable: Ticket OCR runtime is unavailable; review image manually");
        assertThat(process.destroyCalled()).isTrue();
        assertThat(process.destroyForciblyCalled()).isTrue();
    }

    @Test
    void returnsSafeDiagnosticWhenProcessLaunchFails() throws Exception {
        ProcessBuilderTicketOcrProcessRunner runner = new ProcessBuilderTicketOcrProcessRunner(
                command -> {
                    throw new IOException("cannot launch C:/secret/bin/tesseract.exe");
                });

        TicketOcrProcessResult result = runner.run(List.of("tesseract", "generated-input.png", "stdout"), Duration.ofSeconds(5));

        assertThat(result.status()).isEqualTo(TicketOcrProcessResult.Status.FAILED_TO_START);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.diagnostic()).isEqualTo("ticket-ocr-runtime-unavailable: Ticket OCR runtime is unavailable; review image manually");
        assertThat(result.diagnostic()).doesNotContain("C:/secret", "tesseract.exe");
    }

    private record FakeProcessStarter(FakeRunningProcess process) implements ProcessBuilderTicketOcrProcessRunner.ProcessStarter {

        private static List<String> lastCommand;

        @Override
        public ProcessBuilderTicketOcrProcessRunner.RunningProcess start(List<String> command) {
            lastCommand = List.copyOf(command);
            return process;
        }

        private List<String> lastCommand() {
            return lastCommand;
        }
    }

    private static final class FakeRunningProcess implements ProcessBuilderTicketOcrProcessRunner.RunningProcess {

        private final boolean completed;
        private final int exitCode;
        private final byte[] stdout;
        private final byte[] stderr;
        private boolean destroyCalled;
        private boolean destroyForciblyCalled;

        private FakeRunningProcess(boolean completed, int exitCode, String stdout, String stderr) {
            this.completed = completed;
            this.exitCode = exitCode;
            this.stdout = stdout.getBytes(StandardCharsets.UTF_8);
            this.stderr = stderr.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public boolean waitFor(Duration timeout) {
            return completed;
        }

        @Override
        public int exitCode() {
            return exitCode;
        }

        @Override
        public ByteArrayInputStream stdoutStream() {
            return new ByteArrayInputStream(stdout);
        }

        @Override
        public ByteArrayInputStream stderrStream() {
            return new ByteArrayInputStream(stderr);
        }

        @Override
        public void destroy() {
            destroyCalled = true;
        }

        @Override
        public void destroyForcibly() {
            destroyForciblyCalled = true;
        }

        private boolean destroyCalled() {
            return destroyCalled;
        }

        private boolean destroyForciblyCalled() {
            return destroyForciblyCalled;
        }
    }
}
