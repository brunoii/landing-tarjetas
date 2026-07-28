# Tasks: OCR CLI Runtime

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 180-260 |
| 400-line budget risk | Low |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 process-runner seam/foundation → PR 2 CLI engine switch/temp artifact lifecycle → PR 3 dependency removal/docs/verification |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Low

## Phase 1: Process Runner Foundation

- [x] 1.1 Add a mockable `TicketOcrProcessRunner` / `TicketOcrProcessResult` seam that keeps OCR command invocation list-based and safe for deterministic tests.
- [x] 1.2 Add `ProcessBuilderTicketOcrProcessRunner` with timeout enforcement, bounded stream capture, forced process cleanup on timeout, and fixed redacted diagnostics.
- [x] 1.3 Add deterministic unit tests for success, non-zero exit redaction, launch failure redaction, and timeout cleanup without requiring a host `tesseract` binary.

## Phase 2: Deferred CLI Rollout

- [x] 2.1 Switch the primary OCR engine to a CLI-backed adapter that writes a generated temporary PNG and parses bounded stdout.
- [x] 2.2 Remove the legacy Tess4J/JNA adapter and dependency wiring after the CLI seam is proven.
- [x] 2.3 Update runtime configuration and operator docs for executable path, timeout, and troubleshooting.
