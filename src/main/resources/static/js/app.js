import { api } from "./api.js";
import { categoryFormPayload, renderCategories, resetCategoryForm, showCategoryFeedback } from "./categories.js";
import { renderDashboard } from "./dashboard.js";
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
        setStatus("Loading dashboard data...", false, true);
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
            ? "Loaded projected installment data for a future month."
            : confirmedStatements.length || transactions.length
                ? "Loaded confirmed dashboard data."
                : statements.length
                ? "Draft statements are waiting for review; public dashboard data is still empty for this month."
                : "No statements or transactions are loaded for this month yet.");
    } catch (error) {
        setStatus(`Dashboard data could not be loaded: ${error.message}`, true);
    }
}

async function loadTransactions() {
    try {
        setStatus("Loading transactions...", false, true);
        const transactions = await api.transactions(transactionFilters(state.month));
        renderTransactions(transactions, state.month);
        setStatus(transactions.length ? "Transactions loaded with the selected filters." : "No confirmed transactions match the selected filters.");
    } catch (error) {
        setStatus(`Transactions could not be loaded: ${error.message}`, true);
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
        showCategoryFeedback(`Categories could not be loaded: ${error.message}`, true);
    }
}

async function createCategory() {
    const button = document.querySelector("#category-form button[type='submit']");
    try {
        setButtonBusy(button, true, "Creating...");
        await api.createCategory(categoryFormPayload());
        resetCategoryForm();
        showCategoryFeedback("Category created.");
        await loadCategories();
        await loadTransactions();
    } catch (error) {
        showCategoryFeedback(`Category could not be created: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function updateCategory(id, payload) {
    try {
        await api.updateCategory(id, payload);
        showCategoryFeedback("Category updated.");
        await loadCategories();
        await loadTransactions();
    } catch (error) {
        showCategoryFeedback(`Category could not be updated: ${error.message}`, true);
    }
}

async function deleteCategory(id) {
    try {
        await api.deleteCategory(id);
        showCategoryFeedback("Category deleted or deactivated safely.");
        await loadCategories();
        await loadTransactions();
    } catch (error) {
        showCategoryFeedback(`Category could not be deleted: ${error.message}`, true);
    }
}

function setStatus(message, isError = false, isLoading = false) {
    const status = document.querySelector("#app-status");
    status.textContent = message;
    status.classList.toggle("error", isError);
    status.classList.toggle("loading", isLoading && !isError);
}
