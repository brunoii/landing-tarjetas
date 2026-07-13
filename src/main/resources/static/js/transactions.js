import { cardLabel, escapeHtml, formatDate, formatMonth, formatPesos, formatUsd, toYearMonth, typeLabel } from "./utils.js";

let categories = [];
let lastRows = [];
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
    renderFilterSummary(lastRows, lastMonth, lastRows.length);
}

export function transactionFilters(defaultMonth) {
    const selectedMonth = document.querySelector("#filter-month")?.value || defaultMonth;
    return {
        month: selectedMonth,
        card: document.querySelector("#filter-card").value,
        category: document.querySelector("#filter-category").value,
        type: document.querySelector("#filter-type").value,
        origin: document.querySelector("#filter-origin").value
    };
}

export function syncTransactionMonth(month) {
    const monthInput = document.querySelector("#filter-month");
    if (monthInput) {
        monthInput.value = month || "";
    }
}

export function renderTransactions(monthDetailOrRows, month = lastMonth) {
    const rows = Array.isArray(monthDetailOrRows) ? monthDetailOrRows : monthDetailOrRows?.rows || [];
    const effectiveMonth = toYearMonth(monthDetailOrRows?.month || month);
    lastRows = rows;
    lastMonth = effectiveMonth;

    const filters = transactionFilters(effectiveMonth);
    const visibleRows = rows.filter((row) => matchesFilters(row, filters));
    const table = document.querySelector("#transactions-table");
    const empty = document.querySelector("#transactions-empty");
    table.innerHTML = "";

    visibleRows.forEach((expense) => {
        const row = document.createElement("tr");
        row.className = expense.kind === "PROJECTION" ? "projection-row" : "actual-row";
        row.innerHTML = `
            <td data-label="Mes">${escapeHtml(formatMonth(expense.month || effectiveMonth))}</td>
            <td data-label="Fecha">${formatDate(expense.transactionDate)}</td>
            <td data-label="Origen"><div class="expense-badges">${originBadges(expense)}</div></td>
            <td data-label="Tarjeta / Medio">${escapeHtml(expenseMediumLabel(expense))}</td>
            <td data-label="Descripción">${escapeHtml(expense.description || "—")}</td>
            <td data-label="Tipo">${escapeHtml(expenseTypeLabel(expense.type))}</td>
            <td data-label="Categoría">${escapeHtml(expense.categoryName || "Sin categoría")}</td>
            <td data-label="Cuota">${installmentText(expense)}</td>
            <td class="amount" data-label="Pesos">${formatPesos(expense.amountPesos)}</td>
            <td class="amount" data-label="USD">${formatUsd(expense.amountUsd)}</td>
            <td data-label="Finalización">${formatMonth(expense.estimatedFinishMonth)}</td>
            <td data-label="Resumen origen">${sourceText(expense)}</td>
            <td data-label="Notas">${escapeHtml(expense.notes || "—")}</td>
            <td data-label="Acciones">${actionCell(expense)}</td>
        `;
        table.append(row);
    });

    empty.hidden = visibleRows.length > 0;
    empty.textContent = emptyMessage(rows.length, visibleRows.length, filters);
    renderFilterSummary(visibleRows, effectiveMonth, rows.length);
    return { rowCount: rows.length, visibleCount: visibleRows.length, month: effectiveMonth };
}

export function rerenderTransactionsAfterSearch() {
    return renderTransactions(lastRows, lastMonth);
}

export function resetTransactionFilters(defaultMonth = lastMonth) {
    syncTransactionMonth(defaultMonth);
    document.querySelector("#filter-card").value = "";
    document.querySelector("#filter-category").value = "";
    document.querySelector("#filter-type").value = "";
    document.querySelector("#filter-origin").value = "";
    document.querySelector("#filter-search").value = "";
}

function renderFilterSummary(visibleRows, month, totalRows) {
    const summary = document.querySelector("#filters-summary");
    const filters = transactionFilters(month);
    const parts = [`Mes: ${formatMonth(filters.month || month)}`];

    if (filters.card) {
        parts.push(`Tarjeta/medio: ${filterCardLabel(filters.card)}`);
    }
    if (filters.category) {
        const category = document.querySelector("#filter-category");
        parts.push(`Categoría: ${category.selectedOptions[0]?.textContent || "Categoría seleccionada"}`);
    }
    if (filters.type) {
        parts.push(`Tipo: ${expenseTypeLabel(filters.type)}`);
    }
    if (filters.origin) {
        parts.push(`Origen: ${originFilterLabel(filters.origin)}`);
    }
    const search = document.querySelector("#filter-search").value.trim();
    if (search) {
        parts.push(`Búsqueda: "${search}"`);
    }

    const noun = visibleRows.length === 1 ? "gasto" : "gastos";
    summary.textContent = `${visibleRows.length} ${noun} de ${totalRows} filas reales/proyectadas. ${parts.join(" · ")}.`;
}

function emptyMessage(totalRows, visibleRows, filters) {
    if (visibleRows > 0) {
        return "";
    }
    if (totalRows === 0) {
        return "No hay gastos reales ni proyectados para este mes.";
    }
    if (hasAppliedFilters(filters)) {
        return "No hay gastos que coincidan con los filtros seleccionados.";
    }
    return "No hay gastos reales ni proyectados para este mes.";
}

function hasAppliedFilters(filters) {
    const search = document.querySelector("#filter-search").value.trim();
    return Boolean(filters.card || filters.category || filters.type || filters.origin || search);
}

function matchesFilters(expense, filters) {
    return matchesCardOrMedium(expense, filters.card)
        && (!filters.category || String(expense.categoryId || "") === String(filters.category))
        && (!filters.type || expense.type === filters.type)
        && matchesOrigin(expense, filters.origin)
        && matchesSearch(expense, document.querySelector("#filter-search").value.trim().toLowerCase());
}

function matchesCardOrMedium(expense, filter) {
    if (!filter) {
        return true;
    }
    if (filter === "MANUAL") {
        return expense.source === "MANUAL_EXPENSE" || expense.provider === "MANUAL";
    }
    if (filter === "CASH") {
        return expense.type === "CASH";
    }
    if (filter === "LOAN") {
        return expense.type === "LOAN";
    }
    return expense.cardBrand === filter;
}

function matchesOrigin(expense, origin) {
    if (!origin) {
        return true;
    }
    if (origin === "REAL") {
        return expense.kind !== "PROJECTION";
    }
    if (origin === "PROJECTION") {
        return expense.kind === "PROJECTION";
    }
    if (origin === "MANUAL") {
        return expense.source === "MANUAL_EXPENSE" || expense.provider === "MANUAL";
    }
    if (origin === "STATEMENT") {
        return expense.source === "STATEMENT" || Boolean(expense.sourceStatementId);
    }
    return true;
}

function matchesSearch(expense, search) {
    if (!search) {
        return true;
    }
    return [
        expense.description,
        expense.notes,
        expense.categoryName,
        expenseMediumLabel(expense),
        expenseTypeLabel(expense.type),
        sourceText(expense),
        expense.kind === "PROJECTION" ? "Proyección" : "Real"
    ].some((value) => String(value || "").toLowerCase().includes(search));
}

function originBadges(expense) {
    const badges = [
        badge(expense.kind === "PROJECTION" ? "Proyección" : "Real", expense.kind === "PROJECTION" ? "projection" : "loaded")
    ];
    if (expense.source === "MANUAL_EXPENSE" || expense.provider === "MANUAL") {
        badges.push(badge("Manual", "manual"));
    } else if (expense.sourceStatementId) {
        badges.push(badge("PDF", "statement"));
    }
    if (expense.type === "LOAN") {
        badges.push(badge("Préstamo", "debt"));
    } else if (expense.type === "CASH") {
        badges.push(badge("Efectivo", "manual"));
    } else if (expense.type === "FEE") {
        badges.push(badge("Cargo", "debt"));
    } else if (expense.type === "TAX") {
        badges.push(badge("Impuesto", "debt"));
    }
    return badges.join("");
}

function badge(label, className) {
    return `<span class="status-chip ${className}">${escapeHtml(label)}</span>`;
}

function expenseMediumLabel(expense) {
    if (expense.source === "MANUAL_EXPENSE") {
        return expense.cardAlias || manualMediumLabel(expense.type);
    }
    return expense.cardAlias || cardLabel(expense.cardBrand);
}

function manualMediumLabel(type) {
    const labels = {
        LOAN: "Préstamo manual",
        CASH: "Efectivo",
        ONE_PAYMENT: "Gasto manual"
    };
    return labels[type] || "Gasto manual";
}

function filterCardLabel(value) {
    const labels = {
        MANUAL: "Manual",
        CASH: "Efectivo",
        LOAN: "Préstamo"
    };
    return labels[value] || cardLabel(value);
}

function originFilterLabel(value) {
    const labels = {
        REAL: "Real",
        PROJECTION: "Proyección",
        MANUAL: "Manual",
        STATEMENT: "Resumen"
    };
    return labels[value] || value;
}

function expenseTypeLabel(type) {
    const labels = {
        ONE_PAYMENT: "Un pago",
        CASH: "Efectivo",
        LOAN: "Préstamo"
    };
    return labels[type] || typeLabel(type);
}

function installmentText(expense) {
    if (!expense.installmentNumber && !expense.currentInstallment && !expense.totalInstallments) {
        return "—";
    }
    return `${expense.installmentNumber || expense.currentInstallment || "?"}/${expense.totalInstallments || "?"}`;
}

function sourceText(expense) {
    if (expense.sourceStatementId) {
        return `Resumen #${expense.sourceStatementId} · ${formatMonth(expense.sourceStatementMonth)}`;
    }
    if (expense.source === "MANUAL_EXPENSE") {
        return `Manual · ${formatMonth(expense.sourceStatementMonth || expense.month)}`;
    }
    return "—";
}

function actionCell(expense) {
    if (expense.kind === "PROJECTION") {
        return '<button type="button" class="secondary-button" disabled title="Las proyecciones se editan desde su gasto o resumen de origen.">Editar origen</button>';
    }
    if (expense.source === "MANUAL_EXPENSE") {
        return '<button type="button" class="secondary-button" disabled title="Edite gastos manuales desde Cargar Gastos.">Editar manual</button>';
    }
    return '<button type="button" class="secondary-button" disabled title="La edición queda reservada para un flujo seguro posterior.">Editar</button>';
}
