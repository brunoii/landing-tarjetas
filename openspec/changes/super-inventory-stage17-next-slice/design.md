# Design: Super Inventory Stage 17 Next Slice

## Technical Approach

Implement `inventory-scan-session` as persisted, short-lived, browser-session-owned scan queues and editable movement drafts. Stage 16 barcode lookup (camera or manual text) remains identity resolution only; a resolved item is added to the active scan session, then explicitly converted into a purchase or consumption draft. A single confirm endpoint validates and applies all drafts in one transaction. This implements the proposal while preserving the existing direct movement modal as a separate single-item workflow.

## Architecture Decisions

| Decision | Choice | Alternative / tradeoff | Rationale |
|---|---|---|---|
| Session ownership and bounds | Persist `SuperInventoryScanSession` with the HTTP session id, `ACTIVE/CONFIRMED/EXPIRED` state, 2-hour expiry, and a 50-line limit. | Client-only queue or unbounded history. | Survives refreshes, is auditable during its useful window, and cannot become a second inventory ledger. |
| Draft boundary | Persist `SuperInventoryMovementDraft` records linked to a session and `SuperItem`; scan entries are metadata, not stock commands. | Call `/purchases` or `/consumptions` after lookup. | Lookup/manual entry cannot mutate stock; all editable intent is visible before confirmation. |
| Confirmation | `POST .../confirm` locks referenced items in ascending id order, validates every draft, writes movements with source `SCAN_SESSION`, marks the session confirmed, and commits once. | Loop through existing HTTP endpoints or partial success. | Reuses stock rules while guaranteeing all-or-nothing batch mutation and reducing lock-order deadlocks. |
| UI separation | Barcode results add to the scan session; the new session panel owns draft edit/remove/confirm. Existing `#super-movement-modal` remains available from item-table actions only. | Reuse/overload the modal. | Makes batch review distinguishable from the established immediate movement flow. |

## Data Flow

    camera/manual barcode -> existing alias lookup -> active server session
                                      |                 |
                                      |             add/edit/remove drafts
                                      v                 v
                              manual fallback      explicit confirm
                                                        |
                           locked items -> validation -> stock movements + session CONFIRMED

`GET`/create active session, lookup, queueing, and draft editing never change `currentStock`, `checked`, or movement history. On expiry, wrong owner, invalid/inactive item, unknown stock, invalid quantity, or unapproved negative consumption, confirmation fails with no mutation; drafts remain editable unless the session is expired. Confirming an already-confirmed session is rejected idempotently (no duplicate movements).

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperInventoryScanSession*.java` | Create | Session entity, draft entity, repositories, DTOs, validation requests/responses, and controller. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperInventoryScanSessionService.java` | Create | Ownership, expiry, bounded queue/draft lifecycle, and transactional confirmation. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modify | Expose/reuse stock-command validation and movement persistence for batch confirmation without changing direct endpoints. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRepository.java` | Modify | Add deterministic pessimistic-lock batch lookup. |
| `src/main/resources/static/js/api.js` | Modify | Add scan-session lifecycle, draft, and confirm API helpers. |
| `src/main/resources/static/js/supermarket.js` | Modify | Route resolved camera/manual aliases into the session; render and operate the separate draft panel. |
| `src/main/resources/static/index.html` | Modify | Add accessible queue/draft review and confirm controls beside barcode UI, not inside the movement modal. |
| `src/main/resources/static/css/styles.css` | Modify | Add responsive queue/draft status and review styles. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Cover lifecycle, ownership/bounds, no-mutation paths, confirmation, rollback, and locking. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Assert panel/modal separation and API boundaries. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Add payload, rendering, manual fallback, and confirm-only client contracts. |

## Interfaces / Contracts

```text
GET/POST /api/super/scan-sessions/active
POST     /api/super/scan-sessions/{id}/resolved-items   { itemId, barcodeCode? }
POST     /api/super/scan-sessions/{id}/drafts           { itemId, type, quantity, notes?, allowNegativeStock? }
PUT      /api/super/scan-sessions/{id}/drafts/{draftId} { type, quantity, notes?, allowNegativeStock? }
DELETE   /api/super/scan-sessions/{id}/drafts/{draftId}
POST     /api/super/scan-sessions/{id}/confirm
```

Draft `type` is `PURCHASE` or `CONSUMPTION`; quantities use the existing positive decimal rules. Confirmation returns the confirmed session plus movement summaries. Direct `/api/super/items/{id}/purchases` and `/consumptions` remain unchanged.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Controller/service | owner isolation, expiry, 50-line cap, validation, duplicate confirm, known-stock and negative-stock behavior | MockMvc/JPA RED tests; assert drafts/lookups create no movements or stock changes. |
| Transaction | mixed drafts, item lock order, failure after a valid line | RED test asserts every item stock, movement count, and session state roll back together. |
| Static UI | manual lookup fallback, resolved-to-session handoff, editable drafts, explicit confirm, modal separation | Extend Node and Java static contracts; retain Stage 16 camera cleanup tests. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

Hibernate `ddl-auto=update` creates additive session/draft tables. No backfill or feature flag is required. Roll back by removing the new endpoints/UI/tables; existing aliases and direct movement commands are untouched.

## Open Questions

- [ ] None.
