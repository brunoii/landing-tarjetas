# Apply Progress: Supermarket Ticket Mobile Navigation

**Change**: `supermarket-ticket-mobile-navigation`
**Mode**: Strict TDD
**Artifact store**: hybrid
**Delivery slice**: PR 1 — OCR readiness/local-only verification

## Completed Tasks

- [x] 1.1 Add RED specs in `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md` for invalid type, decode failure, missing runtime/data, and empty OCR extraction.
- [x] 1.2 Add RED spec coverage in `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md` for local-only verification from `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` with no copy/move/upload/cache/persist.
- [x] 3.1 Write RED contract cases for stage-specific OCR feedback and local JPG-only verification in `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md`.
- [x] 3.3 Verify the OCR path with representative JPGs from `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` outside the repo; confirm the UI reports the expected OCR stage instead of a generic failure.

## Files Changed

| File | Action | What Was Done |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrOutcome.java` | Created | Added stable OCR outcomes for ready/invalid/decode/runtime/empty states. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrException.java` | Created | Added outcome-aware OCR exception for safe 400 envelopes. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modified | Classified OCR responses into explicit outcomes without persisting JPG data. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrResponse.java` | Modified | Added `outcome` to transient OCR payloads. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiErrorResponse.java` | Modified | Added optional `outcome` field for OCR-specific error envelopes. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified | Returned explicit OCR outcomes for safe bad-request responses. |
| `src/main/resources/static/js/supermarket.js` | Modified | Mapped OCR outcomes to non-generic retry/manual-review guidance. |
| `src/main/resources/static/index.html` | Modified | Added local-only JPG verification guidance without exposing file paths. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified | Added RED→GREEN API coverage for invalid, decode, runtime, and empty outcomes. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Added UI contract checks for OCR outcome mapping and local-only guidance. |
| `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md` | Modified | Expanded PR1-ready OCR readiness and local verification scenarios. |
| `openspec/changes/supermarket-ticket-mobile-navigation/tasks.md` | Modified | Marked PR1 OCR readiness/local verification tasks complete. |
| `README.md` | Modified | Documented portable local OCR runtime setup, the exact `tessdata` path requirement, and safe language overrides. |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests,Tess4jTicketOcrEngineTests` → 135/135 | ✅ Added failing `INVALID_FILE` / `DECODE_FAILED` outcome assertions first | ✅ `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` → 133/133 | ✅ Invalid type + undecodable image + runtime + empty paths | ✅ Outcome classification extracted into service helpers |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ Same safety net above | ✅ Added failing local-only JPG guidance assertion first | ✅ `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` → 133/133 | ✅ Guidance copy + no storage regression assertions | ✅ Reused outcome message helpers instead of inline branching |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Integration + Static UI contract | ✅ Same safety net above | ✅ Added failing stage-specific outcome coverage before code | ✅ `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` → 133/133 | ✅ Covered invalid/decode/runtime/empty plus UI outcome mapping | ✅ None needed beyond helper extraction |
| 3.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` + local runtime harness | Integration + Runtime harness | ✅ Same safety net above | ✅ Stage-specific contract tests were written before runtime verification | ✅ Local JPG runtime check returned `HTTP=200 Outcome=RUNTIME_UNAVAILABLE Lines=0 Dates=0 Sources=0 Warnings=1` | ✅ Automated contract tests + representative local JPG verification | ➖ Runtime evidence only |

## Test Summary

- **Total tests written**: 6
- **Total tests passing**: 133 focused tests passing
- **Layers used**: Unit (0), Integration (5), E2E (0), Static UI contract (1)
- **Approval tests**: None — behavior change slice
- **Pure functions created**: 3 (`classifyOutcome`, `hasRuntimeWarning`, `hasUsableCandidates`)

## Work Unit Evidence

| Evidence | Required value |
|---|---|
| Focused test command and exact result | `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` → BUILD SUCCESS, 133 tests run, 0 failures, 0 errors, 0 skipped |
| Runtime harness command/scenario and exact result | Started local app with security disabled in temp workspace, posted one authorized local JPG from the approved folder, and observed `HTTP=200 Outcome=RUNTIME_UNAVAILABLE Lines=0 Dates=0 Sources=0 Warnings=1` without printing filename, OCR text, or persisting the response |
| Rollback boundary | Revert OCR outcome contract files (`TicketOcrOutcome`, `TicketOcrException`, `TicketOcrService`, `TicketOcrResponse`, `ApiErrorResponse`, `ApiExceptionHandler`) plus OCR UI copy/mapping (`index.html`, `supermarket.js`) and the PR1 tests/spec/task updates |

## Deviations from Design

None — runtime readiness uses HTTP 200 with `RUNTIME_UNAVAILABLE`, matching the design open question resolution while keeping sanitized warnings.

## Issues Found

- The original local-only spec text forbids “upload”, but the requested local JPG verification necessarily exercises the local OCR endpoint. This batch preserved the no-copy/no-persist/no-log/no-version contract and recorded only stage/count evidence.

## PR1 Runtime Remediation

- Diagnosed that no system `tesseract.exe` is installed or required for this workspace; Tess4J loads its bundled Windows native DLLs successfully.
- Diagnosed that the local failure was configuration, not missing binaries: `TESSDATA_PATH` must point to the concrete `tessdata` directory.
- Confirmed the bundled Tess4J dependency includes `eng.traineddata` and `osd.traineddata` only. `spa.traineddata` remains an external prerequisite if the runtime keeps `spa+eng`.
- Provisioned a safe local runtime outside the repository under `C:\Users\BIIbr\AppData\Local\Temp\opencode\tess4j-local-runtime\tessdata` using bundled non-ticket OCR assets only.
- Verified local OCR against the authorized JPG directory with `TESSDATA_PATH` pointing to that external `tessdata` directory and `APP_SUPER_TICKET_OCR_UPLOAD_LANGUAGES=eng`, recording only aggregate outcome counts.

## PR1 Spanish OCR Runtime Follow-up

- Confirmed the external runtime `C:\Users\BIIbr\AppData\Local\Temp\opencode\tess4j-local-runtime\tessdata` existed but lacked `spa.traineddata`.
- Installed `spa.traineddata` from the official free Tesseract language-data source `https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/spa.traineddata` into that external `tessdata` directory only.
- Re-verified the local OCR runtime with the exact `tessdata` directory plus the default multilingual setting `APP_SUPER_TICKET_OCR_UPLOAD_LANGUAGES=spa+eng`.
- Ran local-only OCR verification against the three authorized JPGs already present in the active temp runtime and reported aggregate counts only; no ticket text, filenames, or extracted payloads were persisted or exposed.

### Remediation Evidence

| Evidence | Result |
|---|---|
| Focused regression command | `mvn test -Dtest=Tess4jTicketOcrEngineTests` → BUILD SUCCESS, 5 tests run, 0 failures, 0 errors, 0 skipped |
| Runtime diagnosis | `where.exe tesseract` → not found; Tess4J `5.16.0` jar contains `win32-x86-64/libtesseract551.dll`, `tessdata/eng.traineddata`, and `tessdata/osd.traineddata`; no bundled `spa.traineddata` |
| Runtime harness (misconfigured path) | `TESSDATA_PATH=C:\Users\BIIbr\AppData\Local\Temp\opencode\tess4j-local-runtime` + `APP_SUPER_TICKET_OCR_UPLOAD_LANGUAGES=eng` → 3/3 `RUNTIME_UNAVAILABLE` with sanitized `ticket-ocr-runtime-misconfigured` warning |
| Runtime harness (ready path) | `TESSDATA_PATH=C:\Users\BIIbr\AppData\Local\Temp\opencode\tess4j-local-runtime\tessdata` + `APP_SUPER_TICKET_OCR_UPLOAD_LANGUAGES=eng` → 3 verified files, 3/3 `READY`, 0 runtime warnings reported |
| Runtime provisioning (Spanish data) | External temp `tessdata` updated with official `spa.traineddata` from `tesseract-ocr/tessdata_fast`; repository contents unchanged |
| Runtime harness (exact Spanish-ready path) | `TESSDATA_PATH=C:\Users\BIIbr\AppData\Local\Temp\opencode\tess4j-local-runtime\tessdata` + `APP_SUPER_TICKET_OCR_UPLOAD_LANGUAGES=spa+eng` → 3 verified files, 3/3 `READY`, structured candidates produced in 3/3 responses, aggregate candidates: 0 dates / 0 sources / 93 lines, aggregate warnings: 3 |
| Rollback boundary | Revert README OCR runtime guidance and this apply-progress remediation note; the safe local temp `tess4j-local-runtime` folder can be deleted independently outside the repository |

## Remaining Tasks

- [ ] 2.1 Update `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-mobile-subtabs/spec.md` to require separate list/barcode/ticket/category subtabs and compact mobile navigation.
- [ ] 2.2 Update `openspec/changes/supermarket-ticket-mobile-navigation/specs/super-inventory/spec.md` to preserve existing behavior while relocating supermarket surfaces into subtabs.
- [ ] 2.3 Update `openspec/changes/supermarket-ticket-mobile-navigation/specs/privacy-safe-pwa-shell/spec.md` only if needed to keep manual fallback routes visible on mobile.
- [ ] 3.2 Write RED contract cases for mobile subtabs, visible fallback routes, and preserved inventory behavior in the three spec files above.
- [ ] 4.1 Trim task/spec copy so the change stays reviewable under chained slices and keeps OCR, subtabs, and mobile navigation scope isolated.
- [ ] 4.2 Note any remaining harness gaps in the change docs without adding repo-local image fixtures or test assets.

## Workload / PR Boundary

- Mode: stacked PR slice
- Current work unit: PR 1 — OCR readiness/local JPG guard
- Boundary: starts at OCR outcome contract + local-only verification guidance, ends before any supermarket sub-tab/mobile navigation edits
- Estimated review budget impact: ~181 changed lines by `git diff --stat`, within the PR slice budget

## Status

4/10 tasks complete. Ready for next batch.
