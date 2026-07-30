# Design: App Shell Domain Navigation

## Technical Approach

Replace the primary `.primary-tabs` tablist with one responsive hamburger-controlled navigation drawer. The drawer’s links update a canonical hash route; `navigation.js` resolves that route to the existing monthly panels and supermarket subtabs, while `app.js` applies it on startup and `hashchange`. This implements the proposal without storage or business/API changes.

## Architecture Decisions

| Decision | Options / trade-off | Choice and rationale |
|---|---|---|
| Route format | In-memory state loses reload/history; query strings add server-visible semantics | Use `#monthly/{section}` and `#stock/{section}`. Fragments stay client-side and provide reload plus back/forward restoration. Empty or invalid hashes are canonicalized with `replaceState` to `#monthly/summary`, avoiding a bogus history entry. |
| Drawer semantics | `menu` roles require menu keyboard behavior; plain links are native navigation | Use a labelled `<nav>` containing grouped anchor links, controlled by a button with `aria-expanded` and `aria-controls`. Keep native Tab and link activation behavior. |
| Stock navigation | Rebuild stock panels risks regressions; retain two independent states risks drift | Retain the four existing supermarket tabpanels and tablist. Route activation selects the supermarket primary panel plus the requested subtab; subtab activation writes its matching hash. |
| PWA caching | Cache navigation state or add route documents; preserve privacy boundary | Hashes are not requests and are excluded from service-worker cache keys. Keep API/auth/upload/ticket/private/PDF traffic network-only; bump the shell cache/versioned shell asset allowlist only for changed static files. |

## Data Flow

```text
Drawer link / stock subtab
          -> location.hash
          -> hashchange / startup route resolver
          -> primary panel + supermarket subtab visibility
          -> drawer closes; focus moves as required
```

Routes: `#monthly/summary`, `#monthly/expenses-upload`, `#monthly/expenses-table`, `#monthly/income-table`, `#monthly/income-upload`, `#monthly/simulator`, `#monthly/categories`; `#stock/list`, `#stock/barcode`, `#stock/tickets`, `#stock/categories`.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/index.html` | Modify | Replace primary tab buttons with trigger, drawer, grouped route links, and stable panel labels/headings; retain supermarket subtabs. |
| `src/main/resources/static/js/navigation.js` | Modify | Replace primary-tab controller with route registry, parser/canonicalizer, drawer lifecycle, and panel/subtab activation. |
| `src/main/resources/static/js/app.js` | Modify | Initialize navigation after DOM setup and apply the route on load and `hashchange`. |
| `src/main/resources/static/js/supermarket.js` | Modify | Remove dependency on `#primary-tab-supermarket`; expose/use a route-safe List handoff without resetting loaded in-memory stock state. |
| `src/main/resources/static/css/styles.css` | Modify | Add responsive drawer, backdrop, visible focus, and hidden-state styles; remove obsolete primary-tab styling. |
| `src/main/resources/static/service-worker.js` | Modify | Rotate shell cache and align allowlisted versioned CSS/app/navigation assets; do not cache route or private data. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Replace obsolete primary-tab structural contracts in the implementation phase. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Add route, history, drawer keyboard/focus, and PWA cache-boundary contracts in the implementation phase. |

## Interfaces / Contracts

`navigation.js` will own a route registry whose entries contain `{ domain, section, primaryPanel, supermarketTab? }`. `parseHash(hash)` returns a valid route or the Summary route. `navigate(route, { focusTarget })` updates visibility without data reload; user link/subtab actions set `location.hash`, whereas startup/history handling never pushes a second entry.

Drawer contract: trigger starts closed (`aria-expanded="false"`); opening focuses the first drawer link. Escape, backdrop/outside activation, and an explicit close control close it and return focus to the trigger. Selecting a route closes it, then focuses the destination heading (not a hidden tab). Focus is never stolen during `hashchange` caused by browser back/forward unless the prior focused element is no longer visible.

## Testing Strategy

No code or tests are created or executed in this phase. A later implementation phase should add static and simulated-DOM contracts for route parsing/canonicalization, load/back/forward panel restoration, drawer ARIA/Escape/focus return, stock-subtab hash synchronization, absence of web storage, and unchanged network-only/cache behavior for private requests.

## Threat Matrix

| Boundary | Applicability | Design response | Planned RED tests |
|---|---|---|---|
| Documentation-like paths | N/A — no executable classification | None | None |
| Git repository selection | N/A — no VCS operation | None | None |
| Commit state | N/A — no commit operation | None | None |
| Push state | N/A — no push operation | None | None |
| PR commands | N/A — no PR automation | None | None |

## Migration / Rollout

Ship as one shell release: replace only the primary tab strip, preserve panel IDs and supermarket subtab IDs, rotate cached shell assets, and fall back to Summary for legacy/no/invalid fragments. No data migration, flag, storage, or backend rollout is required. Rollback restores the primary tablist/controller; hashes harmlessly resolve to Summary.

## Open Questions

- [ ] None.
