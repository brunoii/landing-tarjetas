import { api } from "./api.js";
import { cardLabel, escapeHtml, formatPesos, formatUsd, toYearMonth, typeLabel } from "./utils.js";

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
        list.innerHTML = '<p class="empty-state">No draft statements are waiting for review.</p>';
        return;
    }

    drafts.forEach((statement) => {
        const item = document.createElement("article");
        item.className = "draft-card";
        item.innerHTML = `
            <div>
                <strong>${escapeHtml(cardLabel(statement.cardBrand))}</strong>
                <p class="muted">${escapeHtml(statement.provider || "Unknown provider")} · ${escapeHtml(toYearMonth(statement.paymentMonth) || "No payment month")}</p>
            </div>
            <dl>
                <div><dt>Total ARS</dt><dd>${formatPesos(statement.totalPesos)}</dd></div>
                <div><dt>Total USD</dt><dd>${formatUsd(statement.totalUsd)}</dd></div>
                <div><dt>Rows</dt><dd>${statement.transactionCount || 0}</dd></div>
            </dl>
            <button type="button" class="secondary-button" data-review-draft="${statement.id}">Review draft</button>
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
        showUploadFeedback("Uploading PDFs locally for metadata-only parsing...");
        const response = await api.uploadStatements(files);
        renderUploadResults(response.files || []);
        input.value = "";
        showSelectedFileHint();
        await callbacks.onDraftChanged();
        const firstDraft = (response.files || []).find((result) => result.draftStatement)?.draftStatement;
        if (firstDraft) {
            await openDraft(firstDraft.id);
        }
        showUploadFeedback("Upload finished. Review each draft before confirming it.");
    } catch (error) {
        showUploadFeedback(error.message, true);
        callbacks.setStatus(error.message, true);
    }
}

function validateFiles(files) {
    if (files.length === 0) {
        return "Select one or more PDF files before uploading.";
    }
    const totalSize = files.reduce((current, file) => current + file.size, 0);
    if (totalSize > MAX_REQUEST_SIZE_BYTES) {
        return "The selected files exceed the 5 MB request limit.";
    }
    const invalid = files.find((file) => !isPdf(file));
    if (invalid) {
        return `${invalid.name} is not a PDF file.`;
    }
    const tooLarge = files.find((file) => file.size > MAX_PDF_SIZE_BYTES);
    if (tooLarge) {
        return `${tooLarge.name} exceeds the 1 MB per-file limit.`;
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
        hint.textContent = "PDF only. Maximum 1 MB per file and 5 MB per request.";
        return;
    }
    const names = files.map((file) => `${file.name} (${formatBytes(file.size)})`).join(", ");
    hint.textContent = `${files.length} selected: ${names}`;
}

function renderUploadResults(results) {
    const container = document.querySelector("#upload-results");
    container.innerHTML = "";
    if (results.length === 0) {
        container.innerHTML = '<p class="empty-state">No upload results returned.</p>';
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
                    <strong>${escapeHtml(uploadedFile.originalFilename || "Unnamed PDF")}</strong>
                    <p class="muted">${escapeHtml(result.parsingStatus || uploadedFile.parsingStatus || "UNKNOWN")} · ${escapeHtml(result.parserName || "No parser selected")}</p>
                </div>
                <span class="status-chip ${result.parsingStatus === "PARSED" ? "loaded" : ""}">${escapeHtml(result.parsingStatus || "UNKNOWN")}</span>
            </header>
            <dl>
                <div><dt>Hash</dt><dd>${escapeHtml(uploadedFile.checksumSha256 || "Unavailable")}</dd></div>
                <div><dt>Detected provider</dt><dd>${escapeHtml(result.detectedProvider || "Unknown")}</dd></div>
                <div><dt>Detected card</dt><dd>${escapeHtml(cardLabel(result.detectedCardBrand))}</dd></div>
                <div><dt>Message</dt><dd>${escapeHtml(result.error || uploadedFile.parsingMessage || "No parser message")}</dd></div>
            </dl>
            ${renderWarnings(result.warnings)}
            ${draft ? `<p class="muted">Draft #${draft.id}: ${escapeHtml(toYearMonth(draft.paymentMonth) || "no payment month")} · ${formatPesos(draft.totalPesos)} / ${formatUsd(draft.totalUsd)} · ${draft.transactions?.length || 0} rows</p><button type="button" class="secondary-button" data-open-upload-draft="${draft.id}">Review draft #${draft.id}</button>` : '<p class="muted">No draft was created for this file.</p>'}
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
        showReviewFeedback("Loading draft statement...");
        activeDraft = await api.statement(id);
        renderDraftReview(activeDraft);
        showReviewFeedback("Draft loaded for review.");
    } catch (error) {
        showReviewFeedback(error.message, true);
    }
}

async function saveActiveDraft(event) {
    event.preventDefault();
    if (!activeDraft) {
        return;
    }

    try {
        const response = await api.updateStatement(activeDraft.id, statementPayload());
        activeDraft = response;
        renderDraftReview(activeDraft);
        await callbacks.onDraftChanged();
        showReviewFeedback("Draft statement saved.");
    } catch (error) {
        showReviewFeedback(error.message, true);
    }
}

async function confirmActiveDraft() {
    if (!activeDraft) {
        return;
    }
    const payload = statementPayload();
    if (!payload.paymentMonth || (!payload.totalPesos && !payload.totalUsd)) {
        showReviewFeedback("Payment month and at least one statement total are required before confirmation.", true);
        return;
    }

    try {
        await api.updateStatement(activeDraft.id, payload);
        activeDraft = await api.confirmStatement(activeDraft.id);
        renderDraftReview(activeDraft);
        await callbacks.onDraftConfirmed(activeDraft);
        showReviewFeedback("Statement confirmed. Public dashboard data was refreshed.");
    } catch (error) {
        showReviewFeedback(error.message, true);
    }
}

function renderDraftReview(statement) {
    document.querySelector("#draft-empty-state").hidden = true;
    const panel = document.querySelector("#draft-review-panel");
    panel.hidden = false;
    document.querySelector("#draft-review-title").textContent = `Draft statement #${statement.id}`;
    document.querySelector("#draft-review-meta").textContent = `${statement.status} · ${statement.transactions?.length || 0} detected transaction rows`;
    renderStatementForm(statement);
    renderDraftTransactions(statement.transactions || []);
}

function renderStatementForm(statement) {
    document.querySelector("#statement-provider").innerHTML = options(providers, statement.provider, (value) => value.replaceAll("_", " "));
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
    document.querySelector("#statement-notes-placeholder").value = "Statement notes are not supported by the current API.";
    document.querySelector("#confirm-statement-button").disabled = statement.status !== "DRAFT";
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
            <td><select name="categoryId"><option value="">Uncategorized</option>${categoryOptions(transaction.category?.id)}</select></td>
            <td><input name="currentInstallment" type="number" min="1" step="1" value="${escapeHtml(transaction.currentInstallment || "")}"></td>
            <td><input name="totalInstallments" type="number" min="1" step="1" value="${escapeHtml(transaction.totalInstallments || "")}"></td>
            <td><input name="amountPesos" type="number" min="0" step="0.01" value="${decimalValue(transaction.amountPesos)}"></td>
            <td><input name="amountUsd" type="number" min="0" step="0.01" value="${decimalValue(transaction.amountUsd)}"></td>
            <td><input name="notes" type="text" maxlength="500" value="${escapeHtml(transaction.notes || "")}"></td>
            <td class="row-actions">
                <button type="button" class="secondary-button" data-save-transaction>Save</button>
                <button type="button" class="danger-button" data-delete-transaction>Delete</button>
            </td>
        `;
        row.querySelector("[data-save-transaction]").addEventListener("click", () => saveDraftTransaction(row));
        row.querySelector("[data-delete-transaction]").addEventListener("click", () => deleteDraftTransaction(transaction.id));
        table.append(row);
    });
    empty.hidden = transactions.length > 0;
}

async function saveDraftTransaction(row) {
    try {
        const description = row.querySelector('[name="description"]').value.trim();
        if (!description) {
            showReviewFeedback("Transaction description is required.", true);
            return;
        }
        await api.updateTransaction(row.dataset.transactionId, transactionPayload(row, description));
        await reloadActiveDraft("Transaction saved.");
    } catch (error) {
        showReviewFeedback(error.message, true);
    }
}

async function deleteDraftTransaction(id) {
    try {
        await api.deleteTransaction(id);
        await reloadActiveDraft("Transaction deleted from the draft.");
    } catch (error) {
        showReviewFeedback(error.message, true);
    }
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

function formatBytes(bytes) {
    if (bytes >= 1_048_576) {
        return `${(bytes / 1_048_576).toFixed(2)} MB`;
    }
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}
