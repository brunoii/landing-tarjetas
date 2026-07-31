# Tasks: Ticket OCR Barcode Catalog — Slice 1 OCR Cleanup

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 180-260 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-forecast |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Lock OCR cleanup contract with failing parser/UI tests for bounded Vea/Gómez Pardo blocks and hidden debug noise. | PR 1 | `mvn -Dtest=TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests test` | `node --check src/main/resources/static/js/supermarket.js` | Remove only the new RED tests and fixture assertions. |
| 2 | Implement parser-only normalization/blocking and additive transient response fields without catalog or persistence changes. | PR 1 | `mvn -Dtest=TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests test` | `mvn -DskipTests spring-boot:run` against a synthetic OCR upload scenario | Revert `TicketOcrCandidateParser` and response DTO changes only. |
| 3 | Wire the static review UI to render useful candidates first and keep debug closed by default. | PR 1 | `mvn -Dtest=StaticUiContractTests test` | `node --check src/main/resources/static/js/supermarket.js` in the browser flow | Revert `index.html`, `styles.css`, and `supermarket.js` UI-only edits. |

## Phase 1: RED — Contract Tests First

- [x] 1.1 Add failing table-driven parser cases in `TicketOcrCandidateParserTests` for Vea/Gómez Pardo block boundaries, normalized amounts, and no inferred product IDs.
- [x] 1.2 Add failing `SupermarketControllerTests` for additive `debugLines` plus safe transient OCR responses with no repository writes.
- [x] 1.3 Add failing `StaticUiContractTests` for hidden-by-default debug disclosure and unchanged OCR review IDs/API contract.

## Phase 2: GREEN — Parser and DTO Cleanup Only

- [x] 2.1 Update `TicketOcrCandidateParser.java` to normalize whitespace/amounts and emit bounded useful candidates plus debug-only noise lines.
- [x] 2.2 Update `TicketOcrLineCandidateResponse.java`, `TicketOcrEngineResult.java`, and `TicketOcrResponse.java` with nullable review fields and `debugLines`.
- [x] 2.3 Update `TicketOcrService.java` to pass through the richer transient parser output without adding catalog, enrichment, scanner, or persistence logic.

## Phase 3: GREEN — Static UI Wiring

- [x] 3.1 Update `src/main/resources/static/index.html` and `src/main/resources/static/css/styles.css` to add accessible debug disclosure and candidate-field presentation.
- [x] 3.2 Update `src/main/resources/static/js/supermarket.js` to render useful fields first and keep debug detail collapsed by default.

## Phase 4: REFACTOR / VERIFY

- [x] 4.1 Re-run `mvn -Dtest=TicketOcrCandidateParserTests,SupermarketControllerTests,StaticUiContractTests test` and `node --check src/main/resources/static/js/supermarket.js` until RED→GREEN stays clean.
- [x] 4.2 Remove any temporary fixtures or scaffolding and confirm no product/catalog/enrichment/scanner code was introduced.
