# Proposal: Super Inventory Stage 12 Reference Price Source Reuse

## Intent

Remove the Stage 11 asymmetry where price observations can reuse `SuperPriceSource` but the current/reference price on `SuperItem` still accepts free text only. Stage 12 should let the product price reuse the same catalog without breaking legacy items.

## Scope

### In Scope
- Allow `SuperItem` current/reference price to use an optional source expressed as `commercialPresentationPriceSourceId`, free-text `commercialPresentationPriceSourceLabel`, or neither source field.
- Preserve legacy compatibility: existing items may keep free text with `priceSourceId=null`; no backfill.
- Enforce a closed mutual-exclusion rule in API and UI so only `{id only}`, `{label only}`, or `{neither}` are valid, and `{id + label}` is rejected.
- Reuse existing Stage 11 price-source listing/inline-create flow; no new source-management surface.

### Out of Scope
- Full source administration, rename/deactivate flows, or store/commerce semantics.
- Comparison, analytics, multiple price tracks, OCR, barcode, ticket, photo, or Stage 15 scope.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `super-inventory`: extend the current/default commercial presentation price contract so the product-level source may be an active reusable source id or a free-text label, with legacy-safe null behavior.

## Approach

Mirror the Stage 11 observation pattern on `SuperItem`: nullable source FK plus text snapshot/fallback. Valid source states stay closed to `{id only}`, `{label only}`, or `{neither}`. When an id is provided, backend must resolve an active source and copy its name into the label snapshot. When free text is provided, id remains `null`. When neither is provided, price remains valid with both source fields `null`. Clearing price or presentation clears both source fields.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Add Stage 12 product-price source reuse rules and explicit non-goals |
| `src/main/java/.../supermarket/*` | Modified | Nullable source relation, request/response contract, XOR validation |
| `src/main/resources/static/index.html` | Modified | Product form selector/free-text XOR UI |
| `src/main/resources/static/js/supermarket.js` | Modified | Payload, edit state, rendering, validation |
| `src/test/.../SupermarketControllerTests.java` | Modified | Reusable source, free text, legacy, invalid XOR coverage |
| `src/test/.../StaticUiContractTests.java` | Modified | Contract allow-list and scope guards |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| XOR confusion in UI | Med | Disable/clear the alternate input when one path is chosen |
| Legacy ambiguity | Low | Spec and tests must preserve `priceSourceId=null` without migration |
| Slice grows beyond review budget | Med | Deliver as chained backend/API/tests → UI/static/tests → OpenSpec |

## Rollback Plan

Revert the nullable product-source linkage and UI selector, returning product price source handling to free text only while preserving legacy labels.

## Dependencies

- Existing Stage 11 `SuperPriceSource` catalog and `/api/super/price-sources` flow

## Success Criteria

- [ ] `/api/super/items` accepts only these Stage 12 source states for product reference price: `{id only}`, `{label only}`, or `{neither}`; it rejects `{id + label}`.
- [ ] Legacy items with free-text source and `priceSourceId=null` remain valid without backfill.
- [ ] UI prevents dual entry and supports editing/rendering both legacy and reusable-source items.
