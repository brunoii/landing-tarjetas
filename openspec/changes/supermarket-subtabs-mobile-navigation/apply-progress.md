# Apply Progress: Supermarket Subtabs Mobile Navigation

**Change**: `supermarket-subtabs-mobile-navigation`
**Mode**: Strict TDD
**Artifact store**: hybrid
**Delivery slice**: PR 3 — default-state preservation, primary-tab handoff, and mobile viewport evidence

## Completed Tasks

- [x] 1.1 Add failing coverage for `src/main/resources/static/index.html` nested tab roles, `aria-controls`, and one-active-panel semantics.
- [x] 1.2 Add failing coverage for compact mobile navigation reachability and no overflow-only access in `src/main/resources/static/css/styles.css`.
- [x] 1.3 Add failing coverage for preserved List/Barcode/Tickets/Categories controls after regrouping, including manual barcode and ticket/category reachability.
- [x] 2.1 Add keyboard/focus RED tests for `src/main/resources/static/js/navigation.js` tab switching, roving `tabIndex`, and predictable focus movement.
- [x] 2.2 Add RED tests for `src/main/resources/static/js/supermarket.js` defaulting to List without clearing scan-session, OCR-review, or ticket state.
- [x] 2.3 Add RED tests for desktop/mobile section switching that keep existing supermarket handlers reachable by stable IDs.
- [x] 3.1 Refactor `src/main/resources/static/js/navigation.js` into a reusable `setupTabs()` path and initialize supermarket subtabs with List default.
- [x] 3.2 Reorganize `src/main/resources/static/index.html` into four `role="tabpanel"` supermarket panels while preserving IDs and form structure.
- [x] 3.3 Update `src/main/resources/static/css/styles.css` for compact wrapping subtabs and retire the quick-link shell.
- [x] 3.4 Adjust `src/main/resources/static/js/supermarket.js` only for default-subtab and focus handoff integration after regrouping.
- [x] 4.1 Run the supermarket subtab test suite and confirm keyboard activation, mobile reachability, and preserved flows pass.
- [x] 4.2 Manually verify List, Barcode, Tickets, and Categories on a mobile viewport with no horizontal-trap overflow.
- [x] 4.3 Remove any obsolete quick-link references or dead CSS selectors left behind by the regrouping.

## Boundary Notes

- This PR 3 slice stays inside the stacked-to-main supermarket state-preservation and verification boundary.
- Existing supermarket IDs, form structure, API calls, scan-session controls, OCR review controls, and category/product handlers remain unchanged.
- `supermarket.js` only adds a primary-tab return-to-List handoff; it does not reload or reset supermarket module state.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ `mvn -Dtest=StaticUiContractTests test` → 31/31 passing baseline | ✅ Added failing template/ARIA assertions first; focused run failed because `supermarket-subtabs-contract` and its single-active tabpanel contract did not exist | ✅ `mvn -Dtest=StaticUiContractTests test` → 34/34 passing | ✅ Covered tablist ownership plus one-active-tab / one-visible-panel semantics as separate expectations | ✅ Kept the foundation inert by using a non-rendered contract template instead of live DOM regrouping |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ Same 31-test baseline | ✅ Added failing mobile reachability and no-overflow-only selector assertions before CSS changes existed | ✅ `mvn -Dtest=StaticUiContractTests test` → 34/34 passing | ✅ Covered base selector contract and mobile-media flex behavior separately so one declaration could not satisfy the whole requirement | ➖ None needed |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ Same 31-test baseline | ✅ Added failing stable-control mapping assertions before the future-panel contract metadata existed | ✅ `mvn -Dtest=StaticUiContractTests test` → 34/34 passing | ✅ Verified four distinct panel-to-control mappings for List, Barcode, Tickets, and Categories | ➖ None needed |
| 2.1 | `src/test/resources/static-ui-contract-tests.mjs` | JS DOM harness | ✅ `mvn -Dtest=StaticUiContractTests test` → 34/34 passing baseline | ✅ Added failing `setupTabs()` keyboard assertions for Arrow/Home/End focus movement and roving `tabIndex` before the reusable controller existed | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Covered click activation plus three keyboard paths so a single hardcoded branch could not pass | ✅ Extracted a generic tab setup path while keeping `setupPrimaryTabs()` as the primary entrypoint |
| 2.2 | `src/test/resources/static-ui-contract-tests.mjs` | JS DOM harness | ✅ Same 34-test baseline | ✅ Added a failing primary-tab return scenario first; the focused run failed because re-entering supermarket did not restore List or preserve the live barcode/OCR review DOM state | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Preserved both barcode session summary and OCR review content after the List handoff instead of checking only one state bucket | ✅ Converted the OCR assertion to an approval-style “same summary / same selected line” check so the test preserves current behavior instead of inventing a new ticket-selection rule |
| 2.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + JS DOM harness | ✅ Same 34-test baseline | ✅ Replaced inert-contract expectations with failing live-markup grouping/order assertions before DOM relocation | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Verified List/Barcode/Tickets/Categories markup grouping, hidden-panel semantics, and stable ID reachability across desktop/mobile contracts | ✅ Removed obsolete quick-link shell expectations once live grouped panels proved the preserved flows |
| 3.1 | `src/test/resources/static-ui-contract-tests.mjs` | JS DOM harness | ✅ Same 34-test baseline | ✅ `setupTabs()` tests failed first because the reusable API and supermarket initialization path did not exist | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Exercised primary tabs and supermarket subtabs through different dataset keys and default targets | ✅ Consolidated duplicated tab activation logic into one reusable controller |
| 3.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + JS DOM harness | ✅ Same 34-test baseline | ✅ Live panel-order and ARIA assertions failed while the supermarket content was still a single long panel | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Covered all four panels with preserved controls instead of only a single happy-path section | ✅ Regrouped markup without renaming existing supermarket IDs or changing handler targets |
| 3.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ Same 34-test baseline | ✅ Added failing compact tab-button and shell-removal CSS expectations before the live styles existed | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Covered base wrap layout, active-state styling, and 680px compact behavior separately | ✅ Removed the quick-link selectors instead of layering a second mobile navigation system |
| 3.4 | `src/test/resources/static-ui-contract-tests.mjs` | JS DOM harness | ✅ Same 34-test baseline | ✅ The new return-to-supermarket RED case failed before `supermarket.js` wired the primary tab back to List | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Reused the same scenario to prove both focus handoff and state preservation across barcode + OCR tabs | ✅ Kept the production change minimal by adding a tiny activation helper instead of changing data-loading paths |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Focused verification | ✅ Prior PR2-focused runs already green | ✅ Verification task uses the RED evidence above; no new production test needed | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ➖ Verification-only slice | ➖ None needed |
| 4.2 | `src/test/resources/static-ui-contract-tests.mjs` + headless Edge viewport evidence | Runtime verification | ✅ Same 34-test baseline | ✅ Reused the mobile reachability / no-overflow contracts plus the return-to-List state-preservation RED before gathering viewport evidence | ✅ `msedge --headless=new --disable-gpu --window-size=390,844 --screenshot="C:\Users\BIIbr\AppData\Local\Temp\opencode\supermarket-mobile-390x844.png" http://127.0.0.1:4173/index.html` → wrote `227012` bytes; `--dump-dom` on the same viewport confirmed the four supermarket subtab buttons and grouped panels in the rendered document | ✅ Cross-checked viewport evidence with focused runtime tab switching in `node src/test/resources/static-ui-contract-tests.mjs` so screenshot-only evidence could not hide broken tab behavior | ➖ Used best-effort headless Edge evidence because no interactive browser MCP/manual device is available in this workspace |
| 4.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ Same 34-test baseline | ✅ Added failing assertions that the quick-link shell/template references were gone before deleting them | ✅ `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 | ✅ Checked both markup and CSS removals so partial cleanup would fail | ➖ None needed |

## Test Summary

- **Total tests written**: 8
- **Total tests passing**: 34
- **Layers used**: Static UI contract (34), JS DOM harness (embedded in the same focused suite), runtime viewport evidence (headless Edge)
- **Approval tests**: 3 behavior-preservation updates for live supermarket markup/navigation/state handoff
- **Pure functions created**: 0

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `mvn -Dtest=StaticUiContractTests test` → BUILD SUCCESS, Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 |
| Runtime harness command/scenario and exact result | `node src/test/resources/static-ui-contract-tests.mjs` → `exit=0`; exercised reusable tab keyboard switching, primary-tab return-to-List handoff, barcode session persistence, OCR review persistence, and preserved supermarket flows (console includes the expected degraded price-source warnings from the negative-path harness) |
| Rollback boundary | Revert only `src/main/resources/static/js/supermarket.js`, `src/test/resources/static-ui-contract-tests.mjs`, and this change folder’s `tasks.md` / `apply-progress.md` to remove the PR 3 state-handoff slice without disturbing the prior PR1/PR2 tab markup/CSS work |

## Mobile Viewport Evidence

- `msedge --headless=new --disable-gpu --window-size=390,844 --screenshot="C:\Users\BIIbr\AppData\Local\Temp\opencode\supermarket-mobile-390x844.png" http://127.0.0.1:4173/index.html` → screenshot captured at mobile width (`227012` bytes).
- `msedge --headless=new --disable-gpu --window-size=390,844 --dump-dom http://127.0.0.1:4173/index.html` → rendered DOM showed the four supermarket subtab buttons and grouped `super-panel-list|barcode|tickets|categories` sections at the same viewport.
- This is best-effort mobile verification evidence. An actual manual tap-through was not possible in this workspace because no interactive browser MCP/device session is available.

## Files Changed

| File | Action | Notes |
|---|---|---|
| `src/main/resources/static/js/supermarket.js` | Modified | Added a minimal primary-tab click handoff that re-activates and focuses the List supermarket subtab without resetting barcode/OCR/session state. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Added RED→GREEN coverage for primary-tab return-to-List behavior, barcode/OCR state preservation, and fake DOM support for supermarket subtab focus assertions. |
| `openspec/changes/supermarket-subtabs-mobile-navigation/tasks.md` | Modified | Marked the final PR 3 tasks complete. |
| `openspec/changes/supermarket-subtabs-mobile-navigation/apply-progress.md` | Modified | Recorded cumulative PR 1 + PR 2 + PR 3 progress, TDD evidence, viewport evidence, and the final rollback boundary. |

## Status

- Completed this slice: 13 / 13 tasks
- Remaining for next batch: None
- Ready for verify
