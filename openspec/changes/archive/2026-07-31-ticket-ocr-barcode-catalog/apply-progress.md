# Apply Progress: Ticket OCR Barcode Catalog — Slice 1 OCR Cleanup

## Status

- Mode: Strict TDD
- Delivery mode: single PR
- Scope: Slice 1 only (OCR cleanup; no catalog, enrichment, scanner-session, runtime swap, or persistence mutation changes)
- Tasks complete: 10/10

## Completed Tasks

- [x] 1.1 Add failing table-driven parser cases in `TicketOcrCandidateParserTests` for Vea/Gómez Pardo block boundaries, normalized amounts, and no inferred product IDs.
- [x] 1.2 Add failing `SupermarketControllerTests` for additive `debugLines` plus safe transient OCR responses with no repository writes.
- [x] 1.3 Add failing `StaticUiContractTests` for hidden-by-default debug disclosure and unchanged OCR review IDs/API contract.
- [x] 2.1 Update `TicketOcrCandidateParser.java` to normalize whitespace/amounts and emit bounded useful candidates plus debug-only noise lines.
- [x] 2.2 Update `TicketOcrLineCandidateResponse.java`, `TicketOcrEngineResult.java`, and `TicketOcrResponse.java` with nullable review fields and `debugLines`.
- [x] 2.3 Update `TicketOcrService.java` to pass through the richer transient parser output without adding catalog, enrichment, scanner, or persistence logic.
- [x] 3.1 Update `src/main/resources/static/index.html` and `src/main/resources/static/css/styles.css` to add accessible debug disclosure and candidate-field presentation.
- [x] 3.2 Update `src/main/resources/static/js/supermarket.js` to render useful fields first and keep debug detail collapsed by default.
- [x] 4.1 Re-run `mvn -Dtest=TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests test` and `node --check src/main/resources/static/js/supermarket.js` until RED→GREEN stays clean.
- [x] 4.2 Remove any temporary fixtures or scaffolding and confirm no product/catalog/enrichment/scanner code was introduced.

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `mvn "-Dtest=TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests" test` → PASS, exit 0, `Tests run: 144, Failures: 0, Errors: 0, Skipped: 0` |
| Runtime harness command/scenario and exact result | `node --check "src/main/resources/static/js/supermarket.js"` → PASS, exit 0, syntax check clean |
| Rollback boundary | Revert only `TicketOcrCandidateParser*`, OCR DTO/service response wiring, OCR review UI markup/styles/JS, and OCR-focused test updates; no catalog, enrichment, scanner-session, or persistence behavior outside OCR review is required to roll back |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/TicketOcrCandidateParserTests.java` | Unit | ✅ `mvn ...` baseline 141/141 + `node --check` exit 0 | ✅ Written — Added failing Vea/Gómez Pardo parser cases before production changes | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ 2 supported block cases + malformed partial block + arbitrary garbage regression | ✅ Parser helpers extracted for block parsing, normalization, and non-reviewable-line filtering |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ `mvn ...` baseline 141/141 | ✅ Written — Added additive `debugLines`/transient assertions before DTO-service changes | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ Candidate field assertions + debug payload assertions | ➖ None needed beyond DTO passthrough cleanup |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` + `src/test/resources/static-ui-contract-tests.mjs` | Static UI | ✅ `mvn ...` baseline 141/141 + `node --check` exit 0 | ✅ Written — Added hidden debug disclosure and useful-field contract assertions first | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ Static contract + behavior harness assertions | ✅ UI rendering split into candidate/debug render paths |
| 2.1 | `src/test/java/com/gentleia/landingtarjetas/TicketOcrCandidateParserTests.java` | Unit | ✅ Covered by parser safety net above | ✅ Written — Implemented only after parser RED cases existed | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ Block happy paths + malformed path + arbitrary garbage path | ✅ Normalization/block helpers kept parser bounded |
| 2.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ Covered by controller safety net above | ✅ Written — DTO additions were driven by failing API assertions | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ Candidate additive fields + debug payload | ✅ Added auxiliary constructors to keep existing call sites stable |
| 2.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ Covered by controller safety net above | ✅ Written — Service passthrough changed only after transient response tests failed | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ Debug + candidate passthrough asserted together | ➖ None needed beyond safe list passthrough |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI | ✅ Covered by UI safety net above | ✅ Written — Added markup/style disclosure assertions first | ✅ Passed — Included in focused `mvn ...` pass 144/144 | ✅ Useful-field columns + hidden debug disclosure | ✅ Minimal markup/style additions only |
| 3.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` + `src/test/resources/static-ui-contract-tests.mjs` | Static UI | ✅ Covered by UI safety net above | ✅ Written — Added behavior assertions before JS wiring changes | ✅ Passed — Included in focused `mvn ...` pass 144/144 and `node --check` pass | ✅ Upload, selection, debug rendering, discard flow | ✅ Debug rendering isolated in helper |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/{TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests}.java` | Verification | ✅ N/A (verification task) | ✅ Written — Verification command captured after RED work existed | ✅ Passed — Final `mvn ...` pass 144/144 and `node --check` exit 0 | ➖ Single verification sweep | ➖ None needed |
| 4.2 | Same focused suite | Verification | ✅ N/A (verification task) | ✅ Written — Existing RED tests guard against out-of-scope behavior | ✅ Passed — Final `mvn ...` pass 144/144 confirms no extra scope introduced | ➖ Structural scope check only | ✅ No temporary scaffolding left in source |

## Test Summary

- Total tests written: 6 new/expanded OCR-focused cases across unit, integration, and static UI layers
- Total tests passing: 144 in the focused suite
- Layers used: Unit (parser), Integration (controller), Static UI (contract + JS harness)
- Approval tests: None — no legacy behavior refactor task required approval capture beyond safety-net reruns
- Pure functions created: 4 parser/debug helpers (`normalizeLine`, `parseSupportedBlock`, `parseQuantity`, `debugLine`)

## Notes

- Debug noise remains transient and hidden by default behind a disclosure.
- Arbitrary non-product OCR garbage now falls back to debug noise instead of entering `lineCandidates`.
- Candidate confirmation still uses the existing manual observation flow only.
- No product/catalog/enrichment/scanner/runtime-swap code was added.
