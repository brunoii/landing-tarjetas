# Apply Progress: Stage 15 Ticket OCR Candidates

## Mode

Strict TDD with `mvn test`.

## Completed Tasks

- [x] 1.1 Created `TicketOcr*Response.java`, candidate DTOs, and `TicketOcrUploadProperties.java` under `src/main/java/com/gentleia/landingtarjetas/supermarket/` with safe response-only fields.
- [x] 1.2 Created `TicketOcrEngine.java`, `TicketOcrService.java`, and `TicketOcrController.java` for `POST /api/super/ticket-ocr/candidates`, validating exactly one allowlisted image and size.
- [x] 1.3 Added MVC tests in `SupermarketControllerTests.java` for valid upload, invalid type/size/count, and unchanged repositories before confirmation.
- [x] 2.1 Added `net.sourceforge.tess4j:tess4j:5.16.0` and created `Tess4jTicketOcrEngine.java` using Tess4J `ITesseract.doOCR(BufferedImage)` as the normative in-memory OCR input path.
- [x] 2.2 Created `TicketOcrCandidateParser.java` for deterministic date/source/line/price extraction and warnings for low-confidence or unparsable lines.
- [x] 2.3 Added parser unit tests for valid receipt lines, ambiguous lines, low OCR confidence, and null product candidate fields to preserve human review.
- [x] 2.4 Added OCR adapter tests for unavailable/misconfigured Tess4J runtime returning a safe generic warning without leaking runtime paths or payloads.
- [x] 2.5 Added privacy/runtime guard tests for no temp artifacts in the `BufferedImage` path, no raw OCR/candidate/file payload logging before confirmation, unchanged super-inventory repositories, and no WebP allowlist without decoding support.
- [x] 3.1 Added `uploadSuperTicketOcrCandidates(file)` in `src/main/resources/static/js/api.js` using the existing CSRF-aware multipart upload helper and `file` form field.
- [x] 3.2 Added a compact Stage 15 OCR upload/review panel in `index.html`, `css/styles.css`, and `js/supermarket.js` with transient candidate summary, warnings, selectable lines, and editable confirmation fields.
- [x] 3.3 Wired OCR confirmation only to the existing `createSuperItemPriceObservation` flow with explicit `syncCurrentReferencePrice`, while keeping barcode, stock, totals, comparison, source admin, local/session storage, and auto-product creation out of scope.
- [x] 3.4 Extended `StaticUiContractTests.java` and `src/test/resources/static-ui-contract-tests.mjs` for OCR upload, warnings, line selection, existing confirmation API calls, discard-on-refresh semantics, and forbidden-scope guards.
- [x] 4.1 Ran `mvn test` and verified the Stage 15 scenarios for valid candidates, invalid upload no persistence, poor OCR warnings, human confirmation only, and scope boundaries.
- [x] 4.2 Updated OpenSpec task checkboxes and merged cumulative apply progress instead of overwriting prior PR1/PR2 evidence.
- [x] 4.3 Applied the maintainer-authorized Stage 15 correction: inspect image dimensions before decoding/OCR, reject oversized decoded images without OCR or persistence, and preserve dot-decimal price parsing while retaining comma-decimal and dot-thousands formats.
- [x] 4.4 Fixed the MIME/extension validation bypass: a declared non-PNG/JPEG MIME type is rejected even with a `.png` or `.jpeg` filename, while a missing MIME type still uses the allowed-extension fallback.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | MVC integration | ✅ `mvn -Dtest=SupermarketControllerTests test` — 80/80 passing before edits | ✅ DTO/property/seam imports failed compile before production classes existed | ✅ `mvn -Dtest=SupermarketControllerTests#ticketOcr* test` — 4/4 passing | ✅ Valid response asserts metadata, date/source/line candidates, warning, omitted product auto-confirmation fields | ✅ Null product candidate fields omitted via DTO JSON inclusion; focused and full suites still passing |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | MVC integration | ✅ `mvn -Dtest=SupermarketControllerTests test` — 80/80 passing before edits | ✅ `POST /api/super/ticket-ocr/candidates` test failed before controller/service existed | ✅ `mvn -Dtest=SupermarketControllerTests#ticketOcr* test` — 4/4 passing | ✅ Valid PNG, invalid content type, oversized image, and multiple-image count paths covered | ✅ Validation and hashing kept in service; OCR kept behind mockable `TicketOcrEngine` seam |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | MVC integration | ✅ `mvn -Dtest=SupermarketControllerTests test` — 80/80 passing before edits | ✅ MVC no-persistence assertions written before endpoint existed | ✅ `mvn -Dtest=SupermarketControllerTests` — 84/84 passing | ✅ Valid and invalid requests assert all super-inventory repositories remain unchanged before confirmation | ✅ Full `mvn test` — 265/265 passing |
| 2.1 | `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` | Unit/adapter | N/A (new adapter; baseline `mvn -Dtest=SupermarketControllerTests test` — 84/84 passing) | ✅ Test compile failed before Tess4J dependency and `Tess4jTicketOcrEngine` existed | ✅ Focused OCR/parser/MVC command passed — 5/5, then 6/6 after privacy test | ✅ Success path verifies the exact `BufferedImage` instance is sent to `ITesseract.doOCR(BufferedImage)`; failure path verifies safe runtime warning | ✅ Constructor annotated for Spring injection after full-suite context failure revealed ambiguous constructors |
| 2.2 | `src/test/java/com/gentleia/landingtarjetas/TicketOcrCandidateParserTests.java` | Unit | N/A (new pure parser) | ✅ Parser tests failed compile before `TicketOcrCandidateParser` existed | ✅ Focused OCR/parser/MVC command passed — 5/5, then 6/6 after privacy test | ✅ Valid receipt text extracts date/source/two prices; ambiguous/low-confidence text yields warnings and still returns review-only candidates | ✅ Parser kept deterministic and side-effect free |
| 2.3 | `src/test/java/com/gentleia/landingtarjetas/TicketOcrCandidateParserTests.java` | Unit | N/A (new tests) | ✅ Product auto-confirmation/null candidate assertions written before parser existed | ✅ Focused OCR/parser/MVC command passed — 5/5, then 6/6 after privacy test | ✅ Valid and ambiguous paths assert product candidate id/name stay null while candidates remain reviewable | ✅ No product lookup or persistence dependencies introduced |
| 2.4 | `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` | Unit/adapter | N/A (new adapter test) | ✅ Misconfigured runtime test failed before adapter existed | ✅ Focused OCR/parser/MVC command passed — 5/5, then 6/6 after privacy test | ✅ Success path parses OCR text; failure path catches `TesseractException` and returns only generic warning | ✅ Safe failure suppresses native/tessdata path details and raw payloads |
| 2.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` | MVC integration + unit/adapter | ✅ `mvn -Dtest=SupermarketControllerTests test` — 84/84 passing before edits | ✅ WebP rejection test failed until WebP was removed from the allowlist; adapter temp/log guard tests were added for the new privacy boundary | ✅ `mvn -Dtest=SupermarketControllerTests test` — 86/86 passing; focused OCR/parser/MVC command — 6/6 passing | ✅ Repository no-mutation assertions, no raw OCR/candidate/file logging, no adapter temp artifacts, and WebP rejection paths covered | ✅ Full `mvn test` — 271/271 passing after Spring constructor annotation fix |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + node-backed behavior | ✅ `mvn -Dtest=StaticUiContractTests test` — 28/28 passing before edits | ✅ Cache-token/API expectations failed before `uploadSuperTicketOcrCandidates(file)` and `/api/super/ticket-ocr/candidates` existed in the static client | ✅ `mvn -Dtest=StaticUiContractTests test` — 29/29 passing | ✅ API contract now proves multipart field name, endpoint, cache tokens, and `FormData` file wiring | ✅ Reused existing `uploadRequest` instead of duplicating multipart fetch behavior |
| 3.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + node-backed behavior | ✅ `mvn -Dtest=StaticUiContractTests test` — 28/28 passing before edits | ✅ Static OCR panel selectors, copy, and style-contract assertions failed before the panel existed | ✅ `mvn -Dtest=StaticUiContractTests test` — 29/29 passing | ✅ Upload summary, warning lists, selectable candidate rows, editable confirmation form, and discard state all execute in the node-backed DOM harness | ✅ Centralized review-state rendering kept the panel compact and avoided ad-hoc DOM mutations |
| 3.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + node-backed behavior | ✅ `mvn -Dtest=StaticUiContractTests test` — 28/28 passing before edits | ✅ OCR confirmation-path assertions failed before rows could call the existing price-observation API with explicit sync | ✅ `mvn -Dtest=StaticUiContractTests test` — 29/29 passing | ✅ Invalid upload, line selection, existing confirmation call, explicit sync path, and discard/reset scenarios cover both happy and safety paths | ✅ Confirmation reuses `superPriceObservationPayloadFromValues`/`validateSuperPriceObservationPayload` to preserve existing observation validation |
| 3.4 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static contract + behavior evidence | ✅ `mvn -Dtest=StaticUiContractTests test` — 28/28 passing before edits | ✅ New OCR contract assertions were added before production UI/API changes existed, producing the initial failure state | ✅ `mvn -Dtest=StaticUiContractTests test` — 29/29 passing | ✅ Java static contracts and node-backed behavioral cases now cover upload, warnings, select/edit, confirmation, discard, and forbidden-scope guards | ✅ Updated fake DOM/helpers once so OCR tests stayed readable instead of scattering ad-hoc selector stubs |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, full Maven suite | Verification | ✅ Focused static contract safety net already green before the full-suite run | ➖ Verification-only task; RED evidence is supplied by tasks 3.1-3.4 | ✅ `mvn test` — 272/272 passing | ✅ Full suite re-proves valid candidates, invalid upload handling, warning-only OCR review, explicit confirmation, and scope boundaries | ➖ No code refactor; verification gate only |
| 4.2 | `openspec/changes/.../tasks.md`, `openspec/changes/.../apply-progress.md` | Artifact bookkeeping | ✅ Prior apply-progress and Engram task state were read before updates | ✅ Files still showed PR3/PR4 tasks incomplete before this batch updated them | ✅ Re-read task artifact after edits confirms 3.1-4.2 are `[x]` | ➖ Triangulation skipped: structural artifact update with one intended cumulative output | ✅ Merged prior PR1/PR2 progress instead of overwriting historical evidence |
| 4.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, `src/test/java/com/gentleia/landingtarjetas/TicketOcrCandidateParserTests.java` | MVC integration + unit | ✅ `mvn -Dtest=SupermarketControllerTests,TicketOcrCandidateParserTests test` — 88/88 passing before edits | ✅ New test compile failed because the decoded-dimension limit did not exist; dot-decimal assertion described the incorrect existing normalization | ✅ `mvn -Dtest=SupermarketControllerTests,TicketOcrCandidateParserTests test` — 90/90 passing | ✅ Rejects a valid encoded PNG exceeding the configured decoded dimension before OCR, asserts no persistence or filename logging, and covers dot-decimal plus dot-thousands parsing | ✅ Used `ImageReader` header inspection before `BufferedImage` allocation; no further refactor needed |
| 4.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | MVC integration | ✅ `mvn -Dtest=SupermarketControllerTests test` — 87/87 passing before edits | ✅ `mvn -Dtest=SupermarketControllerTests#ticketOcrRejectsInvalidDeclaredMimeTypeDespite* test` — 2 errors after both allowed-extension uploads reached the mocked OCR engine | ✅ Same focused command — 2/2 passing after rejecting any declared non-PNG/JPEG MIME type | ✅ Covers `.png` with `image/gif` and `.jpeg` with `text/plain`, each asserting 400, no OCR invocation, and unchanged repositories | ➖ No refactor needed; validation remains minimal and retains extension fallback only for missing MIME |

## Test Summary

- **Total focused regression tests written across slices**: 16
- **Total tests passing**: 276
- **Layers used**: MVC integration (4), Unit/adapter (4), Static UI contract and node-backed behavior (4)
- **Approval tests**: None — no refactoring tasks
- **Pure functions created**: 1 (`TicketOcrCandidateParser.parse` and helpers)

## Verification Commands

- `mvn -Dtest=StaticUiContractTests test` — baseline passed before edits, 28 tests.
- `mvn -Dtest=StaticUiContractTests test` — RED failed after adding Stage 15 OCR UI/API/static expectations before implementation.
- `mvn -Dtest=StaticUiContractTests test` — passed, 29 tests.
- `node --check src/main/resources/static/js/api.js`
- `node --check src/main/resources/static/js/app.js`
- `node --check src/main/resources/static/js/supermarket.js`
- `node --check src/test/resources/static-ui-contract-tests.mjs`
- `mvn test` — passed, 272 tests.
- `mvn -Dtest=SupermarketControllerTests,TicketOcrCandidateParserTests test` — baseline passed, 88/88 tests.
- `mvn -Dtest=SupermarketControllerTests,TicketOcrCandidateParserTests test` — RED failed at test compile because `maxDecodedDimension` was absent.
- `mvn -Dtest=SupermarketControllerTests,TicketOcrCandidateParserTests test` — passed, 90/90 tests after the correction.
- `mvn test` — passed, 274/274 tests after the approved correction.
- `mvn -Dtest=SupermarketControllerTests#ticketOcrRejectsInvalidDeclaredMimeTypeDespite* test` — RED: 2 errors because invalid declared MIME types with allowed extensions reached OCR.
- `mvn -Dtest=SupermarketControllerTests#ticketOcrRejectsInvalidDeclaredMimeTypeDespite* test` — passed, 2/2 tests after the MIME validation fix.
- `mvn test` — passed, 276/276 tests after the MIME/extension mismatch regression fix.

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `mvn -Dtest=StaticUiContractTests test` — exit 0, 29/29 passing |
| Runtime harness command/scenario and exact result | `mvn test` — exit 0, 272/272 passing, including Spring MVC, parser, OCR adapter, and static UI contract coverage |
| Rollback boundary | Revert `src/main/resources/static/js/api.js`, `src/main/resources/static/js/app.js`, `src/main/resources/static/js/supermarket.js`, `src/main/resources/static/index.html`, `src/main/resources/static/css/styles.css`, `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs`, and this change folder’s `tasks.md` / `apply-progress.md` to remove Stage 15 PR3 only |

## Approved Correction Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `mvn -Dtest=SupermarketControllerTests,TicketOcrCandidateParserTests test` — exit 0, 90/90 passing |
| Runtime harness command/scenario and exact result | `mvn test` — exit 0, 274/274 passing, including Spring MVC upload rejection, OCR adapter, parser, and static UI contracts |
| Rollback boundary | Revert `TicketOcrUploadProperties.java`, `TicketOcrService.java`, `TicketOcrCandidateParser.java`, the two focused test files, and this change folder's `tasks.md` / `apply-progress.md`; frozen review artifacts remain untouched |

## MIME Validation Correction Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `mvn -Dtest=SupermarketControllerTests#ticketOcrRejectsInvalidDeclaredMimeTypeDespite* test` — exit 0, 2/2 passing |
| Runtime harness command/scenario and exact result | `mvn test` — exit 0, 276/276 passing, including MVC rejection before OCR and repository persistence guards |
| Rollback boundary | Revert `TicketOcrService.java`, `SupermarketControllerTests.java`, and this change folder's `tasks.md` / `apply-progress.md`; frozen review artifacts remain untouched |

## Workload / PR Boundary

- Mode: stacked PR slice.
- Current work unit: PR 3 UI/static review flow and explicit confirmation.
- Boundary: static OCR upload/review UI, existing price-observation confirmation wiring, static contract updates, full verification, and SDD bookkeeping only.
- Estimated review budget impact: focused frontend/static contract slice stacked after PR2; no backend OCR/parser contract changes beyond direct UI integration.

## Deviations

None — implementation matches the approved design and keeps OCR source/date candidates transient until explicit confirmation.

## Issues Found

- Static asset cache tokens had to move to a new Stage 15 UI/API token pair because `api.js`, `app.js`, `supermarket.js`, `index.html`, and `styles.css` all changed in the same slice.
- The previous byte-size-only upload guard allowed compressed images to reach full `BufferedImage` decoding before any decoded-size validation; the approved correction adds a 4,096-pixel maximum width or height check from image metadata before decoding.

## Remaining Tasks

None.
