# Tasks: Super Inventory Stage 16 Barcode Scanning

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 260-360 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: scanner UI/contracts → PR 2: scanner lifecycle/hand-off (rebased onto updated main) |
| Delivery strategy | stacked-to-main |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Add scanner controls/status/video markup and responsive styles for the barcode card; PR 1 merges to main. | PR 1 | `mvn test -Dtest=StaticUiContractTests` | N/A: static UI slice only | Revert `index.html`, `styles.css`, and UI contract edits for scan controls/preview/status |
| 2 | Add scanner lifecycle, duplicate-scan gating, alias hand-off, and explicit movement-modal actions; rebase PR 2 onto updated main. | PR 2 | `mvn test -Dtest=StaticUiContractTests` | Manual camera-supported browser check for start/stop, denied permission, and explicit action handoff | Revert scanner lifecycle and hand-off code in `supermarket.js` |

## Phase 1: Foundation / Infrastructure

- [x] 1.1 Extend `src/main/resources/static/index.html` barcode card with scan start/stop controls, hidden preview video, scanner status, and explicit purchase/consume buttons.
- [x] 1.2 Update `src/main/resources/static/css/styles.css` with responsive preview, action-group, and scan-state/focus-visible styles.
- [x] 1.3 Add `src/test/resources/static-ui-contract-tests.mjs` and `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` assertions for the new scanner markup and the preserved OCR boundary.
- [x] 1.4 Keep PR 1 limited to UI/static-contract slice so it can merge directly to `main` under the 400-line cap.

## Phase 2: Core Implementation

- [x] 2.1 Wire `src/main/resources/static/js/supermarket.js` scanner state skeleton: capability checks, idle/starting/scanning/unavailable states, and safe stop/reset hooks.
- [x] 2.2 Connect scan result handoff to the existing barcode lookup path in `src/main/resources/static/js/supermarket.js`, preserving text values and manual fallback.
- [x] 2.3 Gate resolved-item actions in `src/main/resources/static/js/supermarket.js` so purchase/consume buttons call the existing movement modal only after explicit user action.
- [x] 2.4 Rebase PR 2 onto updated `main` after PR 1 merges; keep the JS diff free of PR 1 UI-only changes.

## Phase 3: Testing / Verification

- [x] 3.1 Write RED tests in `src/test/resources/static-ui-contract-tests.mjs` for scanner UI presence, manual fallback visibility, and no OCR API drift.
- [x] 3.2 Write RED tests in `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` for the same static contract boundaries.
- [x] 3.3 Verify the first slice with `mvn test -Dtest=StaticUiContractTests` and confirm the diff stays within the PR 1 boundary.
- [x] 3.4 Verify PR 2 with the same targeted test command plus a manual secure-context camera check for start/stop, denied permission, and explicit modal handoff.

## Phase 4: Cleanup / Documentation

- [x] 4.1 Keep comments and labels in `src/main/resources/static/index.html` and `src/main/resources/static/js/supermarket.js` aligned with the manual-first, no-auto-mutation contract.
- [x] 4.2 Remove any temporary scan-layout markup or CSS experiments before handing off PR 1.
