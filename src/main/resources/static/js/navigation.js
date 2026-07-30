import { activateSupermarketEntry } from "./supermarket.js";

export const DEFAULT_PRIMARY_TAB_ID = "summary";
export const DEFAULT_SUPERMARKET_TAB_ID = "list";
export const DEFAULT_ROUTE_HASH = "#monthly/summary";

const MONTHLY_SECTIONS = new Set(["summary", "expenses-upload", "expenses-table", "income-table", "income-upload", "simulator", "categories"]);
const STOCK_SECTIONS = new Set(["list", "barcode", "tickets", "categories"]);
const LEGACY_HASHES = new Set(["#summary", "#supermarket"]);
const MONTHLY_FOCUS_SELECTORS = {
    summary: "#summary-home-title",
    "expenses-upload": "#upload-title",
    "expenses-table": "#transactions-title",
    "income-table": "#income-table-title",
    "income-upload": "#income-upload-title",
    simulator: "#simulator-title",
    categories: "#categories-title"
};
const STOCK_FOCUS_SELECTORS = {
    list: "#supermarket-title",
    barcode: "#super-barcode-title",
    tickets: "#super-ticket-ocr-title",
    categories: "#super-category-title"
};

export function parseHash(hash = "") {
    const normalized = normalizeHash(hash);
    if (!normalized || normalized === "#" || LEGACY_HASHES.has(normalized)) {
        return monthlyRoute(DEFAULT_PRIMARY_TAB_ID);
    }

    const [domain, section = ""] = normalized.slice(1).split("/");
    if (domain === "monthly") {
        return MONTHLY_SECTIONS.has(section) ? monthlyRoute(section) : monthlyRoute(DEFAULT_PRIMARY_TAB_ID);
    }
    if (domain === "stock") {
        return stockRoute(STOCK_SECTIONS.has(section) ? section : DEFAULT_SUPERMARKET_TAB_ID);
    }
    return monthlyRoute(DEFAULT_PRIMARY_TAB_ID);
}

export function setupTabs({
    buttons = [],
    panels = [],
    defaultTabId = "",
    targetDatasetKey = "tabTarget",
    panelDatasetKey = "tabPanel",
    onActivated = null
} = {}) {
    if (buttons.length === 0 || panels.length === 0) {
        return;
    }

    const tabIds = buttons
        .map((button) => button?.dataset?.[targetDatasetKey])
        .filter(Boolean);
    const fallbackTabId = tabIds.includes(defaultTabId) ? defaultTabId : tabIds[0];

    const activateTab = (requestedTabId, { shouldFocus = false, emitChange = false } = {}) => {
        const activeTabId = tabIds.includes(requestedTabId) ? requestedTabId : fallbackTabId;

        buttons.forEach((button) => {
            const selected = button.dataset[targetDatasetKey] === activeTabId;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-selected", String(selected));
            button.tabIndex = selected ? 0 : -1;
            if (selected && shouldFocus) {
                button.focus?.();
            }
        });

        panels.forEach((panel) => {
            panel.hidden = panel.dataset[panelDatasetKey] !== activeTabId;
        });

        if (emitChange) {
            onActivated?.(activeTabId);
        }
    };

    buttons.forEach((button, index) => {
        button.addEventListener("click", () => activateTab(button.dataset[targetDatasetKey], { emitChange: true }));
        button.addEventListener("keydown", (event) => {
            const nextIndex = keyboardTabIndex(event.key, index, buttons.length);
            if (nextIndex === index) {
                return;
            }
            event.preventDefault();
            activateTab(buttons[nextIndex].dataset[targetDatasetKey], { shouldFocus: true, emitChange: true });
        });
    });

    activateTab(fallbackTabId);
}

export function setupPrimaryTabs() {
    setupShellDrawer();
    setupTabs({
        buttons: querySelectorAllOptional("[data-super-tab-target]"),
        panels: querySelectorAllOptional("[data-super-tab-panel]"),
        defaultTabId: DEFAULT_SUPERMARKET_TAB_ID,
        targetDatasetKey: "superTabTarget",
        panelDatasetKey: "superTabPanel",
        onActivated: (tabId) => syncHashForSupermarketTab(tabId)
    });
}

export function syncPrimaryRouteFromLocation({ replace = false } = {}) {
    const route = parseHash(globalThis.location?.hash);
    const currentHash = normalizeHash(globalThis.location?.hash);
    if (replace || currentHash !== route.hash) {
        globalThis.history?.replaceState?.(null, "", route.hash);
        if (globalThis.location) {
            globalThis.location.hash = route.hash;
        }
    }
    activateRoute(route);
    return route;
}

function activateRoute(route, { focusTarget = "none" } = {}) {
    querySelectorAllOptional("[data-tab-panel]").forEach((panel) => {
        panel.hidden = panel.dataset.tabPanel !== route.primaryPanel;
    });

    if (route.primaryPanel === "supermarket") {
        activateSupermarketEntry(route.supermarketTab || DEFAULT_SUPERMARKET_TAB_ID);
    }

    querySelectorAllOptional("[data-shell-route]").forEach((link) => {
        if (link.dataset.shellRoute === route.hash) {
            link.setAttribute("aria-current", "page");
        } else {
            link.removeAttribute?.("aria-current");
        }
    });

    setDrawerOpen(false);
    if (focusTarget === "content") {
        focusRouteContent(route);
    }
}

function setupShellDrawer() {
    const trigger = querySelectorOptional("#app-shell-menu-button");
    const drawer = querySelectorOptional("#app-shell-drawer");
    const closeButton = querySelectorOptional("#app-shell-drawer-close");
    if (!trigger || !drawer) {
        return;
    }

    setDrawerOpen(false);
    trigger.addEventListener("click", () => setDrawerOpen(drawer.hidden !== false));
    closeButton?.addEventListener("click", () => setDrawerOpen(false, { restoreFocus: true }));
    globalThis.document?.addEventListener?.("keydown", (event) => {
        if (event.key !== "Escape" || drawer.hidden !== false) {
            return;
        }
        event.preventDefault?.();
        setDrawerOpen(false, { restoreFocus: true });
    });

    querySelectorAllOptional("[data-shell-route]").forEach((link) => {
        link.addEventListener("click", (event) => {
            event.preventDefault?.();
            navigateToRoute(parseHash(link.dataset.shellRoute), { focusTarget: "content" });
        });
    });
}

function navigateToRoute(route, { focusTarget = "none" } = {}) {
    if (normalizeHash(globalThis.location?.hash) !== route.hash && globalThis.location) {
        globalThis.location.hash = route.hash;
    }
    activateRoute(route, { focusTarget });
}

function setDrawerOpen(open, { restoreFocus = false } = {}) {
    const trigger = querySelectorOptional("#app-shell-menu-button");
    const drawer = querySelectorOptional("#app-shell-drawer");
    if (!trigger || !drawer) {
        return;
    }
    trigger.setAttribute("aria-expanded", String(open));
    drawer.hidden = !open;
    if (open) {
        querySelectorAllOptional("[data-shell-route]")[0]?.focus?.();
        return;
    }
    if (restoreFocus) {
        trigger.focus?.();
    }
}

function syncHashForSupermarketTab(tabId) {
    const supermarketPanel = querySelectorOptional("#tab-supermarket");
    if (!supermarketPanel || supermarketPanel.hidden) {
        return;
    }
    const hash = stockRoute(tabId).hash;
    if (normalizeHash(globalThis.location?.hash) !== hash && globalThis.location) {
        globalThis.location.hash = hash;
    }
}

function focusRouteContent(route) {
    querySelectorOptional(route.focusSelector)?.focus?.();
}

function monthlyRoute(section) {
    return {
        domain: "monthly",
        section,
        hash: `#monthly/${section}`,
        primaryPanel: section,
        focusSelector: MONTHLY_FOCUS_SELECTORS[section] || "#summary-home-title"
    };
}

function stockRoute(section) {
    return {
        domain: "stock",
        section,
        hash: `#stock/${section}`,
        primaryPanel: "supermarket",
        supermarketTab: section,
        focusSelector: STOCK_FOCUS_SELECTORS[section] || "#supermarket-title"
    };
}

function normalizeHash(hash = "") {
    const route = String(hash || "").trim().toLowerCase();
    if (!route) {
        return "";
    }
    return route.startsWith("#") ? route : `#${route}`;
}

function querySelectorOptional(selector) {
    try {
        return globalThis.document?.querySelector?.(selector) ?? null;
    } catch {
        return null;
    }
}

function querySelectorAllOptional(selector) {
    try {
        return [...(globalThis.document?.querySelectorAll?.(selector) ?? [])];
    } catch {
        return [];
    }
}

function keyboardTabIndex(key, currentIndex, tabCount) {
    if (key === "ArrowRight") {
        return (currentIndex + 1) % tabCount;
    }
    if (key === "ArrowLeft") {
        return (currentIndex - 1 + tabCount) % tabCount;
    }
    if (key === "Home") {
        return 0;
    }
    if (key === "End") {
        return tabCount - 1;
    }
    return currentIndex;
}
