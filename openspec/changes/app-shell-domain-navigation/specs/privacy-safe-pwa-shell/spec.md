# Delta for privacy-safe-pwa-shell

## ADDED Requirements

### Requirement: Runtime-only shell navigation state

The system MUST restore shell destinations from the URL hash only. Drawer open or close state and Stock navigation context MUST remain runtime-only. The shell MUST NOT persist that navigation state in localStorage, sessionStorage, IndexedDB, or service-worker caches, and MUST NOT expand cache scope to include Stock, Barcode, Tickets, or Stock categories payloads.

#### Scenario: Reload restores only the URL-backed destination
- GIVEN the current URL hash targets Home or a Stock destination
- WHEN the page reloads
- THEN the system MUST restore only the destination represented by that hash

#### Scenario: Navigation state is never persisted as browser data
- GIVEN the user opens the drawer or changes shell destinations
- WHEN shell navigation state changes
- THEN the system MUST NOT write that state to persistent browser storage or cache

#### Scenario: Shell navigation does not widen sensitive caching
- GIVEN Stock-related traffic occurs after shell navigation
- WHEN offline shell caching rules are applied
- THEN the system MUST keep that traffic out of persistent caches
