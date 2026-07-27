## Exploration: super-inventory-stage17-next-slice

### Current State
Super Inventory already has the full Stage 16 scanner slice in code: barcode aliases are manual-first, the scanner is client-only (`BarcodeDetector` + `getUserMedia`), and a resolved barcode only hands off to explicit purchase/consume actions through the existing movement modal. Stock changes still happen only through the existing movement/adjustment endpoints, and movement history is already readable.

What is missing is a persistent step between “scan/resolve” and “execute movement”: there is no scan session, batch queue, or draft movement model. That means each scan is still an isolated interaction, which limits auditability and makes repeated inventory work clumsy.

### Affected Areas
- `src/main/java/com/gentleia/landingtarjetas/supermarket/` — new scan-session/draft movement controller, service, DTOs, and possibly persistence entities.
- `src/main/resources/static/js/supermarket.js` — scanner/result flow would need batch queue or session UI hooks.
- `src/main/resources/static/index.html` — likely a small scan-session panel or draft queue in the barcode area.
- `src/main/resources/static/css/styles.css` — styles for queued scans, draft rows, and batch actions.
- `src/test/java/com/gentleia/landingtarjetas/` — controller/service/static-contract coverage for scan-session lifecycle and draft confirmation.

### Approaches
1. **Local-only scan queue** — Keep the scanner client-side and accumulate resolved scans in the UI until the user manually confirms individual movements.
   - Pros: smallest UI-only follow-up; no new persistence.
   - Cons: still ephemeral; weak audit trail; little improvement over today.
   - Effort: Low

2. **Server-backed scan session + movement drafts** — Persist a short-lived scan session with queued resolved items and explicit draft purchase/consume lines, then confirm them atomically.
   - Pros: best auditability; supports batching and retry; natural next step after explicit scanner resolution.
   - Cons: adds backend persistence and validation; more surface area than UI-only batching.
   - Effort: Medium/High

### Recommendation
Choose the server-backed scan session + movement draft slice. It is the most coherent follow-up to Stage 16 because it keeps the scanner as identity resolution, but adds the missing operational layer: a user can scan several items, review a queued draft, and confirm explicit stock changes in one controlled step.

### Risks
- It can accidentally drift into automatic stock mutation if the draft/confirm boundary is not enforced tightly.
- It may overlap with the existing movement modal unless the new session model is clearly separate.
- If the slice grows into full inventory counting/reconciliation, it will likely exceed the next review budget.

### Ready for Proposal
Yes — propose a bounded scan-session and movement-draft stage, with explicit confirmation and no automatic stock mutation.
