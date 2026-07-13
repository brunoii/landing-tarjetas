import { escapeHtml, formatMonth, formatPesos, setButtonBusy } from "./utils.js";

let simulatorApi = null;
let categories = [];

export const MAX_SIMULATOR_INSTALLMENTS = 60;

export function setupSimulator({ apiClient } = {}) {
    simulatorApi = apiClient || simulatorApi;
    document.querySelector("#simulator-form")?.addEventListener("submit", submitSimulation);
    document.querySelector("#clear-simulation")?.addEventListener("click", clearSimulation);
}

export function setSimulatorApi(apiClient) {
    simulatorApi = apiClient;
}

export function setSimulatorCategories(nextCategories = []) {
    categories = nextCategories;
    const select = document.querySelector("#simulator-category");
    if (!select) {
        return;
    }
    const currentValue = select.value;
    select.innerHTML = `<option value="">Sin categoría</option>${categories.map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`).join("")}`;
    select.value = categories.some((category) => String(category.id) === currentValue) ? currentValue : "";
}

export function simulationPayloadFromValues(values) {
    return {
        description: String(values.description || "").trim(),
        totalAmount: values.totalAmount || "",
        installmentCount: numberOrNull(values.installmentCount),
        startMonth: values.startMonth || "",
        categoryId: numberOrNull(values.categoryId)
    };
}

export function validateSimulationPayload(payload) {
    if (!payload.totalAmount || Number(payload.totalAmount) <= 0) {
        return "El importe total es obligatorio.";
    }
    if (!payload.installmentCount && payload.installmentCount !== 0) {
        return "La cantidad de cuotas es obligatoria.";
    }
    if (Number(payload.installmentCount) <= 0) {
        return "La cantidad de cuotas debe ser mayor que cero.";
    }
    if (Number(payload.installmentCount) > MAX_SIMULATOR_INSTALLMENTS) {
        return `La cantidad de cuotas no puede superar ${MAX_SIMULATOR_INSTALLMENTS}.`;
    }
    if (!payload.startMonth) {
        return "El mes de inicio es obligatorio.";
    }
    return "";
}

export function calculateMonthlyInstallment(totalAmount, installmentCount) {
    return Number(totalAmount || 0) / Number(installmentCount || 1);
}

export function affectedMonths(startMonth, installmentCount) {
    if (!startMonth || !installmentCount) {
        return [];
    }
    const [year, month] = String(startMonth).split("-").map(Number);
    return Array.from({ length: Number(installmentCount) }, (_, index) => {
        const date = new Date(year, month - 1 + index, 1);
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
    });
}

export async function runPurchaseSimulation(payload, apiClient = simulatorApi) {
    const validation = validateSimulationPayload(payload);
    if (validation) {
        throw new Error(validation);
    }
    if (!apiClient?.summary) {
        throw new Error("No se pudo acceder a los datos del panel para simular la compra.");
    }
    const months = affectedMonths(payload.startMonth, payload.installmentCount);
    const summaries = await Promise.all(months.map((month) => apiClient.summary(month)));
    return buildSimulationRows(payload, summaries, months);
}

export function buildSimulationRows(payload, summaries = [], months = affectedMonths(payload.startMonth, payload.installmentCount)) {
    const simulatedInstallment = calculateMonthlyInstallment(payload.totalAmount, payload.installmentCount);
    return months.map((month, index) => {
        const summary = summaries[index] || {};
        const monthlyIncome = Number(summary.incomeTotalPesos || 0);
        const currentExpenses = Number(summary.expenseTotalPesos ?? summary.totalPesos ?? 0);
        const currentBalance = Number(summary.monthlyBalancePesos ?? (monthlyIncome - currentExpenses));
        return {
            month,
            monthlyIncome,
            currentExpenses,
            simulatedInstallment,
            currentBalance,
            simulatedBalance: currentBalance - simulatedInstallment
        };
    });
}

export function renderSimulationResults(rows = [], payload = {}) {
    const table = document.querySelector("#simulation-results-table");
    const empty = document.querySelector("#simulation-empty");
    const summary = document.querySelector("#simulator-summary");
    if (!table || !empty || !summary) {
        return;
    }
    table.innerHTML = "";
    rows.forEach((row) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td class="simulation-month-cell" data-label="Mes">${escapeHtml(formatMonth(row.month))}</td>
            <td class="amount simulation-amount-cell" data-label="Ingresos del mes">${formatPesos(row.monthlyIncome)}</td>
            <td class="amount simulation-amount-cell" data-label="Deuda/gastos actuales del mes">${formatPesos(row.currentExpenses)}</td>
            <td class="amount simulation-amount-cell" data-label="Nueva cuota simulada">${formatPesos(row.simulatedInstallment)}</td>
            <td class="amount simulation-amount-cell" data-label="Saldo actual sin simulación">${formatPesos(row.currentBalance)}</td>
            <td class="amount simulation-amount-cell" data-label="Saldo final con simulación">${formatPesos(row.simulatedBalance)}</td>
        `;
        table.append(tr);
    });
    empty.hidden = rows.length > 0;
    const description = payload.description ? ` para ${payload.description}` : "";
    summary.textContent = rows.length
        ? `${rows.length} ${rows.length === 1 ? "mes afectado" : "meses afectados"}${description}. Cuota mensual simulada: ${formatPesos(rows[0].simulatedInstallment)}.`
        : "No hay una simulación activa.";
}

export function clearSimulation() {
    document.querySelector("#simulator-form")?.reset();
    const table = document.querySelector("#simulation-results-table");
    const empty = document.querySelector("#simulation-empty");
    const summary = document.querySelector("#simulator-summary");
    if (table) {
        table.innerHTML = "";
    }
    if (empty) {
        empty.hidden = false;
    }
    if (summary) {
        summary.textContent = "No hay una simulación activa.";
    }
    showSimulatorFeedback("Simulación limpiada.");
}

async function submitSimulation(event) {
    event.preventDefault();
    const button = document.querySelector("#simulator-form button[type='submit']");
    const payload = simulationPayloadFromValues({
        description: value("#simulator-description"),
        totalAmount: value("#simulator-total-amount"),
        installmentCount: value("#simulator-installment-count"),
        startMonth: value("#simulator-start-month"),
        categoryId: value("#simulator-category")
    });
    const validation = validateSimulationPayload(payload);
    if (validation) {
        showSimulatorFeedback(validation, true);
        return;
    }

    try {
        setButtonBusy(button, true, "Simulando...");
        const rows = await runPurchaseSimulation(payload);
        renderSimulationResults(rows, payload);
        showSimulatorFeedback("Simulación calculada. No se guardó en la base de datos.");
    } catch (error) {
        showSimulatorFeedback(`No se pudo calcular la simulación: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function value(selector) {
    return document.querySelector(selector).value;
}

function numberOrNull(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

function showSimulatorFeedback(message, isError = false) {
    const feedback = document.querySelector("#simulator-feedback");
    if (!feedback) {
        return;
    }
    feedback.textContent = message;
    feedback.classList.toggle("error-text", isError);
}
