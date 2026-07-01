import { cardLabel, escapeHtml, formatDate, formatMonth, formatPesos, formatUsd, typeLabel } from "./utils.js";

let categories = [];
let lastTransactions = [];
let lastMonth = "";

export function setTransactionCategories(nextCategories) {
    categories = nextCategories;
    const categorySelect = document.querySelector("#filter-category");
    const currentValue = categorySelect.value;
    categorySelect.innerHTML = '<option value="">All categories</option>';
    categories.forEach((category) => {
        const option = document.createElement("option");
        option.value = category.id;
        option.textContent = category.name;
        categorySelect.append(option);
    });
    categorySelect.value = categories.some((category) => String(category.id) === currentValue) ? currentValue : "";
    renderFilterSummary(lastTransactions, lastMonth);
}

export function transactionFilters(month) {
    return {
        month,
        card: document.querySelector("#filter-card").value,
        category: document.querySelector("#filter-category").value,
        type: document.querySelector("#filter-type").value
    };
}

export function renderTransactions(transactions, month = lastMonth) {
    lastTransactions = transactions;
    lastMonth = month;
    const search = document.querySelector("#filter-search").value.trim().toLowerCase();
    const visibleTransactions = search ? transactions.filter((transaction) => matchesSearch(transaction, search)) : transactions;
    const table = document.querySelector("#transactions-table");
    const empty = document.querySelector("#transactions-empty");
    table.innerHTML = "";

    visibleTransactions.forEach((transaction) => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${formatDate(transaction.transactionDate)}</td>
            <td>${escapeHtml(cardLabel(transaction.cardBrand))}</td>
            <td>${escapeHtml(transaction.description || "—")}</td>
            <td>${escapeHtml(typeLabel(transaction.type))}</td>
            <td>${escapeHtml(transaction.category?.name || "Uncategorized")}</td>
            <td>${installmentText(transaction)}</td>
            <td class="amount">${formatPesos(transaction.amountPesos)}</td>
            <td class="amount">${formatUsd(transaction.amountUsd)}</td>
            <td>${escapeHtml(transaction.notes || "—")}</td>
            <td><button type="button" class="secondary-button" disabled title="Editing is reserved for a later safe workflow.">Edit</button></td>
        `;
        table.append(row);
    });

    empty.hidden = visibleTransactions.length > 0;
    empty.textContent = emptyMessage(transactions.length, search);
    renderFilterSummary(visibleTransactions, month);
}

export function rerenderTransactionsAfterSearch() {
    renderTransactions(lastTransactions, lastMonth);
}

export function resetTransactionFilters() {
    document.querySelector("#filter-card").value = "";
    document.querySelector("#filter-category").value = "";
    document.querySelector("#filter-type").value = "";
    document.querySelector("#filter-search").value = "";
}

function renderFilterSummary(visibleTransactions, month) {
    const summary = document.querySelector("#filters-summary");
    const parts = [`Month: ${formatMonth(month)}`];
    const card = document.querySelector("#filter-card").value;
    const category = document.querySelector("#filter-category");
    const type = document.querySelector("#filter-type").value;
    const search = document.querySelector("#filter-search").value.trim();

    if (card) {
        parts.push(`Card: ${cardLabel(card)}`);
    }
    if (category.value) {
        parts.push(`Category: ${category.selectedOptions[0]?.textContent || "Selected category"}`);
    }
    if (type) {
        parts.push(`Type: ${typeLabel(type)}`);
    }
    if (search) {
        parts.push(`Search: "${search}"`);
    }

    summary.textContent = `${visibleTransactions.length} confirmed row${visibleTransactions.length === 1 ? "" : "s"}. ${parts.join(" · ")}.`;
}

function emptyMessage(apiRowCount, search) {
    if (apiRowCount > 0 && search) {
        return "Confirmed rows loaded, but none match the current text search.";
    }
    if (apiRowCount > 0) {
        return "Confirmed rows loaded, but none match the current browser filters.";
    }
    return "No confirmed transactions match the selected month, card, category, and type filters.";
}

function matchesSearch(transaction, search) {
    return [
        transaction.description,
        transaction.notes,
        transaction.category?.name,
        cardLabel(transaction.cardBrand),
        typeLabel(transaction.type)
    ].some((value) => String(value || "").toLowerCase().includes(search));
}

function installmentText(transaction) {
    if (!transaction.currentInstallment && !transaction.totalInstallments) {
        return "—";
    }
    return `${transaction.currentInstallment || "?"}/${transaction.totalInstallments || "?"}`;
}
