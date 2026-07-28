# Proposal: Mobile Scanner OCR PWA Foundation

## Intent

Harden the current mobile scanner/OCR shell before any broader mobile or PWA expansion. The slice protects receipt privacy, keeps manual-first behavior, and removes readiness gaps around scanner lifecycle, OCR runtime diagnosis, responsive mobile behavior, and privacy-safe PWA primitives.

## Scope

### In Scope
- Enforce the strict ticket privacy boundary, including no reads/uploads/caching/versioning of the real receipt directory.
- Harden scanner capability detection, repeated start/stop cleanup, secure-context readiness messaging, and mandatory manual fallback.
- Diagnose OCR runtime prerequisites (datapath/native/langdata), bound temporary upload handling, and keep review-first failure behavior.
- Add evidence-based mobile nav/table improvements plus minimal privacy-safe PWA primitives (manifest, registration, offline rules, non-sensitive caching only).

### Out of Scope
- External/paid OCR, frontend migration, auto-persistence of receipt artifacts, or real ticket-file access.
- HTTPS/deploy work, install-prompt polish, full offline data sync, or broader product rewrites.

## Capabilities

### New Capabilities
- `privacy-safe-pwa-shell`: Defines manifest/service-worker/offline primitives with explicit non-caching rules for sensitive scanner/OCR receipt data.

### Modified Capabilities
- `super-inventory`: Tightens scanner/OCR/mobile-shell requirements so manual fallback, review-first OCR, secure-context gating, and privacy boundaries remain mandatory.

## Approach

Apply a foundation-first slice: preserve current flows, add runtime/config diagnostics and cleanup guarantees, validate mobile UI behavior on real devices, then introduce minimal PWA scaffolding with deny-by-default caching for sensitive paths.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Manifest/registration hooks and mobile shell affordances. |
| `src/main/resources/static/js/supermarket.js` | Modified | Scanner lifecycle/readiness/manual fallback hardening. |
| `src/main/resources/static/css/styles.css` | Modified | Evidence-based mobile nav/table adjustments. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` | Modified | Explicit OCR runtime readiness/config diagnosis. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modified | Safe temporary upload handling/privacy guardrails. |
| `openspec/specs/super-inventory/spec.md` | Modified | Scanner/OCR/privacy requirement deltas. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Sensitive receipt data cached or exposed | Med | Deny-by-default caching and explicit path exclusions. |
| Browser/runtime variability | High | Capability checks, diagnostics, and manual fallback. |

## Rollback Plan

Remove the PWA shell files/hooks and revert scanner/OCR hardening deltas; the existing manual-first barcode and review-first OCR paths remain the fallback baseline.

## Dependencies

- Existing native browser scanner APIs and current Tess4J-based OCR stack.

## Success Criteria

- [ ] Scanner start/stop is repeatable without leaked streams/listeners, and manual entry always remains available.
- [ ] OCR readiness failures surface actionable diagnostics without persisting or exposing sensitive receipt data.
- [ ] Mobile nav/table behavior is validated by evidence, not CSS-only assumptions.
- [ ] PWA primitives avoid caching receipt uploads, OCR payloads, or other sensitive scanner data.
