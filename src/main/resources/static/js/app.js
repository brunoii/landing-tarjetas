import { api } from "./api.js";
import { categoryFormPayload, renderCategories, resetCategoryForm, showCategoryFeedback } from "./categories.js";
import { renderDashboard } from "./dashboard.js";
import { renderDraftStatementList, setStatementCategories, setupStatementUpload } from "./statements.js";
import { renderTransactions, rerenderTransactionsAfterSearch, setTransactionCategories, transactionFilters } from "./transactions.js";
import { currentYearMonth } from "./utils.js";

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
        setStatus("Loading dashboard data...");
        const [summary, statements, transactions, allStatements] = await Promise.all([
            api.summary(state.month),
            api.statements({ month: state.month }),
            api.transactions({ month: state.month }),
            api.statements()
        ]);
        const confirmedStatements = statements.filter((statement) => statement.status === "CONFIRMED");
        const confirmedAllStatements = allStatements.filter((statement) => statement.status === "CONFIRMED");
        renderDashboard({ month: state.month, summary, statements: confirmedStatements, transactions, allStatements: confirmedAllStatements });
        renderDraftStatementList(allStatements);
        renderTransactions(transactions);
        setStatus(confirmedStatements.length || transactions.length
            ? "Loaded confirmed dashboard data."
            : statements.length
                ? "Draft statements are waiting for review; public dashboard data is still empty for this month."
                : "No statements or transactions are loaded for this month yet.");
    } catch (error) {
        setStatus(error.message, true);
    }
}

async function loadTransactions() {
    try {
        setStatus("Loading transactions...");
        const transactions = await api.transactions(transactionFilters(state.month));
        renderTransactions(transactions);
        setStatus(transactions.length ? "Transactions loaded." : "No transactions match the selected API filters.");
    } catch (error) {
        setStatus(error.message, true);
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
        showCategoryFeedback(error.message, true);
    }
}

async function createCategory() {
    try {
        await api.createCategory(categoryFormPayload());
        resetCategoryForm();
        showCategoryFeedback("Category created.");
        await loadCategories();
    } catch (error) {
        showCategoryFeedback(error.message, true);
    }
}

async function updateCategory(id, payload) {
    try {
        await api.updateCategory(id, payload);
        showCategoryFeedback("Category updated.");
        await loadCategories();
    } catch (error) {
        showCategoryFeedback(error.message, true);
    }
}

async function deleteCategory(id) {
    try {
        await api.deleteCategory(id);
        showCategoryFeedback("Category deleted or deactivated safely.");
        await loadCategories();
    } catch (error) {
        showCategoryFeedback(error.message, true);
    }
}

function setStatus(message, isError = false) {
    const status = document.querySelector("#app-status");
    status.textContent = message;
    status.classList.toggle("error", isError);
}
