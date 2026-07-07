import { cardLabel, escapeHtml, formatDate, formatMonth, formatPesos, formatUsd, typeLabel } from "./utils.js";

let categories = [];
let lastTransactions = [];
let lastMonth = "";

export function setTransactionCategories(nextCategories) {
    categories = nextCategories;
    const categorySelect = document.querySelector("#filter-category");
    const currentValue = categorySelect.value;
    categorySelect.innerHTML = '<option value="">Todas las categorías</option>';
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
            <td>${escapeHtml(transactionCardLabel(transaction.cardBrand))}</td>
            <td>${escapeHtml(transaction.description || "—")}</td>
            <td>${escapeHtml(transactionTypeLabel(transaction.type))}</td>
            <td>${escapeHtml(transaction.category?.name || "Sin categoría")}</td>
            <td>${installmentText(transaction)}</td>
            <td class="amount">${formatPesos(transaction.amountPesos)}</td>
            <td class="amount">${formatUsd(transaction.amountUsd)}</td>
            <td>${escapeHtml(transaction.notes || "—")}</td>
            <td><button type="button" class="secondary-button" disabled title="La edición queda reservada para un flujo seguro posterior.">Editar</button></td>
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
    const parts = [`Mes: ${formatMonth(month)}`];
    const card = document.querySelector("#filter-card").value;
    const category = document.querySelector("#filter-category");
    const type = document.querySelector("#filter-type").value;
    const search = document.querySelector("#filter-search").value.trim();

    if (card) {
        parts.push(`Tarjeta: ${transactionCardLabel(card)}`);
    }
    if (category.value) {
        parts.push(`Categoría: ${category.selectedOptions[0]?.textContent || "Categoría seleccionada"}`);
    }
    if (type) {
        parts.push(`Tipo: ${transactionTypeLabel(type)}`);
    }
    if (search) {
        parts.push(`Búsqueda: "${search}"`);
    }

    summary.textContent = `${visibleTransactions.length} ${visibleTransactions.length === 1 ? "fila confirmada" : "filas confirmadas"}. ${parts.join(" · ")}.`;
}

function emptyMessage(apiRowCount, search) {
    if (apiRowCount > 0 && search) {
        return "Hay filas confirmadas cargadas, pero ninguna coincide con la búsqueda actual.";
    }
    if (apiRowCount > 0) {
        return "Hay filas confirmadas cargadas, pero ninguna coincide con los filtros actuales del navegador.";
    }
    return "No hay transacciones confirmadas que coincidan con el mes, la tarjeta, la categoría y el tipo seleccionados.";
}

function matchesSearch(transaction, search) {
    return [
        transaction.description,
        transaction.notes,
        transaction.category?.name,
        transactionCardLabel(transaction.cardBrand),
        transactionTypeLabel(transaction.type)
    ].some((value) => String(value || "").toLowerCase().includes(search));
}

function transactionCardLabel(cardBrand) {
    const labels = {
        OTHER: "Otra"
    };
    return labels[cardBrand] || cardLabel(cardBrand).replace("Unknown card", "Tarjeta desconocida");
}

function transactionTypeLabel(type) {
    const labels = {
        PURCHASE: "Compra",
        INSTALLMENT: "Cuota",
        FEE: "Cargo",
        TAX: "Impuesto",
        PAYMENT: "Pago",
        REFUND: "Reintegro",
        ADJUSTMENT: "Ajuste"
    };
    return labels[type] || typeLabel(type);
}

function installmentText(transaction) {
    if (!transaction.currentInstallment && !transaction.totalInstallments) {
        return "—";
    }
    return `${transaction.currentInstallment || "?"}/${transaction.totalInstallments || "?"}`;
}
