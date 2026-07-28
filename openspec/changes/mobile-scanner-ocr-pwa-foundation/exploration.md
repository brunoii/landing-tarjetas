## Exploration: mobile-scanner-ocr-pwa-foundation

### Current State
The app is already a local Spring Boot 3.5.9 / Java 17 system with a framework-free static UI. Barcode scanning is native-only (`BarcodeDetector` + `getUserMedia`), manual-first, and secure-context gated; the cleanup path exists, but repeated setup can still duplicate listeners or streams if lifecycle handling is sloppy.

OCR is server-side and handled by `Tess4jTicketOcrEngine`. The engine already fails safely with a manual-review warning, but runtime configuration is incomplete: `Tess4jTicketOcrEngine` only sets the language, while datapath/native runtime/language data remain unconfigured risks.

Responsive tables already collapse into cards on small screens, but primary navigation is still horizontally scrollable and needs device validation, not just CSS assumptions. There is no PWA scaffold yet: no manifest, no service worker, no registration, no icons, and no caching strategy.

The ticket-image privacy boundary is strict: `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` must never be read, moved, uploaded, cached, or versioned.

### Affected Areas
- `src/main/resources/static/index.html` — current shell has no PWA manifest link or service worker hook; mobile navigation and scanner/OCR panels live here.
- `src/main/resources/static/js/supermarket.js` — owns scanner lifecycle, barcode lookup, OCR review wiring, and the listener/stream cleanup risk.
- `src/main/resources/static/css/styles.css` — responsive card tables and horizontally scrollable tabs already exist; mobile validation will depend on this layer.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` — safe OCR runtime handling exists, but Tess4J setup is incomplete.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` — upload validation, image decoding, and hash/metadata handling define the OCR boundary.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrUploadProperties.java` — current size/dimension guardrails exist, but not the full Tess4J runtime configuration.
- `src/test/resources/static-ui-contract-tests.mjs` — static contracts already pin barcode/OCR/mobile behavior and should expand only after the foundation is agreed.
- `openspec/specs/super-inventory/spec.md` — current barcode/OCR rules capture the manual-first and review-first contract that this foundation must preserve.

### Approaches
1. **Foundation-first hardening** — stabilize the existing scanner/OCR/mobile surface, then add PWA scaffolding only after the runtime and privacy boundary are proven.
   - Pros: smallest risk; preserves current behavior; isolates scanner/OCR/PWA concerns.
   - Cons: slower to reach offline/installable UX.
   - Effort: Medium

2. **Broad mobile/PWA rewrite** — add scanner, OCR runtime fixes, responsive validation, and PWA packaging in one sweep.
   - Pros: one pass over the shell.
   - Cons: higher coupling; harder rollback; easier to blur privacy and lifecycle boundaries.
   - Effort: High

### Recommendation
Choose foundation-first hardening. Keep the current manual-first scanner, keep OCR review-first, validate mobile navigation on real devices, and add PWA primitives only after the runtime/path/privacy questions are closed. Six-step incremental plan: 1) lock the privacy boundary and exclude `archivosJPG`; 2) harden scanner lifecycle so repeated setup cannot leak listeners/streams; 3) confirm Tess4J datapath/runtime/language data handling; 4) validate responsive tables and horizontal nav on mobile devices; 5) add manifest/service worker/icons/caching with explicit offline rules; 6) extend static/UI contracts and OpenSpec once the shell is stable.

### Risks
- Tess4J may still fail in production-like environments unless datapath and native runtime setup are explicit.
- Scanner support remains browser-dependent, so manual fallback must stay first-class.
- PWA caching can accidentally retain sensitive imagery if the privacy boundary is not enforced.
- Mobile layout changes can regress the existing card-table behavior or tab navigation if not validated on device.

### Ready for Proposal
Yes — tell the user the diagnosis is confirmed and the next proposal should be a foundation slice, not a full mobile/PWA rewrite.
