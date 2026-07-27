import { api } from "./api.js?v=20260727-super-inventory-stage17-session-shell-api";
import { escapeHtml, formatPesos, setButtonBusy } from "./utils.js";

let supermarketApi = api;
let superItems = [];
let superPriceSources = [];
let editingItemId = null;
let editingItemOriginalStock = null;
let editingCategoryId = null;
let superCategoryTableCollapsed = true;
let superCategoryCount = 0;
let currentBarcodeAlias = null;
let selectedPriceObservationItem = null;
let currentTicketOcrReview = null;
let superBarcodeScannerState = createSuperBarcodeScannerState();

const SUPER_RECENT_HISTORY_LIMIT = 50;
const SUPER_BARCODE_SCAN_DEBOUNCE_MS = 2000;

export const SUPER_FIELD_LIMITS = Object.freeze({
    categoryName: 80,
    itemName: 160,
    itemNotes: 500,
    itemUnit: 40,
    presentationLabel: 120,
    priceSourceLabel: 120,
    barcodeCode: 80,
    barcodeFormat: 40
});

export function setupSupermarket({ apiClient = api } = {}) {
    supermarketApi = apiClient;
    stopSuperBarcodeScanner({ nextPhase: "idle", statusMessage: "", announce: false });
    superPriceSources = [];
    editingItemId = null;
    editingItemOriginalStock = null;
    editingCategoryId = null;
    superCategoryTableCollapsed = true;
    currentBarcodeAlias = null;
    selectedPriceObservationItem = null;
    currentTicketOcrReview = null;
    superBarcodeScannerState = createSuperBarcodeScannerState();

    applySupermarketFieldLimits();

    document.querySelector("#super-category-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        await createSuperCategory();
    });

    document.querySelector("#super-item-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        await saveSuperItem();
    });

    document.querySelector("#super-item-cancel-edit")?.addEventListener("click", () => resetSuperItemForm());
    document.querySelector("#super-barcode-form")?.addEventListener("submit", submitSuperBarcodeLookup);
    document.querySelector("#super-barcode-attach")?.addEventListener("click", attachSuperBarcodeAlias);
    document.querySelector("#super-barcode-remove")?.addEventListener("click", removeSuperBarcodeAlias);
    document.querySelector("#super-barcode-scan-start")?.addEventListener("click", () => startSuperBarcodeScanner());
    document.querySelector("#super-barcode-scan-stop")?.addEventListener("click", () => stopSuperBarcodeScanner({ nextPhase: "idle", statusMessage: "Escaneo detenido. Podés ingresar el código manualmente." }));
    document.querySelector("#super-barcode-purchase")?.addEventListener("click", handleSuperBarcodeResolvedAction);
    document.querySelector("#super-barcode-consume")?.addEventListener("click", handleSuperBarcodeResolvedAction);
    document.querySelector("#super-category-toggle")?.addEventListener("click", toggleSuperCategoryTable);
    document.querySelector("#super-generate-list")?.addEventListener("click", generateSuperList);
    document.querySelector("#super-copy-list")?.addEventListener("click", copyGeneratedSuperList);
    document.querySelector("#super-download-list")?.addEventListener("click", downloadGeneratedSuperList);
    document.querySelector("#super-whatsapp-list")?.addEventListener("click", shareGeneratedSuperList);
    document.querySelector("#super-uncheck-all")?.addEventListener("click", uncheckAllSuperItems);
    document.querySelector("#super-movement-form")?.addEventListener("submit", submitSuperMovementForm);
    document.querySelector("#super-movement-cancel")?.addEventListener("click", closeSuperMovementModal);
    document.querySelector("#super-movement-close")?.addEventListener("click", closeSuperMovementModal);
    document.querySelector("#super-price-observation-form")?.addEventListener("submit", submitSuperPriceObservationForm);
    document.querySelector("#super-price-observation-global-reset")?.addEventListener("click", resetSuperPriceObservationContext);
    document.querySelector("#super-price-source-form")?.addEventListener("submit", submitSuperPriceSourceForm);
    document.querySelector("#super-ticket-ocr-form")?.addEventListener("submit", submitSuperTicketOcrUploadForm);
    document.querySelector("#super-ticket-ocr-confirm-form")?.addEventListener("submit", submitSuperTicketOcrConfirmForm);
    document.querySelector("#super-ticket-ocr-discard")?.addEventListener("click", () => clearSuperTicketOcrReview("Revisión OCR descartada."));
    document.querySelector("#super-ticket-ocr-date-candidate")?.addEventListener("change", syncSuperTicketOcrDateCandidate);
    document.querySelector("#super-ticket-ocr-source-candidate")?.addEventListener("change", syncSuperTicketOcrSourceCandidate);
    document.querySelector("#super-item-presentation-price-source")?.addEventListener("change", () => syncSuperItemPriceSourceInputs("reusable"));
    document.querySelector("#super-item-presentation-price-source-label")?.addEventListener("input", () => syncSuperItemPriceSourceInputs("manual"));
    document.querySelector("#super-price-observation-item")?.addEventListener("change", (event) => {
        prefillSuperPriceObservationForm(itemById(event.currentTarget.value));
    });
    document.addEventListener?.("visibilitychange", handleSuperBarcodeVisibilityLoss);
    globalThis.addEventListener?.("pagehide", handleSuperBarcodePageHide);

    syncSuperBarcodeScannerUi();
    syncSuperBarcodeResolvedActions();

    document.querySelector("#super-items-table")?.addEventListener("change", async (event) => {
        const checkbox = event.target.closest("input[data-super-action='checked']");
        if (checkbox) {
            await updateSuperItemChecked(checkbox.dataset.superItemId, checkbox.checked, checkbox);
        }
    });

    document.querySelector("#super-items-table")?.addEventListener("click", async (event) => {
        const button = event.target.closest("button[data-super-action]");
        if (!button) {
            return;
        }
        await handleSuperItemAction(button);
    });

    document.querySelector("#super-category-list")?.addEventListener("click", async (event) => {
        const button = event.target.closest("button[data-super-category-action]");
        if (!button) {
            return;
        }
        await handleSuperCategoryAction(button);
    });

    document.querySelector("#super-ticket-ocr-table")?.addEventListener("click", async (event) => {
        const button = event.target.closest("button[data-super-ticket-ocr-action]");
        if (!button) {
            return;
        }
        await handleSuperTicketOcrLineAction(button);
    });

    loadSupermarket();
}

function applySupermarketFieldLimits() {
    document.querySelectorAll?.("[data-super-limit]")?.forEach((field) => {
        if (!field) {
            return;
        }
        const limit = SUPER_FIELD_LIMITS[field.dataset.superLimit];
        if (!limit) {
            return;
        }
        field.maxLength = limit;
        field.setAttribute?.("maxlength", String(limit));
    });
}

function superFieldLimitAttribute(fieldName) {
    return `maxlength="${SUPER_FIELD_LIMITS[fieldName]}"`;
}

export function superItemPayloadFromValues(values) {
    const categoryId = Number(values.categoryId || 0);
    const commercialPresentationPriceSourceId = Number(values.commercialPresentationPriceSourceId || 0);
    const unit = String(values.unit || "").trim();
    const commercialPresentationLabel = String(values.commercialPresentationLabel || "").trim();
    const commercialPresentationQuantity = String(values.commercialPresentationQuantity || "").trim();
    const commercialPresentationPricePesos = String(values.commercialPresentationPricePesos || "").trim();
    const commercialPresentationPriceSourceLabel = String(values.commercialPresentationPriceSourceLabel || "").trim();
    const commercialPresentationPriceObservedDate = String(values.commercialPresentationPriceObservedDate || "").trim();
    const habitualObjective = String(values.habitualObjective || "").trim();
    const quickQuantity = String(values.quickQuantity || "").trim();
    const payload = {
        name: String(values.name || "").trim(),
        categoryId: categoryId > 0 ? categoryId : null,
        checked: values.checked === true || values.checked === "true" || values.checked === "on",
        notes: String(values.notes || "").trim()
    };
    if (unit) {
        payload.unit = unit;
    }
    if (commercialPresentationLabel) {
        payload.commercialPresentationLabel = commercialPresentationLabel;
    }
    if (commercialPresentationQuantity) {
        payload.commercialPresentationQuantity = commercialPresentationQuantity;
    }
    if (commercialPresentationPricePesos) {
        payload.commercialPresentationPricePesos = commercialPresentationPricePesos;
    }
    if (commercialPresentationPriceSourceId > 0) {
        payload.commercialPresentationPriceSourceId = commercialPresentationPriceSourceId;
    } else if (commercialPresentationPriceSourceLabel) {
        payload.commercialPresentationPriceSourceLabel = commercialPresentationPriceSourceLabel;
    }
    if (commercialPresentationPriceObservedDate) {
        payload.commercialPresentationPriceObservedDate = commercialPresentationPriceObservedDate;
    }
    if (habitualObjective) {
        payload.habitualObjective = habitualObjective;
    }
    if (quickQuantity) {
        payload.quickQuantity = quickQuantity;
    }
    return payload;
}

export function validateSuperItemPayload(payload) {
    if (!payload.name) {
        return "El nombre del producto es obligatorio.";
    }
    if (!payload.categoryId) {
        return "La categoría del producto es obligatoria.";
    }
    if (payload.habitualObjective && (!Number.isFinite(Number(payload.habitualObjective)) || Number(payload.habitualObjective) <= 0)) {
        return "El objetivo habitual debe ser mayor que cero.";
    }
    if (payload.quickQuantity && (!Number.isFinite(Number(payload.quickQuantity)) || Number(payload.quickQuantity) <= 0)) {
        return "La cantidad rápida debe ser mayor que cero.";
    }
    if (payload.commercialPresentationQuantity && (!Number.isFinite(Number(payload.commercialPresentationQuantity)) || Number(payload.commercialPresentationQuantity) <= 0)) {
        return "La cantidad de presentación debe ser mayor que cero.";
    }
    if (payload.commercialPresentationQuantity && !payload.unit) {
        return "La cantidad de presentación requiere unidad de inventario.";
    }
    if (payload.commercialPresentationQuantity && !payload.commercialPresentationLabel) {
        return "La cantidad de presentación requiere una presentación comercial.";
    }
    if (payload.commercialPresentationPricePesos && (!Number.isFinite(Number(payload.commercialPresentationPricePesos)) || Number(payload.commercialPresentationPricePesos) <= 0)) {
        return "El precio de referencia debe ser mayor que cero.";
    }
    if (payload.commercialPresentationPriceSourceId && payload.commercialPresentationPriceSourceLabel) {
        return "Elegí una fuente reutilizable o una manual, no ambas.";
    }
    if (payload.commercialPresentationPriceSourceId && !payload.commercialPresentationPricePesos) {
        return "La fuente del precio requiere un precio de referencia.";
    }
    if (payload.commercialPresentationPriceSourceId && !payload.commercialPresentationLabel) {
        return "La fuente del precio requiere una presentación comercial.";
    }
    if (payload.commercialPresentationPriceSourceLabel && !payload.commercialPresentationPricePesos) {
        return "La fuente del precio requiere un precio de referencia.";
    }
    if (payload.commercialPresentationPriceSourceLabel && !payload.commercialPresentationLabel) {
        return "La fuente del precio requiere una presentación comercial.";
    }
    if (payload.commercialPresentationPriceObservedDate && !payload.commercialPresentationPricePesos) {
        return "La fecha observada del precio requiere un precio de referencia.";
    }
    if (payload.commercialPresentationPriceObservedDate && !payload.commercialPresentationLabel) {
        return "La fecha observada del precio requiere una presentación comercial.";
    }
    if (payload.commercialPresentationPriceObservedDate && !isDateOnlyValue(payload.commercialPresentationPriceObservedDate)) {
        return "La fecha observada del precio debe usar formato YYYY-MM-DD.";
    }
    if (payload.commercialPresentationPriceObservedDate && payload.commercialPresentationPriceObservedDate > todayDateOnlyValue()) {
        return "La fecha observada del precio no puede ser futura.";
    }
    if (payload.commercialPresentationPricePesos && !payload.commercialPresentationLabel) {
        return "El precio de referencia requiere una presentación comercial.";
    }
    return "";
}

function selectedPriceSourceId(value) {
    const id = Number(value || 0);
    return id > 0 ? String(id) : "";
}

function currentSuperItemPriceSourceState() {
    const sourceSelect = document.querySelector("#super-item-presentation-price-source");
    const sourceLabelInput = document.querySelector("#super-item-presentation-price-source-label");
    return {
        sourceSelect,
        sourceLabelInput,
        selectedPriceSourceId: selectedPriceSourceId(sourceSelect?.value),
        sourceLabel: String(sourceLabelInput?.value || "").trim()
    };
}

export function syncSuperItemPriceSourceInputs(preferredMode = "") {
    const { sourceSelect, sourceLabelInput, selectedPriceSourceId, sourceLabel } = currentSuperItemPriceSourceState();
    const sourceHelp = document.querySelector("#super-item-presentation-price-source-help");
    if (!sourceSelect || !sourceLabelInput) {
        return;
    }
    if (preferredMode === "manual" && sourceLabel) {
        sourceSelect.value = "";
    } else if (preferredMode === "reusable" && selectedPriceSourceId) {
        sourceLabelInput.value = "";
    } else if (selectedPriceSourceId) {
        sourceLabelInput.value = "";
    } else if (sourceLabel) {
        sourceSelect.value = "";
    }
    sourceSelect.disabled = Boolean(sourceLabel);
    sourceLabelInput.disabled = Boolean(selectedPriceSourceId);
    if (sourceHelp) {
        sourceHelp.textContent = selectedPriceSourceId
            ? "Fuente reutilizable opcional del precio ref."
            : sourceLabel
                ? "Fuente manual opcional para el precio ref."
                : "Use una fuente reutilizable o una manual. Nunca ambas.";
    }
}

export function superPriceObservationPayloadFromValues(values) {
    const priceSourceId = Number(values?.priceSourceId || 0);
    const sourceLabel = String(values?.sourceLabel || "").trim().slice(0, SUPER_FIELD_LIMITS.priceSourceLabel);
    const observedDate = String(values?.observedDate || "").trim();
    const payload = {
        pricePesos: String(values?.pricePesos || "").trim(),
        syncCurrentReferencePrice: values?.syncCurrentReferencePrice === true || values?.syncCurrentReferencePrice === "true" || values?.syncCurrentReferencePrice === "on"
    };
    if (!payload.syncCurrentReferencePrice) {
        delete payload.syncCurrentReferencePrice;
    }
    if (priceSourceId > 0) {
        payload.priceSourceId = priceSourceId;
    } else if (sourceLabel) {
        payload.sourceLabel = sourceLabel;
    }
    if (observedDate) {
        payload.observedDate = observedDate;
    }
    return payload;
}

export function superPriceSourcePayloadFromValues(values) {
    return {
        name: String(values?.name || "").trim().slice(0, SUPER_FIELD_LIMITS.priceSourceLabel)
    };
}

export function validateSuperPriceSourcePayload(payload) {
    if (!payload?.name) {
        return "El nombre de la fuente de precio es obligatorio.";
    }
    return "";
}

export function validateSuperPriceObservationPayload(payload) {
    if (!Number.isFinite(Number(payload?.pricePesos)) || Number(payload.pricePesos) <= 0) {
        return "El precio observado debe ser mayor que cero.";
    }
    if (payload.observedDate && !isDateOnlyValue(payload.observedDate)) {
        return "La fecha observada de la observación debe usar formato YYYY-MM-DD.";
    }
    if (payload.observedDate && payload.observedDate > todayDateOnlyValue()) {
        return "La fecha observada de la observación no puede ser futura.";
    }
    return "";
}

function isDateOnlyValue(value) {
    return /^\d{4}-\d{2}-\d{2}$/.test(String(value || ""));
}

function todayDateOnlyValue() {
    const today = new Date();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${today.getFullYear()}-${month}-${day}`;
}

export function normalizeSuperBarcodeCode(value) {
    return String(value ?? "").trim();
}

function createSuperBarcodeScannerState() {
    return { phase: "idle", statusMessage: "", detector: null, stream: null, animationFrameId: null, lookupInFlight: false, lastCode: "", lastAcceptedAt: 0 };
}

export function getSuperBarcodeScannerAvailability(environment = globalThis) {
    const secureContext = Boolean(environment?.isSecureContext);
    const hasBarcodeDetector = typeof environment?.BarcodeDetector === "function";
    const hasMediaDevices = typeof environment?.navigator?.mediaDevices?.getUserMedia === "function";
    return { secureContext, hasBarcodeDetector, hasMediaDevices, supported: secureContext && hasBarcodeDetector && hasMediaDevices };
}

export function shouldAcceptSuperBarcodeScan({ nextCode, lastCode = "", lastAcceptedAt = 0, now = Date.now(), debounceMs = SUPER_BARCODE_SCAN_DEBOUNCE_MS, lookupInFlight = false }) {
    const normalizedCode = normalizeSuperBarcodeCode(nextCode);
    return Boolean(normalizedCode) && !lookupInFlight && (normalizedCode !== normalizeSuperBarcodeCode(lastCode) || now - Number(lastAcceptedAt || 0) >= debounceMs);
}

export function superBarcodePayloadFromValues(values) {
    const payload = { code: normalizeSuperBarcodeCode(values?.code) };
    const format = String(values?.format || "").trim();
    if (format) {
        payload.format = format;
    }
    return payload;
}

export function validateSuperBarcodeLookup(payload) {
    if (!payload.code) {
        return "Ingresá un código de barras para buscar.";
    }
    if (payload.code.length > SUPER_FIELD_LIMITS.barcodeCode) {
        return `El código de barras no puede superar ${SUPER_FIELD_LIMITS.barcodeCode} caracteres.`;
    }
    if (payload.format && payload.format.length > SUPER_FIELD_LIMITS.barcodeFormat) {
        return `El formato del código no puede superar ${SUPER_FIELD_LIMITS.barcodeFormat} caracteres.`;
    }
    return "";
}

export function superBarcodeAliasLabel(alias) {
    const code = normalizeSuperBarcodeCode(alias?.code);
    const format = String(alias?.format || "").trim();
    return format ? `${code} · ${format}` : code;
}

export function superItemConfigurationLabel(item) {
    return item.configured ? "Configurado" : "Pendiente";
}

export function superItemStockLabel(item) {
    if (item.currentStock === null || item.currentStock === undefined || item.currentStock === "") {
        return "Sin cargar";
    }
    return quantityWithUnit(item.currentStock, item.unit);
}

export function superItemQuickQuantityLabel(item) {
    if (!item.quickQuantity || !item.unit) {
        return "—";
    }
    return quantityWithUnit(item.quickQuantity, item.unit);
}

export function superItemCommercialPresentationLabel(item) {
    if (!item.commercialPresentationLabel) {
        return "—";
    }
    if (!item.commercialPresentationQuantity) {
        return item.commercialPresentationLabel;
    }
    return `${item.commercialPresentationLabel} · ${quantityWithUnit(item.commercialPresentationQuantity, item.unit)}`;
}

export function superItemCommercialPresentationPriceLabel(item) {
    if (item.commercialPresentationPricePesos === null || item.commercialPresentationPricePesos === undefined || item.commercialPresentationPricePesos === "") {
        return "—";
    }
    return formatPesos(item.commercialPresentationPricePesos);
}

export function superItemCommercialPresentationPriceSourceLabel(item) {
    const sourceLabel = String(item.commercialPresentationPriceSourceLabel || "").trim();
    return sourceLabel ? `Fuente: ${sourceLabel}` : "";
}

export function superItemCommercialPresentationPriceObservedDateLabel(item) {
    const observedDate = String(item.commercialPresentationPriceObservedDate || "").trim();
    return observedDate ? `Observado: ${observedDate}` : "";
}

export function superItemCommercialPresentationPriceHtml(item) {
    const value = `<span>${escapeHtml(superItemCommercialPresentationPriceLabel(item))}</span>`;
    const secondaryTexts = [
        superItemCommercialPresentationPriceSourceLabel(item),
        superItemCommercialPresentationPriceObservedDateLabel(item)
    ].filter(Boolean);
    if (secondaryTexts.length === 0) {
        return value;
    }
    return `${value}${secondaryTexts.map((text) => `<small class="super-fuente-precio">${escapeHtml(text)}</small>`).join("")}`;
}

export function superMovementTypeLabel(type) {
    return {
        ADJUSTMENT: "Ajuste",
        PURCHASE: "Compra",
        CONSUMPTION: "Consumo",
        QUICK_CONSUMPTION: "Consumo rápido"
    }[type] || "Movimiento";
}

export function superMovementQuantityLabel(movement) {
    if (movement.movementType === "ADJUSTMENT") {
        return `Ajuste a ${quantityWithUnit(movement.resultingStock, movement.itemUnit)}`;
    }
    const sign = movement.movementType === "PURCHASE" ? "+" : "-";
    return `${sign}${quantityWithUnit(movement.quantity, movement.itemUnit)}`;
}

export function superMovementSummary(movement) {
    return `${superMovementTypeLabel(movement.movementType)} · ${movement.itemName || "Producto"} · ${superMovementQuantityLabel(movement)} · stock ${quantityWithUnit(movement.resultingStock, movement.itemUnit)}`;
}

export function superPriceObservationPresentationLabel(observation) {
    const label = String(observation?.presentationLabelSnapshot || "").trim();
    const quantity = String(observation?.presentationQuantitySnapshot || "").trim();
    if (!label) {
        return "—";
    }
    return quantity ? `${label} · ${quantity}` : label;
}

export function superPriceObservationRowHtml(observation) {
    return `
        <td data-label="Creada">${escapeHtml(formatMovementDate(observation.createdAt))}</td>
        <td data-label="Producto">${escapeHtml(observation.itemName || "—")}</td>
        <td data-label="Presentación">${escapeHtml(superPriceObservationPresentationLabel(observation))}</td>
        <td data-label="Precio">${escapeHtml(formatPesos(observation.pricePesos))}</td>
        <td data-label="Fuente">${observation.sourceLabel ? escapeHtml(observation.sourceLabel) : "—"}</td>
        <td data-label="Observada">${observation.observedDate ? escapeHtml(observation.observedDate) : "—"}</td>
    `;
}

export function groupSuperItems(items) {
    const sorted = [...items].sort(compareSuperItems);
    return sorted.reduce((groups, item) => {
        const categoryName = item.categoryName || "Sin categoría";
        if (!groups.has(categoryName)) {
            groups.set(categoryName, []);
        }
        groups.get(categoryName).push(item);
        return groups;
    }, new Map());
}

export function generatedSuperListText(items) {
    const checkedItems = items.filter((item) => item.checked);
    if (checkedItems.length === 0) {
        return "No hay productos marcados para comprar.";
    }

    const lines = ["Lista del super"];
    for (const [categoryName, categoryItems] of groupSuperItems(checkedItems)) {
        lines.push("", categoryName);
        categoryItems.forEach((item) => {
            const quantityHint = item.quickQuantity && item.unit ? ` (${quantityWithUnit(item.quickQuantity, item.unit)})` : "";
            const notes = item.notes ? ` — ${item.notes}` : "";
            lines.push(`- ${item.name}${quantityHint}${notes}`);
        });
    }
    return lines.join("\n").trim();
}

export function renderSuperSuggestedItems(suggestions) {
    const list = document.querySelector("#super-suggested-list");
    const empty = document.querySelector("#super-suggested-empty");
    const summary = document.querySelector("#super-suggested-summary");
    if (!list) {
        return;
    }
    list.innerHTML = "";
    if (!Array.isArray(suggestions) || suggestions.length === 0) {
        if (empty) {
            empty.hidden = false;
        }
        if (summary) {
            summary.textContent = "Sin sugerencias por ahora.";
            summary.classList.toggle("loading", false);
        }
        return;
    }

    list.innerHTML = suggestions.map(superSuggestedItemHtml).join("");
    if (empty) {
        empty.hidden = true;
    }
    if (summary) {
        summary.textContent = `${suggestions.length} ${suggestions.length === 1 ? "producto sugerido" : "productos sugeridos"} para reponer.`;
        summary.classList.toggle("loading", false);
    }
}

export function superSuggestedItemText(item) {
    return `Comprar ${quantityWithUnit(item.suggestedQuantity, item.unit)}`;
}

function superSuggestedItemHtml(item) {
    const category = item.categoryName || "Sin categoría";
    const stock = quantityWithUnit(item.currentStock, item.unit);
    const objective = quantityWithUnit(item.habitualObjective, item.unit);
    return `
        <article class="super-suggested-item" data-super-suggested-item-id="${escapeHtml(String(item.itemId || ""))}">
            <div>
                <strong>${escapeHtml(item.name || "Producto")}</strong>
                <span>${escapeHtml(category)}</span>
            </div>
            <p class="super-suggested-quantity">${escapeHtml(superSuggestedItemText(item))}</p>
            <small>Stock actual ${escapeHtml(stock)} · objetivo ${escapeHtml(objective)}</small>
        </article>
    `;
}

function showSuperSuggestedLoading() {
    const list = document.querySelector("#super-suggested-list");
    const empty = document.querySelector("#super-suggested-empty");
    const summary = document.querySelector("#super-suggested-summary");
    if (list) {
        list.innerHTML = "";
    }
    if (empty) {
        empty.hidden = true;
    }
    if (summary) {
        summary.textContent = "Cargando sugerencias...";
        summary.classList.toggle("loading", true);
    }
}

async function loadSupermarket() {
    try {
        showSuperFeedback("Cargando lista del super...", false, true);
        showSuperSuggestedLoading();
        const [categories, items, suggestedItems, priceSourceState] = await Promise.all([
            supermarketApi.superCategories(),
            supermarketApi.superItems(),
            supermarketApi.superSuggestedList(),
            loadSuperPriceSourcesSafely({ silent: true, context: "initial-load" })
        ]);
        superItems = items;
        superPriceSources = Array.isArray(priceSourceState?.priceSources) ? priceSourceState.priceSources : [];
        renderSuperCategories(categories);
        renderSuperCategoryOptions(categories);
        renderSuperBarcodeItemOptions(items);
        renderSuperPriceObservationItemOptions(items);
        refreshSuperItemPriceSourceOptions();
        renderSuperItems(items);
        renderSuperSuggestedItems(suggestedItems);
        applySuperBarcodeHighlight(currentBarcodeAlias?.item?.id);
        syncSuperBarcodeResolvedActions();
        await loadSuperPriceObservations();
        await loadSuperMovementHistory();
        renderSuperTicketOcrReview();
        clearGeneratedSuperList();
        const loadedMessage = items.length ? "Lista del super cargada." : "Todavía no hay productos cargados.";
        showSuperFeedback(priceSourceState?.error ? `${loadedMessage} Fuentes de precio no disponibles por ahora.` : loadedMessage);
        return null;
    } catch (error) {
        showSuperFeedback(`No se pudo cargar la lista del super: ${error.message}`, true);
        return error;
    }
}

async function createSuperCategory() {
    const form = document.querySelector("#super-category-form");
    const button = form?.querySelector("button[type='submit']");
    const name = document.querySelector("#super-category-name")?.value.trim() || "";
    if (!name) {
        showSuperCategoryFeedback("El nombre de la categoría es obligatorio.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Creando...");
        await supermarketApi.createSuperCategory({ name, active: true });
        form.reset();
        editingCategoryId = null;
        showSuperCategoryFeedback("Categoría creada.");
        await loadSupermarket();
    } catch (error) {
        showSuperCategoryFeedback(`No se pudo crear la categoría: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function renderSuperCategories(categories) {
    const table = document.querySelector("#super-category-list");
    if (!table) {
        return;
    }
    superCategoryCount = categories.length;
    table.innerHTML = "";
    if (categories.length === 0) {
        const row = document.createElement("tr");
        row.innerHTML = '<td colspan="2" class="muted" data-label="Categorías">Todavía no hay categorías del super.</td>';
        table.append(row);
        updateSuperCategoryCollapseState(categories.length);
        return;
    }

    categories.forEach((category) => {
        const row = document.createElement("tr");
        row.className = "super-category-row";
        row.dataset.superCategoryId = String(category.id);
        row.innerHTML = category.id === editingCategoryId ? superCategoryEditRowHtml(category) : superCategoryDisplayRowHtml(category);
        table.append(row);
    });
    updateSuperCategoryCollapseState(categories.length);
}

function superCategoryDisplayRowHtml(category) {
    return `
        <td data-label="Categoría">${escapeHtml(category.name)}</td>
        <td data-label="Acciones">
            <div class="super-category-actions">
                <button type="button" class="secondary-button icon-button" data-super-category-action="edit" data-super-category-id="${category.id}" aria-label="Editar categoría ${escapeHtml(category.name)}" title="Editar">
                    <span aria-hidden="true">✎</span><span class="sr-only">Editar</span>
                </button>
                <button type="button" class="danger-button icon-button" data-super-category-action="delete" data-super-category-id="${category.id}" aria-label="Eliminar categoría ${escapeHtml(category.name)}" title="Eliminar">
                    <span aria-hidden="true">🗑</span><span class="sr-only">Eliminar</span>
                </button>
            </div>
        </td>
    `;
}

function superCategoryEditRowHtml(category) {
    return `
        <td data-label="Categoría">
            <label class="sr-only" for="super-category-edit-${category.id}">Editar categoría ${escapeHtml(category.name)}</label>
            <input id="super-category-edit-${category.id}" name="name" type="text" ${superFieldLimitAttribute("categoryName")} required value="${escapeHtml(category.name)}">
        </td>
        <td data-label="Acciones">
            <div class="super-category-actions">
                <button type="button" class="secondary-button icon-button" data-super-category-action="save" data-super-category-id="${category.id}" aria-label="Guardar categoría ${escapeHtml(category.name)}" title="Guardar">
                    <span aria-hidden="true">✓</span><span class="sr-only">Guardar</span>
                </button>
                <button type="button" class="secondary-button icon-button" data-super-category-action="cancel" data-super-category-id="${category.id}" aria-label="Cancelar edición de categoría ${escapeHtml(category.name)}" title="Cancelar">
                    <span aria-hidden="true">×</span><span class="sr-only">Cancelar</span>
                </button>
            </div>
        </td>
    `;
}

async function handleSuperCategoryAction(button) {
    const id = Number(button.dataset.superCategoryId || 0);
    if (!id) {
        return;
    }
    if (button.dataset.superCategoryAction === "edit") {
        editingCategoryId = id;
        await loadSupermarket();
        document.querySelector(`#super-category-edit-${id}`)?.focus?.();
        return;
    }
    if (button.dataset.superCategoryAction === "cancel") {
        editingCategoryId = null;
        await loadSupermarket();
        return;
    }
    if (button.dataset.superCategoryAction === "save") {
        await saveSuperCategory(id, button);
        return;
    }
    if (button.dataset.superCategoryAction === "delete") {
        await deleteSuperCategory(id, button);
    }
}

async function saveSuperCategory(id, button) {
    const row = button.closest("tr[data-super-category-id]");
    const name = row?.querySelector("input[name='name']")?.value.trim() || "";
    if (!name) {
        showSuperCategoryFeedback("El nombre de la categoría es obligatorio.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Guardando...");
        await supermarketApi.updateSuperCategory(id, { name, active: true });
        editingCategoryId = null;
        showSuperCategoryFeedback("Categoría actualizada.");
        await loadSupermarket();
    } catch (error) {
        showSuperCategoryFeedback(`No se pudo actualizar la categoría: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function deleteSuperCategory(id, button) {
    if (globalThis.confirm && !globalThis.confirm("¿Seguro que querés eliminar esta categoría de la lista del super?")) {
        return;
    }
    try {
        setButtonBusy(button, true, "Eliminando...");
        await supermarketApi.deleteSuperCategory(id);
        if (editingCategoryId === id) {
            editingCategoryId = null;
        }
        showSuperCategoryFeedback("Categoría eliminada.");
        await loadSupermarket();
    } catch (error) {
        showSuperCategoryFeedback(`No se pudo eliminar la categoría: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function toggleSuperCategoryTable() {
    superCategoryTableCollapsed = !superCategoryTableCollapsed;
    updateSuperCategoryCollapseState();
}

function updateSuperCategoryCollapseState(categoryCount) {
    const tableWrap = document.querySelector("#super-category-table-wrap");
    const toggle = document.querySelector("#super-category-toggle");
    const count = Number.isFinite(categoryCount) ? categoryCount : superCategoryCount;
    if (tableWrap) {
        tableWrap.hidden = superCategoryTableCollapsed;
    }
    if (toggle) {
        toggle.setAttribute("aria-expanded", String(!superCategoryTableCollapsed));
        const countSuffix = Number.isFinite(count) ? ` (${count})` : "";
        toggle.textContent = superCategoryTableCollapsed ? `Mostrar categorías${countSuffix}` : `Ocultar categorías${countSuffix}`;
    }
}

function renderSuperCategoryOptions(categories) {
    const select = document.querySelector("#super-item-category");
    if (!select) {
        return;
    }
    const currentValue = select.value;
    select.innerHTML = '<option value="">Seleccionar categoría</option>';
    categories.forEach((category) => {
        const option = document.createElement("option");
        option.value = String(category.id);
        option.textContent = category.name;
        select.append(option);
    });
    select.value = currentValue;
}

function renderSuperBarcodeItemOptions(items) {
    const select = document.querySelector("#super-barcode-item");
    if (!select) {
        return;
    }
    const currentValue = select.value;
    select.innerHTML = '<option value="">Seleccionar producto</option>';
    groupSuperItems(items).forEach((categoryItems, categoryName) => {
        categoryItems.forEach((item) => {
            const option = document.createElement("option");
            option.value = String(item.id);
            option.textContent = `${item.name} · ${categoryName}`;
            select.append(option);
        });
    });
    select.value = currentValue;
}

function renderSuperPriceObservationItemOptions(items) {
    const select = document.querySelector("#super-price-observation-item");
    if (!select) {
        return;
    }
    const currentValue = select.value;
    select.innerHTML = '<option value="">Seleccionar producto</option>';
    groupSuperItems(items).forEach((categoryItems, categoryName) => {
        categoryItems.forEach((item) => {
            const option = document.createElement("option");
            option.value = String(item.id);
            option.textContent = `${item.name} · ${categoryName}`;
            select.append(option);
        });
    });
    select.value = currentValue;
}

export function renderSuperItemPriceSourceOptions(selectedSourceId = "", selectedSourceLabel = "") {
    const select = document.querySelector("#super-item-presentation-price-source");
    if (!select) {
        return;
    }
    const currentValue = selectedSourceId ? String(selectedSourceId) : select.value;
    select.innerHTML = '<option value="">Sin fuente reutilizable</option>';
    let hasCurrentSource = false;
    (Array.isArray(superPriceSources) ? superPriceSources : []).forEach((source) => {
        const option = document.createElement("option");
        option.value = String(source.id);
        option.textContent = source.name;
        select.append(option);
        hasCurrentSource = hasCurrentSource || option.value === currentValue;
    });
    if (currentValue && !hasCurrentSource) {
        const option = document.createElement("option");
        option.value = currentValue;
        option.textContent = selectedSourceLabel || `Fuente #${currentValue} no cargada`;
        select.append(option);
    }
    select.value = currentValue;
}

export function refreshSuperItemPriceSourceOptions(selectedSourceId = "") {
    renderSuperPriceSources(selectedSourceId);
    renderSuperItemPriceSourceOptions(selectedSourceId);
    syncSuperItemPriceSourceInputs();
}

async function loadSuperPriceSources(selectedSourceId = "") {
    const result = await loadSuperPriceSourcesSafely({ context: "refresh" });
    superPriceSources = result.priceSources;
    refreshSuperItemPriceSourceOptions(selectedSourceId);
    return result.error;
}

async function loadSuperPriceSourcesSafely({ silent = false, context = "refresh" } = {}) {
    if (!supermarketApi.superPriceSources) {
        return { priceSources: [], error: null };
    }
    try {
        const priceSources = await supermarketApi.superPriceSources();
        return { priceSources: Array.isArray(priceSources) ? priceSources : [], error: null };
    } catch (error) {
        reportSuperPriceSourceIssue(`Price sources unavailable during ${context}`, error);
        if (!silent) {
            showSuperPriceSourceFeedback(`No se pudieron cargar las fuentes de precio: ${error.message}`, true);
        }
        return { priceSources: [], error };
    }
}

function renderSuperPriceSources(selectedSourceId = "") {
    const select = document.querySelector("#super-price-observation-price-source");
    if (!select) {
        return;
    }
    const currentValue = selectedSourceId ? String(selectedSourceId) : select.value;
    select.innerHTML = '<option value="">Sin fuente reutilizable</option>';
    (Array.isArray(superPriceSources) ? superPriceSources : []).forEach((source) => {
        const option = document.createElement("option");
        option.value = String(source.id);
        option.textContent = source.name;
        select.append(option);
    });
    select.value = currentValue;
}

function renderSuperItems(items) {
    const table = document.querySelector("#super-items-table");
    const empty = document.querySelector("#super-items-empty");
    const summary = document.querySelector("#super-items-summary");
    if (!table) {
        return;
    }
    table.innerHTML = "";
    for (const [categoryName, categoryItems] of groupSuperItems(items)) {
        const groupRow = document.createElement("tr");
        groupRow.className = "super-category-group-row";
        groupRow.innerHTML = `<th scope="rowgroup" colspan="10">${escapeHtml(categoryName)}</th>`;
        table.append(groupRow);
        categoryItems.forEach((item) => {
            const row = document.createElement("tr");
            row.dataset.superItemId = String(item.id);
            row.className = item.checked ? "super-item-checked" : "";
            row.innerHTML = superItemRowHtml(item);
            table.append(row);
        });
    }
    if (empty) {
        empty.hidden = items.length > 0;
    }
    if (summary) {
        const checkedCount = items.filter((item) => item.checked).length;
        summary.textContent = `${items.length} ${items.length === 1 ? "producto" : "productos"} cargados. ${checkedCount} ${checkedCount === 1 ? "marcado" : "marcados"} para comprar.`;
    }
}

function superItemRowHtml(item) {
    return `
        <td data-label="Estado">
            <input type="checkbox" data-super-action="checked" data-super-item-id="${item.id}" ${item.checked ? "checked" : ""} aria-label="Marcar ${escapeHtml(item.name)} para comprar">
        </td>
        <td data-label="Producto">${escapeHtml(item.name)}</td>
        <td data-label="Categoría">${escapeHtml(item.categoryName)}</td>
        <td data-label="Configuración">${superItemConfigurationBadgeHtml(item)}</td>
        <td data-label="Presentación">${escapeHtml(superItemCommercialPresentationLabel(item))}</td>
        <td data-label="Precio ref.">${superItemCommercialPresentationPriceHtml(item)}</td>
        <td data-label="Stock">${superItemStockHtml(item)}</td>
        <td data-label="Cantidad rápida">${escapeHtml(superItemQuickQuantityLabel(item))}</td>
        <td data-label="Notas">${item.notes ? escapeHtml(item.notes) : "—"}</td>
        <td data-label="Acciones">
            <div class="row-actions super-item-actions">
                <button type="button" class="secondary-button icon-button" data-super-action="edit" data-super-item-id="${item.id}" aria-label="Editar producto ${escapeHtml(item.name)}" title="Editar">
                    <span aria-hidden="true">✎</span><span class="sr-only">Editar</span>
                </button>
                <button type="button" class="secondary-button icon-button" data-super-action="purchase" data-super-item-id="${item.id}" aria-label="Registrar compra de ${escapeHtml(item.name)}" title="Compra">
                    <span aria-hidden="true">＋</span><span class="sr-only">Compra</span>
                </button>
                <button type="button" class="secondary-button icon-button" data-super-action="consume" data-super-item-id="${item.id}" aria-label="Registrar consumo de ${escapeHtml(item.name)}" title="Consumir">
                    <span aria-hidden="true">−</span><span class="sr-only">Consumir</span>
                </button>
                <button type="button" class="secondary-button icon-button" data-super-action="quick-consume" data-super-item-id="${item.id}" aria-label="Consumo rápido de ${escapeHtml(item.name)}" title="Rápido">
                    <span aria-hidden="true">↯</span><span class="sr-only">Rápido</span>
                </button>
                <button type="button" class="secondary-button icon-button" data-super-action="history" data-super-item-id="${item.id}" aria-label="Ver historial de ${escapeHtml(item.name)}" title="Historial">
                    <span aria-hidden="true">↺</span><span class="sr-only">Historial</span>
                </button>
                <button type="button" class="secondary-button icon-button" data-super-action="price-history" data-super-item-id="${item.id}" aria-label="Ver observaciones de precio de ${escapeHtml(item.name)}" title="Precios">
                    <span aria-hidden="true">$</span><span class="sr-only">Precios</span>
                </button>
                <button type="button" class="danger-button icon-button" data-super-action="delete" data-super-item-id="${item.id}" aria-label="Eliminar producto ${escapeHtml(item.name)}" title="Eliminar">
                    <span aria-hidden="true">🗑</span><span class="sr-only">Eliminar</span>
                </button>
            </div>
        </td>
    `;
}

function superItemConfigurationBadgeHtml(item) {
    const label = superItemConfigurationLabel(item);
    const stateClass = item.configured ? "configured" : "pending";
    const unit = item.unit ? escapeHtml(item.unit) : "sin unidad";
    const objective = item.habitualObjective ? escapeHtml(String(item.habitualObjective)) : "sin objetivo";
    return `<span class="super-configuration-badge ${stateClass}" title="${unit} · ${objective}">${label}</span>`;
}

function superItemStockHtml(item) {
    const unknown = item.currentStock === null || item.currentStock === undefined || item.currentStock === "";
    const stateClass = unknown ? " unknown" : "";
    return `<span class="super-stock-value${stateClass}">${escapeHtml(superItemStockLabel(item))}</span>`;
}

async function saveSuperItem() {
    const form = document.querySelector("#super-item-form");
    const button = form?.querySelector("button[type='submit']");
    const payload = superItemPayloadFromValues({
        name: document.querySelector("#super-item-name")?.value,
        categoryId: document.querySelector("#super-item-category")?.value,
        checked: editingItemId ? currentEditingItem()?.checked : false,
        unit: document.querySelector("#super-item-unit")?.value,
        commercialPresentationLabel: document.querySelector("#super-item-presentation-label")?.value,
        commercialPresentationQuantity: document.querySelector("#super-item-presentation-quantity")?.value,
        commercialPresentationPricePesos: document.querySelector("#super-item-presentation-price-pesos")?.value,
        commercialPresentationPriceSourceId: document.querySelector("#super-item-presentation-price-source")?.value,
        commercialPresentationPriceSourceLabel: document.querySelector("#super-item-presentation-price-source-label")?.value,
        commercialPresentationPriceObservedDate: document.querySelector("#super-item-presentation-price-observed-date")?.value,
        habitualObjective: document.querySelector("#super-item-objective")?.value,
        quickQuantity: document.querySelector("#super-item-quick-quantity")?.value,
        notes: document.querySelector("#super-item-notes")?.value
    });
    const currentStock = String(document.querySelector("#super-item-current-stock")?.value || "").trim();
    const validationMessage = validateSuperItemPayload(payload);
    if (validationMessage) {
        showSuperFeedback(validationMessage, true);
        return;
    }
    if (currentStock && (!Number.isFinite(Number(currentStock)) || Number(currentStock) < 0)) {
        showSuperFeedback("El stock actual no puede ser negativo.", true);
        return;
    }
    try {
        setButtonBusy(button, true, editingItemId ? "Guardando..." : "Creando...");
        let stockAdjustmentError = null;
        if (editingItemId) {
            await supermarketApi.updateSuperItem(editingItemId, payload);
            if (shouldAdjustSuperItemStock(currentStock)) {
                stockAdjustmentError = await adjustSuperItemStockSafely(editingItemId, currentStock);
            }
            if (!stockAdjustmentError) {
                showSuperFeedback("Producto actualizado.");
            }
        } else {
            const createdItem = await supermarketApi.createSuperItem(payload);
            if (currentStock !== "" && createdItem?.id) {
                stockAdjustmentError = await adjustSuperItemStockSafely(createdItem.id, currentStock);
            }
            if (!stockAdjustmentError) {
                showSuperFeedback("Producto creado.");
            }
        }
        resetSuperItemForm();
        const refreshError = await loadSupermarket();
        if (stockAdjustmentError) {
            showSuperFeedback(stockAdjustmentFailureMessage(stockAdjustmentError, refreshError), true);
        }
    } catch (error) {
        showSuperFeedback(`No se pudo guardar el producto: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function stockAdjustmentFailureMessage(stockAdjustmentError, refreshError) {
    let message = `Producto guardado, pero no se pudo ajustar el stock: ${stockAdjustmentError.message}`;
    if (refreshError) {
        message += `. Además, no se pudo refrescar la lista: ${refreshError.message}`;
    }
    return message;
}

async function adjustSuperItemStockSafely(itemId, currentStock) {
    try {
        await supermarketApi.adjustSuperItemStock(itemId, currentStock);
        return null;
    } catch (error) {
        return error;
    }
}

async function handleSuperItemAction(button) {
    const id = button.dataset.superItemId;
    if (button.dataset.superAction === "edit") {
        openSuperItemEdit(id);
        return;
    }
    if (button.dataset.superAction === "purchase") {
        openSuperMovementModal("purchase", id);
        return;
    }
    if (button.dataset.superAction === "consume") {
        openSuperMovementModal("consume", id);
        return;
    }
    if (button.dataset.superAction === "quick-consume") {
        await quickConsumeSuperItem(id, button);
        return;
    }
    if (button.dataset.superAction === "history") {
        await loadSuperMovementHistory(itemById(id));
        return;
    }
    if (button.dataset.superAction === "price-history") {
        await loadSuperPriceObservations(itemById(id));
        return;
    }
    if (button.dataset.superAction === "delete") {
        await deleteSuperItem(id, button);
    }
}

async function submitSuperBarcodeLookup(event) {
    event?.preventDefault?.();
    const button = document.querySelector("#super-barcode-form")?.querySelector("button[type='submit']");
    const payload = superBarcodePayloadFromValues({
        code: document.querySelector("#super-barcode-code")?.value,
        format: document.querySelector("#super-barcode-format")?.value
    });
    const validationMessage = validateSuperBarcodeLookup(payload);
    if (validationMessage) {
        showSuperBarcodeFeedback(validationMessage, true);
        return;
    }
    try {
        setButtonBusy(button, true, "Buscando...");
        const lookup = await supermarketApi.lookupSuperItemBarcodeAlias(payload.code);
        document.querySelector("#super-barcode-code").value = payload.code;
        if (lookup?.found && lookup.item) {
            currentBarcodeAlias = {
                aliasId: lookup.aliasId,
                code: lookup.code || payload.code,
                format: lookup.format,
                item: lookup.item
            };
            document.querySelector("#super-barcode-item").value = String(lookup.item.id);
            showSuperBarcodeResult(`Código ${currentBarcodeAlias.code} asociado a ${lookup.item.name}.`);
            showSuperBarcodeFeedback("Alias encontrado.");
            setSuperBarcodeAttachEnabled(false);
            setSuperBarcodeRemoveVisible(true);
            applySuperBarcodeHighlight(lookup.item.id);
            syncSuperBarcodeResolvedActions();
            return;
        }
        currentBarcodeAlias = { code: payload.code, format: payload.format || null, item: null, aliasId: null };
        showSuperBarcodeResult(`Código ${payload.code} no encontrado. Podés asociarlo a un producto existente.`);
        showSuperBarcodeFeedback("No se encontró un alias activo.");
        setSuperBarcodeAttachEnabled(true);
        setSuperBarcodeRemoveVisible(false);
        applySuperBarcodeHighlight(null);
        syncSuperBarcodeResolvedActions();
    } catch (error) {
        showSuperBarcodeFeedback(`No se pudo buscar el código: ${error.message}`, true);
    } finally {
        superBarcodeScannerState.lookupInFlight = false;
        setButtonBusy(button, false);
    }
}

async function attachSuperBarcodeAlias() {
    const button = document.querySelector("#super-barcode-attach");
    const payload = superBarcodePayloadFromValues({
        code: document.querySelector("#super-barcode-code")?.value || currentBarcodeAlias?.code,
        format: document.querySelector("#super-barcode-format")?.value || currentBarcodeAlias?.format
    });
    const validationMessage = validateSuperBarcodeLookup(payload);
    if (validationMessage) {
        showSuperBarcodeFeedback(validationMessage, true);
        return;
    }
    const itemId = document.querySelector("#super-barcode-item")?.value;
    const item = itemById(itemId);
    if (!item) {
        showSuperBarcodeFeedback("Seleccioná un producto existente para asociar el código.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Asociando...");
        const alias = await supermarketApi.attachSuperItemBarcodeAlias(item.id, payload);
        currentBarcodeAlias = { aliasId: alias?.id, code: alias?.code || payload.code, format: alias?.format || payload.format || null, item };
        showSuperBarcodeResult(`Código ${currentBarcodeAlias.code} asociado a ${item.name}.`);
        showSuperBarcodeFeedback("Alias asociado.");
        setSuperBarcodeAttachEnabled(false);
        setSuperBarcodeRemoveVisible(Boolean(currentBarcodeAlias.aliasId));
        applySuperBarcodeHighlight(item.id);
        syncSuperBarcodeResolvedActions();
    } catch (error) {
        showSuperBarcodeFeedback(`No se pudo asociar el código: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function removeSuperBarcodeAlias() {
    const button = document.querySelector("#super-barcode-remove");
    if (!currentBarcodeAlias?.aliasId || !currentBarcodeAlias.item?.id) {
        showSuperBarcodeFeedback("No hay un alias seleccionado para quitar.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Quitando...");
        await supermarketApi.removeSuperItemBarcodeAlias(currentBarcodeAlias.item.id, currentBarcodeAlias.aliasId);
        showSuperBarcodeResult(`Alias ${currentBarcodeAlias.code} quitado de ${currentBarcodeAlias.item.name}.`);
        showSuperBarcodeFeedback("Alias quitado.");
        currentBarcodeAlias = { code: currentBarcodeAlias.code, format: currentBarcodeAlias.format || null, item: null, aliasId: null };
        setSuperBarcodeAttachEnabled(true);
        setSuperBarcodeRemoveVisible(false);
        applySuperBarcodeHighlight(null);
        syncSuperBarcodeResolvedActions();
    } catch (error) {
        showSuperBarcodeFeedback(`No se pudo quitar el alias: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function applySuperBarcodeHighlight(itemId) {
    const table = document.querySelector("#super-items-table");
    Array.from(table?.children || []).forEach((row) => {
        if (!row.dataset?.superItemId) {
            return;
        }
        const matches = itemId && String(row.dataset.superItemId) === String(itemId);
        row.classList.toggle("super-item-barcode-match", Boolean(matches));
        if (matches) {
            row.scrollIntoView?.({ block: "center", behavior: "smooth" });
        }
    });
}

function setSuperBarcodeAttachEnabled(enabled) {
    const button = document.querySelector("#super-barcode-attach");
    if (button) {
        button.disabled = !enabled;
    }
}

function setSuperBarcodeRemoveVisible(visible) {
    const button = document.querySelector("#super-barcode-remove");
    if (button) {
        button.hidden = !visible;
    }
}

function showSuperBarcodeResult(message) {
    const result = document.querySelector("#super-barcode-result");
    if (result) {
        result.textContent = message;
    }
}

function superBarcodeScannerElements() {
    return { codeInput: document.querySelector("#super-barcode-code"), formatInput: document.querySelector("#super-barcode-format"), startButton: document.querySelector("#super-barcode-scan-start"), stopButton: document.querySelector("#super-barcode-scan-stop"), scannerPanel: document.querySelector("#super-barcode-scanner"), preview: document.querySelector("#super-barcode-scanner-preview"), status: document.querySelector("#super-barcode-scanner-status") };
}

function syncSuperBarcodeScannerUi() {
    const availability = getSuperBarcodeScannerAvailability();
    const { startButton, stopButton, scannerPanel, preview, status } = superBarcodeScannerElements();
    const isActive = ["starting", "scanning", "resolving"].includes(superBarcodeScannerState.phase);
    if (startButton) startButton.disabled = ["starting", "scanning", "resolving"].includes(superBarcodeScannerState.phase);
    if (stopButton) stopButton.disabled = !["starting", "scanning"].includes(superBarcodeScannerState.phase);
    if (scannerPanel) scannerPanel.hidden = !isActive;
    if (preview) preview.hidden = !superBarcodeScannerState.stream;
    if (status) {
        if (superBarcodeScannerState.statusMessage) {
            status.textContent = superBarcodeScannerState.statusMessage;
        } else if (!availability.supported) {
            status.textContent = "Escaneo no disponible en este navegador o contexto. Ingresá el código manualmente.";
        } else {
            status.textContent = "Escáner listo. Podés iniciar la cámara o seguir con el ingreso manual.";
        }
    }
}

function syncSuperBarcodeResolvedActions() {
    const actions = document.querySelector("#super-barcode-actions");
    const purchaseButton = document.querySelector("#super-barcode-purchase");
    const consumeButton = document.querySelector("#super-barcode-consume");
    const hasResolvedItem = Boolean(currentBarcodeAlias?.item?.id);
    if (actions) actions.hidden = !hasResolvedItem;
    if (purchaseButton) purchaseButton.disabled = !hasResolvedItem;
    if (consumeButton) consumeButton.disabled = !hasResolvedItem;
}

async function startSuperBarcodeScanner() {
    const availability = getSuperBarcodeScannerAvailability();
    if (!availability.supported) {
        setSuperBarcodeScannerPhase("unavailable", "Escaneo no disponible en este navegador o contexto. Ingresá el código manualmente.");
        focusSuperBarcodeCodeField();
        return;
    }
    try {
        setSuperBarcodeScannerPhase("starting", "Iniciando cámara...");
        const stream = await globalThis.navigator.mediaDevices.getUserMedia({
            audio: false,
            video: { facingMode: { ideal: "environment" } }
        });
        superBarcodeScannerState.stream = stream;
        superBarcodeScannerState.detector = new globalThis.BarcodeDetector();
        const { preview } = superBarcodeScannerElements();
        if (preview) {
            preview.srcObject = stream;
            preview.hidden = false;
            await preview.play?.();
        }
        setSuperBarcodeScannerPhase("scanning", "Escaneando... Apuntá al código de barras.");
        scheduleSuperBarcodeDetection();
    } catch (error) {
        const denied = error?.name === "NotAllowedError" || error?.name === "PermissionDeniedError";
        stopSuperBarcodeScanner({
            nextPhase: denied ? "denied" : "error",
            statusMessage: denied
                ? "Permiso de cámara denegado. Ingresá el código manualmente."
                : `No se pudo iniciar la cámara: ${error.message}`
        });
        focusSuperBarcodeCodeField();
    }
}

function scheduleSuperBarcodeDetection() {
    if (superBarcodeScannerState.phase !== "scanning") {
        return;
    }
    if (typeof globalThis.requestAnimationFrame === "function") {
        superBarcodeScannerState.animationFrameId = globalThis.requestAnimationFrame(() => {
            void pollSuperBarcodeDetector();
        });
        return;
    }
    void pollSuperBarcodeDetector();
}

async function pollSuperBarcodeDetector() {
    if (superBarcodeScannerState.phase !== "scanning" || !superBarcodeScannerState.detector) {
        return;
    }
    try {
        const { preview } = superBarcodeScannerElements();
        const detections = await superBarcodeScannerState.detector.detect(preview);
        const acceptedDetection = Array.isArray(detections) ? detections.find((detection) => shouldAcceptSuperBarcodeScan({ nextCode: detection?.rawValue, lastCode: superBarcodeScannerState.lastCode, lastAcceptedAt: superBarcodeScannerState.lastAcceptedAt, lookupInFlight: superBarcodeScannerState.lookupInFlight })) : null;
        if (acceptedDetection) {
            await acceptSuperBarcodeDetection(acceptedDetection);
            return;
        }
    } catch (error) {
        stopSuperBarcodeScanner({ nextPhase: "error", statusMessage: `No se pudo continuar el escaneo: ${error.message}` });
        focusSuperBarcodeCodeField();
        return;
    }
    scheduleSuperBarcodeDetection();
}

async function acceptSuperBarcodeDetection(detection) {
    const payload = superBarcodePayloadFromValues({ code: detection?.rawValue, format: detection?.format });
    const validationMessage = validateSuperBarcodeLookup(payload);
    if (validationMessage) {
        showSuperBarcodeFeedback(validationMessage, true);
        scheduleSuperBarcodeDetection();
        return;
    }
    const { codeInput, formatInput } = superBarcodeScannerElements();
    if (codeInput) codeInput.value = payload.code;
    if (formatInput) formatInput.value = payload.format || "";
    superBarcodeScannerState.lookupInFlight = true;
    superBarcodeScannerState.lastCode = payload.code;
    superBarcodeScannerState.lastAcceptedAt = Date.now();
    stopSuperBarcodeScanner({ nextPhase: "resolving", statusMessage: `Código ${payload.code} detectado. Resolviendo producto...` });
    await submitSuperBarcodeLookup();
    const resolvedMessage = currentBarcodeAlias?.item ? `Escaneo listo para ${currentBarcodeAlias.item.name}. Confirmá compra o consumo por separado.` : `Código ${payload.code} detectado. Podés asociarlo manualmente.`;
    setSuperBarcodeScannerPhase("idle", resolvedMessage);
}

function stopSuperBarcodeScanner({ nextPhase = "idle", statusMessage = "", announce = true } = {}) {
    if (superBarcodeScannerState.animationFrameId !== null && typeof globalThis.cancelAnimationFrame === "function") globalThis.cancelAnimationFrame(superBarcodeScannerState.animationFrameId);
    superBarcodeScannerState.animationFrameId = null;
    for (const track of superBarcodeScannerState.stream?.getTracks?.() || []) track.stop?.();
    superBarcodeScannerState.stream = null;
    superBarcodeScannerState.detector = null;
    const { preview } = superBarcodeScannerElements();
    if (preview) {
        preview.srcObject = null;
        preview.hidden = true;
    }
    setSuperBarcodeScannerPhase(nextPhase, statusMessage, announce);
}

function setSuperBarcodeScannerPhase(phase, statusMessage = "", announce = true) {
    superBarcodeScannerState.phase = phase;
    superBarcodeScannerState.statusMessage = announce ? statusMessage : "";
    syncSuperBarcodeScannerUi();
}

function focusSuperBarcodeCodeField() { document.querySelector("#super-barcode-code")?.focus?.(); }

function handleSuperBarcodeVisibilityLoss() {
    if (document.hidden && ["starting", "scanning"].includes(superBarcodeScannerState.phase)) stopSuperBarcodeScanner({ nextPhase: "idle", statusMessage: "Escaneo detenido al ocultarse la página. Podés retomarlo cuando quieras." });
}

function handleSuperBarcodePageHide() {
    if (["starting", "scanning"].includes(superBarcodeScannerState.phase)) stopSuperBarcodeScanner({ nextPhase: "idle", statusMessage: "Escaneo detenido al salir de la página.", announce: false });
}

function handleSuperBarcodeResolvedAction(event) {
    const type = event?.currentTarget?.dataset?.superBarcodeStockAction;
    if (!currentBarcodeAlias?.item?.id || !["purchase", "consume"].includes(type)) {
        showSuperBarcodeFeedback("Primero resolvé un producto antes de registrar movimientos.", true);
        return;
    }
    openSuperMovementModal(type, currentBarcodeAlias.item.id);
}

function shouldAdjustSuperItemStock(currentStock) {
    if (currentStock === "") {
        return false;
    }
    if (!editingItemId) {
        return true;
    }
    return !stockFieldMatchesOriginal(currentStock, editingItemOriginalStock);
}

function stockFieldMatchesOriginal(currentStock, originalStock) {
    const original = String(originalStock ?? "").trim();
    if (original === "") {
        return currentStock === "";
    }
    const currentNumber = Number(currentStock);
    const originalNumber = Number(original);
    if (Number.isFinite(currentNumber) && Number.isFinite(originalNumber)) {
        return currentNumber === originalNumber;
    }
    return currentStock === original;
}

function openSuperMovementModal(type, id) {
    const item = itemById(id);
    if (!item) {
        showSuperFeedback("No se encontró el producto seleccionado.", true);
        return;
    }
    document.querySelector("#super-movement-title").textContent = type === "purchase" ? "Registrar compra" : "Registrar consumo";
    document.querySelector("#super-movement-item-id").value = String(item.id);
    document.querySelector("#super-movement-type").value = type;
    document.querySelector("#super-movement-item-name").textContent = item.name;
    document.querySelector("#super-movement-quantity").value = "";
    document.querySelector("#super-movement-notes").value = "";
    const negativeField = document.querySelector(".super-movement-negative-field");
    const negativeInput = document.querySelector("#super-movement-allow-negative");
    negativeInput.checked = false;
    negativeField.hidden = type !== "consume";
    showSuperMovementConflict("", false);
    showSuperMovementFeedback("");
    document.querySelector("#super-movement-modal").hidden = false;
    document.querySelector("#super-movement-quantity")?.focus?.();
}

function closeSuperMovementModal() {
    const modal = document.querySelector("#super-movement-modal");
    if (modal) {
        modal.hidden = true;
    }
    document.querySelector("#super-movement-form")?.reset?.();
    showSuperMovementConflict("", false);
    showSuperMovementFeedback("");
}

async function submitSuperMovementForm(event) {
    event?.preventDefault?.();
    const id = Number(document.querySelector("#super-movement-item-id")?.value || 0);
    const type = document.querySelector("#super-movement-type")?.value;
    const quantity = String(document.querySelector("#super-movement-quantity")?.value || "").trim();
    const notes = String(document.querySelector("#super-movement-notes")?.value || "").trim();
    const allowNegativeStock = Boolean(document.querySelector("#super-movement-allow-negative")?.checked);
    if (!id || !["purchase", "consume"].includes(type)) {
        showSuperMovementFeedback("Seleccione un movimiento válido.", true);
        return;
    }
    if (!Number.isFinite(Number(quantity)) || Number(quantity) <= 0) {
        showSuperMovementFeedback("La cantidad debe ser mayor que cero.", true);
        return;
    }
    const payload = type === "consume" ? { quantity, notes, allowNegativeStock } : { quantity, notes };
    await runSuperMovementCommand(id, type, payload);
}

async function submitSuperPriceObservationForm(event) {
    event?.preventDefault?.();
    const itemId = Number(document.querySelector("#super-price-observation-item")?.value || 0);
    const button = document.querySelector("#super-price-observation-form")?.querySelector("button[type='submit']");
    if (!itemId) {
        showSuperPriceObservationFeedback("Seleccioná un producto para registrar la observación.", true);
        return;
    }
    const payload = superPriceObservationPayloadFromValues({
        pricePesos: document.querySelector("#super-price-observation-price-pesos")?.value,
        priceSourceId: document.querySelector("#super-price-observation-price-source")?.value,
        sourceLabel: document.querySelector("#super-price-observation-source-label")?.value,
        observedDate: document.querySelector("#super-price-observation-observed-date")?.value,
        syncCurrentReferencePrice: document.querySelector("#super-price-observation-sync-current-reference-price")?.checked
    });
    const validationMessage = validateSuperPriceObservationPayload(payload);
    if (validationMessage) {
        showSuperPriceObservationFeedback(validationMessage, true);
        return;
    }
    try {
        setButtonBusy(button, true, "Registrando...");
        await supermarketApi.createSuperItemPriceObservation(itemId, payload);
        document.querySelector("#super-price-observation-form")?.reset?.();
        if (payload.syncCurrentReferencePrice) {
            await loadSupermarket();
            showSuperPriceObservationFeedback("Observación registrada y precio actual/de referencia actualizado.");
        } else {
            await loadSuperPriceObservations();
            showSuperPriceObservationFeedback("Observación de precio registrada.");
        }
    } catch (error) {
        showSuperPriceObservationFeedback(`No se pudo registrar la observación: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function submitSuperPriceSourceForm(event) {
    event?.preventDefault?.();
    const form = document.querySelector("#super-price-source-form");
    const button = form?.querySelector("button[type='submit']");
    const payload = superPriceSourcePayloadFromValues({
        name: document.querySelector("#super-price-source-name")?.value
    });
    const validationMessage = validateSuperPriceSourcePayload(payload);
    if (validationMessage) {
        showSuperPriceSourceFeedback(validationMessage, true);
        return;
    }
    try {
        setButtonBusy(button, true, "Creando...");
        const createdSource = await supermarketApi.createSuperPriceSource(payload);
        form?.reset?.();
        const refreshError = await loadSuperPriceSources(createdSource?.id);
        syncSuperItemPriceSourceInputs();
        await loadSuperPriceObservations();
        showSuperPriceSourceFeedback(refreshError
            ? "Fuente de precio creada. La lista de fuentes se actualizará cuando vuelva a estar disponible."
            : "Fuente de precio creada.");
    } catch (error) {
        showSuperPriceSourceFeedback(`No se pudo crear la fuente de precio: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function reportSuperPriceSourceIssue(message, error) {
    globalThis.console?.warn?.(`[super-price-sources] ${message}`, error);
}

async function loadSuperPriceObservations(item = selectedPriceObservationItem) {
    if (!supermarketApi.superPriceObservations) {
        return;
    }
    selectedPriceObservationItem = item?.id ? { id: item.id, name: item.name || "Producto" } : null;
    const filters = selectedPriceObservationItem?.id
        ? { itemId: String(selectedPriceObservationItem.id), limit: SUPER_RECENT_HISTORY_LIMIT }
        : { limit: SUPER_RECENT_HISTORY_LIMIT };
    try {
        const observations = await supermarketApi.superPriceObservations(filters);
        renderSuperPriceObservations(observations, selectedPriceObservationItem);
    } catch (error) {
        renderSuperPriceObservationContext(selectedPriceObservationItem);
        const table = document.querySelector("#super-price-observation-table");
        if (table) {
            table.innerHTML = "";
        }
        const empty = document.querySelector("#super-price-observation-empty");
        if (empty) {
            empty.hidden = false;
            empty.textContent = selectedPriceObservationItem?.name
                ? `No se pudieron cargar las observaciones de ${selectedPriceObservationItem.name}: ${error.message}`
                : `No se pudieron cargar las observaciones: ${error.message}`;
        }
    }
}

function renderSuperPriceObservations(observations, item = selectedPriceObservationItem) {
    renderSuperPriceObservationContext(item);
    const table = document.querySelector("#super-price-observation-table");
    const empty = document.querySelector("#super-price-observation-empty");
    if (!table) {
        return;
    }
    table.innerHTML = "";
    (Array.isArray(observations) ? observations : []).forEach((observation) => {
        const row = document.createElement("tr");
        row.innerHTML = superPriceObservationRowHtml(observation);
        table.append(row);
    });
    if (empty) {
        empty.hidden = Array.isArray(observations) && observations.length > 0;
        empty.textContent = item?.name
            ? `Todavía no hay observaciones de precio recientes para ${item.name}.`
            : "Todavía no hay observaciones de precio recientes.";
    }
}

function renderSuperPriceObservationContext(item = selectedPriceObservationItem) {
    const title = document.querySelector("#super-price-observation-title");
    const summary = document.querySelector("#super-price-observation-context-summary");
    const reset = document.querySelector("#super-price-observation-global-reset");
    if (title) {
        title.textContent = item?.name ? `Observaciones de precio · ${item.name}` : "Observaciones de precio";
    }
    if (summary) {
        summary.textContent = item?.name ? `Historial filtrado para ${item.name}.` : "Historial reciente global de observaciones de precio.";
    }
    if (reset) {
        reset.hidden = !item?.id;
    }
}

async function resetSuperPriceObservationContext() {
    selectedPriceObservationItem = null;
    await loadSuperPriceObservations(null);
}

function prefillSuperPriceObservationForm(item) {
    document.querySelector("#super-price-observation-price-pesos").value = item?.commercialPresentationPricePesos || "";
    document.querySelector("#super-price-observation-price-source").value = selectedPriceSourceId(item?.commercialPresentationPriceSourceId);
    document.querySelector("#super-price-observation-source-label").value = item?.commercialPresentationPriceSourceId ? "" : item?.commercialPresentationPriceSourceLabel || "";
    document.querySelector("#super-price-observation-observed-date").value = item?.commercialPresentationPriceObservedDate || "";
}

async function submitSuperTicketOcrUploadForm(event) {
    event?.preventDefault?.();
    const form = document.querySelector("#super-ticket-ocr-form");
    const button = form?.querySelector("button[type='submit']");
    const file = document.querySelector("#super-ticket-ocr-file")?.files?.[0];
    if (!file) {
        showSuperTicketOcrFeedback("Seleccioná una imagen PNG o JPEG para extraer candidatos.", true);
        return;
    }
    try {
        setButtonBusy(button, true, "Extrayendo...");
        const response = await supermarketApi.uploadSuperTicketOcrCandidates(file);
        currentTicketOcrReview = buildSuperTicketOcrReviewState(response);
        renderSuperTicketOcrReview();
        showSuperTicketOcrFeedback("Candidatos OCR cargados. Revisá y confirmá solo las filas válidas.");
        showSuperTicketOcrConfirmFeedback("");
    } catch (error) {
        clearSuperTicketOcrReview();
        showSuperTicketOcrFeedback(`No se pudo extraer el ticket: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function submitSuperTicketOcrConfirmForm(event) {
    event?.preventDefault?.();
    const lineIndex = Number(document.querySelector("#super-ticket-ocr-line-index")?.value || -1);
    const line = currentTicketOcrReview?.lines?.[lineIndex];
    const itemId = Number(document.querySelector("#super-ticket-ocr-product")?.value || 0);
    const button = document.querySelector("#super-ticket-ocr-confirm-form")?.querySelector("button[type='submit']");
    if (!line || lineIndex < 0) {
        showSuperTicketOcrConfirmFeedback("Seleccioná una línea OCR para confirmar.", true);
        return;
    }
    if (!itemId) {
        showSuperTicketOcrConfirmFeedback("Seleccioná un producto existente para confirmar la observación.", true);
        return;
    }
    const payload = superPriceObservationPayloadFromValues({
        pricePesos: document.querySelector("#super-ticket-ocr-price-pesos")?.value,
        sourceLabel: document.querySelector("#super-ticket-ocr-source-label")?.value,
        observedDate: document.querySelector("#super-ticket-ocr-date")?.value,
        syncCurrentReferencePrice: document.querySelector("#super-ticket-ocr-sync-current-reference-price")?.checked
    });
    const validationMessage = validateSuperPriceObservationPayload(payload);
    if (validationMessage) {
        showSuperTicketOcrConfirmFeedback(validationMessage, true);
        return;
    }
    try {
        setButtonBusy(button, true, "Confirmando...");
        await supermarketApi.createSuperItemPriceObservation(itemId, payload);
        line.descriptionCandidate = String(document.querySelector("#super-ticket-ocr-description")?.value || "").trim() || line.descriptionCandidate;
        line.pricePesos = payload.pricePesos;
        line.selectedProductId = String(itemId);
        line.status = "confirmed";
        line.statusMessage = payload.syncCurrentReferencePrice
            ? "Observación creada y precio sincronizado."
            : "Observación creada.";
        currentTicketOcrReview.selectedDate = payload.observedDate || "";
        currentTicketOcrReview.selectedSource = payload.sourceLabel || "";
        if (payload.syncCurrentReferencePrice) {
            await loadSupermarket();
        } else {
            await loadSuperPriceObservations();
        }
        selectSuperTicketOcrLine(nextPendingSuperTicketOcrLineIndex(lineIndex));
        renderSuperTicketOcrReview();
        showSuperTicketOcrConfirmFeedback(line.statusMessage);
    } catch (error) {
        showSuperTicketOcrConfirmFeedback(`No se pudo confirmar la observación: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function handleSuperTicketOcrLineAction(button) {
    const index = Number(button.dataset.superTicketOcrLineIndex || -1);
    if (button.dataset.superTicketOcrAction === "select") {
        selectSuperTicketOcrLine(index);
        renderSuperTicketOcrReview();
    }
}

function buildSuperTicketOcrReviewState(response) {
    const dateCandidates = Array.isArray(response?.dateCandidates) ? response.dateCandidates : [];
    const sourceCandidates = Array.isArray(response?.sourceCandidates) ? response.sourceCandidates : [];
    const firstDate = String(dateCandidates[0]?.value || "");
    const firstSource = String(sourceCandidates[0]?.label || "").trim();
    return {
        checksumSha256: String(response?.checksumSha256 || ""),
        originalFilename: String(response?.originalFilename || "ticket-image"),
        contentType: String(response?.contentType || "image/png"),
        sizeBytes: Number(response?.sizeBytes || 0),
        ocrConfidence: response?.ocrConfidence ?? null,
        warnings: Array.isArray(response?.warnings) ? response.warnings : [],
        dateCandidates,
        sourceCandidates,
        selectedDate: firstDate,
        selectedSource: firstSource,
        selectedLineIndex: Array.isArray(response?.lineCandidates) && response.lineCandidates.length > 0 ? 0 : -1,
        lines: (Array.isArray(response?.lineCandidates) ? response.lineCandidates : []).map((lineCandidate, index) => ({
            index,
            rawText: String(lineCandidate?.rawText || ""),
            descriptionCandidate: String(lineCandidate?.descriptionCandidate || ""),
            pricePesos: lineCandidate?.pricePesos === null || lineCandidate?.pricePesos === undefined ? "" : String(lineCandidate.pricePesos),
            confidence: lineCandidate?.confidence === null || lineCandidate?.confidence === undefined ? "" : String(lineCandidate.confidence),
            warnings: Array.isArray(lineCandidate?.warnings) ? lineCandidate.warnings : [],
            selectedProductId: lineCandidate?.productCandidateId ? String(lineCandidate.productCandidateId) : "",
            status: "pending",
            statusMessage: "Pendiente de confirmación."
        }))
    };
}

function renderSuperTicketOcrReview() {
    const summary = document.querySelector("#super-ticket-ocr-summary");
    const meta = document.querySelector("#super-ticket-ocr-meta");
    const reviewPanel = document.querySelector("#super-ticket-ocr-review-panel");
    const table = document.querySelector("#super-ticket-ocr-table");
    const empty = document.querySelector("#super-ticket-ocr-empty");
    const warnings = document.querySelector("#super-ticket-ocr-warning-list");
    if (!currentTicketOcrReview) {
        if (summary) {
            summary.textContent = "No hay candidatos OCR cargados.";
        }
        if (meta) {
            meta.innerHTML = "";
        }
        if (warnings) {
            warnings.innerHTML = "";
        }
        if (table) {
            table.innerHTML = "";
        }
        if (empty) {
            empty.hidden = false;
            empty.textContent = "Subí un ticket para revisar candidatos transitorios.";
        }
        if (reviewPanel) {
            reviewPanel.hidden = true;
        }
        resetSuperTicketOcrConfirmForm();
        return;
    }
    renderSuperTicketOcrCandidateSelects();
    renderSuperTicketOcrProductOptions();
    const lineCount = currentTicketOcrReview.lines.length;
    if (summary) {
        summary.textContent = `${currentTicketOcrReview.originalFilename} · ${lineCount} ${lineCount === 1 ? "línea candidata" : "líneas candidatas"} para revisar.`;
    }
    if (meta) {
        meta.innerHTML = [
            `<small>SHA-256: ${escapeHtml(currentTicketOcrReview.checksumSha256 || "—")}</small>`,
            `<small>Tipo: ${escapeHtml(currentTicketOcrReview.contentType || "—")} · Tamaño: ${escapeHtml(String(currentTicketOcrReview.sizeBytes || 0))} bytes</small>`,
            `<small>Confianza OCR: ${escapeHtml(currentTicketOcrReview.ocrConfidence === null || currentTicketOcrReview.ocrConfidence === undefined ? "—" : String(currentTicketOcrReview.ocrConfidence))}</small>`
        ].join("");
    }
    if (warnings) {
        warnings.innerHTML = (currentTicketOcrReview.warnings.length > 0 ? currentTicketOcrReview.warnings : ["Sin advertencias generales."])
            .map((warning) => `<li>${escapeHtml(warning)}</li>`)
            .join("");
    }
    if (table) {
        table.innerHTML = "";
        currentTicketOcrReview.lines.forEach((line) => {
            const row = document.createElement("tr");
            row.innerHTML = superTicketOcrLineRowHtml(line);
            table.append(row);
        });
    }
    if (empty) {
        empty.hidden = lineCount > 0;
        empty.textContent = "No se detectaron líneas OCR revisables.";
    }
    if (reviewPanel) {
        reviewPanel.hidden = false;
    }
    if (currentTicketOcrReview.selectedLineIndex >= 0) {
        populateSuperTicketOcrConfirmForm(currentTicketOcrReview.lines[currentTicketOcrReview.selectedLineIndex]);
    }
}

function renderSuperTicketOcrCandidateSelects() {
    fillSelectWithOptions(
        document.querySelector("#super-ticket-ocr-date-candidate"),
        currentTicketOcrReview?.dateCandidates?.map((candidate) => ({
            value: String(candidate?.value || ""),
            label: String(candidate?.value || "Sin fecha")
        })) || [],
        "Sin fecha candidata",
        currentTicketOcrReview?.selectedDate || ""
    );
    fillSelectWithOptions(
        document.querySelector("#super-ticket-ocr-source-candidate"),
        currentTicketOcrReview?.sourceCandidates?.map((candidate) => ({
            value: String(candidate?.label || ""),
            label: String(candidate?.label || "Sin fuente")
        })) || [],
        "Sin fuente candidata",
        currentTicketOcrReview?.selectedSource || ""
    );
}

function renderSuperTicketOcrProductOptions() {
    const select = document.querySelector("#super-ticket-ocr-product");
    if (!select) {
        return;
    }
    const currentValue = select.value;
    const options = [];
    groupSuperItems(superItems).forEach((categoryItems, categoryName) => {
        categoryItems.forEach((item) => {
            options.push({ value: String(item.id), label: `${item.name} · ${categoryName}` });
        });
    });
    fillSelectWithOptions(select, options, "Seleccionar producto", currentValue || currentTicketOcrReview?.lines?.[currentTicketOcrReview?.selectedLineIndex]?.selectedProductId || "");
}

function fillSelectWithOptions(select, options, placeholder, selectedValue = "") {
    if (!select) {
        return;
    }
    select.innerHTML = "";
    const placeholderOption = document.createElement("option");
    placeholderOption.value = "";
    placeholderOption.textContent = placeholder;
    select.append(placeholderOption);
    options.forEach((optionValue) => {
        const option = document.createElement("option");
        option.value = optionValue.value;
        option.textContent = optionValue.label;
        select.append(option);
    });
    select.value = selectedValue || "";
}

function superTicketOcrLineRowHtml(line) {
    const warnings = line.warnings.length > 0 ? line.warnings.join(" · ") : "Sin advertencias";
    const statusClass = line.status === "confirmed" ? " confirmed" : "";
    return `
        <tr>
            <td data-label="Línea">${escapeHtml(line.rawText || "—")}</td>
            <td data-label="Descripción">${escapeHtml(line.descriptionCandidate || "—")}</td>
            <td data-label="Precio">${escapeHtml(line.pricePesos ? formatPesos(line.pricePesos) : "—")}</td>
            <td data-label="Advertencias">${escapeHtml(warnings)}</td>
            <td data-label="Estado"><span class="super-ticket-ocr-status${statusClass}">${escapeHtml(line.statusMessage)}</span></td>
            <td data-label="Acción"><button type="button" class="secondary-button" data-super-ticket-ocr-action="select" data-super-ticket-ocr-line-index="${line.index}">Revisar</button></td>
        </tr>
    `;
}

function populateSuperTicketOcrConfirmForm(line) {
    if (!line) {
        resetSuperTicketOcrConfirmForm();
        return;
    }
    document.querySelector("#super-ticket-ocr-line-index").value = String(line.index);
    document.querySelector("#super-ticket-ocr-selected-line").value = line.rawText || "";
    document.querySelector("#super-ticket-ocr-description").value = line.descriptionCandidate || "";
    document.querySelector("#super-ticket-ocr-price-pesos").value = line.pricePesos || "";
    document.querySelector("#super-ticket-ocr-product").value = line.selectedProductId || "";
    document.querySelector("#super-ticket-ocr-date").value = currentTicketOcrReview?.selectedDate || "";
    document.querySelector("#super-ticket-ocr-date-candidate").value = currentTicketOcrReview?.selectedDate || "";
    document.querySelector("#super-ticket-ocr-source-label").value = currentTicketOcrReview?.selectedSource || "";
    document.querySelector("#super-ticket-ocr-source-candidate").value = currentTicketOcrReview?.selectedSource || "";
    document.querySelector("#super-ticket-ocr-sync-current-reference-price").checked = false;
    const selectedWarnings = document.querySelector("#super-ticket-ocr-selected-warnings");
    if (selectedWarnings) {
        selectedWarnings.innerHTML = (line.warnings.length > 0 ? line.warnings : ["Sin advertencias para la línea seleccionada."])
            .map((warning) => `<li>${escapeHtml(warning)}</li>`)
            .join("");
    }
}

function resetSuperTicketOcrConfirmForm() {
    document.querySelector("#super-ticket-ocr-confirm-form")?.reset?.();
    const selectedWarnings = document.querySelector("#super-ticket-ocr-selected-warnings");
    if (selectedWarnings) {
        selectedWarnings.innerHTML = "";
    }
    showSuperTicketOcrConfirmFeedback("");
}

function syncSuperTicketOcrDateCandidate(event) {
    const value = String(event?.currentTarget?.value || "");
    if (!currentTicketOcrReview) {
        return;
    }
    currentTicketOcrReview.selectedDate = value;
    document.querySelector("#super-ticket-ocr-date").value = value;
}

function syncSuperTicketOcrSourceCandidate(event) {
    const value = String(event?.currentTarget?.value || "").trim();
    if (!currentTicketOcrReview) {
        return;
    }
    currentTicketOcrReview.selectedSource = value;
    document.querySelector("#super-ticket-ocr-source-label").value = value;
}

function selectSuperTicketOcrLine(index) {
    if (!currentTicketOcrReview || !Number.isInteger(index) || index < 0 || index >= currentTicketOcrReview.lines.length) {
        return;
    }
    currentTicketOcrReview.selectedLineIndex = index;
    populateSuperTicketOcrConfirmForm(currentTicketOcrReview.lines[index]);
}

function nextPendingSuperTicketOcrLineIndex(currentIndex) {
    if (!currentTicketOcrReview) {
        return -1;
    }
    const pending = currentTicketOcrReview.lines.find((line) => line.status !== "confirmed");
    if (pending) {
        return pending.index;
    }
    return currentIndex;
}

function clearSuperTicketOcrReview(message = "") {
    currentTicketOcrReview = null;
    renderSuperTicketOcrReview();
    if (message) {
        showSuperTicketOcrFeedback(message);
    }
}

async function runSuperMovementCommand(id, type, payload) {
    const button = document.querySelector("#super-movement-submit");
    try {
        setButtonBusy(button, true, "Registrando...");
        if (type === "purchase") {
            await supermarketApi.purchaseSuperItem(id, payload);
        } else {
            await supermarketApi.consumeSuperItem(id, payload);
        }
        closeSuperMovementModal();
        await loadSupermarket();
        showSuperFeedback(type === "purchase" ? "Compra registrada." : "Consumo registrado.");
    } catch (error) {
        if (type === "consume" && isNegativeStockConflict(error) && !payload.allowNegativeStock) {
            await confirmAndRetryNegativeStock(id, type, payload, error);
            return;
        }
        showSuperMovementFeedback(`No se pudo registrar el movimiento: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function confirmAndRetryNegativeStock(id, type, payload, error) {
    const message = negativeStockConfirmationMessage(error);
    showSuperMovementConflict(message, true);
    if (!globalThis.confirm || !globalThis.confirm(message)) {
        showSuperMovementFeedback("Confirmación de stock negativo cancelada.", true);
        return;
    }
    document.querySelector("#super-movement-allow-negative").checked = true;
    await runSuperMovementCommand(id, type, { ...payload, allowNegativeStock: true });
}

async function quickConsumeSuperItem(id, button, allowNegativeStock = false) {
    try {
        setButtonBusy(button, true, "Consumiendo...");
        await supermarketApi.quickConsumeSuperItem(id, { allowNegativeStock });
        await loadSupermarket();
        showSuperFeedback("Consumo rápido registrado.");
    } catch (error) {
        if (isNegativeStockConflict(error) && !allowNegativeStock) {
            const message = negativeStockConfirmationMessage(error);
            if (globalThis.confirm && globalThis.confirm(message)) {
                await quickConsumeSuperItem(id, button, true);
                return;
            }
        }
        showSuperFeedback(`No se pudo registrar el consumo rápido: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function loadSuperMovementHistory(item = null) {
    if (!supermarketApi.superStockMovements) {
        return;
    }
    const filters = item?.id
        ? { itemId: String(item.id), limit: SUPER_RECENT_HISTORY_LIMIT }
        : { limit: SUPER_RECENT_HISTORY_LIMIT };
    try {
        const movements = await supermarketApi.superStockMovements(filters);
        renderSuperMovementHistory(movements, item);
    } catch (error) {
        const empty = document.querySelector("#super-movement-history-empty");
        if (empty) {
            empty.hidden = false;
            empty.textContent = `No se pudo cargar el historial: ${error.message}`;
        }
    }
}

function renderSuperMovementHistory(movements, item = null) {
    const panel = superMovementHistoryPanel();
    const title = document.querySelector("#super-movement-history-title");
    const table = document.querySelector("#super-movement-history-table");
    const empty = document.querySelector("#super-movement-history-empty");
    if (panel) {
        panel.hidden = false;
    }
    if (title) {
        title.textContent = item?.name ? `Historial reciente · ${item.name}` : "Historial reciente";
    }
    if (!table) {
        return;
    }
    table.innerHTML = "";
    movements.forEach((movement) => {
        const row = document.createElement("tr");
        row.innerHTML = superMovementRowHtml(movement);
        table.append(row);
    });
    if (empty) {
        empty.hidden = movements.length > 0;
        empty.textContent = item?.name ? "Todavía no hay movimientos para este producto." : "Todavía no hay movimientos recientes.";
    }
}

function superMovementRowHtml(movement) {
    return `
        <td data-label="Fecha">${escapeHtml(formatMovementDate(movement.createdAt))}</td>
        <td data-label="Producto">${escapeHtml(movement.itemName || "—")}</td>
        <td data-label="Tipo">${escapeHtml(superMovementTypeLabel(movement.movementType))}</td>
        <td data-label="Cantidad">${escapeHtml(superMovementQuantityLabel(movement))}</td>
        <td data-label="Stock">${escapeHtml(quantityWithUnit(movement.resultingStock, movement.itemUnit))}</td>
        <td data-label="Notas">${movement.notes ? escapeHtml(movement.notes) : "—"}</td>
    `;
}

function superMovementHistoryPanel() {
    return document.querySelector("#super-movement-history");
}

function isNegativeStockConflict(error) {
    return error?.status === 409 && (error.movementType === "CONSUMPTION" || error.movementType === "QUICK_CONSUMPTION" || error.body?.movementType);
}

function negativeStockConfirmationMessage(error) {
    const body = error.body || error;
    return `El consumo dejaría stock negativo. Stock actual: ${body.currentStock ?? "—"}. Resultado: ${body.resultingStock ?? "—"}. ¿Confirmás stock negativo?`;
}

function itemById(id) {
    return superItems.find((candidate) => String(candidate.id) === String(id));
}

function openSuperItemEdit(id) {
    const item = superItems.find((candidate) => String(candidate.id) === String(id));
    if (!item) {
        showSuperFeedback("No se encontró el producto seleccionado para editar.", true);
        return;
    }
    editingItemId = item.id;
    editingItemOriginalStock = String(item.currentStock ?? "").trim();
    document.querySelector("#super-item-name").value = item.name;
    document.querySelector("#super-item-category").value = String(item.categoryId);
    document.querySelector("#super-item-unit").value = item.unit || "";
    document.querySelector("#super-item-presentation-label").value = item.commercialPresentationLabel || "";
    document.querySelector("#super-item-presentation-quantity").value = item.commercialPresentationQuantity || "";
    document.querySelector("#super-item-presentation-price-pesos").value = item.commercialPresentationPricePesos || "";
    renderSuperItemPriceSourceOptions(selectedPriceSourceId(item.commercialPresentationPriceSourceId), item.commercialPresentationPriceSourceLabel);
    document.querySelector("#super-item-presentation-price-source").value = selectedPriceSourceId(item.commercialPresentationPriceSourceId);
    document.querySelector("#super-item-presentation-price-source-label").value = item.commercialPresentationPriceSourceId ? "" : item.commercialPresentationPriceSourceLabel || "";
    document.querySelector("#super-item-presentation-price-observed-date").value = item.commercialPresentationPriceObservedDate || "";
    document.querySelector("#super-item-objective").value = item.habitualObjective || "";
    document.querySelector("#super-item-quick-quantity").value = item.quickQuantity || "";
    document.querySelector("#super-item-current-stock").value = item.currentStock ?? "";
    document.querySelector("#super-item-notes").value = item.notes || "";
    syncSuperItemPriceSourceInputs();
    document.querySelector("#super-item-submit").textContent = "Guardar producto";
    document.querySelector("#super-item-cancel-edit").hidden = false;
    document.querySelector("#super-item-name")?.focus?.();
}

function resetSuperItemForm() {
    editingItemId = null;
    editingItemOriginalStock = null;
    document.querySelector("#super-item-form")?.reset();
    const sourceSelect = document.querySelector("#super-item-presentation-price-source");
    if (sourceSelect) {
        sourceSelect.value = "";
    }
    const submit = document.querySelector("#super-item-submit");
    if (submit) {
        submit.textContent = "Crear producto";
    }
    const cancel = document.querySelector("#super-item-cancel-edit");
    if (cancel) {
        cancel.hidden = true;
    }
    syncSuperItemPriceSourceInputs();
}

function currentEditingItem() {
    return superItems.find((item) => String(item.id) === String(editingItemId));
}

async function updateSuperItemChecked(id, checked, checkbox) {
    try {
        await supermarketApi.updateSuperItemChecked(id, checked);
        await loadSupermarket();
    } catch (error) {
        if (checkbox) {
            checkbox.checked = !checked;
        }
        showSuperFeedback(`No se pudo actualizar el producto: ${error.message}`, true);
    }
}

async function deleteSuperItem(id, button) {
    if (globalThis.confirm && !globalThis.confirm("¿Seguro que querés eliminar este producto de la lista del super?")) {
        return;
    }
    try {
        setButtonBusy(button, true, "Eliminando...");
        await supermarketApi.deleteSuperItem(id);
        await loadSupermarket();
        showSuperFeedback("Producto eliminado.");
    } catch (error) {
        showSuperFeedback(`No se pudo eliminar el producto: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function uncheckAllSuperItems() {
    if (globalThis.confirm && !globalThis.confirm("¿Querés desmarcar todos los productos?")) {
        return;
    }
    const button = document.querySelector("#super-uncheck-all");
    try {
        setButtonBusy(button, true, "Desmarcando...");
        await supermarketApi.uncheckAllSuperItems();
        await loadSupermarket();
        showSuperFeedback("Todos los productos quedaron desmarcados.");
    } catch (error) {
        showSuperFeedback(`No se pudieron desmarcar los productos: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

function generateSuperList() {
    const text = generatedSuperListText(superItems);
    const output = document.querySelector("#super-generated-list");
    if (output) {
        output.textContent = text;
    }
    const hasCheckedItems = superItems.some((item) => item.checked);
    document.querySelector("#super-copy-list").disabled = !hasCheckedItems;
    document.querySelector("#super-download-list").disabled = !hasCheckedItems;
    document.querySelector("#super-whatsapp-list").disabled = !hasCheckedItems;
    showSuperFeedback(hasCheckedItems ? "Lista generada." : "No hay productos marcados para comprar.");
}

function clearGeneratedSuperList() {
    const output = document.querySelector("#super-generated-list");
    if (output) {
        output.textContent = "Generá la lista para ver los productos marcados actuales.";
    }
    document.querySelector("#super-copy-list").disabled = true;
    document.querySelector("#super-download-list").disabled = true;
    document.querySelector("#super-whatsapp-list").disabled = true;
}

async function copyGeneratedSuperList() {
    const text = generatedSuperListText(superItems);
    try {
        if (!globalThis.navigator?.clipboard?.writeText) {
            throw new Error("Clipboard unavailable");
        }
        await globalThis.navigator.clipboard.writeText(text);
        showSuperFeedback("Lista copiada al portapapeles.");
    } catch {
        showSuperFeedback("No se pudo copiar la lista al portapapeles.", true);
    }
}

function downloadGeneratedSuperList() {
    const text = generatedSuperListText(superItems);
    const date = new Date().toISOString().slice(0, 10);
    const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `lista-super-${date}.txt`;
    link.click();
    URL.revokeObjectURL(url);
    showSuperFeedback("Archivo TXT generado.");
}

function shareGeneratedSuperList() {
    const text = generatedSuperListText(superItems);
    const url = `https://wa.me/?text=${encodeURIComponent(text)}`;
    globalThis.open?.(url, "_blank", "noopener");
    showSuperFeedback("Se abrió WhatsApp para compartir la lista.");
}

function compareSuperItems(left, right) {
    const categoryComparison = String(left.categoryName || "").localeCompare(String(right.categoryName || ""), "es-AR", { sensitivity: "base" });
    if (categoryComparison !== 0) {
        return categoryComparison;
    }
    return String(left.name || "").localeCompare(String(right.name || ""), "es-AR", { sensitivity: "base" });
}

function quantityWithUnit(value, unit) {
    const text = String(value);
    return unit ? `${text} ${unit}` : text;
}

function showSuperFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-feedback", message, isError, isLoading);
}

function showSuperCategoryFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-category-feedback", message, isError, isLoading);
}

function showSuperBarcodeFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-barcode-feedback", message, isError, isLoading);
}

function showSuperMovementFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-movement-feedback", message, isError, isLoading);
}

function showSuperPriceObservationFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-price-observation-feedback", message, isError, isLoading);
}

function showSuperPriceSourceFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-price-source-feedback", message, isError, isLoading);
}

function showSuperTicketOcrFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-ticket-ocr-feedback", message, isError, isLoading);
}

function showSuperTicketOcrConfirmFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-ticket-ocr-confirm-feedback", message, isError, isLoading);
}

function showSuperMovementConflict(message, visible) {
    const conflict = document.querySelector("#super-movement-conflict");
    if (!conflict) {
        return;
    }
    conflict.textContent = message;
    conflict.hidden = !visible;
    conflict.classList.toggle("error-text", visible);
}

function formatMovementDate(value) {
    if (!value) {
        return "—";
    }
    return String(value).replace("T", " ").slice(0, 16);
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
