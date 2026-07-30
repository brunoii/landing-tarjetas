# Tasks: App Shell Domain Navigation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 320-480 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Route-backed drawer + hash routing | PR 1 | `mvn test -Dtest=StaticUiContractTests,OpenSpecArtifactContractTests` | N/A — TDD contracts only | `index.html`, `navigation.js`, `app.js` route wiring |
| 2 | Stock handoff + shell/PWA migration | PR 2 | `mvn test -Dtest=StaticUiContractTests,SupermarketControllerTests` | N/A — cache/migration contracts only | `supermarket.js`, `service-worker.js`, shell asset allowlist |
| 3 | Accessibility + keyboard/focus verification | PR 3 | `node src/test/resources/static-ui-contract-tests.mjs` | N/A — simulated DOM a11y contracts | `drawer a11y/focus behavior`, `escape/close restore`, `hash restore` |

## Phase 1: Foundation / Infrastructure

- [x] 1.1 Add failing route/drawer contracts in `src/test/resources/static-ui-contract-tests.mjs` for `#monthly/summary`, `#stock/list`, and invalid-hash fallback.
- [x] 1.2 Update `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` to assert drawer trigger/ARIA structure and Summary default route.

## Phase 2: Core Implementation

- [x] 2.1 Implement route parsing/canonicalization in `src/main/resources/static/js/navigation.js` and hash application in `src/main/resources/static/js/app.js`.
- [x] 2.2 Replace `src/main/resources/static/index.html` primary tabs with one hamburger drawer and grouped Monthly/Stock links.
- [x] 2.3 Keep `src/main/resources/static/js/supermarket.js` defaulting Stock to List without resetting in-memory stock state.
- [x] 2.4 Rotate shell cache version and asset allowlist in `src/main/resources/static/service-worker.js`.

## Phase 3: Testing / Verification

- [x] 3.1 RED: add failing a11y/focus tests for opening the drawer, Escape close, and focus return in `src/test/resources/static-ui-contract-tests.mjs`.
- [x] 3.2 GREEN: make drawer interaction pass in `navigation.js` and verify `StaticUiContractTests.java` matches the new shell contract.
- [x] 3.3 RED/GREEN: add migration tests for legacy/no/invalid hashes, back/forward restoration, and stock runtime preservation.

## Phase 4: Cleanup / Documentation

- [x] 4.1 Remove obsolete primary-tab selectors/styles from `src/main/resources/static/css/styles.css` only after the new drawer contracts pass.
- [x] 4.2 Confirm no persistent storage was added for navigation state and keep the privacy-safe PWA boundary explicit in tests.
