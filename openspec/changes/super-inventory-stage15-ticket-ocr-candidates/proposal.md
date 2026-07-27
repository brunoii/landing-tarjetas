# Proposal: Stage 15 Ticket OCR Candidates

## Intent

Add the smallest useful receipt-photo automation slice: upload one ticket image, process it in memory, return reviewable price candidates, and require explicit human confirmation before any observation, current price, product, stock, barcode, or image/text data is persisted.

## Proposal question round

Auto mode assumed: one-image review is enough for Stage 15; candidate loss after refresh is acceptable; OCR confidence/warnings are more important than automatic completion. Product review should validate these assumptions before apply.

## Scope

### In Scope
- Single image upload endpoint for ticket OCR candidate extraction.
- In-memory OCR/extraction response with safe metadata, date/source candidates, line candidates, prices, confidence/warnings.
- UI upload/review panel that lets users edit/select candidates and then use existing explicit price-observation creation.
- Tests proving no persistence happens before confirmation.

### Out of Scope
- Persisting images, raw OCR text, candidates, observations, current prices, products, stock movements, or barcode aliases automatically.
- Barcode mobile scanning, stock increment/decrement, source administration, comparison, charts, ticket totals, multi-price handling, product auto-creation.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: relaxes the prior OCR/ticket/photo prohibition only for transient ticket-image OCR candidates that feed the existing explicit price-observation confirmation flow.

## Approach

Expose `POST /api/super/ticket-ocr/candidates` for one allowlisted image, validate size/type, hash/process bytes in memory, OCR/extract candidate rows, and return transient DTOs. Keep confirmation separate: accepted rows call the existing `POST /api/super/items/{id}/price-observations` path, reusing its validation and optional `syncCurrentReferencePrice` semantics.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `pom.xml` | Modified | Add one OCR/image capability only if design validates it. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | New | Ticket OCR controller/service/DTOs. |
| `src/main/resources/static/js/api.js` | Modified | Multipart helper using existing CSRF/upload patterns. |
| `src/main/resources/static/js/supermarket.js`, `index.html`, `styles.css` | Modified | Compact upload/review UI. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified | Backend contract tests. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Modified | UI/static contracts. |
| `openspec/specs/super-inventory/spec.md` | Modified | Stage 15 requirement delta. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| OCR dependency/runtime fragility | Medium | Validate before design locks library; keep one dependency. |
| OCR/parsing mistakes | High | Return candidates with warnings; require human confirmation. |
| Privacy leakage | Medium | Do not persist image/text/candidates; keep errors safe. |
| Scope drift | High | Explicitly defer barcode, stock, totals, comparison, auto-persistence. |

## Rollback Plan

Remove the endpoint/service/DTOs, OCR dependency, UI panel/helper, tests, and Stage 15 delta spec. No data migration is expected because the first slice persists no ticket artifacts.

## Dependencies

- A locally viable OCR/image-processing dependency selected during design.
- Strict TDD with `mvn test`.
- Chained delivery slices under the 400-line review budget.

## Success Criteria

- [ ] Uploading one valid ticket image returns transient reviewable candidates.
- [ ] Invalid type/size/count is rejected without persistence.
- [ ] No observation, current price, product, stock movement, barcode alias, image, raw text, or candidate row is persisted before explicit confirmation.
- [ ] Confirmed rows use the existing price-observation endpoint and sync option.
