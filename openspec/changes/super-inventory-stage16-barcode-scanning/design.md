# Design: Super Inventory Stage 16 Barcode Scanning

## Technical Approach

Add a client-only progressive scanner to the existing barcode card. `supermarket.js` owns a bounded camera lifecycle, copies a detected barcode into the existing text field, and invokes the existing alias lookup path. The current manual lookup/attach path remains the canonical fallback. A resolved item exposes explicit purchase and consumption handoffs to the existing movement modal; scanning never calls a movement endpoint.

## Architecture Decisions

| Decision | Choice | Alternative / tradeoff | Rationale |
|---|---|---|---|
| Scanner boundary | `BarcodeDetector` plus `getUserMedia` in `supermarket.js` only | New backend scan session adds persistence and API risk | The static UI already owns alias lookup and needs no new server contract. |
| Availability | Detect `globalThis.BarcodeDetector`, `navigator.mediaDevices?.getUserMedia`, and secure context before enabling scan | Hide the feature entirely | Visible unavailable feedback and manual entry preserve a usable path. |
| Recognition | Treat `rawValue` as text; trim only through `normalizeSuperBarcodeCode`; retain detector format when present | Numeric parsing | Preserves leading zeroes and existing alias semantics. |
| Duplicate safety | One in-flight lookup plus last-code/time debounce; pause recognition after accepted code | Continuous lookup or auto-action | Prevents repeated reads/network calls and keeps stock mutation explicit. |
| Movement | Result buttons call `openSuperMovementModal("purchase"|"consume", item.id)` | Direct purchase/consume requests | Existing modal supplies quantity, confirmation, and negative-stock safeguards. |

## Data Flow

    Scan button → capability/permission check → camera stream + video
        → detector loop → normalized text input → existing alias lookup
        → found item / attach selector → explicit movement modal → existing API

Scanner state is in-memory only: `idle`, `starting`, `scanning`, `resolving`, `unavailable`, `denied`, `error`. It retains `stream`, animation-frame handle, detector, `lastCode`, and `lastAcceptedAt`. Start creates the stream with rear-camera preference; stop cancels the loop, stops every track, clears video `srcObject`, and returns to `idle`. Stop on explicit Stop, accepted scan, setup re-entry, and `pagehide`/visibility loss. Permission denial, unavailable APIs, or camera errors stop safely, announce the reason, focus the manual code field, and leave lookup/attach enabled.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/index.html` | Modify | Add scan/start-stop controls, hidden video preview, scanner status, and resolved-item purchase/consume controls to the barcode card. |
| `src/main/resources/static/js/supermarket.js` | Modify | Add isolated scanner state/lifecycle, capability checks, detector loop, text handoff, action gating, and cleanup; reuse alias and modal functions. |
| `src/main/resources/static/css/styles.css` | Modify | Add responsive preview, status, action-group, and focus-visible styles. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Add scanner unit/static contracts and keep OCR free of camera/detector APIs. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Assert scanner markup/static ownership and unchanged API/OCR boundaries. |

## Delivery / Review Boundaries

Delivery uses the user-selected `stacked-to-main` strategy; no tracker PR, feature-branch chain, single-PR delivery, or `size:exception` is permitted. PR 1 targets and merges to `main`: scanner markup, responsive styles, and their static contracts. After PR 1 merges, PR 2 rebases onto updated `main`, targets `main`, and contains scanner lifecycle, alias/action handoff, and the corresponding JS/static regression tests. Each PR must remain at or below 400 changed lines (`additions + deletions`), contain verification with its work unit, and be independently rollbackable. Rebase/retarget before review whenever a PR diff includes an already-merged slice.

## Interfaces / Contracts

No backend, persistence, or `api.js` changes. Existing contracts remain authoritative:

```js
lookupSuperItemBarcodeAlias(code)
attachSuperItemBarcodeAlias(itemId, { code, format })
openSuperMovementModal("purchase" | "consume", itemId)
```

The scanner accepts detector output `{ rawValue, format }`, validates through `superBarcodePayloadFromValues` / `validateSuperBarcodeLookup`, and feeds `submitSuperBarcodeLookup`. It MUST NOT invoke OCR APIs, create products, alter `checked`/`currentStock`, or call purchase/consumption APIs without modal confirmation.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| JS unit/static | Capability matrix, unavailable/denied fallback, track cleanup, leading-zero text, invalid/overlong scans, debounce, in-flight gate, accepted-scan pause | Mock detector/media stream and assert existing lookup receives text once; RED tests precede implementation. |
| UI contract | Named controls/video/status, accessible labels/live status, keyboard manual fallback, result action handoff, responsive CSS | Extend Node static contract tests and Java resource assertions. |
| Regression | Alias attach/remove, OCR isolation, existing movement confirmation/API paths | Assert no scanner API endpoint, no detector/media references in OCR scope, and no new backend tests/contracts. |
| Manual E2E | HTTPS/localhost supported camera, denied permission, unsupported browser, found and missing aliases | Verify scanner cleanup and that only an explicitly submitted movement changes stock. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. Ship as an additive static enhancement behind capability detection. Roll back by reverting these UI/test changes; manual barcode lookup/attach and all stock movement contracts remain intact.

## Open Questions

- [ ] Confirm supported-browser minimums for `BarcodeDetector`; unsupported browsers deliberately use manual entry.
