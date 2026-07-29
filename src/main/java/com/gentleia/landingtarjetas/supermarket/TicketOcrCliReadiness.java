package com.gentleia.landingtarjetas.supermarket;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class TicketOcrCliReadiness {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketOcrCliReadiness.class);
    private static final int MAX_DIAGNOSTIC_CHARS = 1_000;

    private final TicketOcrUploadProperties properties;
    private final TicketOcrProcessRunner processRunner;
    private final FileSystemChecks fileSystemChecks;

    @Autowired
    TicketOcrCliReadiness(TicketOcrUploadProperties properties, TicketOcrProcessRunner processRunner) {
        this(properties, processRunner, new SystemFileSystemChecks());
    }

    TicketOcrCliReadiness(
            TicketOcrUploadProperties properties,
            TicketOcrProcessRunner processRunner,
            FileSystemChecks fileSystemChecks) {
        this.properties = properties;
        this.processRunner = processRunner;
        this.fileSystemChecks = fileSystemChecks;
    }

    TicketOcrRuntimeWarning check() {
        String executable = properties.getExecutable();
        String languages = properties.getLanguages();
        String datapath = properties.getDatapath();
        Duration timeout = effectiveTimeout();
        LOGGER.info("ticket_ocr_cli_config executable={} languages={} datapath={} timeout={}", executable, languages, datapath, timeout);
        if (isBlank(executable)) {
            return warn(TicketOcrRuntimeWarning.EXECUTABLE_EMPTY);
        }
        try {
            if (!fileSystemChecks.exists(executable.trim())) {
                return warn(TicketOcrRuntimeWarning.EXECUTABLE_NOT_FOUND);
            }
            if (!fileSystemChecks.executable(executable.trim())) {
                return warn(TicketOcrRuntimeWarning.EXECUTABLE_NOT_EXECUTABLE);
            }
        } catch (RuntimeException exception) {
            return warn(TicketOcrRuntimeWarning.EXECUTABLE_NOT_FOUND);
        }
        if (isBlank(datapath)) {
            return warn(TicketOcrRuntimeWarning.TESSDATA_PATH_EMPTY);
        }
        try {
            if (!fileSystemChecks.directory(datapath.trim())) {
                return warn(TicketOcrRuntimeWarning.TESSDATA_PATH_NOT_FOUND);
            }
        } catch (RuntimeException exception) {
            return warn(TicketOcrRuntimeWarning.TESSDATA_PATH_NOT_FOUND);
        }

        TicketOcrProcessResult version = probe(List.of(executable.trim(), "--version"), timeout, "version");
        if (!version.succeeded()) {
            return warn(probeFailure(version, TicketOcrRuntimeWarning.VERSION_CHECK_FAILED));
        }

        TicketOcrProcessResult languageResult = probe(
                List.of(executable.trim(), "--list-langs", "--tessdata-dir", datapath.trim()), timeout, "languages");
        if (!languageResult.succeeded()) {
            return warn(probeFailure(languageResult, TicketOcrRuntimeWarning.LANGUAGES_NOT_AVAILABLE));
        }
        Set<String> availableLanguages = languageResult.stdout().lines().map(String::trim).collect(Collectors.toSet());
        boolean allLanguagesAvailable = !isBlank(languages) && List.of(languages.trim().split("\\+")).stream()
                .allMatch(availableLanguages::contains);
        return allLanguagesAvailable ? null : warn(TicketOcrRuntimeWarning.LANGUAGES_NOT_AVAILABLE);
    }

    private TicketOcrProcessResult probe(List<String> command, Duration timeout, String probe) {
        TicketOcrProcessResult result;
        try {
            result = processRunner.run(command, timeout);
        } catch (RuntimeException exception) {
            result = new TicketOcrProcessResult(TicketOcrProcessResult.Status.FAILED_TO_START, "", "");
        }
        LOGGER.info("ticket_ocr_cli_probe probe={} command={} exitCode={} stdout={} stderr={}",
                probe, command, result.exitCode(), truncate(result.stdout()), truncate(result.stderr()));
        return result;
    }

    private TicketOcrRuntimeWarning warn(TicketOcrRuntimeWarning warning) {
        LOGGER.warn("ticket_ocr_cli_unavailable cause={}", warning.category());
        return warning;
    }

    private TicketOcrRuntimeWarning probeFailure(TicketOcrProcessResult result, TicketOcrRuntimeWarning nonZeroWarning) {
        return result.status() == TicketOcrProcessResult.Status.NON_ZERO_EXIT
                ? nonZeroWarning
                : TicketOcrRuntimeWarning.fromProcessStatus(result.status());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Duration effectiveTimeout() {
        Duration timeout = properties.getTimeout();
        return timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofSeconds(15) : timeout;
    }

    static String truncate(String value) {
        if (value == null || value.length() <= MAX_DIAGNOSTIC_CHARS) {
            return value == null ? "" : value;
        }
        return value.substring(0, MAX_DIAGNOSTIC_CHARS);
    }

    interface FileSystemChecks {
        boolean exists(String path);

        boolean executable(String path);

        boolean directory(String path);
    }

    private static final class SystemFileSystemChecks implements FileSystemChecks {

        @Override
        public boolean exists(String path) {
            return Files.exists(Path.of(path));
        }

        @Override
        public boolean executable(String path) {
            return Files.isExecutable(Path.of(path));
        }

        @Override
        public boolean directory(String path) {
            return Files.isDirectory(Path.of(path));
        }
    }
}
