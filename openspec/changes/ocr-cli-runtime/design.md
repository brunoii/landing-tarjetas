# Design: OCR CLI Runtime

## Technical Approach

Replace the primary Tess4J adapter without changing `TicketOcrService`, `TicketOcrEngine`, controller, DTO, or candidate parser contracts. `TesseractCliTicketOcrEngine` will encode the already-decoded `BufferedImage` into one generated PNG, invoke local `tesseract` through an injected runner, parse bounded stdout, and translate every runner failure to the existing sanitized runtime-unavailable result.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Process boundary | Inject `TicketOcrProcessRunner` with a production `ProcessBuilder` implementation | Invoke `ProcessBuilder` in the engine | Makes command construction, timeout, exit handling, and redaction deterministic unit-test seams. |
| Temporary input | Generate a PNG with a fixed prefix/suffix in the JVM temp directory; delete in `finally` | Persist original upload; keep bytes only | The service already validates and decodes in memory. A canonical temporary image is the minimum CLI bridge and never uses the client filename. |
| Failure contract | Return one fixed `ticket-ocr-runtime-unavailable` warning for launch, timeout, non-zero exit, I/O, and cleanup failures | Surface stderr or distinguish every CLI failure | Preserves the safe warning-based engine contract and prevents paths, OCR content, or tool diagnostics leaking. |
| CLI configuration | Add executable and positive timeout properties; retain `languages` and optional `datapath` | Hard-code command/settings | Keeps operations configurable while command arguments remain discrete values, never shell text. |

## Data Flow

    Multipart upload -> TicketOcrService (validate/decode) -> TesseractCliTicketOcrEngine
                                                         -> temp PNG -> ProcessRunner
                                                         <- bounded stdout / safe status
                                                         -> TicketOcrCandidateParser -> existing response

The engine constructs an argument list, not a shell command: `[executable, tempPng, "stdout", "-l", languages]`, adding `--tessdata-dir`, datapath only when configured. It sets `redirectErrorStream(false)`, bounds captured streams, waits only for the configured timeout, then terminates a timed-out process and returns no stderr to callers or logs. It parses stdout only after exit code zero. `finally` deletes the generated file; deletion failure also produces the fixed safe failure.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/.../supermarket/Tess4jTicketOcrEngine.java` | Delete | Remove JNA/Tess4J adapter. |
| `src/main/java/.../supermarket/TesseractCliTicketOcrEngine.java` | Create | Primary CLI adapter, canonical temp PNG lifecycle, parsing and safe mapping. |
| `src/main/java/.../supermarket/TicketOcrProcessRunner.java` | Create | Mockable process execution contract and safe result/status model. |
| `src/main/java/.../supermarket/ProcessBuilderTicketOcrProcessRunner.java` | Create | List-based `ProcessBuilder` runner with timeout, forced termination, bounded capture, and redacted failures. |
| `src/main/java/.../supermarket/TicketOcrUploadProperties.java` | Modify | Add executable and timeout settings; remove Tess4J-specific datapath semantics. |
| `src/main/resources/application.properties` | Modify | Configure `tesseract` executable and bounded timeout via environment-backed properties. |
| `pom.xml` | Modify | Remove `net.sourceforge.tess4j:tess4j` and transitively unused JNA runtime. |
| `README.md` | Modify | Replace Tess4J setup with host `tesseract` prerequisite, configuration, timeout, and safe troubleshooting guidance. |
| `src/test/java/.../Tess4jTicketOcrEngineTests.java` | Delete | Retire native-adapter tests. |
| `src/test/java/.../TesseractCliTicketOcrEngineTests.java` | Create | Test command construction, parser success, cleanup, and safe failure mapping through the runner seam. |
| `src/test/java/.../ProcessBuilderTicketOcrProcessRunnerTests.java` | Create | Test process timeout, bounded/redacted diagnostics, and exit handling without a host Tesseract dependency. |

## Interfaces / Contracts

```java
interface TicketOcrProcessRunner {
    TicketOcrProcessResult run(List<String> arguments, Duration timeout);
}
record TicketOcrProcessResult(boolean succeeded, String stdout) {}
```

`stdout` is available only on successful, zero-exit completion and is bounded. `TicketOcrUploadProperties` adds `executable` (default `tesseract`) and `timeout` (positive, default documented in configuration); languages remain the existing operator value. No API response or HTTP status contract changes.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Exact discrete argument list, no client filename, successful parse | Fake runner and temp-directory fixture. |
| Unit | Timeout, launch/non-zero/I/O failures, stderr/path/OCR-content redaction | Fake runner results; assert fixed warning and empty candidates. |
| Unit | Cleanup after success, failure, timeout, parser exception | Inject temp-file seam; assert file absence in every branch. |
| Integration | Existing multipart validation and transient response behavior | Retain `SupermarketControllerTests` with mocked `TicketOcrEngine`. |

## Threat Matrix

| Boundary | Applicability | Design response | Planned RED tests |
|---|---|---|---|
| Documentation-like paths | N/A — no executable-file classification | N/A | N/A |
| Git repository selection | N/A — no VCS integration | N/A | N/A |
| Commit state | N/A — no VCS integration | N/A | N/A |
| Push state | N/A — no VCS integration | N/A | N/A |
| PR commands | N/A — no PR automation | N/A | N/A |
| OCR subprocess arguments | Applicable — local process integration | List-only arguments; generated path; no shell; fixed safe failure | Verify argument tokens, generated path, no filename, timeout/exit redaction, and cleanup RED tests. |

## Migration / Rollout

No data migration. Operators install a compatible local `tesseract` binary, configure executable/timeout/languages (and optional tessdata directory), then restart. Roll back by restoring the Tess4J adapter/dependency and prior docs.

## Open Questions

- [ ] Confirm the production service account's `tesseract` binary path and suitable timeout before rollout.
