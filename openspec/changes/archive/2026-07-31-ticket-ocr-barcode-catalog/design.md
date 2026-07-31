# Design: Ticket OCR Barcode Catalog — Slice 1 OCR Cleanup

## Technical Approach

Extend `TicketOcrCandidateParser.parse` from independent line parsing to normalized, bounded Vea/Gómez Pardo product blocks. Preserve `POST /api/super/ticket-ocr/candidates`, its upload validation/outcomes, and the client-only review state. The existing `createSuperItemPriceObservation` action remains the only confirmation/persistence boundary. This implements both delta requirements without catalog lookup, enrichment, scanning, or automatic mutation.

## Architecture Decisions

| Decision | Options / trade-off | Decision and rationale |
|---|---|---|
| Candidate model | New endpoint vs. additive response fields | Add nullable review fields to `TicketOcrLineCandidateResponse` and `debugLines` to `TicketOcrResponse`/`TicketOcrEngineResult`. The endpoint and existing fields stay compatible while the UI receives separated data. |
| Parsing | Generic heuristic vs. two bounded format recognizers | Normalize whitespace, amounts, and code tokens, then recognize Vea/Gómez Pardo block boundaries. Emit incomplete blocks with warnings only when still reviewable; otherwise put their normalized lines in debug. This limits false product rows. |
| Confirmation | Auto-create/lookup item vs. current manual flow | Retain the existing product selector and `createSuperItemPriceObservation`. No product ID is inferred and no catalog or stock/price write occurs from OCR alone. |

## Data Flow

```
PNG/JPEG -> TicketOcrService -> TesseractCliTicketOcrEngine
                              -> TicketOcrCandidateParser
                              -> useful candidates + debugLines (HTTP only)
browser in-memory review -> manual existing-item confirmation
                         -> createSuperItemPriceObservation only
```

`TicketOcrService.extractCandidates` continues to validate/decode, calculate the checksum, classify outcomes, and return transient data. Refresh/discard clears `currentTicketOcrReview`; it is never stored in browser storage or repositories.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrCandidateParser.java` | Modify | Normalize and group supported ticket blocks; classify useful versus debug lines. |
| `TicketOcrLineCandidateResponse.java`, `TicketOcrEngineResult.java`, `TicketOcrResponse.java` | Modify | Add optional code, quantity, unit-price, line-total, tax, and debug contracts. |
| `TicketOcrService.java` | Modify | Carry separated transient parser output without changing route, validation, or persistence. |
| `src/main/resources/static/js/supermarket.js` | Modify | Render useful block fields, retain editable confirmation values, and toggle debug detail closed by default. |
| `src/main/resources/static/index.html`, `src/main/resources/static/css/styles.css` | Modify | Add accessible debug disclosure and candidate columns/styles; retain current IDs and confirmation form. |
| `src/test/java/com/gentleia/landingtarjetas/{TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests}.java` | Modify | Add RED-first parser, API/no-persistence, and static UI contracts. |

## Interfaces / Contracts

`POST /api/super/ticket-ocr/candidates` remains unchanged. Each `lineCandidates[]` entry keeps `rawText`, `descriptionCandidate`, `pricePesos`, `confidence`, `warnings`, and nullable product fields, and adds nullable:

```java
String barcodeOrStoreCode; BigDecimal quantity;
BigDecimal unitPricePesos; BigDecimal lineTotalPesos; BigDecimal taxPesos;
```

`TicketOcrResponse` adds `List<TicketOcrDebugLineResponse> debugLines`; each entry contains normalized text and a non-sensitive classification/warning. The main table shows only `lineCandidates`; debug is a closed disclosure. A supported block returns available fields and warnings. Malformed/ambiguous data neither fails the request nor becomes a persistence command.

## Testing Strategy

| Layer | What to test | Approach |
|---|---|---|
| Unit | Vea/Gómez Pardo normalization, block boundaries, comma/thousands numbers, partial blocks, noise | Write failing table-driven JUnit cases in `TicketOcrCandidateParserTests`, then implement. Assert useful/debug separation and no inferred product ID. |
| Integration | Existing multipart route returns additive fields; malformed blocks remain `READY`/safe; no repositories change | Extend `SupermarketControllerTests` with mocked engine results and `assertSuperInventoryRepositoriesRemainEmpty()`. |
| Static UI | Main table excludes debug, disclosure starts closed, existing IDs/API/confirmation remain | Extend `StaticUiContractTests`; run `node --check src/main/resources/static/js/supermarket.js`. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary is changed. Existing OCR CLI validation and safe process behavior remain unchanged.

## Migration / Rollout

No migration required. Additive transient JSON fields and static rendering can roll back with no data cleanup.

## Open Questions

- [ ] Confirm synthetic Vea/Gómez Pardo sample layouts before finalizing block regexes; no production ticket content should enter tests.
