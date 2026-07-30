# Proposal: Supermarket Subtabs Mobile Navigation

## Intent

Reduce supermarket mobile navigation overload by splitting the current long panel into clear internal sections for List, Barcode, Tickets, and Categories without changing inventory, barcode, OCR, or category-management behavior.

## Scope

### In Scope
- Add nested supermarket tabs for List, Barcode, Tickets, and Categories.
- Add compact mobile navigation for those sections using the existing accessible tab pattern.
- Reorganize current supermarket surfaces so existing flows remain reachable inside the new sections.

### Out of Scope
- Inventory, stock, barcode, OCR, or category business-rule changes.
- New APIs, new data models, or scanner/OCR capability work.

## Capabilities

### New Capabilities
- `supermarket-mobile-navigation`: Internal supermarket tab navigation and compact mobile access for List, Barcode, Tickets, and Categories while preserving existing flows.

### Modified Capabilities
- None.

## Approach

Reuse the primary ARIA tab pattern from `src/main/resources/static/js/navigation.js` inside the supermarket panel. Keep existing DOM IDs, handlers, and forms where practical; move them into four tab panels and replace the current quick-link shell with compact mobile subtab navigation.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Group supermarket content into four internal tab panels |
| `src/main/resources/static/js/navigation.js` | Modified | Reuse/extend tab behavior for supermarket subtabs |
| `src/main/resources/static/js/supermarket.js` | Modified | Preserve event wiring and default section behavior after re-layout |
| `src/main/resources/static/css/styles.css` | Modified | Add compact mobile subtab layout without overflow regressions |
| `openspec/changes/supermarket-subtabs-mobile-navigation/specs/` | New | Add delta spec(s) for navigation behavior |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Broken handlers after moving markup | Med | Preserve IDs/selectors and keep sectioning DOM-only where possible |
| Mobile overflow or hidden fallback actions | Med | Keep compact tabs wrapping/scroll-safe and preserve manual paths in each section |

## Rollback Plan

Revert supermarket markup/CSS/JS to the current single-panel layout, restore the existing quick-link shell, and leave all current flows on their original anchors and handlers.

## Dependencies

- Existing `super-inventory` contracts remain the source of truth for preserved flows.

## Success Criteria

- [ ] Users can reach List, Barcode, Tickets, and Categories from nested supermarket tabs on desktop and mobile.
- [ ] Barcode/manual entry, session review, OCR review, product management, and category management keep current behavior.
- [ ] Mobile navigation no longer depends on jumping through long-page anchor shortcuts.
