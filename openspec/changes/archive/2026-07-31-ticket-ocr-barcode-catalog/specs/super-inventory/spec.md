# Delta for super-inventory

## ADDED Requirements

### Requirement: Bounded ticket OCR review candidates

The system MUST normalize OCR lines into bounded product blocks for the supported Vea and Gómez Pardo ticket formats. Each useful candidate MUST expose only reviewable extracted fields such as barcode or store code, description, quantity, unit price, line total, tax, and warnings when present. The system MUST keep catalog lookup, enrichment, scanner-session behavior, and automatic inventory or price mutation out of this slice.

#### Scenario: Vea or Gómez Pardo block becomes a useful candidate
- GIVEN OCR text from a supported Vea or Gómez Pardo ticket block
- WHEN the user requests OCR review candidates
- THEN the system MUST return one review candidate for that block with available code, quantity, price, tax, and warning fields
- AND the candidate MUST be suitable for manual review without requiring catalog enrichment

#### Scenario: Malformed or partial OCR block stays safe
- GIVEN OCR text containing malformed, partial, or ambiguous lines
- WHEN the user requests OCR review candidates
- THEN the system MUST ignore the block as a useful candidate or mark it with warnings
- AND MUST NOT fail the whole review request or persist partial product data

### Requirement: Ticket OCR debug separation and transient privacy boundary

The system MUST separate non-product OCR noise from useful candidates and SHOULD keep that debug detail hidden by default in the review UI. Ticket images, raw OCR text, useful candidates, and debug noise MUST remain transient review artifacts until explicit user confirmation. Before that confirmation, the system MUST NOT persist ticket data, candidate rows, catalog links, stock mutations, or price records.

#### Scenario: Debug noise stays out of the main candidate table
- GIVEN OCR text that includes headers, totals, separators, or garbage rows beside product-like blocks
- WHEN the user requests OCR review candidates
- THEN the system MUST return useful candidates separately from debug noise
- AND the main review table MUST exclude the debug-only rows by default

#### Scenario: Review data remains transient until explicit confirmation
- GIVEN a ticket upload produces useful candidates and debug detail
- WHEN the user closes, refreshes, or abandons the review without explicit confirmation
- THEN the system MUST NOT persist the image, OCR text, candidate data, debug data, stock changes, or price changes
- AND manual confirmation MUST remain the only allowed path to later persistence outside this slice
