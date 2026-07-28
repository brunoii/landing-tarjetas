# Apply Progress: OCR CLI Runtime

**Change**: `ocr-cli-runtime`
**Mode**: Strict TDD
**Artifact store**: hybrid
**Delivery slice**: PR 2 — CLI engine switch

## Completed Tasks

- [x] 1.1 Add a mockable `TicketOcrProcessRunner` / `TicketOcrProcessResult` seam that keeps OCR command invocation list-based and safe for deterministic tests.
- [x] 1.2 Add `ProcessBuilderTicketOcrProcessRunner` with timeout enforcement, bounded stream capture, forced process cleanup on timeout, and fixed redacted diagnostics.
- [x] 1.3 Add deterministic unit tests for success, non-zero exit redaction, launch failure redaction, and timeout cleanup without requiring a host `tesseract` binary.
- [x] 2.1 Switch the primary OCR engine to a CLI-backed adapter that writes a generated temporary PNG and parses bounded stdout.
- [x] 2.2 Remove the legacy Tess4J/JNA adapter and dependency wiring after the CLI seam is proven.
- [x] 2.3 Update runtime configuration and operator docs for executable path, timeout, and troubleshooting.

## Files Changed

| File | Action | What Was Done |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrProcessRunner.java` | Created | Added the CLI process execution seam for future OCR engine wiring. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrProcessResult.java` | Created | Added status + stdout + safe diagnostic modeling for deterministic runner outcomes. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/ProcessBuilderTicketOcrProcessRunner.java` | Created | Added list-based `ProcessBuilder` execution with timeout cleanup, bounded capture, and fixed redacted diagnostics. |
| `src/test/java/com/gentleia/landingtarjetas/ProcessBuilderTicketOcrProcessRunnerTests.java` | Created | Added deterministic unit tests for success, non-zero exit, launch failure, and timeout cleanup. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TesseractCliTicketOcrEngine.java` | Created | Switched the live OCR engine to a local `tesseract` CLI adapter with generated PNG temp-file lifecycle and existing outcome mapping. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrUploadProperties.java` | Modified | Added executable and timeout configuration for the CLI runtime while preserving language/datapath support. |
| `src/main/resources/application.properties` | Modified | Added environment-backed OCR executable and timeout properties. |
| `README.md` | Modified | Replaced Tess4J guidance with local CLI install, timeout, and sanitized troubleshooting guidance. |
| `pom.xml` | Modified | Removed Tess4J/JNA runtime dependency wiring. |
| `src/test/java/com/gentleia/landingtarjetas/supermarket/TesseractCliTicketOcrEngineTests.java` | Created | Added deterministic tests for generated PNG arguments, cleanup, timeout wiring, and runtime-unavailable mapping. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` | Deleted | Removed the legacy Tess4J adapter after the CLI engine replaced it. |
| `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` | Deleted | Removed the retired Tess4J-specific adapter tests. |
| `openspec/changes/ocr-cli-runtime/tasks.md` | Modified | Marked the PR2 CLI rollout tasks complete. |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/ProcessBuilderTicketOcrProcessRunnerTests.java` | Unit | N/A (new) | ✅ Added missing seam test before any production classes existed | ✅ `mvn -Dtest=ProcessBuilderTicketOcrProcessRunnerTests test` → 4/4 passing | ✅ Success + non-zero exit + launch failure + timeout paths | ✅ Introduced small nested process abstractions to keep tests deterministic |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/ProcessBuilderTicketOcrProcessRunnerTests.java` | Unit | N/A (new) | ✅ Timeout + redaction expectations written first | ✅ Same focused command → 4/4 passing | ✅ Distinct timeout cleanup and redaction branches covered | ✅ Capture/read helpers kept private and bounded |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/ProcessBuilderTicketOcrProcessRunnerTests.java` | Unit | N/A (new) | ✅ Deterministic fake-process cases written before implementation | ✅ Same focused command → 4/4 passing | ✅ Four deterministic runner scenarios, no host binary dependency | ➖ None needed beyond helper extraction |
| 2.1 | `src/test/java/com/gentleia/landingtarjetas/supermarket/TesseractCliTicketOcrEngineTests.java` | Unit | ✅ PR1 runner seam already passing | ✅ Added CLI-engine tests first; initial `mvn -Dtest=TesseractCliTicketOcrEngineTests test` failed at compile time because the CLI engine and new OCR properties did not exist | ✅ `mvn -Dtest=TesseractCliTicketOcrEngineTests test` → BUILD SUCCESS, 3 tests run, 0 failures, 0 errors, 0 skipped | ✅ Generated PNG path, discrete CLI args, timeout forwarding, and parsed stdout covered | ✅ Kept temp-file creation behind a tiny store seam for deterministic cleanup checks |
| 2.2 | `src/test/java/com/gentleia/landingtarjetas/supermarket/TesseractCliTicketOcrEngineTests.java` | Unit | ✅ PR1 runner seam already passing | ✅ The same RED compile failure proved the Tess4J runtime was still the active implementation path | ✅ `mvn -Dtest=TesseractCliTicketOcrEngineTests,SupermarketControllerTests#ticketOcrReturnsRuntimeUnavailableOutcomeWhenEngineReportsReadinessWarning test` → BUILD SUCCESS, 4 tests run, 0 failures, 0 errors, 0 skipped | ✅ CLI failure mapping stayed aligned with the existing controller/runtime outcome contract after removing Tess4J wiring | ➖ No extra refactor beyond deleting the retired Tess4J adapter/tests |
| 2.3 | `src/test/java/com/gentleia/landingtarjetas/supermarket/TesseractCliTicketOcrEngineTests.java` | Unit + integration | ✅ PR1 + PR2 focused tests already passing | ✅ Properties/docs changes were introduced only after the CLI test seam existed and the runtime contract was red first | ✅ `mvn test` → BUILD SUCCESS, 298 tests run, 0 failures, 0 errors, 0 skipped | ✅ Full suite proved the dependency removal and new config keys did not break application wiring | ➖ Documentation/config only; no further code refactor needed |

## Test Summary

- **Total tests written**: 7
- **Total tests passing**: 298 full-suite tests passing after the CLI rollout
- **Layers used**: Unit (7), Integration (1 targeted controller contract), E2E (0)
- **Approval tests**: None — deterministic unit + controller contract coverage only
- **Pure functions created**: 0

## Work Unit Evidence

| Evidence | Required value |
|---|---|
| Focused test command and exact result | `mvn -Dtest=ProcessBuilderTicketOcrProcessRunnerTests test` → BUILD SUCCESS, 4 tests run, 0 failures, 0 errors, 0 skipped |
| Runtime harness command/scenario and exact result | `mvn -Dtest=ProcessBuilderTicketOcrProcessRunnerTests test` exercising fake-process success, non-zero exit, launch failure, and timeout cleanup scenarios → BUILD SUCCESS, 4 tests run, 0 failures, 0 errors, 0 skipped |
| Rollback boundary | Revert only `TicketOcrProcessRunner`, `TicketOcrProcessResult`, `ProcessBuilderTicketOcrProcessRunner`, the focused runner test, and the `ocr-cli-runtime` task/apply artifacts; no current OCR engine wiring or dependencies changed |

### PR 2 — CLI engine switch

| Evidence | Required value |
|---|---|
| Focused test command and exact result | `mvn -Dtest=TesseractCliTicketOcrEngineTests test` → BUILD SUCCESS, 3 tests run, 0 failures, 0 errors, 0 skipped |
| Runtime harness command/scenario and exact result | `mvn -Dtest=TesseractCliTicketOcrEngineTests,SupermarketControllerTests#ticketOcrReturnsRuntimeUnavailableOutcomeWhenEngineReportsReadinessWarning test` → BUILD SUCCESS, 4 tests run, 0 failures, 0 errors, 0 skipped; confirmed the CLI warning prefix still maps to `RUNTIME_UNAVAILABLE` through the existing controller/service contract |
| Rollback boundary | Revert `TesseractCliTicketOcrEngine`, OCR property/config/doc changes, `pom.xml`, and the retired Tess4J adapter/tests to restore the previous native runtime path |

## Deviations from Design

None — implementation matches the design.

## Issues Found

- The local filesystem did not contain `openspec/changes/ocr-cli-runtime/*`; PR1 recreated the tasks/apply artifacts locally from the active change scope so hybrid persistence stays unblocked for this slice.
- The resulting PR2 slice stays under the requested 400-line addition budget, but total churn is higher if deletions are counted because removing Tess4J also retires the old adapter and its dedicated tests.

## Remaining Tasks

- [ ] Next recommended: PR 3 verification/docs follow-through only if maintainers still want a separate cleanup slice.

## Workload / PR Boundary

- Mode: stacked PR slice
- Current work unit: PR 2 — CLI engine switch
- Boundary: starts at the PR1 runner seam and ends with the live CLI adapter, temp PNG lifecycle, dependency removal, and runtime configuration/docs wiring
- Estimated review budget impact: additions stayed within the requested sub-400 slice budget; total churn is higher when counting retired Tess4J code deletions

## Status

6/6 tasks complete. Ready for verify.
