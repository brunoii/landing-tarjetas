## Exploration: ocr-cli-runtime

### Current State
OCR is still implemented through `Tess4jTicketOcrEngine`, which wraps `ITesseract` and parses in-memory `BufferedImage` input. `TicketOcrService` keeps the upload boundary transient: it validates one PNG/JPEG, decodes in memory, classifies the engine result into `READY`, `INVALID_FILE`, `DECODE_FAILED`, `RUNTIME_UNAVAILABLE`, or `EMPTY_EXTRACTION`, and returns only response DTOs plus warnings.

There is no CLI execution seam yet. The current fallback is `DefaultTicketOcrEngine`, but the main runtime path is still Tess4J/JNA-based. Existing docs also describe Tess4J setup and `TESSDATA_PATH`, so they will be stale if the backend moves to the local `tesseract` binary.

### Affected Areas
- `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` — current OCR runtime adapter to replace.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrEngine.java` — stable seam to preserve for a new CLI-backed engine.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` — outcome classification and fallback handling must keep working.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/DefaultTicketOcrEngine.java` — existing fallback behavior to preserve or retire intentionally.
- `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` — adapter tests to replace with CLI/process tests.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — MVC outcome tests that should keep proving safe responses.
- `pom.xml` — Tess4J/JNA dependency removal or replacement.
- `README.md` — local OCR runtime setup and failure guidance.

### Approaches
1. **Swap the engine behind the existing seam** — implement a new CLI-backed `TicketOcrEngine` that writes a bounded temp image, runs `tesseract`, reads stdout, and maps non-zero exits/timeouts to sanitized warnings.
   - Pros: smallest surface change; preserves service/controller contracts.
   - Cons: temp-file lifecycle, quoting, and process handling are easy to get wrong.
   - Effort: Medium

2. **Add an explicit executor/runner abstraction** — keep the engine thin and inject a mockable command executor plus temp-file helper for process invocation, timeout, stderr redaction, and cleanup.
   - Pros: best testability; isolates security-sensitive process concerns.
   - Cons: more classes and wiring.
   - Effort: Medium

### Recommendation
Use the explicit executor/runner abstraction. It matches the requested secure-temp-file, timeout, stderr redaction, exit-code handling, and `finally` cleanup requirements, while keeping `TicketOcrService` and its outcome fallback intact.

### Risks
- Leaking temp images if cleanup is not guaranteed on timeout, failure, or parsing exceptions.
- Exposing path/content details through stderr or exception messages unless all process output is redacted before logging/response mapping.
- Misclassifying CLI failures as empty extractions instead of runtime unavailability, which would hide the real Ubuntu Leptonica issue.

### Ready for Proposal
Yes — the next step should define the CLI adapter contract, executor seam, timeout/cleanup rules, and doc/test updates for replacing Tess4J/JNA.
