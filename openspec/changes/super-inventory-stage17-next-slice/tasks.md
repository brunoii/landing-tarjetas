# Tasks: Super Inventory Stage 17 Next Slice

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 320-520 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → session backend/API, PR 2 → UI wiring, PR 3 → verification/docs |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Add scan-session entity/service/controller and persistence rules | PR 1 | `mvn test -Dtest=SupermarketControllerTests` | N/A — server-side contract slice only | `SuperInventoryScanSession*`, service, repository, controller |
| 2 | Wire scan-session queueing + draft review into static UI | PR 2 | `npm test -- static-ui-contract-tests.mjs` | Open the supermarket UI and queue/edit/remove a draft manually | `api.js`, `supermarket.js`, `index.html`, `styles.css` |
| 3 | Lock in RED→GREEN verification for confirm-only mutation and rollback safety | PR 3 | `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` | Confirm one valid draft batch and verify no mutation before confirm | Tests and any final cleanup/docs only |

## Phase 1: Foundation / Infrastructure

- [x] 1.1 Add failing controller/service tests for active-session ownership, expiry, 50-line cap, and no-mutation queueing in `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`.
- [x] 1.2 Define session/draft persistence and request/response DTOs in new `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperInventoryScanSession*.java` files.
- [x] 1.3 Add repository support for session lookup and ordered lock-friendly item access in `SuperItemRepository.java`.

## Phase 2: Core Implementation

- [x] 2.1 Implement `SuperInventoryScanSessionService` to create/own/expire sessions and enqueue resolved items without touching stock.
- [x] 2.2 Implement draft create/update/delete and bounded-session validation; keep `currentStock`, `checked`, and movement history unchanged.
- [x] 2.3 Implement `POST .../confirm` with explicit atomic validation, ascending-id locking, and idempotent confirmed-session rejection.

## Phase 3: Integration / Wiring

- [x] 3.1 Add scan-session API helpers in `src/main/resources/static/js/api.js`.
- [ ] 3.2 Route resolved camera/manual aliases into the session and render the new review panel in `src/main/resources/static/js/supermarket.js`.
- [ ] 3.3 Add accessible session/draft controls in `src/main/resources/static/index.html` and `src/main/resources/static/css/styles.css`.

## Phase 4: Testing / Verification

- [ ] 4.1 Add RED tests for draft edit/remove/cancel remaining non-mutating and confirm applying existing movement rules only.
- [ ] 4.2 Add static contract tests in `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` and `src/test/resources/static-ui-contract-tests.mjs`.
- [ ] 4.3 Verify session expiry, wrong-owner rejection, invalid stock, negative-stock denial, and rollback-on-failure scenarios.

## Phase 5: Cleanup / Documentation

- [ ] 5.1 Remove obsolete UI coupling to the direct movement modal where the new scan session panel owns batch review.
- [ ] 5.2 Update inline comments or docs only where they clarify confirm-only mutation and preserve the existing direct movement flow.
