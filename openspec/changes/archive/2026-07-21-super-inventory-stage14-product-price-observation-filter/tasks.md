# Tasks: Super Inventory Stage 14 Product Price Observation Filter

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 180-280 |
| 400-line budget risk | Low |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: UI/static behavior + tests -> PR 2: verify/archive publication |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add product-scoped price observation UI behavior and tests | PR 1 | Base `main`; includes `mvn test` verification. |
| 2 | Publish SDD verification/archive artifacts after apply | PR 2 | Base PR 1 merge result; no product-code changes. |

## Phase 1: Static UI Foundation

- [x] 1.1 Modify `src/main/resources/static/index.html` price observation card with contextual title/summary target and compact global reset control.
- [x] 1.2 Keep existing observation table/list markup in `index.html`; do not add comparison, chart, commerce, media, or Stage 15 surfaces.

## Phase 2: Product-Scoped Behavior

- [x] 2.1 Add selected price-observation item state in `src/main/resources/static/js/supermarket.js` using `{ id, name }` only.
- [x] 2.2 Add one compact product row action in `superItemRowHtml()` with `data-super-action="price-history"` and accessible label/title.
- [x] 2.3 Extend `handleSuperItemAction()` to load `superPriceObservations({ itemId, limit: 50 })` for the clicked product.
- [x] 2.4 Refactor `loadSuperPriceObservations()`/`renderSuperPriceObservations()` to render global vs product context, empty copy, and error copy.
- [x] 2.5 Wire the reset control to clear selected product state and reload `superPriceObservations({ limit: 50 })`.

## Phase 3: Contract Tests

- [x] 3.1 Extend `src/test/resources/static-ui-contract-tests.mjs` fake DOM selectors for the new contextual title/summary/reset elements.
- [x] 3.2 Add behavior assertions: row action calls `{ itemId, limit: 50 }`, contextual empty state names the product, reset calls global `{ limit: 50 }`.
- [x] 3.3 Extend unsupported-semantics assertions to keep comparison, charts, stores/commerce, multiple prices/presentations, barcode/OCR/ticket/photo, and Stage 15 out.
- [x] 3.4 Keep `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` as the Maven guard unless cache-version constants need updating.

## Phase 4: Verification

- [x] 4.1 Run `mvn test` and confirm the UI/static contract scenarios pass.
- [x] 4.2 Update task checkboxes during apply only after each implementation step is verified.
