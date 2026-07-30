## Exploration: app-shell-domain-navigation

### Current State
The app currently has two separate horizontal tab systems: the primary shell tablist (`summary`, expenses/income, simulator, categories, supermarket) and a nested supermarket tablist (`List`, `Barcode`, `Tickets`, `Categories`). Both are button-based ARIA tablists driven by `setupTabs()` in `js/navigation.js`, with `summary` and `list` as the defaults.

There is no hamburger, drawer, or URL-backed navigation state today. Selection is in-memory only; the service worker caches only the shell and already keeps auth/API/upload/private/PDF requests network-only, so navigation state should remain non-sensitive and non-persistent.

### Affected Areas
- `src/main/resources/static/index.html` — replace the primary tab strip with a hamburger shell and grouped domain navigation; keep Summary as the home entry.
- `src/main/resources/static/js/navigation.js` — current tab controller is tablist-specific; it needs a menu/router layer for domain + section selection.
- `src/main/resources/static/js/app.js` — initial section selection and history/hash restoration will likely live here.
- `src/main/resources/static/js/supermarket.js` — may need a small handoff so entering Stock returns to `List` by default without losing in-memory stock state.
- `src/main/resources/static/css/styles.css` — drawer/menu layout, focus states, responsive wrapping, and menu accessibility styling.
- `src/main/resources/static/service-worker.js` — only if new shell asset URLs/versioning change; keep sensitive requests network-only.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` — current contracts assert horizontal tabs; they would need new accessibility and responsive navigation contracts.

### Approaches
1. **Route-backed hamburger drawer** — Use a hamburger button that opens a two-group drawer: Monthly summary (Summary/Home, Expenses upload/table, Income upload/table, Simulator, Categories) and Stock (List, Barcode, Tickets, Categories). Drive selection through hash/history state so back/forward and reload restore the same section.
   - Pros: best IA for two domains; shareable URLs; browser history works; easier to keep Home as the default; good PWA fit without storage.
   - Cons: more JS than tabs; focus management and aria-expanded/aria-controls must be done carefully.
   - Effort: Medium/High

2. **Drawer-only state with in-memory selection** — Same hamburger UI, but selection is managed only in runtime state and not reflected in the URL.
   - Pros: simpler to code initially; no route parsing.
   - Cons: reload/back-forward lose context; weaker deep-linking and supportability; less robust for PWA use.
   - Effort: Medium

### Recommendation
Choose the route-backed drawer. It best matches the request to replace the tab sprawl with one responsive hamburger shell while preserving Summary as Home and keeping Stock as a distinct domain. Use the URL to encode `domain` + `section`, keep state in memory only, and leave logout outside the drawer.

### Risks
- Moving from tabs to a menu can break keyboard expectations unless focus returns to the trigger and the drawer supports Escape/Tab correctly.
- The duplicate `Categories` label across Monthly summary and Stock will confuse users unless the menu labels are disambiguated by domain context.
- Any attempt to persist nav state in storage would conflict with the project’s privacy/no-sensitive-cache constraints.

### Ready for Proposal
Yes — tell the user the best next step is a proposal for a route-backed hamburger drawer with grouped Monthly summary and Stock navigation, preserving Summary as Home and keeping logout/session handling untouched.
