# Proposal: Ticket OCR Barcode Catalog — Slice 1 OCR Cleanup

## Intent

Fix the current line-by-line ticket OCR review so Vea/Gómez Pardo uploads return useful transient candidates instead of noisy rows, while preserving manual confirmation and privacy boundaries.

## Proposal question round

Auto assumptions: this first slice only improves OCR parsing/review; catalog lookup, external enrichment, and scan-session work stay deferred; explicit confirmation remains the only persistence path.

## Scope

### In Scope
- Normalize OCR lines and group them into bounded product blocks for Gómez Pardo and Vea samples.
- Return useful transient candidates with barcode/store code, quantity, prices, tax, warnings, and hidden-by-default debug noise.
- Update the existing OCR review UI/tests without changing stock, catalog, or price persistence flows.

### Out of Scope
- Local product catalog, external barcode APIs, manual product creation, and enrichment caching.
- Stage 16/17 barcode scan changes, scan sessions, OCR runtime swaps, preprocessing upgrades, or automatic inventory/price mutation.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: ticket OCR review MUST emit block-based useful candidates and separate debug noise while keeping OCR data transient until explicit user confirmation.

## Approach

Extend the current `TicketOcrCandidateParser` from line parsing to normalization + block parsing for the two target formats. Reuse `POST /api/super/ticket-ocr/candidates`, keep the existing confirmation flow separate, and update the static UI to show candidate rows first with OCR detail collapsed by default.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrCandidateParser.java` | Modified | Block parsing, number normalization, noise separation. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modified | Preserve transient response boundary with richer candidate/debug payloads. |
| `src/main/resources/static/js/supermarket.js` | Modified | Useful-candidate table and hidden debug detail. |
| `src/main/resources/static/index.html`, `src/main/resources/static/css/styles.css` | Modified | Review/debug presentation only. |
| `src/test/java/com/gentleia/landingtarjetas/TicketOcrCandidateParserTests.java`, `SupermarketControllerTests.java`, `StaticUiContractTests.java` | Modified | Synthetic OCR coverage and boundary checks. |
| `openspec/specs/super-inventory/spec.md` | Modified | Delta for bounded OCR cleanup behavior. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Format heuristics miss real tickets | Med | Start with synthetic Gómez Pardo/Vea fixtures and explicit warnings/debug fallback. |
| Scope drifts into catalog/scanner roadmap | High | Defer Stage 16/17 and catalog/enrichment explicitly in spec/tasks. |
| Privacy boundary regresses | Low | Keep images/text/candidates transient and reuse existing confirmation gate. |

## Rollback Plan

Revert parser/UI/test/spec changes and fall back to the current transient OCR candidate behavior. No data migration is needed because this slice persists no ticket artifacts.

## Dependencies

- Existing `ticket-ocr-runtime` CLI slice and synthetic OCR text fixtures for Vea/Gómez Pardo.

## Success Criteria

- [ ] Gómez Pardo and Vea OCR samples produce reviewable block candidates with quantity, prices, and code fields.
- [ ] Garbage/non-product lines stay out of the main table and remain available only in debug detail.
- [ ] No ticket image, full OCR text, candidate row, catalog data, stock, or price record is persisted before explicit user action.
