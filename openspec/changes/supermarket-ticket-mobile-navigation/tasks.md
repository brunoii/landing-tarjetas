# Tasks: Supermarket Ticket Mobile Navigation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 420-620 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 OCR readiness/local JPG guard → PR 2 supermarket subtabs/mobile nav → PR 3 verification/docs |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Lock OCR readiness states and the local-only JPG verification contract. | PR 1 | `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` | Manual check from `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` outside repo only | Revert OCR readiness copy, guards, and contract tests. |
| 2 | Split supermarket into list/barcode/ticket/category subtabs with mobile-safe nav. | PR 2 | `mvn test -Dtest=StaticUiContractTests` | Mobile viewport pass over supermarket tabs and quick links | Revert `index.html`, `styles.css`, and `supermarket.js` tab wiring. |
| 3 | Prove strict TDD, OCR fallback, and mobile navigation evidence end to end. | PR 3 | `mvn test -Dtest=SupermarketControllerTests,StaticUiContractTests` | Local JPG OCR verification plus representative mobile navigation smoke check | Revert only verification/docs and any temporary harness notes. |

## Phase 1: Foundation / Contracts

- [x] 1.1 Add RED specs in `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md` for invalid type, decode failure, missing runtime/data, and empty OCR extraction.
- [x] 1.2 Add RED spec coverage in `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md` for local-only verification from `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` with no copy/move/upload/cache/persist.

## Phase 2: Core Implementation

- [ ] 2.1 Update `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-mobile-subtabs/spec.md` to require separate list/barcode/ticket/category subtabs and compact mobile navigation.
- [ ] 2.2 Update `openspec/changes/supermarket-ticket-mobile-navigation/specs/super-inventory/spec.md` to preserve existing behavior while relocating supermarket surfaces into subtabs.
- [ ] 2.3 Update `openspec/changes/supermarket-ticket-mobile-navigation/specs/privacy-safe-pwa-shell/spec.md` only if needed to keep manual fallback routes visible on mobile.

## Phase 3: Testing / Verification

- [x] 3.1 Write RED contract cases for stage-specific OCR feedback and local JPG-only verification in `openspec/changes/supermarket-ticket-mobile-navigation/specs/supermarket-ticket-ocr-readiness/spec.md`.
- [ ] 3.2 Write RED contract cases for mobile subtabs, visible fallback routes, and preserved inventory behavior in the three spec files above.
- [x] 3.3 Verify the OCR path with representative JPGs from `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` outside the repo; confirm the UI reports the expected OCR stage instead of a generic failure.

## Phase 4: Cleanup / Documentation

- [ ] 4.1 Trim task/spec copy so the change stays reviewable under chained slices and keeps OCR, subtabs, and mobile navigation scope isolated.
- [ ] 4.2 Note any remaining harness gaps in the change docs without adding repo-local image fixtures or test assets.
