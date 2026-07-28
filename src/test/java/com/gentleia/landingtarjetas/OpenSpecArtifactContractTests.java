package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OpenSpecArtifactContractTests {

    private static final Path OPENSPEC_ROOT = Path.of("openspec/specs");

    @Test
    void superInventorySpecCapturesScannerLifecycleAndDuplicateScanContracts() throws IOException {
        String spec = Files.readString(OPENSPEC_ROOT.resolve("super-inventory/spec.md"), StandardCharsets.UTF_8);

        assertThat(spec)
                .contains("debe reconocer si corre en contexto seguro")
                .contains("mensajes de readiness")
                .contains("sin filtrar listeners duplicados ni streams de media")
                .contains("MUST suprimir lecturas duplicadas del mismo barcode en vivo hasta que cambie el estado del escaneo o el usuario reinicie el flujo");
    }

    @Test
    void privacySafePwaShellSpecCapturesAllowlistAndNavigationOnlyOfflineFallback() throws IOException {
        String spec = Files.readString(OPENSPEC_ROOT.resolve("privacy-safe-pwa-shell/spec.md"), StandardCharsets.UTF_8);

        assertThat(spec)
                .contains("same-origin GET")
                .contains("exact public shell URLs")
                .contains("unmatched URLs are network-only")
                .contains("offline fallback page only for navigation");
    }
}
