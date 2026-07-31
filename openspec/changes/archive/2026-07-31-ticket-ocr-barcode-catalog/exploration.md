## Exploration: ticket-ocr-barcode-catalog

### Current State
PRD location: `md/PRD_OCR_Tickets_Barcodes_Catalogo.md`. The codebase already has a transient ticket-OCR candidate flow in `TicketOcrService`/`TicketOcrController`, and manual-first barcode alias lookup/attach/remove in `SupermarketService` plus `SuperItemBarcodeAlias*`. The static UI also already wires both flows from `src/main/resources/static/js/supermarket.js` and `src/main/resources/static/js/api.js`.

This PRD overlaps prior SDD work: `openspec/changes/super-inventory-stage15-ticket-ocr-candidates/`, `openspec/changes/super-inventory-stage16-barcode-scanning/`, `openspec/changes/super-inventory-stage17-next-slice/`, and `openspec/changes/ocr-cli-runtime/`. I found no existing `ProductCatalogItem`, `BarcodeEnrichmentService`, `OpenFoodFactsClient`, or `TicketBarcodeDetector` symbols, so the product-catalog/external-enrichment part is still missing.

### Affected Areas
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` — current upload validation, decode, and transient OCR response boundary.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrController.java` — API surface for OCR candidate upload.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — existing barcode alias lookup/attach/remove semantics.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemBarcodeAlias*.java` — barcode alias persistence model and repository.
- `src/main/resources/static/js/supermarket.js` and `src/main/resources/static/js/api.js` — current browser wiring for OCR and barcode flows.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — proves OCR stays transient and barcode alias flow is already contract-tested.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` — locks the static UI contract for ticket OCR and barcode paths.
- `openspec/specs/super-inventory/spec.md` — current canonical inventory/bill/barcode spec that the new PRD will extend.

### Approaches
1. **Phased extension over existing slices** — split the PRD into a first OCR cleanup slice, then catalog/enrichment, then any barcode-driven UI refinement.
   - Pros: matches current code and tests; keeps review size small; reuses existing transient OCR and alias flows.
   - Cons: slower end-to-end delivery; requires explicit sequencing.
   - Effort: Medium

2. **Single umbrella change** — treat the PRD as one large change spanning OCR, barcode scanning, catalog lookup, and enrichment.
   - Pros: one proposal/spec tree; simpler traceability from PRD.
   - Cons: high blast radius; likely exceeds review budget; harder to verify safely.
   - Effort: High

### Recommendation
Use the phased extension. The repository already covers transient OCR candidates and manual barcode aliases, so the risky/new work is the missing product catalog plus external enrichment. Start with the smallest slice that adds measurable value and keeps the confirmation boundary intact.

### Risks
- Repeating work already split across `stage15/16/17` artifacts instead of extending them intentionally.
- Assuming a product catalog or enrichment API exists when the codebase does not currently implement either.
- Letting the scope widen beyond the 400-line review budget.

### Ready for Proposal
Yes — tell the user the change is ready to move to proposal, but it should be split into a phased plan. The first proposal should target one bounded slice, not the full OCR+barcode+catalog roadmap.
