# App Shell Domain Navigation Specification

## Purpose

Define one route-backed drawer for Home and Stock that works on desktop and mobile without changing existing stock business rules.

## Requirements

### Requirement: Route-backed shell destinations

The system MUST expose one shared hamburger drawer for shell navigation. The drawer MUST include Home for Monthly summary and Stock destinations for List, Barcode, Tickets, and Stock categories. Hash routes SHALL activate the matching destination, and the default route MUST resolve to Home.

#### Scenario: Default route opens Home summary
- GIVEN the shell loads without a supported hash route
- WHEN initial navigation is resolved
- THEN the system MUST activate Home summary

#### Scenario: Route selects the requested Stock destination
- GIVEN the user activates a Stock destination from the drawer
- WHEN the hash route changes
- THEN the system MUST reveal only the matching Stock destination

#### Scenario: Browser history restores the destination
- GIVEN the user has moved between Home and Stock destinations
- WHEN browser back or forward is used
- THEN the system MUST restore the destination represented by the current hash route

### Requirement: Accessible drawer interaction

The system MUST provide keyboard-operable drawer behavior. Opening the drawer SHALL move focus into the drawer, activating a destination SHALL move focus to the activated content, and dismissing the drawer with Escape or a close action MUST return focus to the menu trigger.

#### Scenario: Opening the drawer moves focus inside
- GIVEN focus is on the menu trigger
- WHEN the user opens the drawer
- THEN the first actionable drawer item MUST receive focus

#### Scenario: Escape closes and restores focus
- GIVEN the drawer is open
- WHEN the user presses Escape
- THEN the drawer MUST close and focus MUST return to the menu trigger

### Requirement: Stock handoff preserves runtime state

The system MUST treat List as the default Stock entry when a Stock route omits a sub-destination. Switching between Home and Stock SHALL preserve in-memory stock state during the current runtime and MUST NOT reset unsaved stock context solely because shell navigation changed.

#### Scenario: Stock root defaults to List
- GIVEN the user enters the Stock domain without a specific Stock destination
- WHEN the shell resolves that route
- THEN the system MUST activate Stock List

#### Scenario: Shell navigation does not reset current Stock runtime state
- GIVEN the user has active in-memory Stock context
- WHEN the user moves to Home and back to Stock in the same runtime
- THEN the system MUST preserve that Stock context
