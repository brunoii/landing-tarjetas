# Apply Progress: Presentación comercial default Etapa 6A

**Change**: `super-inventory-stage6-presentations-prices`
**Mode**: Strict TDD
**Artifact store**: hybrid/both
**Work unit**: PR 2 UI/static/tests only
**Chain strategy**: stacked-to-main
**Updated**: 2026-07-16

## Status

success — PR 1 backend/API/tests and PR 2 UI/static/tests slices are completed locally. No archive, commit, push, merge or PR was created.

## Completed Tasks

- [x] 1.1 Add `ITEM_PRESENTATION_LABEL_MAX_LENGTH = 120` to `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java`.
- [x] 1.2 Add nullable `commercialPresentationLabel` and `commercialPresentationQuantity` columns to `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java`.
- [x] 1.3 Extend `SuperItemRequest.java` and `SuperItemResponse.java` with nullable presentation fields, preserving legacy request/response compatibility.
- [x] 1.4 Add `SupermarketService.applyCommercialPresentation(...)`: trim blank label to null, require positive quantity and `unit`, reject quantity without label, and clear both when absent.
- [x] 2.1 Extend `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for legacy null presentation and valid create/update/list exposure.
- [x] 2.2 Add invalid presentation tests for blank label, non-positive quantity, quantity without `unit`, and quantity without label; assert persisted item is unchanged.
- [x] 2.3 Add no-collateral-mutation assertions for `checked`, `currentStock`, stock movements, suggested list results, and barcode aliases.
- [x] 3.1 Add presentation inputs and display column/label in `src/main/resources/static/index.html`; update only `/css/styles.css` and `/js/app.js` cache tokens when those assets change.
- [x] 3.2 Update `src/main/resources/static/js/supermarket.js` limits, payload, validation, edit/reset, and render for default presentation only; do not add prices, stores, multiple presentations, or automation.
- [x] 3.3 Update `src/main/resources/static/js/app.js` cache-busting imports only for `./api.js` and `./supermarket.js`; preserve unrelated module tokens.
- [x] 3.4 Add minimal presentation styling in `src/main/resources/static/css/styles.css` only if required by the new markup.
- [x] 4.1 Update `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` to allow only presentation terms and verify `index.html` CSS/app tokens plus `app.js` api/supermarket tokens.
- [x] 4.2 Extend `src/test/resources/static-ui-contract-tests.mjs` for payload/render/edit/reset presentation contracts, cache tokens, and continued blocking of `price`, `prices`, `store`, shops, multiple presentations, and automation.

## Remaining Tasks

- [ ] 5.1 Run `mvn -Dtest=SupermarketControllerTests test` for PR 1 and backend regression scope.
- [ ] 5.2 Run `mvn -Dtest=StaticUiContractTests test` and `node src/test/resources/static-ui-contract-tests.mjs` for PR 2.
- [ ] 5.3 If verification passes, let archive phase sync `openspec/specs/super-inventory/spec.md`; do not archive before implementation verification.

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` | Modified | Added backend presentation label max length constant. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modified | Added nullable label and quantity fields on the existing entity only. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` | Modified | Added optional request fields with size, digits and positive quantity validation. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` | Modified | Exposed nullable presentation fields in item responses. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modified | Added normalization/validation/clearing logic without stock, movement, suggestion or barcode side effects. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified | Added Spanish validation labels for the new request fields. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified | Added integration/API tests for legacy compatibility, valid persistence/exposure, invalid rollback and no-collateral mutation. |
| `src/main/resources/static/index.html` | Modified | Added presentation form fields and table column; updated only the app script token because `app.js` changed; kept CSS token unchanged because CSS did not change. |
| `src/main/resources/static/js/supermarket.js` | Modified | Added presentation field limit, payload, validation, render label, edit and reset support without prices/stores/automation. |
| `src/main/resources/static/js/app.js` | Modified | Updated only the `./supermarket.js` import token; preserved `./api.js` and unrelated module tokens. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Allowed only commercial presentation terms, added token checks, kept forbidden price/store/shop/plural presentation/automation terms blocked. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Added payload, render, edit, reset, limits, cache-token and forbidden-term contracts for presentation. |
| `openspec/changes/super-inventory-stage6-presentations-prices/tasks.md` | Modified | Marked assigned PR 2 tasks complete. |
| `openspec/changes/super-inventory-stage6-presentations-prices/apply-progress.md` | Modified | Persisted cumulative apply progress across PR 1 and PR 2. |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ `mvn -Dtest=SupermarketControllerTests test` → 43/43 before PR 1 edits | ✅ Tests written first; compile failed on missing presentation API | ✅ 47/47 passing | ✅ Legacy null plus DTO behavior | ✅ Clean |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 43/43 before PR 1 edits | ✅ Missing entity getters/setters | ✅ 47/47 passing | ✅ Non-null persistence and null clearing | ✅ Clean |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 43/43 before PR 1 edits | ✅ Missing JSON contract support | ✅ 47/47 passing | ✅ Legacy, valid create/update/list, validation details | ✅ Labels kept with request fields |
| 1.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 43/43 before PR 1 edits | ✅ Normalization, validation rollback and clearing tests first | ✅ 47/47 passing | ✅ Trim, positive quantity, unit requirement, label requirement, clearing, no side effects | ✅ Quantity validator extracted |
| 2.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 43/43 before PR 1 edits | ✅ Legacy and valid tests first | ✅ 47/47 passing | ✅ Legacy null and valid paths | ✅ Helpers added |
| 2.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 43/43 before PR 1 edits | ✅ Invalid rollback test first | ✅ 47/47 passing | ✅ Blank+quantity, non-positive, no unit, no label | ✅ Shared assertion helper |
| 2.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 43/43 before PR 1 edits | ✅ No-collateral-mutation test first | ✅ 47/47 passing | ✅ Checked, stock, movements, suggested list and aliases before/after | ✅ Clean |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract | ✅ `mvn -Dtest=StaticUiContractTests test` → 26/26; ✅ `node src/test/resources/static-ui-contract-tests.mjs` → passed before PR 2 edits | ✅ Tests required new inputs, table header and app token before markup changed; RED failed on missing presentation fields/header/token | ✅ `mvn -Dtest=StaticUiContractTests test` → 26/26; node static contract passed | ✅ Covered form inputs, table display column, CSS token unchanged, app token updated | ✅ No CSS refactor needed |
| 3.2 | `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract / module behavior | ✅ Same static UI safety net | ✅ Tests required new limit, payload, validation, render, edit and reset behavior before `supermarket.js` support existed; RED failed on missing limit/export | ✅ Node static contract passed; Java static test passed through embedded node runner | ✅ Covered full payload, blank omission, invalid quantity, quantity needing unit, quantity needing label, render with/without quantity, edit/reset paths | ✅ Extracted `superItemCommercialPresentationLabel` helper |
| 3.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static cache contract | ✅ Same static UI safety net | ✅ Tests expected `./supermarket.js?v=20260716-super-inventory-stage6-ui` while preserving `./api.js?v=20260716-super-inventory-stage5-api`; RED failed before app import changed | ✅ Java and node static contracts passed | ✅ Verified unrelated module tokens remain `20260713-pending-main` and API token remained Stage 5 | ✅ No refactor needed |
| 3.4 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract | ✅ Same static UI safety net | ✅ Tests asserted the presentation markup works with existing responsive `data-label` contract and unchanged CSS token; RED failed before markup existed | ✅ Java and node static contracts passed | ✅ Confirmed presentation column uses existing responsive table behavior, so no CSS edit was required | ➖ None needed — existing table/card styles covered the new column |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Java static UI contract | ✅ `mvn -Dtest=StaticUiContractTests test` → 26/26 before edits | ✅ Java contract failed until presentation allowlist/token/data-label expectations were updated and production matched them | ✅ `mvn -Dtest=StaticUiContractTests test` → 26/26 | ✅ Continued blocking `price`, `prices`, `store`, `shop`, `shops`, plural/multiple presentation and automation terms | ✅ Static guard kept surgical |
| 4.2 | `src/test/resources/static-ui-contract-tests.mjs` | Node static UI contract | ✅ `node src/test/resources/static-ui-contract-tests.mjs` → passed before edits | ✅ Node contract failed on missing `presentationLabel` limit/export before implementation | ✅ `node src/test/resources/static-ui-contract-tests.mjs` → passed | ✅ Payload/render/edit/reset, cache tokens and forbidden-term assertions covered with multiple cases | ✅ Fake DOM extended only where needed |

## Test Summary

- **Total tests written**: PR 1: 4 integration/API tests; PR 2: static contract assertions across Java and Node for presentation UI behavior.
- **Total tests passing**: 26/26 `StaticUiContractTests`; node static contract script passed; 47/47 `SupermarketControllerTests`.
- **Layers used**: Integration/API and static UI contract/module behavior.
- **Approval tests**: Static UI safety-net contracts before changing existing files.
- **Pure functions created**: 1 (`superItemCommercialPresentationLabel`).

## Tests Run

1. `mvn -Dtest=StaticUiContractTests test` — safety net before PR 2 edits: 26 tests passing.
2. `node src/test/resources/static-ui-contract-tests.mjs` — safety net before PR 2 edits: passed.
3. `mvn -Dtest=StaticUiContractTests test` — RED after tests first: expected failures on missing presentation markup/token/static contract.
4. `node src/test/resources/static-ui-contract-tests.mjs` — RED after tests first: expected failure on missing `presentationLabel` frontend limit/export.
5. `mvn -Dtest=StaticUiContractTests test` — GREEN after implementation: 26 tests passing.
6. `node src/test/resources/static-ui-contract-tests.mjs` — GREEN after implementation: passed.
7. `mvn -Dtest=SupermarketControllerTests test` — backend contract interaction confirmation: 47 tests passing.

## Deviations

- `src/main/resources/static/css/styles.css` was intentionally not modified: the new presentation column uses existing table/responsive `data-label` styling, so no new CSS was required. Therefore the `/css/styles.css` cache token in `index.html` stayed at Stage 5.
- `src/main/resources/static/js/api.js` was intentionally not modified: no payload helper change was needed. Therefore the `./api.js` import token in `app.js` stayed at Stage 5.

## Risks / Notes

- `openspec/config.yaml` is absent in the repository; Strict TDD mode was resolved from orchestrator instruction and Engram testing capabilities.
- `SupermarketControllerTests` passed with an expected logged H2 unique-index warning during a negative duplicate-alias scenario; no test failed.
- PR 1 backend/API files are still locally modified and uncommitted by prior work; this PR 2 slice adds UI/static/tests on top of that local state.

## Workload / PR Boundary

- Mode: chained PR slice.
- Current work unit: PR 2 UI/static/tests only.
- Boundary: local PR 1 backend presentation contract present → UI can capture, validate, render and edit one default commercial presentation with static contracts.
- Out of scope: prices, stores/shops, price history, multiple presentations, external lookup, automation, new endpoints, archive.
- Estimated review budget impact: focused UI/static/test delta; no CSS or API module change.
