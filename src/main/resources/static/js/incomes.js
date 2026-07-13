import { api } from "./api.js?v=20260712-security-hardening";
import { escapeHtml, formatMonth, formatPesos, setButtonBusy } from "./utils.js";

let incomeApi = api;
let notifyIncomeChanged = async () => {};
let visibleIncomes = new Map();

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

    document.querySelector("#income-edit-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        await updateIncomeFromModal(event.submitter || document.querySelector("#income-edit-save"));
    });

    document.querySelector("#income-edit-save-from-month")?.addEventListener("click", async (event) => {
        await updateIncomeFromMonthFromModal(event.currentTarget);
    });

    document.querySelector("#income-edit-recurring")?.addEventListener("change", updateIncomeEditRecurringState);
    document.querySelector("#income-edit-cancel")?.addEventListener("click", closeIncomeEditModal);
    document.querySelector("#income-edit-close")?.addEventListener("click", closeIncomeEditModal);
}

export async function loadIncomes() {
    const month = document.querySelector("#income-filter-month")?.value || "";
    try {
        showIncomeTableFeedback("Cargando ingresos...", false, true);
        const incomes = month ? await incomeApi.incomes({ month }) : await incomeApi.incomes();
        renderIncomes(incomes, month);
        showIncomeTableFeedback(incomeListStatus(incomes.length, month));
    } catch (error) {
        showIncomeTableFeedback(`No se pudieron cargar los ingresos: ${error.message}`, true);
    }
}

export function incomeTypeLabel(type) {
    const labels = {
        SALARY: "Sueldo",
        VARIABLE: "Ingreso vario"
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
    visibleIncomes = new Map(incomes.map((income) => [String(income.id), income]));

    incomes.forEach((income) => {
        const row = document.createElement("tr");
        row.dataset.incomeId = String(income.id);
        row.dataset.effectiveMonth = suggestedEffectiveMonth(income, selectedMonth);
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
    return `
        <td data-label="Mes">${escapeHtml(formatMonth(selectedMonth || income.startMonth))}</td>
        <td data-label="Descripción">${escapeHtml(income.description)}</td>
        <td data-label="Tipo">${escapeHtml(incomeTypeLabel(income.incomeType))}</td>
        <td class="amount" data-label="Monto">${escapeHtml(formatPesos(income.amountPesos))}</td>
        <td data-label="Recurrente">${recurringLabel(income.recurringMonthly)}</td>
        <td data-label="Aplica desde">${escapeHtml(formatMonth(income.startMonth))}</td>
        <td data-label="Aplica hasta">${income.endMonth ? escapeHtml(formatMonth(income.endMonth)) : "—"}</td>
        <td data-label="Estado"><span class="status-chip ${income.projected ? "projection" : "loaded"}">${incomeProjectionLabel(income)}</span></td>
        <td data-label="Notas">${income.notes ? escapeHtml(income.notes) : "—"}</td>
        <td data-label="Acciones">
            <div class="row-actions income-actions">
                <button type="button" class="secondary-button icon-button" data-income-action="edit" aria-label="Editar ingreso ${escapeHtml(income.description)}" title="Editar">
                    <span aria-hidden="true">✎</span><span class="sr-only">Editar</span>
                </button>
                <button type="button" class="danger-button icon-button" data-income-action="delete" aria-label="Eliminar ingreso ${escapeHtml(income.description)}" title="Eliminar">
                    <span aria-hidden="true">🗑</span><span class="sr-only">Eliminar</span>
                </button>
            </div>
        </td>
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
    if (action === "edit") {
        openIncomeEditModal(row.dataset.incomeId, row.dataset.effectiveMonth);
        return;
    }

    if (action === "delete") {
        await deleteIncome(button, row.dataset.incomeId);
        return;
    }
}

function openIncomeEditModal(id, effectiveMonth) {
    const income = visibleIncomes.get(String(id));
    if (!income) {
        showIncomeTableFeedback("No se encontró el ingreso seleccionado para editar.", true);
        return;
    }

    setIncomeEditValue("#income-edit-id", id);
    setIncomeEditValue("#income-edit-description", income.description);
    setIncomeEditValue("#income-edit-type", income.incomeType || "SALARY");
    setIncomeEditValue("#income-edit-amount", income.amountPesos);
    setIncomeEditValue("#income-edit-recurring", String(Boolean(income.recurringMonthly)));
    setIncomeEditValue("#income-edit-start-month", income.startMonth);
    setIncomeEditValue("#income-edit-end-month", income.endMonth || "");
    setIncomeEditValue("#income-edit-effective-month", effectiveMonth || suggestedEffectiveMonth(income, document.querySelector("#income-filter-month")?.value || ""));
    setIncomeEditValue("#income-edit-notes", income.notes || "");
    showIncomeEditFeedback("");
    updateIncomeEditRecurringState();

    const modal = document.querySelector("#income-edit-modal");
    modal.hidden = false;
    document.querySelector("#income-edit-description")?.focus?.();
}

function closeIncomeEditModal() {
    const modal = document.querySelector("#income-edit-modal");
    if (modal) {
        modal.hidden = true;
    }
    showIncomeEditFeedback("");
}

function setIncomeEditValue(selector, value) {
    const field = document.querySelector(selector);
    if (field) {
        field.value = value ?? "";
    }
}

function incomePayloadFromModal() {
    return incomePayloadFromValues({
        description: document.querySelector("#income-edit-description")?.value,
        incomeType: document.querySelector("#income-edit-type")?.value,
        amountPesos: document.querySelector("#income-edit-amount")?.value,
        startMonth: document.querySelector("#income-edit-start-month")?.value,
        endMonth: document.querySelector("#income-edit-end-month")?.value,
        recurringMonthly: document.querySelector("#income-edit-recurring")?.value,
        notes: document.querySelector("#income-edit-notes")?.value
    });
}

function updateIncomeEditRecurringState() {
    const isRecurring = document.querySelector("#income-edit-recurring")?.value === "true";
    const effectiveMonthGroup = document.querySelector("#income-edit-effective-month-group");
    const saveFromMonthButton = document.querySelector("#income-edit-save-from-month");
    if (effectiveMonthGroup) {
        effectiveMonthGroup.hidden = !isRecurring;
    }
    if (saveFromMonthButton) {
        saveFromMonthButton.hidden = !isRecurring;
    }
}

async function updateIncomeFromModal(button) {
    const id = document.querySelector("#income-edit-id")?.value || "";
    const payload = incomePayloadFromModal();
    const validationMessage = validateIncomePayload(payload);
    if (validationMessage) {
        showIncomeEditFeedback(validationMessage, true);
        return;
    }

    await updateIncome(button, id, payload);
}

async function updateIncomeFromMonthFromModal(button) {
    const id = document.querySelector("#income-edit-id")?.value || "";
    const payload = incomePayloadFromModal();
    const validationMessage = validateIncomePayload(payload);
    if (validationMessage) {
        showIncomeEditFeedback(validationMessage, true);
        return;
    }
    if (!payload.recurringMonthly) {
        showIncomeEditFeedback("Guardar cambios desde el mes seleccionado está disponible solo para ingresos recurrentes.", true);
        return;
    }

    await updateIncomeFromMonth(button, id, payload);
}

async function updateIncome(button, id, payload) {
    try {
        setButtonBusy(button, true, "Guardando...");
        showIncomeEditFeedback("Guardando cambios...", false, true);
        await incomeApi.updateIncome(id, payload);
        await loadIncomes();
        await notifyIncomeChanged();
        closeIncomeEditModal();
        showIncomeTableFeedback("Ingreso actualizado correctamente. Tabla actualizada.");
    } catch (error) {
        showIncomeEditFeedback(`No se pudo actualizar el ingreso: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function updateIncomeFromMonth(button, id, payload) {
    const effectiveMonth = document.querySelector("#income-edit-effective-month")?.value || "";
    if (!effectiveMonth) {
        showIncomeEditFeedback("El mes desde el que se aplican los cambios es obligatorio para versionar un ingreso recurrente.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Versionando...");
        showIncomeEditFeedback("Guardando cambios desde el mes seleccionado...", false, true);
        await incomeApi.updateIncomeFromMonth(id, effectiveMonth, {
            ...payload,
            startMonth: effectiveMonth,
            recurringMonthly: true
        });
        await loadIncomes();
        await notifyIncomeChanged();
        closeIncomeEditModal();
        showIncomeTableFeedback("Ingreso recurrente actualizado desde el mes seleccionado. Tabla actualizada.");
    } catch (error) {
        showIncomeEditFeedback(`No se pudo versionar el ingreso recurrente: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function deleteIncome(button, id) {
    const income = visibleIncomes.get(String(id));
    const confirmationText = income?.recurringMonthly
        ? "¿Seguro que desea eliminar este ingreso? Si es recurrente, se eliminará el registro completo."
        : "¿Seguro que desea eliminar este ingreso?";
    if (globalThis.confirm && !globalThis.confirm(confirmationText)) {
        return;
    }
    try {
        setButtonBusy(button, true, "Eliminando...");
        await incomeApi.deleteIncome(id);
        await loadIncomes();
        await notifyIncomeChanged();
        showIncomeTableFeedback("Ingreso eliminado correctamente. Tabla actualizada.");
    } catch (error) {
        showIncomeTableFeedback(`No se pudo eliminar el ingreso: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function incomeListStatus(count, month) {
    const countText = `${count} ${count === 1 ? "ingreso" : "ingresos"}`;
    return month ? `${countText} para ${formatMonth(month)}.` : `${countText} cargados.`;
}

function showIncomeFeedback(message, isError = false, isLoading = false) {
    showFeedback("#income-feedback", message, isError, isLoading);
}

function showIncomeTableFeedback(message, isError = false, isLoading = false) {
    showFeedback("#income-table-feedback", message, isError, isLoading);
}

function showIncomeEditFeedback(message, isError = false, isLoading = false) {
    showFeedback("#income-edit-feedback", message, isError, isLoading);
}

function showFeedback(selector, message, isError = false, isLoading = false) {
    const feedback = document.querySelector(selector);
    if (!feedback) {
        return;
    }
    feedback.textContent = message;
    feedback.classList.toggle("error-text", isError);
    feedback.classList.toggle("loading", isLoading && !isError);
}
