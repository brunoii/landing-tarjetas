# Design: Super Inventory Stage 14 Product Price Observation Filter

## Technical Approach

Implement Stage 14 as a compact UI drilldown over the existing price observation list. The backend already supports `GET /api/super/price-observations?itemId=&limit=50` through `SuperItemPriceObservationController`, `SupermarketService.listPriceObservations`, and `SuperItemPriceObservationRepository.findRecentByItemId`, so no backend change is planned.

The product table will gain one compact row action that loads observations for that product into the existing price observation card. The card keeps the current global recent history as the default state, adds contextual heading/empty text for the selected product, and exposes a compact reset control to return to global recent observations.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Backend scope | Reuse existing `itemId` filter and `limit=50` | Add endpoint or service behavior | Current controller/service/repository already satisfy the spec; backend changes would add review cost without a proven gap. |
| UI surface | Reuse the existing price observation card | Add a new panel, chart, or modal | Keeps the UI compact and avoids Stage 15/comparison drift. |
| Product action | Add one icon-only `price-history` row action | Expand text buttons or overload movement history | Matches existing `row-actions` pattern while separating stock history from price observations. |
| View state | Track selected product in JS state and derive filters | Encode state in backend/session | This is local presentation state only; no persistence or API contract change is needed. |
| Delivery slicing | UI/static/tests → OpenSpec/archive | Single oversized PR | Forced chained strategy and 400-line budget favor small publishable slices. |

## Data Flow

```text
Product row action ──item id──> supermarket.js view state
       │                              │
       │                              ▼
       │                 superPriceObservations({ itemId, limit: 50 })
       │                              │
       ▼                              ▼
Existing price card <── contextual title/empty/list rendering
       │
       └── Global reset ──> superPriceObservations({ limit: 50 })
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/index.html` | Modify | Add a compact global-return button/label target in the existing price observation card. |
| `src/main/resources/static/js/supermarket.js` | Modify | Add selected price-observation product state, row action handling, filtered/global loaders, contextual heading/empty text, and reset behavior. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Add behavior-level static UI coverage for product-scoped observation load, contextual empty state, global reset, and forbidden Stage 15 semantics. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Keep the Maven guard executing the static UI contract test. |
| `openspec/changes/super-inventory-stage14-product-price-observation-filter/design.md` | Create | This design artifact. |

## Interfaces / Contracts

No new backend contract.

Existing API client remains sufficient:

```js
api.superPriceObservations({ itemId: productId, limit: 50 });
api.superPriceObservations({ limit: 50 });
```

Suggested UI state shape:

```js
let selectedPriceObservationItem = null; // { id, name }
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| UI contract | Product action calls `superPriceObservations({ itemId, limit: 50 })` | Extend fake DOM behavior test around `setupSupermarket` and product row clicks. |
| UI contract | Empty state and heading include selected product context | Assert rendered title/empty text after filtered empty response. |
| UI contract | Global reset calls `superPriceObservations({ limit: 50 })` and restores global copy | Simulate reset control click in `static-ui-contract-tests.mjs`. |
| Scope guard | No comparison, stores/commerce, charts, OCR/ticket/photo/barcode, multiple prices/presentations, or Stage 15 additions | Extend existing unsupported semantics assertions. |
| Full suite | Static contract runs under Maven | `mvn test`. |

## Migration / Rollout

No migration required. This is a read-only UI filter over existing append-only observations.

## Open Questions

None.
