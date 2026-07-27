# Design: Super Inventory Stage 15 Ticket OCR Candidates

## Technical Approach

Add a transient OCR slice around a new `POST /api/super/ticket-ocr/candidates` multipart endpoint. The endpoint accepts exactly one allowlisted image, validates type/size, decodes bytes to an in-memory `BufferedImage`, OCRs transient content, parses reviewable line candidates, and returns DTOs with safe metadata, confidence, and warnings. Stage 15 MUST NOT persist image bytes, raw OCR text, candidates, observations, current prices, products, stock movements, barcode aliases, or debug artifacts. Persistence remains only through the existing `POST /api/super/items/{id}/price-observations` confirmation flow after human review.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| OCR runtime | Add `net.sourceforge.tess4j:tess4j` behind `TicketOcrEngine` | PDFBox, external cloud OCR, CLI-only `tesseract`, OpenCV-only preprocessing | PDFBox is PDF text-only; cloud OCR violates local/privacy goals; CLI-only is harder to test and deploy. Tess4J is Java/Spring-friendly but has native Tesseract/tessdata runtime risk. |
| Tess4J usage | Normative path: `ITesseract` over an in-memory `BufferedImage`; set `tessdata` path, prefer `spa+eng`, read confidence when available | Temp-file OCR, persisted uploads, raw-text debug logs | Proposal/spec require in-memory/transient processing. If Tess4J/runtime forces a workaround, it MUST be an implementation-private temp artifact only: created outside app storage, not user-visible, never referenced in responses/logs, deleted in `finally`, and covered by no-persistent-artifact tests/contracts. |
| Candidate storage | No DB tables for images/text/candidates | Draft ticket table or uploaded-file reuse | Spec requires loss after refresh and human review before persistence. |
| Parsing | Deterministic regex/heuristic parser over OCR lines | Product auto-match or totals reconciliation | Stage 15 only returns candidates; product matching remains manual candidate selection. |
| Delivery slicing | Backend contracts/OCR seam → parser tests → UI review panel → OpenSpec/archive | Single PR | Forced chained strategy and 400-line budget require reviewable slices. |

## Data Flow

```text
Browser image ──multipart──> TicketOcrController
    │                         │ validate one image/type/size
    │                         ▼
    │                  TicketOcrService
    │                  ├─ sha256 bytes in memory
    │                  ├─ decode BufferedImage in memory
    │                  ├─ TicketOcrEngine(Tess4J, no persistence)
    │                  └─ TicketOcrCandidateParser
    ▼
Review DTOs/warnings ──user edits/selects──> existing price-observation API
```

## File Changes

| File | Action | Description |
|---|---|---|
| `pom.xml` | Modify | Add Tess4J only; document native/tessdata runtime in comments or docs if needed. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrController.java` | Create | Multipart endpoint under `/api/super`. |
| `.../TicketOcrService.java`, `TicketOcrEngine.java`, `Tess4jTicketOcrEngine.java`, `TicketOcrCandidateParser.java` | Create | Validation, in-memory image decoding, OCR seam, runtime adapter, deterministic parsing. |
| `.../TicketOcrUploadProperties.java` | Create | Image max size and allowed content types/extensions; default similar to PDF upload limit unless product chooses otherwise. |
| `.../TicketOcr*Response.java` | Create | Safe metadata, date/source candidates, line candidates, price, confidence, warnings. |
| `src/main/resources/static/js/api.js` | Modify | Add `uploadSuperTicketOcrCandidates(file)` using existing CSRF `uploadRequest`. |
| `src/main/resources/static/index.html`, `js/supermarket.js`, `css/styles.css` | Modify | Compact upload/review panel that posts candidates and then uses existing confirmation API per selected product. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Multipart contract and no-persistence assertions. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Static/UI behavior and forbidden-scope guards. |

## Interfaces / Contracts

`POST /api/super/ticket-ocr/candidates` accepts `file`. Response shape: `checksumSha256`, `originalFilename`, `contentType`, `sizeBytes`, `ocrConfidence`, `dateCandidates[]`, `sourceCandidates[]`, `lineCandidates[]`, `warnings[]`. Each line has nullable `rawText` for review only, nullable `descriptionCandidate`, nullable `pricePesos`, nullable `confidence`, `warnings[]`, and optional `productCandidateId/name` only if produced by explicit local candidate logic, not auto-confirmation. Raw OCR text and candidates are response-only and MUST NOT be stored in DB, filesystem, application logs, session storage, or browser local storage.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Parser extracts prices, dates, source-like labels; flags unparsable/low-confidence lines | Pure Java tests with fixed OCR text; no native OCR dependency. |
| OCR adapter | `BufferedImage` is the primary Tess4J input; unavailable/misconfigured OCR runtime returns a safe error/warning | Fake/mocked adapter failure for missing tessdata/native dependency; no persisted artifacts. |
| MVC | Valid image returns candidates and invalid type/size/count returns explicit error | Mock/fake `TicketOcrEngine`; assert repositories unchanged. |
| Persistence guard | No image/text/candidate/temp-file/observation/current price/product/stock/barcode mutation before confirmation | Repository counts plus temp/app-storage directory assertions before/after OCR request. |
| UI contract | Upload helper, warnings, editable review rows, confirmation calls existing price-observation API | Extend static contract fake DOM/API tests. |
| Full suite | Regression | `mvn test`. |

## Migration / Rollout

No migration required. Tess4J runtime rollout must verify local `tessdata` availability (`spa`/`eng`) and native dependencies; failures should return safe warnings/errors without persisting OCR artifacts. Task planning must include explicit contracts for OCR runtime unavailable/misconfigured and for absence of persistent image/text/candidate/temp artifacts. Barcode scanning, stock movements, ticket totals, comparison, product/source administration, and auto-persistence remain out of scope.

## Open Questions

None.
