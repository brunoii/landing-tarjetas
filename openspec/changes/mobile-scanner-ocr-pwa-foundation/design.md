# Design: Mobile Scanner OCR PWA Foundation

## Technical Approach

Harden the existing framework-free static shell and Spring OCR boundary without changing inventory semantics. Extend the current native-only `BarcodeDetector` lifecycle, retain in-memory OCR review, add explicit Tess4J readiness diagnostics, and introduce a minimal service worker whose fetch policy caches only an allowlisted public shell. This implements the `super-inventory` delta and adds the proposal's `privacy-safe-pwa-shell` capability.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Scanner lifecycle | Keep native APIs; make cleanup idempotent and generation/phase guarded. | Scanner library; camera-required flow. | Preserves manual-first progressive enhancement and prevents stale async callbacks from reviving stopped streams. |
| OCR readiness | Add injected/configured datapath and language validation with stable, sanitized warning codes/messages. | Leak Tess4J exceptions; fail only on `doOCR`. | Operators can fix native/langdata configuration without exposing paths or ticket content. |
| Multipart boundary | Decode one bounded PNG/JPEG in memory; validate decoded format/dimensions before OCR; do not create files. | Temp files; trust MIME/name alone. | Fits the existing transient service and rejects disguised or decompression-heavy uploads before OCR. |
| PWA cache | Versioned, exact allowlist of public static shell assets; network-only for every other request. | Cache-first all GETs; cache APIs/OCR. | Deny-by-default protects authenticated/API/OCR data while retaining a minimal offline explanation. |

## Data Flow

    Camera -> scanner state -> manual lookup/session review (no stock mutation)
    Image multipart -> bounded decode -> Tess4J readiness/OCR -> transient candidates -> explicit confirmation
    Browser GET -> service worker allowlist -> Cache Storage; all other requests -> network, never cached

Scanner start obtains a stream only after capability/secure-context checks. Stop, page-hide, visibility loss, error, and restart cancel the frame, detach preview, stop every track, invalidate the active run, and leave manual input focusable. OCR bytes, image, raw text, and candidates remain request/client-memory only; diagnostics contain no paths, filenames, raw OCR text, or native exception detail.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/js/supermarket.js` | Modify | Idempotent scanner cleanup, stale-run suppression, actionable readiness states, and transient OCR error rendering. |
| `src/main/resources/static/index.html` | Modify | Manifest/service-worker registration and mobile-safe scanner/OCR affordances. |
| `src/main/resources/static/css/styles.css` | Modify | Targeted small-screen nav, scanner, OCR, session, and responsive-table fixes. |
| `src/main/resources/static/manifest.webmanifest` | Create | Minimal install metadata with no sensitive data. |
| `src/main/resources/static/service-worker.js` | Create | Versioned allowlist cache and network-only deny policy. |
| `src/main/resources/static/offline.html` | Create | Static offline explanation; no user or receipt data. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` | Modify | Configurable datapath/languages and sanitized readiness classification. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modify | Canonical image-format verification and bounded in-memory decode. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrUploadProperties.java` | Modify | OCR-specific byte, pixel, datapath, and language settings. |
| `src/main/resources/application.properties` | Modify | Explicit OCR and multipart limits consistent with the transient boundary. |
| `src/test/java/com/gentleia/landingtarjetas/{Tess4jTicketOcrEngineTests,SupermarketControllerTests,StaticUiContractTests}.java` | Modify | Runtime, upload, PWA, and static-shell contracts. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Browser-level scanner cleanup, responsive, and worker-policy contracts. |

## Interfaces / Contracts

`POST /api/super/ticket-ocr/candidates` remains one `file` only and returns review candidates plus sanitized warnings; it never persists ticket bytes or candidates. New configuration remains under `app.super.ticket-ocr-upload`:

```properties
max-file-size-bytes=1048576
max-decoded-dimension=4096
datapath=${TESSDATA_PATH:}
languages=spa+eng
```

The worker handles same-origin `GET` only: exact public shell URLs may be cached; `/api/**`, `/login`, all request URLs containing sensitive OCR/scan paths, non-GET methods, and unmatched URLs are network-only and never written to Cache Storage.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Tess4J missing datapath/native/langdata; repeated scanner stop/stale callbacks | JUnit mocks; exported JS state helpers. |
| Integration | One-file/type/actual-format/size/pixel rejection and zero persistence/log leakage | MockMvc with engine verification and repository/output assertions. |
| Static/browser | Manifest registration, worker allowlist/denials, mobile reachable controls and card tables | Existing Java static contracts plus `static-ui-contract-tests.mjs`; capture representative mobile-device evidence before accepting CSS changes. |

## Threat Matrix

The service-worker fetch routing changes browser routing; no shell, VCS, PR, or executable-classification boundary exists.

| Boundary | Applicability | Design response | Planned RED tests |
|---|---|---|---|
| Documentation-like paths | N/A — no execution/classification. | None. | None. |
| Git repository selection | N/A — no Git automation. | None. | None. |
| Commit state | N/A — no commit operation. | None. | None. |
| Push state | N/A — no push operation. | None. | None. |
| PR commands | N/A — no PR command. | None. | None. |

Applicable fetch-routing RED tests prove sensitive/API/non-GET/unmatched URLs are network-only and never cached; allowlisted shell assets cache only after a successful network response, and failures return the non-sensitive offline page only for navigation.

## Migration / Rollout

No data migration required. Register the worker only after feature detection; a cache-name bump and activation cleanup remove prior shell entries. Removing registration and static PWA files restores the current shell.

## Open Questions

- [ ] Which representative mobile devices/browsers will provide acceptance evidence for the responsive requirement?
