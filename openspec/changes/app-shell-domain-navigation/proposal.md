# Proposal: App Shell Domain Navigation

## Intent

Replace the current horizontal primary tabs with one hamburger drawer shared by desktop and mobile so users can move between Monthly summary and Stock without tab sprawl. Keep Summary as Home, preserve current stock tools, and keep navigation state privacy-safe.

## Scope

### In Scope
- Replace the primary shell tab strip with a drawer that groups Monthly summary and Stock destinations.
- Back section selection with hash routes so reload and back/forward restore the active destination.
- Keep Stock entries as List, Barcode, Tickets, and Stock categories with accessible focus return and Escape close behavior.

### Out of Scope
- Changes to inventory, barcode, OCR, ticket, category, or auth business rules.
- Local/session storage persistence, sensitive cache expansion, or separate desktop/mobile navigation models.

## Capabilities

### New Capabilities
- `app-shell-domain-navigation`: Shared hamburger drawer navigation for Monthly summary and Stock with hash-based section restoration and Summary as the default home route.

### Modified Capabilities
- `privacy-safe-pwa-shell`: Shell navigation behavior changes to route-backed drawer navigation while keeping mobile usability and non-sensitive caching boundaries intact.

## Approach

Replace the primary tablist in `index.html` with a menu trigger and drawer. Extend `navigation.js` and `app.js` to map hash routes to existing summary panels and current stock subtabs, default to Summary home, and sync browser history without persistent storage. Reuse the existing stock subtab structure, disambiguate labels with domain context, and keep drawer state runtime-only.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Replace primary tabs with drawer trigger, grouped links, and accessible shell markup |
| `src/main/resources/static/js/navigation.js` | Modified | Add drawer interaction, focus return, Escape handling, and section activation |
| `src/main/resources/static/js/app.js` | Modified | Parse/apply hash routes and restore destination on load/history changes |
| `src/main/resources/static/js/supermarket.js` | Modified | Default Stock entry to List while preserving in-memory stock state |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Update shell accessibility and route-backed navigation contracts |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Drawer focus/keyboard regressions | Med | Reuse ARIA patterns, return focus to trigger, support Escape/outside close |
| Ambiguous Categories labels across domains | Med | Label stock entry as Stock categories inside the drawer |

## Rollback Plan

Restore the current primary tablist, remove hash-route drawer logic, and return shell activation to the existing tab-based controller while leaving stock subtabs unchanged.

## Dependencies

- Existing `privacy-safe-pwa-shell` and current stock subtab structure remain the baseline.

## Success Criteria

- [ ] Desktop and mobile users can open one drawer and reach Summary home plus approved Monthly summary and Stock destinations.
- [ ] Hash routes restore the active destination on reload and browser back/forward.
- [ ] Keyboard users get correct focus return/Escape behavior, and no sensitive navigation state is persisted or cached.
