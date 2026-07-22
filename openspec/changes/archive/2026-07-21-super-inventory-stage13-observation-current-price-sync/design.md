# Design: Super Inventory Stage 13 Observation Current Price Sync

## Technical Approach

Extend the existing manual observation command, not the product edit flow. `POST /api/super/items/{id}/price-observations` keeps append-only history by default and accepts a new optional boolean flag, `syncCurrentReferencePrice`, that copies the same validated observation payload into the product reference price fields inside the current `@Transactional` service method.

No automatic history generation is added to `createItem` or `updateItem`; this preserves the Stage 10 frontier and keeps Stage 13 limited to the explicit observation form.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Sync contract | Add `syncCurrentReferencePrice` to `SuperItemPriceObservationRequest` | New endpoint or product-edit side effect | Keeps one explicit manual action and avoids automatic history from product edits. |
| Atomicity | Reuse `SupermarketService.createPriceObservation` transaction for observation save plus optional `SuperItem` mutation | Separate service calls from controller/UI | Existing service layer owns transactional business logic; one transaction gives all-or-nothing behavior. |
| Source handling | Reuse `resolvePriceObservationSource` and copy resolved source to product fields | Re-resolve in a product update path | Prevents source drift and preserves the closed `{id only}`, `{label only}`, `{neither}` rule. |
| Delivery slicing | Backend/API/tests → UI/static/tests → OpenSpec/design/archive | Single PR | Forced chained strategy and 400-line budget make small reviewable slices safer. |

## Data Flow

```text
Observation form ──POST payload──> SuperItemPriceObservationController
       │                              │
       │                              ▼
       │                      SupermarketService.createPriceObservation
       │                              │
       │                validate item, presentation, price, source, date
       │                              │
       │            save SuperItemPriceObservation append-only
       │                              │
       └── if syncCurrentReferencePrice=true ──> mutate SuperItem reference price fields
                                      │
                                      ▼
                              transaction commits or rolls back
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRequest.java` | Modify | Add optional `Boolean syncCurrentReferencePrice`. Missing/null means false for legacy payloads. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modify | After validation and observation creation, conditionally set `commercialPresentationPricePesos`, `commercialPresentationPriceSource`, `commercialPresentationPriceSourceLabel`, and `commercialPresentationPriceObservedDate` on the same active `SuperItem`. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Add strict-TDD coverage for default observation-only behavior, explicit sync, invalid sync rollback, and product create/update non-history behavior. |
| `src/main/resources/static/index.html` | Modify | Add an unchecked explicit checkbox/help text inside the existing observation form only. |
| `src/main/resources/static/js/supermarket.js` | Modify | Include the flag only when checked, refresh items after successful sync, and show sync-specific success feedback. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Lock the checkbox/control contract and continue excluding admin/comparison/OCR/photo/ticket scope. |
| `openspec/changes/super-inventory-stage13-observation-current-price-sync/design.md` | Create | This design artifact. |

## Interfaces / Contracts

```java
public record SuperItemPriceObservationRequest(
        BigDecimal pricePesos,
        Long priceSourceId,
        String sourceLabel,
        LocalDate observedDate,
        Boolean syncCurrentReferencePrice
) {}
```

`Boolean.TRUE.equals(request.syncCurrentReferencePrice())` is the only sync trigger. Response shape remains unchanged; clients refresh item data when they need the updated current/reference price.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Integration | API default remains observation-only and preserves stock, barcodes, movements, product price | Extend `SupermarketControllerTests` with repository assertions. |
| Integration | Explicit sync updates product price/source/date and persists observation | MockMvc POST with `syncCurrentReferencePrice: true`, then assert both repositories. |
| Integration | Invalid source/date/item/presentation requests do not partially mutate | Assert no observation and unchanged product after rejected requests. |
| UI contract | Opt-in checkbox, payload flag, refresh and feedback behavior | Extend static contract tests around `index.html` and `supermarket.js`. |

## Migration / Rollout

No migration required. Hibernate `ddl-auto=update` already manages runtime schema changes, and Stage 13 adds no entity columns.

## Open Questions

None.
