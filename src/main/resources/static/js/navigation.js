export const DEFAULT_PRIMARY_TAB_ID = "summary";
export const DEFAULT_SUPERMARKET_TAB_ID = "list";

export const primaryTabs = [
    { id: "summary", label: "Resumen" },
    { id: "expenses-upload", label: "Cargar Gastos" },
    { id: "expenses-table", label: "Tabla Gastos" },
    { id: "income-table", label: "Tabla Ingresos" },
    { id: "income-upload", label: "Cargar Ingresos" },
    { id: "simulator", label: "Simulador" },
    { id: "categories", label: "Categorías" },
    { id: "supermarket", label: "Lista del super" }
];

export function primaryTabViewState(activeTabId = DEFAULT_PRIMARY_TAB_ID) {
    const effectiveTabId = primaryTabs.some((tab) => tab.id === activeTabId) ? activeTabId : DEFAULT_PRIMARY_TAB_ID;
    return primaryTabs.map((tab) => ({
        id: tab.id,
        label: tab.label,
        panelHidden: tab.id !== effectiveTabId,
        selected: tab.id === effectiveTabId
    }));
}

export function setupTabs({ buttons = [], panels = [], defaultTabId = "", targetDatasetKey = "tabTarget", panelDatasetKey = "tabPanel" } = {}) {
    if (buttons.length === 0 || panels.length === 0) {
        return;
    }

    const tabIds = buttons
        .map((button) => button?.dataset?.[targetDatasetKey])
        .filter(Boolean);
    const fallbackTabId = tabIds.includes(defaultTabId) ? defaultTabId : tabIds[0];

    const activateTab = (requestedTabId, shouldFocus = false) => {
        const activeTabId = tabIds.includes(requestedTabId) ? requestedTabId : fallbackTabId;

        buttons.forEach((button) => {
            const selected = button.dataset[targetDatasetKey] === activeTabId;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-selected", String(selected));
            button.tabIndex = selected ? 0 : -1;
            if (selected && shouldFocus) {
                button.focus();
            }
        });

        panels.forEach((panel) => {
            panel.hidden = panel.dataset[panelDatasetKey] !== activeTabId;
        });
    };

    buttons.forEach((button, index) => {
        button.addEventListener("click", () => activateTab(button.dataset[targetDatasetKey]));
        button.addEventListener("keydown", (event) => {
            const nextIndex = keyboardTabIndex(event.key, index, buttons.length);
            if (nextIndex === index) {
                return;
            }
            event.preventDefault();
            activateTab(buttons[nextIndex].dataset[targetDatasetKey], true);
        });
    });

    activateTab(fallbackTabId);
}

export function setupPrimaryTabs() {
    setupTabs({
        buttons: [...document.querySelectorAll("[data-tab-target]")],
        panels: [...document.querySelectorAll("[data-tab-panel]")],
        defaultTabId: DEFAULT_PRIMARY_TAB_ID
    });

    setupTabs({
        buttons: [...document.querySelectorAll("[data-super-tab-target]")],
        panels: [...document.querySelectorAll("[data-super-tab-panel]")],
        defaultTabId: DEFAULT_SUPERMARKET_TAB_ID,
        targetDatasetKey: "superTabTarget",
        panelDatasetKey: "superTabPanel"
    });
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
