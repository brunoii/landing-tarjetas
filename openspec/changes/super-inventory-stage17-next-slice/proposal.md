# Proposal: Super Inventory Stage 17 Next Slice

## Intent

Add a bounded, server-backed scan session so barcode resolution can feed an auditable draft workflow before any stock change. Keep Stage 16's manual-first fallback and explicit user control; scanning MUST NOT mutate stock automatically.

## Scope

### In Scope
- Create a short-lived scan session that stores resolved items for the current inventory workflow.
- Add explicit movement drafts for purchase/consumption lines reviewed before confirmation.
- Confirm drafts through an explicit server action that applies allowed stock movements atomically.

### Out of Scope
- Automatic stock mutation from scan lookup or camera reads.
- Full counting/reconciliation, pricing, or replacement of the existing manual movement flow.

## Capabilities

### New Capabilities
- `inventory-scan-session`: Server-backed session lifecycle for queued scan results and reviewable movement drafts.

### Modified Capabilities
- `super-inventory`: Add requirements for bounded scan-session behavior, manual fallback continuity, explicit draft confirmation, and prohibition of automatic stock mutation.

## Approach

Persist a lightweight session/draft model in the supermarket backend, separate from direct movement execution. The scanner/UI keeps manual entry and camera fallback from Stage 16, but resolved items are queued into a session. Users explicitly create, edit, remove, and confirm draft movement lines; only confirmation can call stock-movement logic.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modified | Session, draft, validation, and confirm endpoints/services/entities. |
| `src/main/resources/static/js/supermarket.js` | Modified | Session queue, manual fallback, and explicit draft actions. |
| `src/main/resources/static/index.html` | Modified | Scan-session and draft-review panel. |
| `src/main/resources/static/css/styles.css` | Modified | Session/draft UI states. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Draft flow drifts into implicit stock updates | Med | Enforce confirm-only mutation server-side and spec it explicitly. |
| Overlap with current movement modal confuses users | Med | Keep session/draft flow bounded and preserve manual fallback entry points. |

## Rollback Plan

Remove session/draft endpoints and UI wiring, then return barcode resolution to the current direct handoff without changing existing movement commands.

## Dependencies

- Existing `super-inventory` movement commands, validation rules, and history behavior.

## Success Criteria

- [ ] Users can queue resolved scans in a bounded server session and review draft lines before confirmation.
- [ ] Scan lookup/manual entry never changes stock until an explicit confirm action succeeds.
