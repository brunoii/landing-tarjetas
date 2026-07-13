import { escapeHtml, formatMonth, formatPesos, formatUsd, setButtonBusy } from "./utils.js";

const defaultApi = null;
let manualExpenseApi = defaultApi;
let callbacks = { onChanged: async () => {} };
let categories = [];

export function setupManualExpenses({ apiClient, onChanged } = {}) {
    manualExpenseApi = apiClient || manualExpenseApi;
    callbacks = { ...callbacks, onChanged: onChanged || callbacks.onChanged };
    document.querySelector("#manual-expense-form")?.addEventListener("submit", createManualExpense);
}

export function setManualExpenseApi(apiClient) {
    manualExpenseApi = apiClient;
}

export function setManualExpenseCategories(nextCategories) {
    categories = nextCategories;
    const select = document.querySelector("#manual-expense-category");
    if (!select) {
        return;
    }
    const currentValue = select.value;
    select.innerHTML = `<option value="">Sin categoría</option>${categories.map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`).join("")}`;
    select.value = categories.some((category) => String(category.id) === currentValue) ? currentValue : "";
}

export function renderManualExpenses(expenses = [], month = "") {
    const table = document.querySelector("#manual-expenses-table");
    const empty = document.querySelector("#manual-expenses-empty");
    const summary = document.querySelector("#manual-expenses-summary");
    if (!table || !empty || !summary) {
        return;
    }
    table.innerHTML = "";
    expenses.forEach((expense) => {
        const row = document.createElement("tr");
        row.dataset.manualExpenseId = expense.id;
        row.innerHTML = `
            <td data-label="Mes">${escapeHtml(formatMonth(expense.startMonth))}</td>
            <td data-label="Descripción">${escapeHtml(expense.description || "—")}</td>
            <td data-label="Tipo">${escapeHtml(manualExpenseTypeLabel(expense.type))}</td>
            <td data-label="Cuota">${installmentText(expense)}</td>
            <td data-label="Categoría">${escapeHtml(expense.category?.name || "Sin categoría")}</td>
            <td class="amount" data-label="Pesos">${formatPesos(expense.amountPesos)}</td>
            <td class="amount" data-label="USD">${formatUsd(expense.amountUsd)}</td>
            <td data-label="Estado"><span class="status-chip ${expense.projected ? "projection" : "loaded"}">${escapeHtml(manualProjectionLabel(expense))}</span></td>
            <td data-label="Notas">${escapeHtml(expense.notes || "—")}</td>
            <td data-label="Acciones"><button type="button" class="danger-button" data-delete-manual-expense="${expense.id}">Eliminar</button></td>
        `;
        row.querySelector("[data-delete-manual-expense]").addEventListener("click", () => deleteManualExpense(expense.id));
        table.append(row);
    });
    empty.hidden = expenses.length > 0;
    summary.textContent = expenses.length
        ? `${expenses.length} ${expenses.length === 1 ? "gasto manual" : "gastos manuales"} para ${formatMonth(month)}.`
        : `No hay gastos manuales para ${formatMonth(month)}.`;
}

export function manualExpensePayloadFromValues(values) {
    return {
        description: String(values.description || "").trim(),
        type: values.type || "ONE_PAYMENT",
        amountPesos: values.amountPesos || "",
        amountUsd: values.amountUsd || null,
        startMonth: values.startMonth || "",
        totalInstallments: numberOrNull(values.totalInstallments),
        currentInstallment: numberOrNull(values.currentInstallment),
        categoryId: numberOrNull(values.categoryId),
        notes: String(values.notes || "").trim()
    };
}

export function validateManualExpensePayload(payload) {
    if (!payload.description) {
        return "La descripción del gasto manual es obligatoria.";
    }
    if (!payload.amountPesos || Number(payload.amountPesos) <= 0) {
        return "El monto en pesos debe ser mayor que cero.";
    }
    if (payload.amountUsd !== null && payload.amountUsd !== "" && Number(payload.amountUsd) < 0) {
        return "El monto en USD no puede ser negativo.";
    }
    if (!payload.startMonth) {
        return "El mes de inicio es obligatorio.";
    }
    if (isInstallmentLike(payload.type) && !payload.totalInstallments) {
        return "La cantidad de cuotas es obligatoria para cuotas y préstamos.";
    }
    if (payload.currentInstallment && payload.totalInstallments && payload.currentInstallment > payload.totalInstallments) {
        return "La cuota actual no puede superar el total de cuotas.";
    }
    return "";
}

export function manualExpenseTypeLabel(type) {
    const labels = {
        ONE_PAYMENT: "Un pago",
        INSTALLMENT: "Cuota",
        CASH: "Efectivo",
        LOAN: "Préstamo",
        FEE: "Cargo",
        TAX: "Impuesto"
    };
    return labels[type] || type || "Desconocido";
}

export function manualProjectionLabel(expense) {
    return expense.projected ? "Proyectado" : "Real";
}

async function createManualExpense(event) {
    event.preventDefault();
    const button = document.querySelector("#manual-expense-form button[type='submit']");
    const payload = manualExpensePayloadFromValues({
        description: value("#manual-expense-description"),
        type: value("#manual-expense-type"),
        amountPesos: value("#manual-expense-amount-pesos"),
        amountUsd: value("#manual-expense-amount-usd"),
        startMonth: value("#manual-expense-start-month"),
        totalInstallments: value("#manual-expense-total-installments"),
        currentInstallment: value("#manual-expense-current-installment"),
        categoryId: value("#manual-expense-category"),
        notes: value("#manual-expense-notes")
    });
    const validation = validateManualExpensePayload(payload);
    if (validation) {
        showManualExpenseFeedback(validation, true);
        return;
    }

    try {
        setButtonBusy(button, true, "Creando...");
        await manualExpenseApi.createManualExpense(payload);
        document.querySelector("#manual-expense-form").reset();
        document.querySelector("#manual-expense-type").value = "ONE_PAYMENT";
        showManualExpenseFeedback("Gasto manual creado correctamente.");
        await callbacks.onChanged();
    } catch (error) {
        showManualExpenseFeedback(`No se pudo crear el gasto manual: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function deleteManualExpense(id) {
    if (!confirm("¿Eliminar este gasto manual?")) {
        return;
    }
    try {
        await manualExpenseApi.deleteManualExpense(id);
        showManualExpenseFeedback("Gasto manual eliminado.");
        await callbacks.onChanged();
    } catch (error) {
        showManualExpenseFeedback(`No se pudo eliminar el gasto manual: ${error.message}`, true);
    }
}

function installmentText(expense) {
    if (!expense.installmentNumber && !expense.totalInstallments) {
        return "—";
    }
    return `${expense.installmentNumber || expense.currentInstallment || "?"}/${expense.totalInstallments || "?"}`;
}

function isInstallmentLike(type) {
    return type === "INSTALLMENT" || type === "LOAN";
}

function value(selector) {
    return document.querySelector(selector).value;
}

function numberOrNull(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

function showManualExpenseFeedback(message, isError = false) {
    const feedback = document.querySelector("#manual-expense-feedback");
    feedback.textContent = message;
    feedback.classList.toggle("error-text", isError);
}
