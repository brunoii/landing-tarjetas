# Apply Progress: super-inventory-stage4-barcode-ocr

## Scope

PR 1 backend/API/tests slice completed previously. This run completed the PR 2 UI/static contracts slice only: tasks 3.1-3.4.

## Status

partial — PR 1 backend/API/tests and PR 2 UI/static tasks are complete; final verification tasks 4.1-4.3 remain pending.

## Completed Tasks

- [x] 1.1 Backend tests for lookup found/not found, leading zeros, attach, soft remove, inactive alias/item.
- [x] 1.2 Backend invariant tests proving lookup/attach/remove preserve `currentStock`, `checked`, and stock movement rows.
- [x] 1.3 Backend duplicate/schema tests for active uniqueness, inactive duplicate reuse, and DB race conflict translation.
- [x] 1.4 Backend validation tests for blank/oversized `code` and `format` using `SupermarketLimits` constants.
- [x] 1.5 Backend DELETE ownership test for aliases from another item.
- [x] 2.1 Created `SuperItemBarcodeAlias` and repository with text code, optional format, active flag, nullable unique `activeCode`, item FK, and timestamps.
- [x] 2.2 Added `BARCODE_CODE_MAX_LENGTH` and `BARCODE_FORMAT_MAX_LENGTH` to `SupermarketLimits` and used them in entity/request validation.
- [x] 2.3 Created alias request/response and lookup response DTOs with text-only code fields.
- [x] 2.4 Extended `SupermarketService` with trim-only normalization, active item checks, duplicate precheck plus DB-conflict 409 handling, and item-owned soft deactivate.
- [x] 2.5 Created barcode alias controller for lookup, attach, and remove without invoking stock movement methods.
- [x] 3.1 Updated `StaticUiContractTests.java` and `static-ui-contract-tests.mjs` with RED contracts for Stage 4 UI helpers, manual fallback, attach/remove, limits, cache tokens, and narrowed blockers.
- [x] 3.2 Added `api.js` helpers for lookup/attach/remove alias and updated Stage 4 cache-token imports in `app.js` and `supermarket.js`.
- [x] 3.3 Implemented manual-first barcode lookup/attach/remove in `supermarket.js`, including string-only code handling, found-row highlight, existing-item association, and no barcode stock/checked/movement mutation.
- [x] 3.4 Added the manual barcode panel markup/styles in `index.html` and `styles.css` with no camera, `BarcodeDetector`, `getUserMedia`, OCR, external lookup, prices, stores, presentations, suggestions, or purchase automation.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` — 32/32 passing before edits | ✅ Compile RED: missing alias entity/repository/limits | ✅ Focused suite passing: 38/38 | ✅ Found + not-found + attach + soft remove + inactive item | ✅ Kept behavior in API-level contract |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 32/32 baseline | ✅ New invariant test failed before API existed | ✅ 38/38 passing | ✅ Lookup, attach, found lookup, and delete all assert stock/checked/movement invariants | ✅ Shared invariant helper extracted |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API/JPA integration | ✅ 32/32 baseline | ✅ Duplicate/schema tests referenced missing alias model | ✅ 38/38 passing | ✅ Service precheck conflict, direct DB unique constraint, inactive duplicate reuse, mocked DB race | ✅ Switched deprecated `@SpyBean` to `@MockitoSpyBean` |
| 1.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 32/32 baseline | ✅ Validation tests referenced missing limits | ✅ 38/38 passing | ✅ Blank code, oversized code, oversized format | ✅ Reused explicit limit constants in assertions |
| 1.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 32/32 baseline | ✅ DELETE ownership test referenced missing alias endpoint/model | ✅ 38/38 passing | ✅ Alias owner item vs different item path | ✅ Preserved alias active state assertion |
| 2.1 | Same focused suite | JPA/API implementation | Covered by RED tasks 1.1/1.3 | ✅ Tests demanded entity/repository/schema | ✅ 38/38 passing | ✅ Active and inactive alias paths covered | ✅ Entity lifecycle kept compact |
| 2.2 | Same focused suite | Validation/schema | Covered by RED task 1.4 | ✅ Tests demanded constants | ✅ 38/38 passing | ✅ Code and format limits covered | ➖ None needed |
| 2.3 | Same focused suite | API contract | Covered by RED task 1.1 | ✅ Tests demanded response shapes | ✅ 38/38 passing | ✅ Found/not-found/attach response variants covered | ➖ None needed |
| 2.4 | Same focused suite | Service integration | Covered by RED tasks 1.1-1.5 | ✅ Tests demanded normalization, conflict, active item and ownership behavior | ✅ 38/38 passing | ✅ Duplicate precheck plus DB race, lookup ignore inactive alias, active item enforcement | ✅ Kept barcode logic separate from stock commands |
| 2.5 | Same focused suite | Controller/API integration | Covered by RED tasks 1.1-1.5 | ✅ Tests demanded new endpoints | ✅ 38/38 passing | ✅ GET lookup, POST attach, DELETE deactivate | ➖ None needed |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + Node integration | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing; ✅ Node script passing before edits | ✅ New Stage 4 UI/static assertions failed on missing cache tokens, markup, helpers, exported pure functions, and barcode UI behavior | ✅ `mvn -Dtest=StaticUiContractTests test` and Node script passed | ✅ API helper calls, found/not-found lookup, attach/remove, limits, leading-zero string handling, and blocker terms covered | ✅ Kept blockers narrow: local barcode allowed; OCR/camera/external/price/store/presentation/suggestion automation still rejected |
| 3.2 | Same focused suite | Static API contract | ✅ Same baseline as 3.1 | ✅ Expected `lookupSuperItemBarcodeAlias`, `attachSuperItemBarcodeAlias`, `removeSuperItemBarcodeAlias`, and Stage 4 cache tokens were absent | ✅ Focused static suite and Node script passed | ✅ GET lookup with leading-zero code, POST attach payload, DELETE remove endpoint all asserted | ➖ None needed |
| 3.3 | `src/test/resources/static-ui-contract-tests.mjs` | Node DOM interaction contract | ✅ Same baseline as 3.1 | ✅ Fake DOM expected lookup found highlight, not-found attach, remove alias, and no stock/checked/movement calls before implementation | ✅ Node script passed after implementation | ✅ Found alias, not-found manual association, remove alias, validation, and code normalization paths covered | ✅ Extracted pure helpers for normalization/payload/validation/labeling |
| 3.4 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static markup/CSS contract | ✅ Same baseline as 3.1 | ✅ Markup/style/cache-token assertions failed before panel/styles existed | ✅ Focused static suite and Node script passed | ✅ Panel fields, responsive limits, item selector, action buttons, highlight style, and no camera/OCR/external terms covered | ➖ None needed |

## Verification Evidence

- Previous backend slice evidence remains valid from PR 1: focused backend suite passed 38/38 after implementation.
- Baseline before PR 2 UI edits: `mvn -Dtest=StaticUiContractTests test` PASS — 26 tests, 0 failures, 0 errors.
- Baseline before PR 2 UI edits: `node src/test/resources/static-ui-contract-tests.mjs` PASS.
- RED after UI/static test edits: `mvn -Dtest=StaticUiContractTests test` FAIL as expected because Stage 4 tokens, barcode panel, API helpers, and UI functions were absent.
- RED after UI/static test edits: `node src/test/resources/static-ui-contract-tests.mjs` FAIL as expected because barcode field limits/helpers/DOM behavior were absent.
- GREEN after UI implementation: `mvn -Dtest=StaticUiContractTests test` PASS — 26 tests, 0 failures, 0 errors.
- GREEN after UI implementation: `node src/test/resources/static-ui-contract-tests.mjs` PASS.
- Final required run: `mvn -Dtest=StaticUiContractTests test` PASS — 26 tests, 0 failures, 0 errors.
- Final required run: `node src/test/resources/static-ui-contract-tests.mjs` PASS.
- Feasible integration run: `mvn "-Dtest=SupermarketControllerTests,StaticUiContractTests" test` PASS — 65 tests, 0 failures, 0 errors.
- Initial unquoted PowerShell attempt for the combined Maven selector failed before running tests because `,` was parsed as a parameter-list separator; reran with the `-Dtest` argument quoted successfully.
- `git diff --check`: PASS with line-ending warnings only.

## Review / PR Boundary

- Delivery strategy: auto-chain.
- Chain strategy: feature-branch-chain.
- Current work unit: PR 2 — UI/static contracts only, based on PR 1 backend/API/tests.
- Boundary: starts from completed local alias API contracts and ends with manual-first static UI lookup/attach/remove; no backend broadening beyond consuming existing endpoints; no camera, OCR, external lookup, prices, stores, presentations, suggestions, or purchase automation.

## Deviations

- None for the assigned UI/static slice. Implementation follows the design: manual input/clipboard-first local barcode aliases over existing `SuperItem`, no camera dependency, and barcode UI does not call stock, checked, or movement commands.

## Remaining Tasks

- [ ] 4.1 Run `mvn -Dtest=SupermarketControllerTests test`.
- [ ] 4.2 Run `mvn -Dtest=StaticUiContractTests test` and `node src/test/resources/static-ui-contract-tests.mjs`.
- [ ] 4.3 Run `mvn test`, inspect `git diff --stat`, and keep PR 1/PR 2 under the 800-line review budget when possible.

## Risks / Notes

- Expected H2 duplicate-key logging appears during the schema test that intentionally asserts the `active_code` unique constraint.
- `openspec/config.yaml` and change `state.yaml` were not present in the workspace; readiness was resolved from the injected apply prompt plus the OpenSpec artifacts and Engram artifacts.
- The UI remove action can only remove the alias currently returned by lookup or created by attach because the backend slice does not expose per-item alias lists.
