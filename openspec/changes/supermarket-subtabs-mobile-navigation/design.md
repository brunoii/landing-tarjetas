# Design: Supermarket Subtabs Mobile Navigation

## Technical Approach

Nest an ARIA tablist inside the existing `#tab-supermarket` primary panel. Reuse the primary-tab interaction model for four DOM-only groups: **List**, **Barcode**, **Tickets**, and **Categories**. Existing elements retain their IDs, forms, and event delegation targets; only their containing panels change. This implements the proposal without changing the `super-inventory` API or business rules.

## Architecture Decisions

| Decision | Options / trade-off | Decision and rationale |
|---|---|---|
| Tab behavior | Copy primary-tab listeners; generalize the existing pattern | Generalize `navigation.js` around a reusable ARIA-tab setup and retain `setupPrimaryTabs()` as its primary configuration. This preserves roving focus, ArrowLeft/ArrowRight, Home/End, `aria-selected`, `tabIndex`, and `hidden` semantics without duplicate logic. |
| DOM grouping | Rebuild supermarket UI; move existing nodes | Move existing nodes into four panels while preserving IDs and selector relationships. `supermarket.js` binds by stable IDs and delegated table listeners, so DOM-only grouping avoids behavior and API changes. |
| Subtab state | Persist selection; reset on entry | Keep selection in page-local tab state, defaulting to List at initialization. Switching tabs MUST NOT clear `currentTicketOcrReview`, scan-session state, barcode resolution, or loaded inventory; it only toggles panel visibility. |
| Mobile navigation | Retain anchor shortcuts; use tabs | Replace `#super-mobile-shell-nav` anchors with the semantic tablist. CSS will make four 44px-minimum controls compact, wrap safely, and reuse current tab styling rather than adding a parallel navigation system. |

## Data Flow

```text
Subtab button / keyboard
        -> navigation.js reusable tab controller
        -> aria-selected, tabIndex, active class, panel.hidden
        -> existing supermarket.js handlers and in-memory state unchanged
        -> existing /api/super/* calls unchanged
```

Panel allocation:

- **List**: list actions, item table, movement modal/history, price observations, suggested list, and generated list.
- **Barcode**: manual/camera barcode lookup and the linked scan-session review.
- **Tickets**: OCR upload, transient candidate review, and confirmation form.
- **Categories**: product-management form and supermarket category creation/listing.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/index.html` | Modify | Add nested tab buttons and four `role="tabpanel"` containers; relocate current supermarket surfaces without changing their IDs or form structure; remove quick-link anchors. |
| `src/main/resources/static/js/navigation.js` | Modify | Extract reusable ARIA-tab controller/configuration and initialize supermarket subtabs with List as default. |
| `src/main/resources/static/js/supermarket.js` | Modify | Keep existing event wiring and data state; add only any required default-subtab/focus handoff integration after the DOM regrouping. |
| `src/main/resources/static/css/styles.css` | Modify | Style nested subtabs for desktop and compact mobile wrapping without page-overflow masking; retire quick-link styles. |
| `openspec/changes/supermarket-subtabs-mobile-navigation/design.md` | Create | This design artifact. |

## Interfaces / Contracts

```js
// navigation.js conceptual configuration; no API or backend contract changes
setupTabs({ buttons, panels, defaultTabId });
// buttons use data-super-tab-target; panels use data-super-tab-panel
```

Each subtab button MUST use `role="tab"`, `aria-controls`, `aria-selected`, and roving `tabIndex`; each panel MUST use `role="tabpanel"`, `aria-labelledby`, and `hidden` when inactive. Existing `/api/super/*` requests, DOM IDs, and `supermarket.js` module state remain unchanged.

## Testing Strategy

No code or tests will be created in this phase, per request. Future verification should cover static DOM/ARIA contracts, keyboard activation and focus movement, mobile no-overflow layout, and preservation of barcode manual fallback, scan-session review, OCR transient review, product actions, and category actions after panel switches.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. Browser barcode/OCR behavior is only regrouped; it is not changed.

## Migration / Rollout

No migration required. The change is static UI organization only. Roll back by restoring the current single supermarket panel and anchor quick-link shell; persisted data and APIs are untouched.

## Open Questions

- [ ] None.
