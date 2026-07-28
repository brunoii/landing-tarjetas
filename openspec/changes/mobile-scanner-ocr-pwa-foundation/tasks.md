# Tasks: Mobile Scanner OCR PWA Foundation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 450-700 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 scanner/OCR hardening → PR 2 responsive shell → PR 3 PWA/privacy shell |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Harden scanner/OCR boundaries, readiness, and bounded upload handling. | PR 1 | `mvn test -Dtest=Tess4jTicketOcrEngineTests,SupermarketControllerTests` | N/A: server-side and contract slice only | Revert `Tess4jTicketOcrEngine.java`, `TicketOcrService.java`, and OCR props/tests. |
| 2 | Prove mobile-responsive shell behavior for nav, tables, scanner affordances, and manual fallback. | PR 2 | `mvn test -Dtest=StaticUiContractTests` | Representative mobile-device check for nav/table/scanner reachability | Revert `index.html`, `styles.css`, and static UI contract edits. |
| 3 | Add privacy-safe PWA primitives and cache allowlist/deny rules for shell-only offline behavior. | PR 3 | `mvn test -Dtest=StaticUiContractTests` | Browser load with service worker registration and offline navigation | Revert `manifest.webmanifest`, `service-worker.js`, `offline.html`, and registration hooks. |

## Phase 1: Scanner / OCR Foundation

- [x] 1.1 Add RED tests in `src/test/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngineTests.java` for missing datapath/native/langdata diagnostics and sanitized warnings.
- [x] 1.2 Add RED tests in `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for one-file-only upload, bounded image size/dimensions, and no ticket persistence.
- [x] 1.3 Implement `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrUploadProperties.java`, `Tess4jTicketOcrEngine.java`, and `TicketOcrService.java` to enforce transient OCR-only flow.

## Phase 2: Responsive Mobile Shell

- [x] 2.1 Add RED browser-contract assertions in `src/test/resources/static-ui-contract-tests.mjs` for mobile nav reachability, review-first fallback, and scanner/OCR entry visibility.
- [x] 2.2 Update `src/main/resources/static/index.html` and `src/main/resources/static/css/styles.css` with mobile-safe scanner/OCR affordances and responsive table/nav fixes.
- [x] 2.3 Wire `src/main/resources/static/js/supermarket.js` to preserve manual fallback, repeatable scanner cleanup, and actionable readiness states.

## Phase 3: PWA / Privacy Shell

- [x] 3.1 Add RED tests for allowlisted shell caching and network-only denial of `/api/**`, login, and ticket-related requests in `StaticUiContractTests` plus `static-ui-contract-tests.mjs`.
- [x] 3.2 Create `src/main/resources/static/manifest.webmanifest`, `service-worker.js`, and `offline.html` with no sensitive data and versioned cache names.
- [x] 3.3 Register the manifest/worker from `src/main/resources/static/index.html` and keep offline behavior limited to non-sensitive shell assets.

## Phase 4: Cleanup / Verification

- [x] 4.1 Update `openspec/specs/super-inventory/spec.md` and `openspec/specs/privacy-safe-pwa-shell/spec.md` only if task wording needs contract alignment.
- [x] 4.2 Trim comments, copy, and any temporary scaffolding so rollback stays isolated to each slice.
