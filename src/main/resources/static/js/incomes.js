import { api } from "./api.js?v=20260712-security-hardening";
import { escapeHtml, formatMonth, setButtonBusy } from "./utils.js";

let incomeApi = api;
let notifyIncomeChanged = async () => {};

export function setupIncomes({ apiClient = api, onChanged = async () => {} } = {}) {
    incomeApi = apiClient;
    notifyIncomeChanged = onChanged;

    document.querySelector("#income-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        await createIncome();
    });

    document.querySelector("#income-filter-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        await loadIncomes();
    });

    document.querySelector("#clear-income-filter")?.addEventListener("click", async () => {
        document.querySelector("#income-filter-month").value = "";
        await loadIncomes();
    });

    document.querySelector("#incomes-table")?.addEventListener("click", async (event) => {
        const button = event.target.closest("button[data-income-action]");
        if (!button) {
            return;
        }
        const row = button.closest("tr[data-income-id]");
        if (!row) {
            return;
        }
        await handleIncomeAction(button, row);
    });
}

export async function loadIncomes() {
    const month = document.querySelector("#income-filter-month")?.value || "";
    try {
        showIncomeFeedback("Cargando ingresos...", false, true);
        const incomes = month ? await incomeApi.incomes({ month }) : await incomeApi.incomes();
        renderIncomes(incomes, month);
        showIncomeFeedback(incomeListStatus(incomes.length, month));
    } catch (error) {
        showIncomeFeedback(`No se pudieron cargar los ingresos: ${error.message}`, true);
    }
}

export function incomeTypeLabel(type) {
    const labels = {
        SALARY: "Sueldo",
        VARIABLE: "Variable"
    };
    return labels[type] || "Ingreso";
}

export function recurringLabel(value) {
    return value ? "Sí" : "No";
}

export function incomeProjectionLabel(income) {
    return income?.projected ? "Proyectado" : "Real";
}

export function incomePayloadFromValues(values) {
    return {
        description: String(values.description || "").trim(),
        incomeType: values.incomeType || "SALARY",
        amountPesos: String(values.amountPesos || "").trim(),
        startMonth: String(values.startMonth || "").trim(),
        endMonth: String(values.endMonth || "").trim(),
        recurringMonthly: values.recurringMonthly === true || values.recurringMonthly === "true" || values.recurringMonthly === "on",
        notes: String(values.notes || "").trim()
    };
}

export function validateIncomePayload(payload) {
    if (!payload.description) {
        return "La descripción del ingreso es obligatoria.";
    }
    if (!payload.incomeType) {
        return "El tipo de ingreso es obligatorio.";
    }
    if (!payload.amountPesos || Number(payload.amountPesos) <= 0) {
        return "El monto del ingreso debe ser mayor que cero.";
    }
    if (!payload.startMonth) {
        return "El mes de inicio del ingreso es obligatorio.";
    }
    if (payload.endMonth && payload.endMonth < payload.startMonth) {
        return "El mes de aplicación hasta no puede ser anterior al mes de inicio.";
    }
    return "";
}

function renderIncomes(incomes, selectedMonth) {
    const table = document.querySelector("#incomes-table");
    const empty = document.querySelector("#incomes-empty");
    const summary = document.querySelector("#income-filters-summary");
    table.innerHTML = "";

    incomes.forEach((income) => {
        const row = document.createElement("tr");
        row.dataset.incomeId = income.id;
        row.className = income.projected ? "projection-row" : "actual-row";
        row.innerHTML = incomeRowHtml(income, selectedMonth);
        table.append(row);
    });

    empty.hidden = incomes.length > 0;
    empty.textContent = selectedMonth
        ? "No hay ingresos que apliquen al mes seleccionado."
        : "Todavía no hay ingresos cargados.";
    summary.textContent = incomeListStatus(incomes.length, selectedMonth);
}

function incomeRowHtml(income, selectedMonth) {
    const effectiveMonth = suggestedEffectiveMonth(income, selectedMonth);
    return `
        <td>${escapeHtml(formatMonth(selectedMonth || income.startMonth))}</td>
        <td><input name="description" type="text" maxlength="240" value="${escapeHtml(income.description)}" aria-label="Descripción del ingreso"></td>
        <td>${incomeTypeSelect(income.incomeType)}</td>
        <td><input name="amountPesos" type="number" min="0" step="0.01" inputmode="decimal" value="${escapeHtml(income.amountPesos)}" aria-label="Monto del ingreso"></td>
        <td>${recurringSelect(income.recurringMonthly)}</td>
        <td><input name="startMonth" type="month" value="${escapeHtml(income.startMonth)}" aria-label="Aplica desde"></td>
        <td><input name="endMonth" type="month" value="${escapeHtml(income.endMonth || "")}" aria-label="Aplica hasta"></td>
        <td><span class="status-chip ${income.projected ? "projection" : "loaded"}">${incomeProjectionLabel(income)}</span></td>
        <td><input name="notes" type="text" maxlength="500" value="${escapeHtml(income.notes || "")}" aria-label="Notas del ingreso"></td>
        <td>
            <div class="row-actions income-actions">
                <button type="button" class="secondary-button" data-income-action="save">Guardar</button>
                ${income.recurringMonthly ? futureVersionControls(effectiveMonth) : ""}
                <button type="button" class="danger-button" data-income-action="delete">Eliminar</button>
            </div>
        </td>
    `;
}

function incomeTypeSelect(currentType) {
    return `
        <select name="incomeType" aria-label="Tipo de ingreso">
            <option value="SALARY" ${currentType === "SALARY" ? "selected" : ""}>Sueldo</option>
            <option value="VARIABLE" ${currentType === "VARIABLE" ? "selected" : ""}>Variable</option>
        </select>
    `;
}

function recurringSelect(isRecurring) {
    return `
        <select name="recurringMonthly" aria-label="Ingreso recurrente mensual">
            <option value="true" ${isRecurring ? "selected" : ""}>Sí</option>
            <option value="false" ${!isRecurring ? "selected" : ""}>No</option>
        </select>
    `;
}

function futureVersionControls(effectiveMonth) {
    return `
        <label class="inline-edit-field">
            Mes efectivo
            <input name="effectiveMonth" type="month" value="${escapeHtml(effectiveMonth)}" aria-label="Mes efectivo para editar ingreso recurrente">
        </label>
        <button type="button" class="secondary-button" data-income-action="save-from-month">Guardar desde mes</button>
    `;
}

function suggestedEffectiveMonth(income, selectedMonth) {
    if (selectedMonth && selectedMonth >= income.startMonth) {
        return selectedMonth;
    }
    return income.startMonth;
}

async function createIncome() {
    const form = document.querySelector("#income-form");
    const button = form.querySelector("button[type='submit']");
    const payload = incomePayloadFromValues({
        description: document.querySelector("#income-description").value,
        incomeType: document.querySelector("#income-type").value,
        amountPesos: document.querySelector("#income-amount").value,
        startMonth: document.querySelector("#income-start-month").value,
        recurringMonthly: document.querySelector("#income-recurring-monthly").value,
        notes: document.querySelector("#income-notes").value
    });
    const validationMessage = validateIncomePayload(payload);
    if (validationMessage) {
        showIncomeFeedback(validationMessage, true);
        return;
    }

    try {
        setButtonBusy(button, true, "Guardando...");
        await incomeApi.createIncome(payload);
        form.reset();
        await loadIncomes();
        await notifyIncomeChanged();
        showIncomeFeedback("Ingreso creado correctamente. Tabla actualizada.");
    } catch (error) {
        showIncomeFeedback(`No se pudo crear el ingreso: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function handleIncomeAction(button, row) {
    const action = button.dataset.incomeAction;
    if (action === "delete") {
        await deleteIncome(button, row.dataset.incomeId);
        return;
    }

    const payload = incomePayloadFromRow(row);
    const validationMessage = validateIncomePayload(payload);
    if (validationMessage) {
        showIncomeFeedback(validationMessage, true);
        return;
    }

    if (action === "save-from-month") {
        await updateIncomeFromMonth(button, row.dataset.incomeId, row, payload);
        return;
    }

    await updateIncome(button, row.dataset.incomeId, payload);
}

function incomePayloadFromRow(row) {
    return incomePayloadFromValues({
        description: row.querySelector("[name='description']").value,
        incomeType: row.querySelector("[name='incomeType']").value,
        amountPesos: row.querySelector("[name='amountPesos']").value,
        startMonth: row.querySelector("[name='startMonth']").value,
        endMonth: row.querySelector("[name='endMonth']").value,
        recurringMonthly: row.querySelector("[name='recurringMonthly']").value,
        notes: row.querySelector("[name='notes']").value
    });
}

async function updateIncome(button, id, payload) {
    try {
        setButtonBusy(button, true, "Guardando...");
        await incomeApi.updateIncome(id, payload);
        await loadIncomes();
        await notifyIncomeChanged();
        showIncomeFeedback("Ingreso actualizado correctamente. Tabla actualizada.");
    } catch (error) {
        showIncomeFeedback(`No se pudo actualizar el ingreso: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function updateIncomeFromMonth(button, id, row, payload) {
    const effectiveMonth = row.querySelector("[name='effectiveMonth']")?.value || "";
    if (!effectiveMonth) {
        showIncomeFeedback("El mes efectivo es obligatorio para versionar un ingreso recurrente.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Versionando...");
        await incomeApi.updateIncomeFromMonth(id, effectiveMonth, {
            ...payload,
            startMonth: effectiveMonth,
            recurringMonthly: true
        });
        await loadIncomes();
        await notifyIncomeChanged();
        showIncomeFeedback("Ingreso recurrente actualizado desde el mes efectivo. Tabla actualizada.");
    } catch (error) {
        showIncomeFeedback(`No se pudo versionar el ingreso recurrente: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function deleteIncome(button, id) {
    if (globalThis.confirm && !globalThis.confirm("¿Eliminar este ingreso? Esta acción también actualizará el resumen del panel.")) {
        return;
    }
    try {
        setButtonBusy(button, true, "Eliminando...");
        await incomeApi.deleteIncome(id);
        await loadIncomes();
        await notifyIncomeChanged();
        showIncomeFeedback("Ingreso eliminado correctamente. Tabla actualizada.");
    } catch (error) {
        showIncomeFeedback(`No se pudo eliminar el ingreso: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function incomeListStatus(count, month) {
    const countText = `${count} ${count === 1 ? "ingreso" : "ingresos"}`;
    return month ? `${countText} para ${formatMonth(month)}.` : `${countText} cargados.`;
}

function showIncomeFeedback(message, isError = false, isLoading = false) {
    const feedback = document.querySelector("#income-feedback");
    if (!feedback) {
        return;
    }
    feedback.textContent = message;
    feedback.classList.toggle("error-text", isError);
    feedback.classList.toggle("loading", isLoading && !isError);
}
