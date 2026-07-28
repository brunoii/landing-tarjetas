# Delta for super-inventory

## ADDED Requirements

### Requirement: Sectioned supermarket navigation preserves inventory behavior

The system MUST preserve existing list, barcode, ticket, and category behavior while relocating those surfaces into supermarket sub-tabs. Navigation changes MUST NOT mutate stock, checked state, aliases, prices, or movement history.

#### Scenario: Tab change is non-mutating
- GIVEN supermarket data already exists
- WHEN the user switches between list, barcode, tickets, and categories
- THEN the system MUST only change the visible section
- AND MUST NOT change inventory data or history

#### Scenario: Existing flows remain reachable
- GIVEN the supermarket area uses sub-tabs
- WHEN the user needs list, barcode, ticket, or category flows
- THEN the system MUST preserve access to each existing flow
