export const DEFAULT_PRIMARY_TAB_ID = "summary";

export const primaryTabs = [
    { id: "summary", label: "Resumen" },
    { id: "expenses-upload", label: "Cargar Gastos" },
    { id: "expenses-table", label: "Tabla Gastos" },
    { id: "income-table", label: "Tabla Ingresos" },
    { id: "income-upload", label: "Cargar Ingresos" },
    { id: "simulator", label: "Simulador" },
    { id: "categories", label: "Categorías" }
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

export function setupPrimaryTabs() {
    const tabButtons = [...document.querySelectorAll("[data-tab-target]")];
    const tabSections = [...document.querySelectorAll("[data-tab-panel]")];

    if (tabButtons.length === 0 || tabSections.length === 0) {
        return;
    }

    const activateTab = (tabId, shouldFocus = false) => {
        const viewState = primaryTabViewState(tabId);

        viewState.forEach((state) => {
            tabButtons
                .filter((button) => button.dataset.tabTarget === state.id)
                .forEach((button) => {
                    button.classList.toggle("active", state.selected);
                    button.setAttribute("aria-selected", String(state.selected));
                    button.tabIndex = state.selected ? 0 : -1;
                    if (state.selected && shouldFocus) {
                        button.focus();
                    }
                });

            tabSections
                .filter((section) => section.dataset.tabPanel === state.id)
                .forEach((section) => {
                    section.hidden = state.panelHidden;
                });
        });
    };

    tabButtons.forEach((button, index) => {
        button.addEventListener("click", () => activateTab(button.dataset.tabTarget));
        button.addEventListener("keydown", (event) => {
            const nextIndex = keyboardTabIndex(event.key, index, tabButtons.length);
            if (nextIndex === index) {
                return;
            }
            event.preventDefault();
            activateTab(tabButtons[nextIndex].dataset.tabTarget, true);
        });
    });

    activateTab(DEFAULT_PRIMARY_TAB_ID);
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
