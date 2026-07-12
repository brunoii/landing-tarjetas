import { api } from "./api.js?v=20260712-security-hardening";
import { cardLabel, escapeHtml, formatPesos, formatUsd, setButtonBusy, toYearMonth, typeLabel } from "./utils.js";

const MAX_PDF_SIZE_BYTES = 1_048_576;
const MAX_REQUEST_SIZE_BYTES = 5_242_880;

const providers = ["VISA_HOME", "MASTERCARD_HOME", "BANK_PORTAL", "SANTANDER", "NARANJA_X", "MANUAL"];
const cardBrands = ["VISA", "MASTERCARD", "AMERICAN_EXPRESS", "OTHER"];
const transactionTypes = ["PURCHASE", "INSTALLMENT", "FEE", "TAX", "PAYMENT", "REFUND", "ADJUSTMENT"];

let categories = [];
let activeDraft = null;
let callbacks = {
    onDraftChanged: async () => {},
    onDraftConfirmed: async () => {},
    setStatus: () => {}
};

export function setupStatementUpload(nextCallbacks) {
    callbacks = { ...callbacks, ...nextCallbacks };
    document.querySelector("#statement-upload-form").addEventListener("submit", uploadSelectedStatements);
    document.querySelector("#statement-files").addEventListener("change", showSelectedFileHint);
    document.querySelector("#statement-form")?.addEventListener("submit", saveActiveDraft);
    document.querySelector("#missing-transaction-form")?.addEventListener("submit", addMissingTransaction);
    document.querySelector("#confirm-statement-button")?.addEventListener("click", confirmActiveDraft);
}

export function setStatementCategories(nextCategories) {
    categories = nextCategories;
    if (activeDraft) {
        renderDraftReview(activeDraft);
    }
}

export function renderDraftStatementList(statements) {
    const list = document.querySelector("#draft-statement-list");
    list.innerHTML = "";
    const drafts = statements.filter((statement) => statement.status === "DRAFT");

    if (drafts.length === 0) {
        list.innerHTML = '<p class="empty-state">No hay resúmenes en borrador pendientes de revisión.</p>';
        return;
    }

    drafts.forEach((statement) => {
        const item = document.createElement("article");
        item.className = "draft-card";
        item.innerHTML = `
            <div>
                <strong>${escapeHtml(cardLabel(statement.cardBrand))}</strong>
                <p class="muted">${escapeHtml(statement.provider || "Proveedor desconocido")} · ${escapeHtml(toYearMonth(statement.paymentMonth) || "Sin mes de pago")}</p>
            </div>
            <dl>
                <div><dt>Total ARS</dt><dd>${formatPesos(statement.totalPesos)}</dd></div>
                <div><dt>Total USD</dt><dd>${formatUsd(statement.totalUsd)}</dd></div>
                <div><dt>Filas</dt><dd>${statement.transactionCount || 0}</dd></div>
            </dl>
            <button type="button" class="secondary-button" data-review-draft="${statement.id}">Revisar borrador</button>
        `;
        item.querySelector("[data-review-draft]").addEventListener("click", () => openDraft(statement.id));
        list.append(item);
    });
}

async function uploadSelectedStatements(event) {
    event.preventDefault();
    const input = document.querySelector("#statement-files");
    const files = [...input.files];
    const validation = validateFiles(files);
    if (validation) {
        showUploadFeedback(validation, true);
        return;
    }

    try {
        setButtonBusy(document.querySelector("#statement-upload-button"), true, "Cargando...");
        showUploadFeedback("Cargando PDFs localmente para analizar solo metadatos. Los bytes del PDF original no se persisten.");
        const response = await api.uploadStatements(files);
        renderUploadResults(response.files || []);
        input.value = "";
        showSelectedFileHint();
        await callbacks.onDraftChanged();
        const firstDraft = (response.files || []).find((result) => result.draftStatement)?.draftStatement;
        if (firstDraft) {
            await openDraft(firstDraft.id);
        }
        showUploadFeedback("Carga finalizada. Revise cada borrador antes de confirmarlo.");
    } catch (error) {
        showUploadFeedback(`No se pudo completar la carga: ${error.message}`, true);
        callbacks.setStatus(`No se pudo completar la carga: ${error.message}`, true);
    } finally {
        setButtonBusy(document.querySelector("#statement-upload-button"), false);
    }
}

function validateFiles(files) {
    if (files.length === 0) {
        return "Seleccione uno o más archivos PDF antes de cargarlos.";
    }
    const totalSize = files.reduce((current, file) => current + file.size, 0);
    if (totalSize > MAX_REQUEST_SIZE_BYTES) {
        return "Los archivos seleccionados superan el límite de 5 MB por solicitud.";
    }
    const invalid = files.find((file) => !isPdf(file));
    if (invalid) {
        return `${invalid.name} no es un archivo PDF.`;
    }
    const tooLarge = files.find((file) => file.size > MAX_PDF_SIZE_BYTES);
    if (tooLarge) {
        return `${tooLarge.name} supera el límite de 1 MB por archivo.`;
    }
    return "";
}

function isPdf(file) {
    return file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
}

function showSelectedFileHint() {
    const files = [...document.querySelector("#statement-files").files];
    const hint = document.querySelector("#statement-file-hint");
    if (files.length === 0) {
        hint.textContent = "Solo PDF. Máximo 1 MB por archivo y 5 MB por solicitud.";
        return;
    }
    const names = files.map((file) => `${file.name} (${formatBytes(file.size)})`).join(", ");
    hint.textContent = `${files.length} seleccionado${files.length === 1 ? "" : "s"}: ${names}`;
}

function renderUploadResults(results) {
    const container = document.querySelector("#upload-results");
    container.innerHTML = "";
    if (results.length === 0) {
        container.innerHTML = '<p class="empty-state">No se recibieron resultados de carga.</p>';
        return;
    }

    results.forEach((result) => {
        const uploadedFile = result.uploadedFile || {};
        const draft = result.draftStatement;
        const article = document.createElement("article");
        article.className = "upload-result";
        article.innerHTML = `
            <header>
                <div>
                    <strong>${escapeHtml(uploadedFile.originalFilename || "PDF sin nombre")}</strong>
                    <p class="muted">${escapeHtml(parsingStatusLabel(result.parsingStatus || uploadedFile.parsingStatus))} · ${escapeHtml(parserDisplayLabel(result.parserName))}</p>
                </div>
                <span class="status-chip ${result.parsingStatus === "PARSED" ? "loaded" : ""}">${escapeHtml(parsingStatusLabel(result.parsingStatus))}</span>
            </header>
            <dl>
                <div><dt>Hash</dt><dd>${escapeHtml(uploadedFile.checksumSha256 || "No disponible")}</dd></div>
                <div><dt>Proveedor detectado</dt><dd>${escapeHtml(result.detectedProvider || "Desconocido")}</dd></div>
                <div><dt>Tarjeta detectada</dt><dd>${escapeHtml(cardLabel(result.detectedCardBrand))}</dd></div>
                <div><dt>Mensaje</dt><dd>${escapeHtml(uploadResultMessage(result, uploadedFile))}</dd></div>
            </dl>
            ${renderWarnings(result.warnings)}
            ${draft ? `<p class="muted">Borrador #${draft.id}: ${escapeHtml(toYearMonth(draft.paymentMonth) || "sin mes de pago")} · ${formatPesos(draft.totalPesos)} / ${formatUsd(draft.totalUsd)} · ${draft.transactions?.length || 0} filas</p><button type="button" class="secondary-button" data-open-upload-draft="${draft.id}">Revisar borrador #${draft.id}</button>` : '<p class="muted">No se creó ningún borrador para este archivo.</p>'}
        `;
        article.querySelector("[data-open-upload-draft]")?.addEventListener("click", () => openDraft(draft.id));
        container.append(article);
    });
}

function renderWarnings(warnings) {
    if (!Array.isArray(warnings) || warnings.length === 0) {
        return "";
    }
    const items = warnings.map((warning) => `<li>${escapeHtml(warning)}</li>`).join("");
    return `<ul class="warning-list">${items}</ul>`;
}

async function openDraft(id) {
    try {
        showReviewFeedback("Cargando resumen en borrador...");
        activeDraft = await api.statement(id);
        renderDraftReview(activeDraft);
        showReviewFeedback("Borrador cargado para revisión.");
    } catch (error) {
        showReviewFeedback(`No se pudo cargar el borrador: ${error.message}`, true);
    }
}

async function saveActiveDraft(event) {
    event.preventDefault();
    if (!activeDraft) {
        return;
    }

    try {
        setButtonBusy(document.querySelector("#statement-form button[type='submit']"), true, "Guardando...");
        const response = await api.updateStatement(activeDraft.id, statementPayload());
        activeDraft = response;
        renderDraftReview(activeDraft);
        await callbacks.onDraftChanged();
        showReviewFeedback("Resumen en borrador guardado.");
    } catch (error) {
        showReviewFeedback(`No se pudo guardar el borrador: ${error.message}`, true);
    } finally {
        setButtonBusy(document.querySelector("#statement-form button[type='submit']"), false);
    }
}

async function confirmActiveDraft() {
    if (!activeDraft) {
        return;
    }
    const payload = statementPayload();
    if (!payload.paymentMonth || (!payload.totalPesos && !payload.totalUsd)) {
        showReviewFeedback("El mes de pago y al menos un total del resumen son obligatorios antes de confirmar.", true);
        return;
    }

    try {
        setButtonBusy(document.querySelector("#confirm-statement-button"), true, "Confirmando...");
        await api.updateStatement(activeDraft.id, payload);
        activeDraft = await api.confirmStatement(activeDraft.id);
        renderDraftReview(activeDraft);
        await callbacks.onDraftConfirmed(activeDraft);
        showReviewFeedback("Resumen confirmado. Se actualizaron los datos del panel público.");
    } catch (error) {
        showReviewFeedback(`No se pudo confirmar el resumen: ${error.message}`, true);
    } finally {
        setButtonBusy(document.querySelector("#confirm-statement-button"), false);
        if (activeDraft) {
            document.querySelector("#confirm-statement-button").disabled = activeDraft.status !== "DRAFT";
        }
    }
}

function renderDraftReview(statement) {
    document.querySelector("#draft-empty-state").hidden = true;
    const panel = document.querySelector("#draft-review-panel");
    panel.hidden = false;
    document.querySelector("#draft-review-title").textContent = `Resumen en borrador #${statement.id}`;
    document.querySelector("#draft-review-meta").textContent = draftTransactionCountLabel(statement);
    renderStatementForm(statement);
    renderMissingTransactionForm(statement);
    renderDraftTransactions(statement.transactions || []);
}

export function draftTransactionCountLabel(statement) {
    const count = statement.transactions?.length || 0;
    return `${statementStatusLabel(statement.status)} · ${count} ${count === 1 ? "fila de transacción en borrador" : "filas de transacciones en borrador"}`;
}

function renderStatementForm(statement) {
    const controls = missingTransactionControlsState(statement);
    document.querySelector("#statement-provider").innerHTML = options(providers, statement.provider, providerLabel);
    document.querySelector("#statement-card-brand").innerHTML = options(cardBrands, statement.cardBrand, cardLabel);
    document.querySelector("#statement-card-alias").value = statement.cardAlias || "";
    document.querySelector("#statement-period-start").value = statement.periodStart || "";
    document.querySelector("#statement-period-end").value = statement.periodEnd || "";
    document.querySelector("#statement-closing-date").value = statement.closingDate || "";
    document.querySelector("#statement-due-date").value = statement.dueDate || "";
    document.querySelector("#statement-payment-month").value = toYearMonth(statement.paymentMonth);
    document.querySelector("#statement-total-pesos").value = decimalValue(statement.totalPesos);
    document.querySelector("#statement-total-usd").value = decimalValue(statement.totalUsd);
    document.querySelector("#statement-minimum-payment-pesos").value = decimalValue(statement.minimumPaymentPesos);
    document.querySelector("#statement-notes-placeholder").value = "La API actual no permite guardar notas del resumen.";
    document.querySelector("#confirm-statement-button").disabled = controls.confirmDisabled;
}

function renderMissingTransactionForm(statement) {
    const form = document.querySelector("#missing-transaction-form");
    form.hidden = missingTransactionControlsState(statement).formHidden;
    if (form.hidden) {
        return;
    }

    document.querySelector("#missing-transaction-type").innerHTML = options(transactionTypes, "PURCHASE", typeLabel);
    document.querySelector("#missing-transaction-category").innerHTML = `<option value="">Sin categoría</option>${categoryOptions()}`;
    resetMissingTransactionForm(false);
}

export function missingTransactionControlsState(statement) {
    const isDraft = statement?.status === "DRAFT";
    return {
        confirmDisabled: !isDraft,
        formHidden: !isDraft
    };
}

function statementPayload() {
    return {
        provider: document.querySelector("#statement-provider").value,
        cardBrand: document.querySelector("#statement-card-brand").value,
        cardAlias: textOrNull("#statement-card-alias"),
        periodStart: textOrNull("#statement-period-start"),
        periodEnd: textOrNull("#statement-period-end"),
        closingDate: textOrNull("#statement-closing-date"),
        dueDate: textOrNull("#statement-due-date"),
        paymentMonth: monthOrNull("#statement-payment-month"),
        totalPesos: decimalOrNull("#statement-total-pesos"),
        totalUsd: decimalOrNull("#statement-total-usd"),
        minimumPaymentPesos: decimalOrNull("#statement-minimum-payment-pesos")
    };
}

function renderDraftTransactions(transactions) {
    const table = document.querySelector("#draft-transactions-table");
    const empty = document.querySelector("#draft-transactions-empty");
    table.innerHTML = "";
    transactions.forEach((transaction) => {
        const row = document.createElement("tr");
        row.dataset.transactionId = transaction.id;
        row.innerHTML = `
            <td><input name="transactionDate" type="date" value="${escapeHtml(transaction.transactionDate || "")}"></td>
            <td><input name="description" type="text" maxlength="240" required value="${escapeHtml(transaction.description || "")}"></td>
            <td><select name="type">${options(transactionTypes, transaction.type, typeLabel)}</select></td>
            <td><select name="categoryId"><option value="">Sin categoría</option>${categoryOptions(transaction.category?.id)}</select></td>
            <td><input name="currentInstallment" type="number" min="1" step="1" value="${escapeHtml(transaction.currentInstallment || "")}"></td>
            <td><input name="totalInstallments" type="number" min="1" step="1" value="${escapeHtml(transaction.totalInstallments || "")}"></td>
            <td><input name="amountPesos" type="number" min="0" step="0.01" value="${decimalValue(transaction.amountPesos)}"></td>
            <td><input name="amountUsd" type="number" min="0" step="0.01" value="${decimalValue(transaction.amountUsd)}"></td>
            <td><input name="notes" type="text" maxlength="500" value="${escapeHtml(transaction.notes || "")}"></td>
            <td class="row-actions">
                <button type="button" class="secondary-button" data-save-transaction>Guardar fila</button>
                <button type="button" class="danger-button" data-delete-transaction>Eliminar fila</button>
            </td>
        `;
        row.querySelector("[data-save-transaction]").addEventListener("click", () => saveDraftTransaction(row));
        row.querySelector("[data-delete-transaction]").addEventListener("click", () => deleteDraftTransaction(transaction.id));
        table.append(row);
    });
    empty.hidden = transactions.length > 0;
}

async function saveDraftTransaction(row) {
    const button = row.querySelector("[data-save-transaction]");
    try {
        const description = row.querySelector('[name="description"]').value.trim();
        if (!description) {
            showReviewFeedback("La descripción de la transacción es obligatoria.", true);
            return;
        }
        setButtonBusy(button, true, "Guardando...");
        await api.updateTransaction(row.dataset.transactionId, transactionPayload(row, description));
        await reloadActiveDraft("Transacción guardada.");
    } catch (error) {
        showReviewFeedback(`No se pudo guardar la transacción: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function deleteDraftTransaction(id) {
    const row = document.querySelector(`[data-transaction-id="${id}"]`);
    const button = row?.querySelector("[data-delete-transaction]");
    try {
        setButtonBusy(button, true, "Eliminando...");
        await api.deleteTransaction(id);
        await reloadActiveDraft("Transacción eliminada del borrador.");
    } catch (error) {
        showReviewFeedback(`No se pudo eliminar la transacción: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function addMissingTransaction(event) {
    event.preventDefault();
    if (!activeDraft || activeDraft.status !== "DRAFT") {
        return;
    }

    const button = document.querySelector("#add-missing-transaction-button");
    let errorPrefix = "No se pudo agregar la transacción faltante:";
    try {
        const description = textOrNull("#missing-transaction-description");
        const intent = missingTransactionSubmitIntent(
                activeDraft,
                description,
                decimalOrNull("#missing-transaction-amount-pesos"),
                decimalOrNull("#missing-transaction-amount-usd")
        );
        if (!intent.shouldSubmit) {
            if (intent.feedback) {
                showReviewFeedback(intent.feedback, true);
            }
            return;
        }

        errorPrefix = intent.errorPrefix;
        setButtonBusy(button, true, intent.loadingLabel);
        await api.createStatementTransaction(intent.statementId, missingTransactionPayload(intent.description));
        resetMissingTransactionForm();
        await reloadActiveDraft(intent.successFeedback);
    } catch (error) {
        showReviewFeedback(`${errorPrefix} ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

export function missingTransactionSubmitIntent(activeStatement, description, amountPesos, amountUsd) {
    const normalizedDescription = String(description || "").trim();
    const hasAmount = (amountPesos !== null && amountPesos !== undefined && amountPesos !== "")
            || (amountUsd !== null && amountUsd !== undefined && amountUsd !== "");

    if (!activeStatement || activeStatement.status !== "DRAFT") {
        return { reason: "not-draft", shouldSubmit: false };
    }
    if (!normalizedDescription) {
        return {
            feedback: "La descripción de la transacción faltante es obligatoria.",
            reason: "missing-description",
            shouldSubmit: false
        };
    }
    if (!hasAmount) {
        return {
            feedback: "La transacción faltante requiere un importe en pesos o USD.",
            reason: "missing-amount",
            shouldSubmit: false
        };
    }

    return {
        clearBusyInFinally: true,
        description: normalizedDescription,
        errorPrefix: "No se pudo agregar la transacción faltante:",
        loadingLabel: "Agregando...",
        notifyDraftChanged: true,
        reloadDraft: true,
        resetForm: true,
        shouldSubmit: true,
        statementId: activeStatement.id,
        successFeedback: "Transacción faltante agregada al borrador."
    };
}

async function reloadActiveDraft(message) {
    activeDraft = await api.statement(activeDraft.id);
    renderDraftReview(activeDraft);
    await callbacks.onDraftChanged();
    showReviewFeedback(message);
}

function transactionPayload(row, description) {
    return {
        transactionDate: textFromRow(row, "transactionDate"),
        description,
        type: textFromRow(row, "type"),
        categoryId: numberFromRow(row, "categoryId"),
        amountPesos: decimalFromRow(row, "amountPesos"),
        amountUsd: decimalFromRow(row, "amountUsd"),
        currentInstallment: numberFromRow(row, "currentInstallment"),
        totalInstallments: numberFromRow(row, "totalInstallments"),
        notes: textFromRow(row, "notes")
    };
}

function missingTransactionPayload(description) {
    return {
        transactionDate: textOrNull("#missing-transaction-date"),
        description,
        type: textOrNull("#missing-transaction-type"),
        categoryId: numberOrNull("#missing-transaction-category"),
        amountPesos: decimalOrNull("#missing-transaction-amount-pesos"),
        amountUsd: decimalOrNull("#missing-transaction-amount-usd"),
        currentInstallment: numberOrNull("#missing-transaction-current-installment"),
        totalInstallments: numberOrNull("#missing-transaction-total-installments"),
        notes: textOrNull("#missing-transaction-notes")
    };
}

function resetMissingTransactionForm(clearFeedback = true) {
    const form = document.querySelector("#missing-transaction-form");
    form.reset();
    document.querySelector("#missing-transaction-type").value = "PURCHASE";
    if (clearFeedback) {
        showReviewFeedback("");
    }
}

function options(values, selected, labeler) {
    return values.map((value) => `<option value="${value}" ${value === selected ? "selected" : ""}>${escapeHtml(labeler(value))}</option>`).join("");
}

function categoryOptions(selectedId) {
    return categories.map((category) => {
        const selected = Number(selectedId) === Number(category.id) ? "selected" : "";
        return `<option value="${category.id}" ${selected}>${escapeHtml(category.name)}</option>`;
    }).join("");
}

function textOrNull(selector) {
    const value = document.querySelector(selector).value.trim();
    return value || null;
}

function monthOrNull(selector) {
    const value = document.querySelector(selector).value;
    return value ? `${value}-01` : null;
}

function numberOrNull(selector) {
    const value = document.querySelector(selector).value;
    return value === "" ? null : Number(value);
}

function decimalOrNull(selector) {
    const value = document.querySelector(selector).value;
    return value === "" ? null : value;
}

function decimalValue(value) {
    return value === null || value === undefined ? "" : String(value);
}

function textFromRow(row, name) {
    const value = row.querySelector(`[name="${name}"]`).value.trim();
    return value || null;
}

function numberFromRow(row, name) {
    const value = row.querySelector(`[name="${name}"]`).value;
    return value === "" ? null : Number(value);
}

function decimalFromRow(row, name) {
    const value = row.querySelector(`[name="${name}"]`).value;
    return value === "" ? null : value;
}

function showUploadFeedback(message, isError = false) {
    const feedback = document.querySelector("#statement-upload-feedback");
    feedback.textContent = message;
    feedback.classList.toggle("error-text", isError);
}

function showReviewFeedback(message, isError = false) {
    const feedback = document.querySelector("#draft-review-feedback");
    feedback.textContent = message;
    feedback.classList.toggle("error-text", isError);
}

function uploadResultMessage(result, uploadedFile) {
    if (result.error) {
        return `${result.error} No se muestra texto del resumen ni contenido del PDF original.`;
    }
    return uploadedFile.parsingMessage || "Sin mensaje del analizador";
}

function parsingStatusLabel(status) {
    const labels = {
        NOT_STARTED: "No iniciado",
        PENDING: "Pendiente",
        PARSED: "Analizado",
        FAILED: "Fallido",
        SKIPPED: "Omitido",
        UNKNOWN: "Desconocido"
    };
    return labels[status] || status || "Desconocido";
}

export function parserDisplayLabel(parserName) {
    const labels = {
        SantanderVisaParser: "Santander Visa",
        SantanderAmexParser: "Santander American Express",
        NaranjaXParser: "Naranja X"
    };
    const text = String(parserName || "").trim();
    if (!text) {
        return "Sin analizador seleccionado";
    }
    if (labels[text]) {
        return labels[text];
    }
    return /^[A-Z][A-Za-z0-9]+Parser$/.test(text) ? "Analizador compatible" : text;
}

function providerLabel(provider) {
    const labels = {
        VISA_HOME: "Visa Home",
        MASTERCARD_HOME: "Mastercard Home",
        BANK_PORTAL: "Portal bancario",
        SANTANDER: "Santander",
        NARANJA_X: "Naranja X",
        MANUAL: "Manual"
    };
    return labels[provider] || provider || "Proveedor desconocido";
}

function statementStatusLabel(status) {
    const labels = {
        CONFIRMED: "Confirmado",
        DRAFT: "Borrador"
    };
    return labels[status] || status || "Sin estado";
}

function formatBytes(bytes) {
    if (bytes >= 1_048_576) {
        return `${(bytes / 1_048_576).toFixed(2)} MB`;
    }
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}
