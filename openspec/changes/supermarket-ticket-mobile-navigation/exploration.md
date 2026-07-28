## Exploration: supermarket-ticket-mobile-navigation

### Current State
The supermarket area is still one large primary tab (`supermarket`) with stacked cards inside it. The only mobile-specific navigation is a quick-link strip (`super-mobile-shell-nav`); there is no hamburger, drawer, or tab-level split for main list / barcode / OCR / categories.

The OCR backend is strict: `TicketOcrController` forwards a single uploaded file to `TicketOcrService`, which accepts only one PNG/JPEG file, decodes it eagerly with `ImageIO`, and rejects unsupported formats, oversize files, or undecodable images before Tess4J runs. Tess4J itself falls back to sanitized warnings when datapath/native runtime/langdata are missing, but the frontend can still show a generic upload failure if the response is empty/non-JSON.

Privacy constraint: the local ticket-image folder at `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG` must not be read, copied, moved, uploaded, cached, or versioned.

### Affected Areas
- `src/main/resources/static/index.html` — supermarket markup is a single section; mobile quick links and the ticket OCR panel live here.
- `src/main/resources/static/css/styles.css` — primary tabs are horizontal pills, mobile shell is only a link strip, and responsive table behavior is defined here.
- `src/main/resources/static/js/navigation.js` — primary navigation is tab-based only; no hamburger/drawer state exists.
- `src/main/resources/static/js/supermarket.js` — owns supermarket rendering, OCR upload/confirmation, and the current one-pane layout behavior.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` — image validation/decoding layer where local ticket files can fail before OCR.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` — runtime fallback and sanitized warning behavior.
- `src/main/resources/static/js/api.js` — generic API fallback text can be misleading for OCR failures.
- `src/main/resources/application.properties` — OCR datapath is env-driven (`TESSDATA_PATH`) and may be unset locally.

### Approaches
1. **Split supermarket into dedicated sub-tabs** — keep the main product tab, but move barcode, OCR, and categories into separate supermarket sub-tabs.
   - Pros: lower cognitive load; clearer mobile navigation; reduces initial visual density.
   - Cons: more UI state; more wiring in JS/CSS.
   - Effort: Medium

2. **Add a mobile-only hamburger shell** — keep the current content layout, but add an off-canvas menu or collapsible navigation for mobile.
   - Pros: minimal content reshuffle; solves the missing hamburger complaint directly.
   - Cons: does not really reduce the supermarket page density; still leaves heavy tables in place.
   - Effort: Medium

3. **Improve OCR error surfacing first** — keep the layout unchanged and clarify backend/frontend messages for invalid, undecodable, or runtime-missing OCR uploads.
   - Pros: fastest way to explain the local image failure; low risk.
   - Cons: does not address supermarket overcrowding or mobile navigation.
   - Effort: Low

### Recommendation
Do this incrementally: first fix OCR feedback so image-validation/runtime failures are explicit and non-misleading, then split the supermarket into smaller sub-tabs, and only after that add a true mobile hamburger shell if the product still needs it. The current code already has responsive card-table patterns; the bigger gap is navigation/sectioning, not basic table CSS.

### Risks
- A real ticket image can fail before OCR if it is unsupported, oversized, or undecodable; the current frontend can obscure which step failed.
- `TESSDATA_PATH` may be unset, which makes Tess4J degrade to warnings instead of real extraction.
- Reorganizing supermarket into sub-tabs can regress the existing responsive card-table behavior if table wrappers and IDs are moved carelessly.

### Ready for Proposal
Yes — propose a bounded supermarket UX slice that starts with OCR error clarity and then splits the supermarket into smaller mobile-friendly sections.
