package com.gentleia.landingtarjetas.supermarket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ProcessBuilderTicketOcrProcessRunner implements TicketOcrProcessRunner {

    static final String SAFE_DIAGNOSTIC = "ticket-ocr-runtime-unavailable: Ticket OCR runtime is unavailable; review image manually";
    private static final int MAX_CAPTURE_BYTES = 4_096;

    private final ProcessStarter processStarter;

    public ProcessBuilderTicketOcrProcessRunner() {
        this(new SystemProcessStarter());
    }

    public ProcessBuilderTicketOcrProcessRunner(ProcessStarter processStarter) {
        this.processStarter = processStarter;
    }

    @Override
    public TicketOcrProcessResult run(List<String> command, Duration timeout) {
        try {
            RunningProcess process = processStarter.start(List.copyOf(command));
            if (!process.waitFor(timeout)) {
                process.destroy();
                process.destroyForcibly();
                return new TicketOcrProcessResult(TicketOcrProcessResult.Status.TIMED_OUT, "", SAFE_DIAGNOSTIC);
            }

            String stdout = readBounded(process.stdoutStream());
            String stderr = readBounded(process.stderrStream());
            int exitCode = process.exitCode();
            if (exitCode != 0) {
                return new TicketOcrProcessResult(TicketOcrProcessResult.Status.NON_ZERO_EXIT, "", stderr, exitCode, SAFE_DIAGNOSTIC);
            }
            return new TicketOcrProcessResult(TicketOcrProcessResult.Status.SUCCESS, stdout, stderr, exitCode, "");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new TicketOcrProcessResult(TicketOcrProcessResult.Status.INTERRUPTED, "", SAFE_DIAGNOSTIC);
        } catch (IOException | RuntimeException exception) {
            return new TicketOcrProcessResult(TicketOcrProcessResult.Status.FAILED_TO_START, "", SAFE_DIAGNOSTIC);
        }
    }

    private String readBounded(InputStream stream) throws IOException {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int remaining = MAX_CAPTURE_BYTES;
        while (remaining > 0) {
            int read = stream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read < 0) {
                break;
            }
            capture.write(buffer, 0, read);
            remaining -= read;
        }
        return capture.toString(StandardCharsets.UTF_8);
    }

    public interface ProcessStarter {
        RunningProcess start(List<String> command) throws IOException;
    }

    public interface RunningProcess {
        boolean waitFor(Duration timeout) throws InterruptedException;

        int exitCode();

        InputStream stdoutStream();

        InputStream stderrStream();

        void destroy();

        void destroyForcibly();
    }

    private static final class SystemProcessStarter implements ProcessStarter {

        @Override
        public RunningProcess start(List<String> command) throws IOException {
            Process process = new ProcessBuilder(command).start();
            return new SystemRunningProcess(process);
        }
    }

    private record SystemRunningProcess(Process process) implements RunningProcess {

        @Override
        public boolean waitFor(Duration timeout) throws InterruptedException {
            return process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        @Override
        public int exitCode() {
            return process.exitValue();
        }

        @Override
        public InputStream stdoutStream() {
            return process.getInputStream();
        }

        @Override
        public InputStream stderrStream() {
            return process.getErrorStream();
        }

        @Override
        public void destroy() {
            process.destroy();
        }

        @Override
        public void destroyForcibly() {
            process.destroyForcibly();
        }
    }
}
