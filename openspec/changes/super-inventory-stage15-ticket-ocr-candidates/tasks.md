# Tasks: Stage 15 Ticket OCR Candidates

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-950 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend contract seam → PR 2 OCR/parser guards → PR 3 UI review flow → PR 4 OpenSpec/report-only |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Endpoint contract, DTOs, validation, fake OCR seam | PR 1 | Base `main`; include MVC no-persistence tests. |
| 2 | Tess4J adapter, parser, runtime/temp-artifact guards | PR 2 | Base after PR 1; include unit/adapter persistence tests. |
| 3 | Static upload/review UI using existing confirmation API | PR 3 | Base after PR 2; include static UI contract tests. |
| 4 | SDD bookkeeping only | PR 4 | Base after PR 3; no product code. |

## Phase 1: Backend Contract Foundation

- [x] 1.1 Create `TicketOcr*Response.java`, candidate DTOs, and `TicketOcrUploadProperties.java` under `src/main/java/com/gentleia/landingtarjetas/supermarket/` with safe response-only fields.
- [x] 1.2 Create `TicketOcrEngine.java`, `TicketOcrService.java`, and `TicketOcrController.java` for `POST /api/super/ticket-ocr/candidates`, validating exactly one allowlisted image and size.
- [x] 1.3 Add MVC tests in `SupermarketControllerTests.java` for valid upload, invalid type/size/count, and unchanged repositories before confirmation.

## Phase 2: OCR Adapter, Parser, and Privacy Guards

- [x] 2.1 Modify `pom.xml` to add Tess4J only, then create `Tess4jTicketOcrEngine.java` using `BufferedImage` as the normative input path.
- [x] 2.2 Create `TicketOcrCandidateParser.java` with deterministic date/source/line/price candidates plus warnings for low-confidence or unparsable lines.
- [x] 2.3 Add parser/unit tests for valid lines, ambiguous lines, low confidence, and no product auto-confirmation.
- [x] 2.4 Add OCR adapter tests for unavailable or misconfigured native/tessdata runtime returning safe errors/warnings.
- [x] 2.5 Add tests proving no image, raw text, candidates, temp files, logs, observations, current prices, products, stock, or barcodes persist; any workaround artifact is private and deleted in `finally`.

## Phase 3: UI Review Flow

- [x] 3.1 Modify `src/main/resources/static/js/api.js` with `uploadSuperTicketOcrCandidates(file)` using existing CSRF multipart upload patterns.
- [x] 3.2 Modify `index.html`, `js/supermarket.js`, and `css/styles.css` to add a compact upload/review panel with editable/selectable candidates and visible warnings.
- [x] 3.3 Wire confirmed rows only to the existing price-observation API and explicit `syncCurrentReferencePrice`; do not add barcode, stock, totals, comparison, source admin, local/session storage, or auto-product creation.
- [x] 3.4 Extend `StaticUiContractTests.java` and `src/test/resources/static-ui-contract-tests.mjs` for upload, warnings, edit/select, existing confirmation call, refresh discard, and forbidden-scope guards.

## Phase 4: Verification and SDD Bookkeeping

- [x] 4.1 Run `mvn test` and verify Stage 15 spec scenarios: valid candidates, invalid upload no persistence, poor OCR warnings, human confirmation only, and scope boundary.
- [x] 4.2 Update SDD task checkboxes only after implementation slices land; keep archive/report changes in the final docs-only PR.

## Approved Stage 15 Correction

- [x] 4.3 Enforce the documented maximum decoded image dimension before OCR and preserve dot-decimal price parsing while retaining comma-decimal and dot-thousands parsing; add focused regression coverage without changing frozen review artifacts.
- [x] 4.4 Reject a declared non-PNG/JPEG MIME type even when the filename has an allowed PNG/JPEG extension; preserve valid MIME handling, extension fallback when MIME is absent, and no-OCR/no-persistence rejection behavior.
