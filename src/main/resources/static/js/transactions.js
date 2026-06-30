import { cardLabel, escapeHtml, formatDate, formatPesos, formatUsd, typeLabel } from "./utils.js";

let categories = [];
let lastTransactions = [];

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
    categorySelect.value = currentValue;
}

export function transactionFilters(month) {
    return {
        month,
        card: document.querySelector("#filter-card").value,
        category: document.querySelector("#filter-category").value,
        type: document.querySelector("#filter-type").value
    };
}

export function renderTransactions(transactions) {
    lastTransactions = transactions;
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
}

export function rerenderTransactionsAfterSearch() {
    renderTransactions(lastTransactions);
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
