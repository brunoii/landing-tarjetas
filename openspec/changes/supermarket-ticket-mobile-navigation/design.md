# Design: Supermarket Ticket Mobile Navigation

## Technical Approach

Extend the existing Spring OCR boundary and framework-free static UI; do not replace Tess4J or alter inventory APIs. Map validation, decode, runtime/readiness, and empty-extraction outcomes to stable OCR codes/messages, then render them with retry/manual-review guidance. Reorganize the current stacked supermarket panel into local sub-tabs (list, barcode, tickets, categories) using the existing ARIA-tab pattern. Receipt evidence remains transient and network-only.

## Architecture Decisions

| Decision | Alternatives considered | Rationale |
|---|---|---|
| Add a typed OCR failure contract at the service/API boundary | Infer stage from free-text errors; expose Tess4J details | Stable codes prevent UI ambiguity and keep paths/runtime details private. |
| Treat zero usable candidates as `EMPTY_EXTRACTION` | Present a successful blank review; fail generically | It identifies a distinct recoverable OCR outcome and keeps manual entry visible. |
| Use in-memory sub-tab state and existing tab semantics | New router, drawer, or persisted preference | The app already uses DOM/ARIA tabs; this is non-mutating, accessible, and avoids new storage. |
| Keep JPG verification operator-local | Fixtures, automated uploads, cache/localStorage, server persistence | Authorized images are sensitive evidence and are explicitly prohibited from transmission or persistence. |

## Data Flow

```text
JPG/PNG selected → TicketOcrService validation/decode → Tess4J/parser
       │                         │                         │
       └─ local-only policy      └─ OCR outcome code  ← candidates/empty/runtime
                                                   ↓
API error/response → api.js mapping → supermarket.js feedback + retry/manual review

Sub-tab button → navigation.js state → selected supermarket panel (DOM only)
```

The upload remains a single request and is not stored by the application. The existing service worker's `/api/` and ticket network-only rules remain authoritative; no receipt payload, checksum-derived review, or OCR response enters Cache Storage, localStorage, IndexedDB, logs, or fixtures. A confirmed price observation remains the only existing persistence path and requires explicit user confirmation.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modify | Classify validation, decode, and empty extraction outcomes without data leakage. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` | Modify | Preserve sanitized readiness warnings and expose a stable runtime outcome. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrResponse.java` | Modify | Add an outcome/readiness field for successful-but-empty and recoverable responses. |
| `src/main/resources/static/js/api.js` | Modify | Replace the PDF-specific fallback for ticket OCR with a safe OCR-stage message. |
| `src/main/resources/static/js/supermarket.js` | Modify | Map OCR outcomes to visible retry/manual-review feedback and initialize sub-tabs. |
| `src/main/resources/static/js/navigation.js` | Modify | Reuse/export tab-state mechanics for supermarket sub-tabs. |
| `src/main/resources/static/index.html` | Modify | Group existing surfaces into list, barcode, tickets, and categories tab panels; retain IDs and manual controls. |
| `src/main/resources/static/css/styles.css` | Modify | Add compact, wrapping mobile sub-tab controls without horizontal overflow. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Cover API outcome mapping and no-persistence behavior. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Cover tab reachability, selection, and manual fallback visibility. |

## Interfaces / Contracts

```java
enum TicketOcrOutcome {
  READY, INVALID_FILE, DECODE_FAILED, RUNTIME_UNAVAILABLE, EMPTY_EXTRACTION
}

record TicketOcrResponse(TicketOcrOutcome outcome, /* existing transient fields */) {}
```

`INVALID_FILE` and `DECODE_FAILED` are safe 400 responses; `RUNTIME_UNAVAILABLE` and `EMPTY_EXTRACTION` are safe, non-persistent OCR responses with manual-review guidance. UI tab changes only toggle visibility/ARIA state and MUST NOT call mutating inventory endpoints.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Stage classification and sanitized runtime/empty outcomes | Extend Tess4J/service tests; assert no raw paths or receipt data. |
| Integration | Multipart API and persistence boundary | MockMvc tests for invalid, corrupt, runtime, empty, and candidate cases; assert repositories/logs remain clean. |
| UI contract | Four sub-tabs, keyboard/ARIA state, compact no-overflow layout, visible manual fallback | Extend static Node contract tests; no external JPG fixture. |
| Manual local verification | Authorized JPG behavior | Operator selects files only from the authorized folder; do not copy, upload, cache, log, persist, or version them. Record only pass/fail observations. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. In-app DOM tab selection is not application routing.

## Migration / Rollout

No migration required. Roll back by restoring the single supermarket pane and prior OCR presentation; inventory data and endpoints are unchanged.

## Open Questions

- [ ] Should the API represent runtime unavailability as HTTP 200 with `RUNTIME_UNAVAILABLE` or a safe 503 error envelope? Decide before implementation so frontend and tests share one contract.
