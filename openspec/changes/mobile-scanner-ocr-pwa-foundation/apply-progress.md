# Apply Progress: Mobile Scanner OCR PWA Foundation

**Change**: `mobile-scanner-ocr-pwa-foundation`
**Mode**: Strict TDD
**Artifact store**: hybrid
**Delivery slice**: PR 3 — privacy-safe PWA shell

## Completed Tasks

- [x] 1.1 Add RED tests in `src/test/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngineTests.java` for missing datapath/native/langdata diagnostics and sanitized warnings.
- [x] 1.2 Add RED tests in `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for one-file-only upload, bounded image size/dimensions, and no ticket persistence.
- [x] 1.3 Implement `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrUploadProperties.java`, `Tess4jTicketOcrEngine.java`, and `TicketOcrService.java` to enforce transient OCR-only flow.
- [x] 2.1 Add RED browser-contract assertions in `src/test/resources/static-ui-contract-tests.mjs` for mobile nav reachability, review-first fallback, and scanner/OCR entry visibility.
- [x] 2.2 Update `src/main/resources/static/index.html` and `src/main/resources/static/css/styles.css` with mobile-safe scanner/OCR affordances and responsive table/nav fixes.
- [x] 2.3 Wire `src/main/resources/static/js/supermarket.js` to preserve manual fallback, repeatable scanner cleanup, and actionable readiness states.
- [x] 3.1 Add RED tests for allowlisted shell caching and network-only denial of `/api/**`, login, and ticket-related requests in `StaticUiContractTests` plus `static-ui-contract-tests.mjs`.
- [x] 3.2 Create `src/main/resources/static/manifest.webmanifest`, `service-worker.js`, and `offline.html` with no sensitive data and versioned cache names.
- [x] 3.3 Register the manifest/worker from `src/main/resources/static/index.html` and keep offline behavior limited to non-sensitive shell assets.
- [x] 4.1 Update `openspec/specs/super-inventory/spec.md` and `openspec/specs/privacy-safe-pwa-shell/spec.md` only if task wording needs contract alignment.
- [x] 4.2 Trim comments, copy, and any temporary scaffolding so rollback stays isolated to each slice.

## Boundary Notes

- This slice stays inside the stacked-to-main PR 3 boundary: privacy-safe PWA shell only.
- Desktop tabs, responsive mobile shell behavior, review-first OCR, and privacy-safe in-memory behavior remain intact.
- No access to `archivosJPG`; the implementation stays in-memory and request-bound.
- The service worker caches only exact allowlisted shell assets and keeps `/api/**`, `/login`, ticket/upload/PDF/private paths, and non-GET traffic network-only.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` | Unit | ✅ `mvn test "-Dtest=Tess4jTicketOcrEngineTests,SupermarketControllerTests"` → 100 passed | ✅ Same command failed first with 5 compilation errors for missing OCR config constructor/getters and warning contracts | ✅ `mvn test "-Dtest=Tess4jTicketOcrEngineTests"` → 5 passed | ✅ Added datapath, native-runtime, langdata, and generic misconfiguration paths | ✅ Extracted warning classification/default config helpers and reran 5/5 |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ `mvn test "-Dtest=Tess4jTicketOcrEngineTests,SupermarketControllerTests"` → 100 passed | ✅ Added disguised-GIF rejection contract first; combined RED run failed before the new OCR hardening API existed | ✅ `mvn test "-Dtest=SupermarketControllerTests"` → 99 passed | ✅ Existing one-file/size/dimension/no-persistence coverage plus the new disguised-format case now cover distinct upload paths | ➖ None needed |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java`, `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Unit + Integration | ✅ Same 100-test baseline run passed before edits | ✅ The RED compile failure proved `TicketOcrUploadProperties`, `Tess4jTicketOcrEngine`, and `TicketOcrService` lacked the new config/format boundary | ✅ `mvn test "-Dtest=Tess4jTicketOcrEngineTests,SupermarketControllerTests"` → BUILD SUCCESS, Tests run: 104, Failures: 0, Errors: 0, Skipped: 0 | ✅ Properties, sanitized OCR readiness classification, and canonical PNG/JPEG verification exercised different code paths | ✅ Kept helpers small and reran focused unit + integration passes |
| 2.1 | `src/test/resources/static-ui-contract-tests.mjs` | Static/browser contract | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 | ✅ Added failing assertions first for mobile shell shortcuts, review-first fallback copy, and scanner/OCR entry anchors in the responsive supermarket shell | ✅ Same focused command → BUILD SUCCESS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 | ✅ Added separate reachability expectations for mobile shortcuts and shell affordance CSS selectors instead of one broad markup assertion | ✅ Kept the shell evidence in the existing contract harness without broadening scope beyond responsive behavior |
| 2.2 | `src/test/resources/static-ui-contract-tests.mjs` | Static/browser contract | ✅ Same 30-test static baseline passed before markup/CSS edits | ✅ The new shortcut-nav and responsive affordance assertions failed until `index.html` and `styles.css` exposed the mobile shell entry points | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 | ✅ Verified both markup (`href` targets / review-first copy) and CSS (`.super-mobile-shell-nav`, `.super-mobile-shell-link`) instead of a single cosmetic change | ✅ Reused existing dark-shell patterns so desktop tabs and privacy copy stayed unchanged |
| 2.3 | `src/test/resources/static-ui-contract-tests.mjs` | Static/browser contract | ✅ Same 30-test static baseline passed before scanner lifecycle edits | ✅ Added a failing delayed-camera test first: stopping a pending scanner run had to prevent stale async media resolution from reviving preview/stream state | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 | ✅ Kept the existing visibility/pagehide coverage and added the delayed `getUserMedia` path to force real stale-run protection | ✅ Added only a run-id guard to scanner state so cleanup stays idempotent and manual fallback messaging remains authoritative |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static/browser contract | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 | ✅ Added failing manifest/service-worker contract reads first; the focused suite failed with missing `manifest.webmanifest`/`service-worker.js`/`offline.html` artifacts | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 31, Failures: 0, Errors: 0, Skipped: 0 | ✅ Exercised allowlisted `/` caching, network-only `/api/**` and `/login`, ticket/PDF bypass, and offline navigation fallback in the worker runtime harness | ✅ Extracted a small in-test service-worker runtime so cache policy stays behavioral instead of regex-only |
| 3.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static/browser contract | ✅ Same 30-test static baseline passed before PR 3 edits | ✅ The new artifact assertions failed until manifest, offline shell, worker, and safe icons existed on disk | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 31, Failures: 0, Errors: 0, Skipped: 0 | ✅ Separate assertions cover manifest metadata, offline copy, icons, and worker deny-by-default paths so one file cannot satisfy the whole slice alone | ➖ None needed |
| 3.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static/browser contract | ✅ Same 30-test static baseline passed before registration edits | ✅ Registration assertions failed until `index.html` linked the manifest and gated worker registration behind secure-context + feature detection | ✅ `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 31, Failures: 0, Errors: 0, Skipped: 0 | ✅ Verified manifest link, registration scope, and offline fallback remains shell-only while protected traffic stays network-only | ➖ None needed |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/OpenSpecArtifactContractTests.java` | Artifact contract | ✅ Prior focused static baseline preserved in this artifact: `mvn test "-Dtest=StaticUiContractTests"` → BUILD SUCCESS, Tests run: 31, Failures: 0, Errors: 0, Skipped: 0 | ✅ Added OpenSpec contract assertions first; the focused Maven run failed until the live `super-inventory` and `privacy-safe-pwa-shell` specs described secure-context/readiness cleanup, duplicate-scan suppression, exact shell allowlisting, and navigation-only offline fallback | ✅ `mvn test "-Dtest=OpenSpecArtifactContractTests,StaticUiContractTests"` → BUILD SUCCESS, Tests run: 33, Failures: 0, Errors: 0, Skipped: 0 | ✅ Separate assertions cover scanner lifecycle/duplicate barcode wording and PWA allowlist/offline wording so one spec file cannot satisfy both tasks | ➖ None needed |
| 4.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static/browser + runtime harness | ✅ Same 31-test static baseline passed before cleanup expectations changed | ✅ Updated token/copy expectations failed first because `index.html`, `app.js`, `supermarket.js`, and `service-worker.js` still referenced Stage 17 session-shell tokens and placeholder next-slice copy | ✅ `mvn test "-Dtest=OpenSpecArtifactContractTests,StaticUiContractTests"` → BUILD SUCCESS, Tests run: 33, Failures: 0, Errors: 0, Skipped: 0 | ✅ Java static assertions plus the Node harness both verify the renamed foundation tokens and the trimmed session copy while rejecting leftover `super-inventory-stage17-session-shell` / `próximo slice` scaffolding | ✅ Renamed the token constants to `FOUNDATION_*` in the static contract and trimmed the supermarket session placeholder copy without changing runtime scope |

## Test Summary

- **Total tests written**: 11
- **Total tests passing**: 33 in the focused artifact/static suite; PR 1 focused evidence remains preserved above at 104 passing tests
- **Layers used**: Unit (5), Integration (99), Static/browser contract (31), Artifact contract (2)
- **Approval tests**: None — no refactoring tasks
- **Pure functions created**: 0

## PR 3 Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `mvn test "-Dtest=OpenSpecArtifactContractTests,StaticUiContractTests"` → BUILD SUCCESS, Tests run: 33, Failures: 0, Errors: 0, Skipped: 0 |
| Runtime harness command/scenario and exact result | `cmd /c "node src\test\resources\static-ui-contract-tests.mjs && echo EXIT=0"` → EXIT=0 (the expected degraded `super-price-sources` console warnings still appear while the harness proves foundation cache tokens, trimmed supermarket session copy, allowlisted shell caching, network-only protected routes, and offline navigation fallback) |
| Rollback boundary | Revert only `openspec/specs/{super-inventory/spec.md,privacy-safe-pwa-shell/spec.md}`, `openspec/changes/mobile-scanner-ocr-pwa-foundation/{tasks.md,apply-progress.md}`, `src/main/resources/static/{index.html,service-worker.js,js/app.js,js/supermarket.js}`, `src/test/java/com/gentleia/landingtarjetas/{OpenSpecArtifactContractTests.java,StaticUiContractTests.java}`, and `src/test/resources/static-ui-contract-tests.mjs` to remove the cleanup/alignment batch without touching PR 1 OCR hardening or PR 2 responsive shell behavior |

## Files Changed

| File | Action | Notes |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrUploadProperties.java` | Modified | Added explicit OCR datapath and language configuration alongside existing byte/dimension limits. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` | Modified | Added sanitized datapath/native/langdata warning classification and property-driven Tess4J setup. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modified | Enforced canonical decoded PNG/JPEG validation before OCR while keeping the flow transient. |
| `src/main/resources/application.properties` | Modified | Declared OCR upload datapath/language/size defaults under `app.super.ticket-ocr-upload`. |
| `src/test/java/com/gentleia/landingtarjetas/Tess4jTicketOcrEngineTests.java` | Modified | Added RED-to-GREEN coverage for datapath, native runtime, langdata, and generic sanitized warnings. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified | Added disguised-image rejection coverage while preserving no-persistence assertions. |
| `src/main/resources/static/index.html` | Modified | Added manifest linkage and secure-context service-worker registration without broadening offline scope. |
| `src/main/resources/static/css/styles.css` | Modified | Added responsive mobile shell shortcut styling while preserving desktop tab/table behavior. |
| `src/main/resources/static/js/supermarket.js` | Modified | Added scanner run-id invalidation so stale async camera resolution cannot revive a stopped scan. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Added RED→GREEN PWA shell behavior checks, including a service-worker runtime harness. |
| `src/main/resources/static/manifest.webmanifest` | Created | Added install metadata with safe icon references and no sensitive content. |
| `src/main/resources/static/service-worker.js` | Created | Added versioned allowlist caching plus network-only deny rules for API/auth/ticket/upload/PDF/private requests. |
| `src/main/resources/static/offline.html` | Created | Added a non-sensitive offline fallback page for navigation failures only. |
| `src/main/resources/static/icons/{icon-192.svg,icon-512.svg}` | Created | Added shell-only PWA icons with no ticket/OCR/private content. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Added static contracts for manifest, worker, offline shell, and safe icon publication. |
| `src/test/java/com/gentleia/landingtarjetas/OpenSpecArtifactContractTests.java` | Created | Added live OpenSpec contract checks for scanner lifecycle wording and privacy-safe PWA allowlist/fallback wording. |
| `openspec/specs/super-inventory/spec.md` | Modified | Aligned the live scanner contract with secure-context readiness, duplicate-scan suppression, and restart cleanup wording. |
| `openspec/specs/privacy-safe-pwa-shell/spec.md` | Modified | Aligned the live PWA contract with same-origin GET handling, exact allowlist wording, and navigation-only offline fallback. |

## Status

- Completed this slice: 11 / 11 tasks
- Ready for verify
