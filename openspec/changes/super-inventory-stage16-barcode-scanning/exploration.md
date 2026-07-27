## Exploration: super-inventory-stage16-barcode-scanning

### Current State
The app is a local Spring Boot 3.5.9 / Java 17 system with a framework-free static UI. Super-inventory already has manual barcode aliases that preserve leading zeros, plus explicit stock movement endpoints for purchase, consumption, quick consumption, and stock adjustment.

Barcode handling today is text-first only: users type or paste a code, look it up, attach it to an existing `SuperItem`, or remove it later. That flow does not mutate inventory. Stage 15 OCR already exists as a separate ticket-review workflow, and the current UI/tests explicitly keep `BarcodeDetector` and `getUserMedia` out of the OCR path.

For Stage 16, the cleanest first slice is barcode scanning as a progressive enhancement on top of the existing barcode alias workflow, not a new inventory engine. The scanner should resolve a code, then let the user explicitly choose existing purchase/consume actions; it should not auto-move stock.

### Affected Areas
- `src/main/resources/static/index.html` — add the mobile scan entry point and a scan result panel inside the existing barcode card.
- `src/main/resources/static/js/supermarket.js` — own scanner lifecycle, scan result handling, alias lookup/attach, and explicit stock-action handoff.
- `src/main/resources/static/js/api.js` — likely no backend helper needed for the first slice; only change if a small scan-specific wrapper improves clarity.
- `src/main/resources/static/css/styles.css` — mobile camera/result layout, button grouping, and scan-state feedback.
- `src/test/resources/static-ui-contract-tests.mjs` / `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` — update UI contracts to allow barcode-scanner browser APIs only in the barcode feature, while still forbidding OCR drift.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — only if the slice adds a backend contract; otherwise existing barcode/movement tests remain the reference boundary.

### Approaches
1. **Client-side progressive barcode scanner** — Use `BarcodeDetector` when available, with camera permission via `getUserMedia`, then feed the scanned text into the existing lookup/attach flow; keep purchase/consume as explicit follow-up actions.
   - Pros: smallest useful slice; no backend changes; mobile-friendly on supported browsers; keeps scan, lookup, and inventory movement separate.
   - Cons: browser support varies; camera permission UX can be fragile; requires a robust manual fallback.
   - Effort: Medium

2. **Scan session + movement draft backend** — Add server-side scan/session endpoints and a draft step before purchase/consume execution.
   - Pros: more auditability and a cleaner future path for batching.
   - Cons: larger API/domain surface; more validation and persistence risk; more likely to blur lookup semantics with stock movement.
   - Effort: High

### Recommendation
Start with the client-side progressive scanner. Reuse the existing barcode alias endpoints for resolution/attachment and the existing purchase/consume endpoints for inventory changes after the product is explicit on screen. That gives mobile scanning value without mixing OCR, lookup, and stock mutation into one opaque flow.

### Risks
- `BarcodeDetector` and camera access are not universal; localhost/HTTPS and browser support need a mandatory manual fallback.
- Continuous camera streams can emit duplicate scans; debounce and require an explicit user action before any mutation.
- Barcode text must keep leading zeros and stay validated as plain text to avoid wrong alias matching or accidental cross-product attachment.
- Scan UI must not auto-increment/decrement stock; movement still needs explicit confirmation and existing negative-stock handling.

### Ready for Proposal
Yes — propose a bounded first slice that adds a mobile scanner to the existing barcode card, resolves or attaches aliases, and then routes the user to explicit existing stock movement actions.
