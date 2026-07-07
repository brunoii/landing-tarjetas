import { api } from "./api.js";
import { categoryFormPayload, renderCategories, resetCategoryForm, showCategoryFeedback } from "./categories.js";
import { renderDashboard } from "./dashboard.js?v=20260707-dashboard-spanish-copy";
import { renderDraftStatementList, setStatementCategories, setupStatementUpload } from "./statements.js";
import { renderTransactions, rerenderTransactionsAfterSearch, resetTransactionFilters, setTransactionCategories, transactionFilters } from "./transactions.js";
import { currentYearMonth, setButtonBusy } from "./utils.js";

const state = {
    month: currentYearMonth(),
    categories: []
};

document.addEventListener("DOMContentLoaded", () => {
    const monthInput = document.querySelector("#month-input");
    monthInput.value = state.month;
    monthInput.addEventListener("change", () => {
        state.month = monthInput.value || currentYearMonth();
        loadDashboard();
    });

    document.querySelector("#transaction-filters").addEventListener("submit", (event) => {
        event.preventDefault();
        loadTransactions();
    });
    document.querySelector("#filter-search").addEventListener("input", rerenderTransactionsAfterSearch);
    document.querySelector("#clear-transaction-filters").addEventListener("click", () => {
        resetTransactionFilters();
        loadTransactions();
    });

    document.querySelector("#category-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await createCategory();
    });

    setupStatementUpload({
        onDraftChanged: loadDashboard,
        onDraftConfirmed: async (statement) => {
            state.month = String(statement.paymentMonth || state.month).slice(0, 7);
            monthInput.value = state.month;
            await loadDashboard();
        },
        setStatus
    });

    loadAll();
});

async function loadAll() {
    await loadCategories();
    await loadDashboard();
}

async function loadDashboard() {
    try {
        setStatus("Cargando datos del panel...", false, true);
        const [summary, statements, transactions, allStatements, months, monthDetail] = await Promise.all([
            api.summary(state.month),
            api.statements({ month: state.month }),
            api.transactions({ month: state.month }),
            api.statements(),
            api.dashboardMonths(),
            api.dashboardMonthDetail(state.month)
        ]);
        const confirmedStatements = statements.filter((statement) => statement.status === "CONFIRMED");
        const confirmedAllStatements = allStatements.filter((statement) => statement.status === "CONFIRMED");
        renderDashboard({
            month: state.month,
            summary,
            statements: confirmedStatements,
            transactions,
            allStatements: confirmedAllStatements,
            months,
            monthDetail
        });
        renderDraftStatementList(allStatements);
        renderTransactions(transactions, state.month);
        setStatus(monthDetail.projectionOnly
            ? "Datos proyectados de cuotas cargados para un mes futuro."
            : confirmedStatements.length || transactions.length
                ? "Datos confirmados del panel cargados."
                : statements.length
                ? "Hay borradores pendientes de revisión; el panel público sigue vacío para este mes."
                : "Todavía no hay resúmenes ni transacciones cargadas para este mes.");
    } catch (error) {
        setStatus(`No se pudieron cargar los datos del panel: ${error.message}`, true);
    }
}

async function loadTransactions() {
    try {
        setStatus("Cargando transacciones...", false, true);
        const transactions = await api.transactions(transactionFilters(state.month));
        renderTransactions(transactions, state.month);
        setStatus(transactions.length ? "Transacciones cargadas con los filtros seleccionados." : "No hay transacciones confirmadas que coincidan con los filtros seleccionados.");
    } catch (error) {
        setStatus(`No se pudieron cargar las transacciones: ${error.message}`, true);
    }
}

async function loadCategories() {
    try {
        state.categories = await api.categories();
        setTransactionCategories(state.categories);
        setStatementCategories(state.categories);
        renderCategories(state.categories, {
            onUpdate: updateCategory,
            onDelete: deleteCategory
        });
    } catch (error) {
        showCategoryFeedback(`No se pudieron cargar las categorías: ${error.message}`, true);
    }
}

async function createCategory() {
    const button = document.querySelector("#category-form button[type='submit']");
    try {
        setButtonBusy(button, true, "Creando...");
        await api.createCategory(categoryFormPayload());
        resetCategoryForm();
        showCategoryFeedback("Categoría creada.");
        await loadCategories();
        await loadTransactions();
    } catch (error) {
        showCategoryFeedback(`No se pudo crear la categoría: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function updateCategory(id, payload) {
    try {
        await api.updateCategory(id, payload);
        showCategoryFeedback("Categoría actualizada.");
        await loadCategories();
        await loadTransactions();
    } catch (error) {
        showCategoryFeedback(`No se pudo actualizar la categoría: ${error.message}`, true);
    }
}

async function deleteCategory(id) {
    try {
        await api.deleteCategory(id);
        showCategoryFeedback("Categoría eliminada o desactivada de forma segura.");
        await loadCategories();
        await loadTransactions();
    } catch (error) {
        showCategoryFeedback(`No se pudo eliminar la categoría: ${error.message}`, true);
    }
}

function setStatus(message, isError = false, isLoading = false) {
    const status = document.querySelector("#app-status");
    status.textContent = message;
    status.classList.toggle("error", isError);
    status.classList.toggle("loading", isLoading && !isError);
}
