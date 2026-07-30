# Tasks: Supermarket Subtabs Mobile Navigation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 260-420 |
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
| 1 | Add failing UI/ARIA coverage for nested tabs and mobile reachability | PR 1 | `npm test -- supermarket-subtabs-mobile-navigation` | N/A — tests only | Remove new supermarket navigation tests |
| 2 | Implement reusable tab controller and regroup supermarket markup/CSS | PR 2 | `npm test -- supermarket-subtabs-mobile-navigation` | Open supermarket page in mobile viewport | Revert `navigation.js`, `index.html`, `styles.css` changes |
| 3 | Wire supermarket defaults/focus handoff and verify preserved flows | PR 3 | `npm test -- supermarket-subtabs-mobile-navigation` | Click List/Barcode/Tickets/Categories on desktop and mobile | Revert `supermarket.js` wiring and verification tests |

## Phase 1: Test Contract Foundation

- [x] 1.1 Add failing coverage for `src/main/resources/static/index.html` nested tab roles, `aria-controls`, and one-active-panel semantics.
- [x] 1.2 Add failing coverage for compact mobile navigation reachability and no overflow-only access in `src/main/resources/static/css/styles.css`.
- [x] 1.3 Add failing coverage for preserved List/Barcode/Tickets/Categories controls after regrouping, including manual barcode and ticket/category reachability.

## Phase 2: RED Tests for Behavior Preservation

- [x] 2.1 Add keyboard/focus RED tests for `src/main/resources/static/js/navigation.js` tab switching, roving `tabIndex`, and predictable focus movement.
- [x] 2.2 Add RED tests for `src/main/resources/static/js/supermarket.js` defaulting to List without clearing scan-session, OCR-review, or ticket state.
- [x] 2.3 Add RED tests for desktop/mobile section switching that keep existing supermarket handlers reachable by stable IDs.

## Phase 3: GREEN Implementation

- [x] 3.1 Refactor `src/main/resources/static/js/navigation.js` into a reusable `setupTabs()` path and initialize supermarket subtabs with List default.
- [x] 3.2 Reorganize `src/main/resources/static/index.html` into four `role="tabpanel"` supermarket panels while preserving IDs and form structure.
- [x] 3.3 Update `src/main/resources/static/css/styles.css` for compact wrapping subtabs and retire the quick-link shell.
- [x] 3.4 Adjust `src/main/resources/static/js/supermarket.js` only for default-subtab and focus handoff integration after regrouping.

## Phase 4: Verification / Cleanup

- [x] 4.1 Run the supermarket subtab test suite and confirm keyboard activation, mobile reachability, and preserved flows pass.
- [x] 4.2 Manually verify List, Barcode, Tickets, and Categories on a mobile viewport with no horizontal-trap overflow.
- [x] 4.3 Remove any obsolete quick-link references or dead CSS selectors left behind by the regrouping.
