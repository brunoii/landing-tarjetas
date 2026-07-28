# Supermarket Mobile Subtabs Specification

## Purpose

Define lower-density supermarket navigation for mobile without removing manual fallback routes.

## Requirements

### Requirement: Supermarket sub-tab structure

The system MUST expose separate supermarket sub-tabs for list, barcode, tickets, and categories instead of one dense pane.

#### Scenario: Focused section selection
- GIVEN the user opens the supermarket area
- WHEN the user selects a sub-tab
- THEN the system MUST show only the selected section as the active focus area

#### Scenario: Required destinations stay available
- GIVEN the supermarket area is rendered
- WHEN the user inspects the available sections
- THEN list, barcode, tickets, and categories MUST each remain reachable

### Requirement: Compact mobile navigation

The system MUST provide lower-density mobile navigation for those sub-tabs and SHALL keep manual fallback entry paths visible.

#### Scenario: Mobile navigation reduces density
- GIVEN a mobile viewport
- WHEN the supermarket navigation is shown
- THEN the system MUST present a compact navigation pattern without horizontal-trap overflow

#### Scenario: Manual fallback survives navigation changes
- GIVEN scanner or OCR capabilities are limited or unavailable
- WHEN the user navigates the supermarket area
- THEN the system MUST keep manual entry and review paths visible
