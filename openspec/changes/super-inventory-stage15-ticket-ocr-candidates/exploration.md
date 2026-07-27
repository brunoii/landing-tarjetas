## Exploration: super-inventory-stage15-ticket-ocr-candidates

### Current State

The project is a local-only Spring Boot 3.5.9 application on Java 17 with Maven, JPA/H2, Spring Security, and a framework-free static UI under `src/main/resources/static`. Existing upload infrastructure is PDF-only for credit-card statements: `POST /api/statements/upload` accepts multipart `files`, validates size/type, processes bytes in memory with Apache PDFBox, stores metadata/hash/status in `UploadedFile`, and intentionally does not persist raw PDF bytes or extracted text.

There is no OCR/image dependency in `pom.xml`. PDFBox exists, but it extracts embedded PDF text; it does not OCR receipt photos. The current supermarket domain already has `SuperItem`, local barcode aliases, stock movements, price sources, append-only price observations, and optional synchronization from a manually confirmed observation into the product current/reference price. Stage 14 added product-scoped price observation viewing. Stage 4 already implemented manual-first local barcode aliases, but mobile barcode scanning and barcode-driven inventory movement remain outside the current contract.

Stage 15 should therefore be the first receipt-photo automation slice: upload one image, extract text, parse reviewable candidate rows, and require human confirmation before creating any price observation or current price update. It should not auto-create products, stock movements, purchases, consumptions, barcode aliases, stores, comparisons, or inventory deductions.

### Affected Areas

- `pom.xml` — add exactly one OCR/image-processing capability if Stage 15 implements backend OCR; no OCR dependency exists today.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/` — likely home for a small ticket OCR controller/service/DTO set near existing supermarket price observation flows.
- `src/main/java/com/gentleia/landingtarjetas/statement/PdfTextExtractionService.java` — useful reference for upload validation, hashing, in-memory processing, and privacy-safe failure messaging, but should not be reused directly because it is PDF-specific.
- `src/main/java/com/gentleia/landingtarjetas/statement/UploadedFile.java` — reference pattern for metadata-only upload persistence; avoid coupling supermarket tickets to statement uploads unless a shared upload abstraction is intentionally designed later.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationController.java` and `SupermarketService.java` — confirmation should eventually call the existing explicit price-observation creation path, not bypass its validation/sync rules.
- `src/main/resources/static/js/api.js` — add multipart helper for ticket OCR, likely using the existing `uploadRequest`/CSRF pattern.
- `src/main/resources/static/js/supermarket.js`, `index.html`, `styles.css` — add a compact upload/review panel near price observations; it should render candidates for user correction/confirmation.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — backend/API contract tests for upload validation, candidate extraction shape, no persistence before confirmation, and confirmation handoff boundaries.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` and `src/test/resources/static-ui-contract-tests.mjs` — static/UI contracts for multipart helper, review workflow, no barcode/mobile scanner drift, and no automatic stock movement calls.
- `openspec/specs/super-inventory/spec.md` — add Stage 15 requirements that relax the prior OCR/ticket/photo prohibition only for this reviewable-candidates slice.

### Approaches

1. **Transient OCR candidates only** — Accept one receipt image, validate size/content type, OCR in memory, parse text into response-only candidates, and let the UI review/correct them before calling existing price-observation creation explicitly.
   - Pros: Smallest useful slice; preserves privacy; no new tables; avoids half-baked persistence; aligns with current PDF upload privacy pattern.
   - Cons: Refresh/navigation loses candidates; debugging OCR quality is harder without stored raw text; confirmation may require repeated correction in one session.
   - Effort: Medium

2. **Persist ticket session metadata and candidate lines** — Store upload metadata, extracted text or normalized text, and candidate rows in new `super_ticket_*` tables, then confirm selected rows later.
   - Pros: Recoverable review session; easier audit/debugging; can support multi-step refinement later.
   - Cons: Larger schema/API/UI surface; privacy risk from storing receipt text; higher review cost; not necessary for first proof of value.
   - Effort: High

3. **Automatic persistence into observations/current prices** — OCR and immediately create price observations or update product prices.
   - Pros: Fastest happy path when OCR is perfect.
   - Cons: Wrong default for receipts; OCR/parsing errors would corrupt price history/current prices; violates the user-confirmation boundary and existing manual-sync semantics.
   - Effort: Medium/High but high risk

### Recommendation

Proceed with **Transient OCR candidates only** for Stage 15. The backend should expose a focused endpoint such as `POST /api/super/ticket-ocr/candidates` accepting a single image file, validating it with a small explicit size/type allowlist, processing bytes in memory, and returning:

- upload metadata safe for display: original filename, content type, size, SHA-256 hash, OCR status/message;
- optional extracted text diagnostics only if privacy-safe and explicitly bounded, preferably not displayed by default;
- candidate receipt metadata: observed date candidate and source label candidate;
- candidate line rows: raw line, product-name candidate, optional matched `SuperItem` candidate if a conservative name match is possible, price candidate, quantity/confidence/warnings.

Confirmation should remain a separate human action. For each accepted row, the UI should let the user choose an existing active `SuperItem`, edit price/date/source, and then call the existing `POST /api/super/items/{id}/price-observations` endpoint. The existing `syncCurrentReferencePrice` checkbox can be reused as an explicit confirmation to update the product current/reference price. This keeps Stage 15 integrated with Stages 10-14 without inventing a parallel persistence path.

For storage, do not persist uploaded images, raw OCR text, or candidate rows in the first slice. If metadata persistence is needed for parity with statement uploads, persist only non-sensitive metadata/hash/status; however, the smallest slice can return everything transiently and still provide value.

Keep barcode scanning separate. The project already has manual barcode aliases, so Stage 15 does not need barcode as a prerequisite. Mobile scanning plus barcode-driven stock increment/decrement should be a later stage because it mixes identity lookup with inventory movement semantics, while ticket OCR is about price/date/source candidate review.

### Risks

- OCR accuracy: receipt photos vary by lighting, paper, font, rotation, and compression; confidence/warnings must be visible and confirmation mandatory.
- Parsing ambiguity: receipt totals, discounts, quantities, deposits, and payment lines can look like products; Stage 15 should return candidates, not facts.
- Privacy/storage: receipt images/text may reveal consumption patterns; first slice should avoid persisting raw image/text and keep errors privacy-safe.
- Large uploads: images can be much bigger than the current 1 MB PDF limit; add explicit image size/count limits before OCR.
- Dependency/runtime risk: no OCR dependency exists; adding one may require native binaries, language data, or heavy model files. Validate locally before design locks in a library.
- Review workflow: if the UI does not make correction/selection obvious, users may trust wrong candidates; confirmation must be explicit per row or batch with clear selected rows.
- Scope drift: barcode scanning, stock movement automation, product auto-creation, store comparison, multiple prices, and purchase/consumption semantics must stay out of Stage 15.

### Ready for Proposal

Yes — propose Stage 15 as a narrow OCR ticket candidate workflow: upload one receipt image, OCR/extract transient candidates, review/edit/select, then explicitly confirm into existing price observation/current-price flows. Tell the user barcode scanning remains a later stage unless a concrete blocker appears; current code already has manual barcode alias support, so it is not a prerequisite for OCR candidates.
