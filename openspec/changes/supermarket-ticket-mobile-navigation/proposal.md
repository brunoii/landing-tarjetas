# Proposal: Supermarket Ticket Mobile Navigation

## Intent

Clarify OCR readiness/failures and reduce supermarket mobile density without changing inventory semantics. Local OCR verification is authorized only with files from `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG`; those files MUST NOT be copied, moved, uploaded, cached, persisted, logged, or versioned.

## Scope

### In Scope
- Precise OCR error/readiness states for invalid file, decode failure, missing runtime/data, and empty OCR responses.
- Local-only verification policy and operator guidance for receipt checks using the authorized JPG folder.
- Supermarket sub-tabs for list, barcode, tickets, and categories with lower-density mobile navigation.

### Out of Scope
- Reading repository-external images into source control, app storage, caches, logs, or automated fixtures.
- OCR engine replacement, bulk imports, or production sync/telemetry.

## Capabilities

### New Capabilities
- `supermarket-ticket-ocr-readiness`: explicit OCR readiness/error contract plus local-only verification policy.
- `supermarket-mobile-subtabs`: supermarket information architecture and compact mobile navigation for list/barcode/tickets/categories.

### Modified Capabilities
- `super-inventory`: preserve list, barcode, ticket, and category behaviors while relocating them into sub-tabs with mobile-density rules.
- `privacy-safe-pwa-shell`: extend privacy-safe mobile shell rules so receipt verification data stays local-only and non-cacheable.

## Approach

Ship in one bounded UX slice: surface backend/frontend OCR failure reasons first, define the local verification policy in product flow/docs, then split the supermarket area into focused sub-tabs with mobile-first navigation that keeps manual fallback paths visible.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Supermarket sub-tab structure and OCR policy copy |
| `src/main/resources/static/css/styles.css` | Modified | Mobile density, sub-tab, and compact-nav styling |
| `src/main/resources/static/js/navigation.js` | Modified | Mobile nav/sub-tab state |
| `src/main/resources/static/js/supermarket.js` | Modified | OCR readiness messaging and supermarket section routing |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/*.java` | Modified | Precise OCR validation/runtime responses |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Misleading OCR failure mapping persists | Med | Normalize API/UI error states end-to-end |
| Mobile re-layout breaks existing responsive tables | Med | Preserve IDs/wrappers and validate each sub-tab path |
| Privacy leak from receipt evidence handling | Low | Hard-ban copy/cache/log/persist/version flows in contract |

## Rollback Plan

Revert sub-tab/mobile-nav wiring and restore current single-pane supermarket layout; keep prior OCR messages if readiness/error contract regresses.

## Dependencies

- Local access to `archivosJPG` for manual-only verification under the non-persistence policy.
- `TESSDATA_PATH` and existing Tess4J runtime configuration.

## Success Criteria

- [ ] OCR failures identify the failing stage and readiness state without generic upload-error ambiguity.
- [ ] Mobile users can reach list, barcode, tickets, and categories through lower-density navigation without losing manual fallback paths.
