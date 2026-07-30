# Supermarket Mobile Navigation Specification

## Purpose

Define accessible internal supermarket navigation that splits the current dense panel into List, Barcode, Tickets, and Categories sections without removing any existing supermarket controls.

## Requirements

### Requirement: Accessible supermarket nested tabs

The system MUST expose List, Barcode, Tickets, and Categories as nested supermarket tabs using an accessible tab pattern with one active panel at a time.

#### Scenario: Select a supermarket section
- GIVEN the user opens the supermarket area
- WHEN the user activates a nested supermarket tab
- THEN the system MUST mark that tab as active and show only its associated panel

#### Scenario: Keyboard-accessible section switching
- GIVEN focus is on the nested supermarket tab list
- WHEN the user navigates and activates another supermarket tab with the keyboard
- THEN the system MUST move focus predictably and reveal the associated panel

### Requirement: Compact mobile supermarket navigation

The system MUST provide a compact mobile navigation for the nested supermarket tabs and SHALL keep all four destinations reachable without horizontal-trap overflow.

#### Scenario: Mobile access to every section
- GIVEN a mobile viewport
- WHEN the supermarket navigation is rendered
- THEN List, Barcode, Tickets, and Categories MUST each remain directly reachable

#### Scenario: Dense labels do not hide navigation
- GIVEN a small mobile viewport
- WHEN the nested supermarket navigation wraps or compresses
- THEN the system MUST keep every tab operable and MUST NOT hide a section behind overflow-only access

### Requirement: Existing supermarket controls remain reachable

The system MUST preserve current supermarket behavior by keeping existing controls reachable inside the new sections.

#### Scenario: List and Barcode controls survive regrouping
- GIVEN the supermarket content is reorganized into nested tabs
- WHEN the user opens List or Barcode
- THEN inventory/list flows, manual barcode entry, and barcode review or association controls MUST remain reachable

#### Scenario: Tickets and Categories controls survive regrouping
- GIVEN the supermarket content is reorganized into nested tabs
- WHEN the user opens Tickets or Categories
- THEN ticket session or OCR review flows, product management, and category management controls MUST remain reachable
