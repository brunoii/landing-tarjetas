import { api } from "./api.js?v=20260716-super-inventory-stage5-api";
import { escapeHtml, setButtonBusy } from "./utils.js";

let supermarketApi = api;
let superItems = [];
let editingItemId = null;
let editingItemOriginalStock = null;
let editingCategoryId = null;
let superCategoryTableCollapsed = true;
let superCategoryCount = 0;
let currentBarcodeAlias = null;

export const SUPER_FIELD_LIMITS = Object.freeze({
    categoryName: 80,
    itemName: 160,
    itemNotes: 500,
    itemUnit: 40,
    barcodeCode: 80,
    barcodeFormat: 40
});

export function setupSupermarket({ apiClient = api } = {}) {
    supermarketApi = apiClient;
    editingItemId = null;
    editingItemOriginalStock = null;
    editingCategoryId = null;
    superCategoryTableCollapsed = true;
    currentBarcodeAlias = null;

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
    document.querySelector("#super-category-toggle")?.addEventListener("click", toggleSuperCategoryTable);
    document.querySelector("#super-generate-list")?.addEventListener("click", generateSuperList);
    document.querySelector("#super-copy-list")?.addEventListener("click", copyGeneratedSuperList);
    document.querySelector("#super-download-list")?.addEventListener("click", downloadGeneratedSuperList);
    document.querySelector("#super-whatsapp-list")?.addEventListener("click", shareGeneratedSuperList);
    document.querySelector("#super-uncheck-all")?.addEventListener("click", uncheckAllSuperItems);
    document.querySelector("#super-movement-form")?.addEventListener("submit", submitSuperMovementForm);
    document.querySelector("#super-movement-cancel")?.addEventListener("click", closeSuperMovementModal);
    document.querySelector("#super-movement-close")?.addEventListener("click", closeSuperMovementModal);

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
    const unit = String(values.unit || "").trim();
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
    return "";
}

export function normalizeSuperBarcodeCode(value) {
    return String(value ?? "").trim();
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
        const [categories, items, suggestedItems] = await Promise.all([
            supermarketApi.superCategories(),
            supermarketApi.superItems(),
            supermarketApi.superSuggestedList()
        ]);
        superItems = items;
        renderSuperCategories(categories);
        renderSuperCategoryOptions(categories);
        renderSuperBarcodeItemOptions(items);
        renderSuperItems(items);
        renderSuperSuggestedItems(suggestedItems);
        applySuperBarcodeHighlight(currentBarcodeAlias?.item?.id);
        await loadSuperMovementHistory();
        clearGeneratedSuperList();
        showSuperFeedback(items.length ? "Lista del super cargada." : "Todavía no hay productos cargados.");
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
        groupRow.innerHTML = `<th scope="rowgroup" colspan="8">${escapeHtml(categoryName)}</th>`;
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
            return;
        }
        currentBarcodeAlias = { code: payload.code, format: payload.format || null, item: null, aliasId: null };
        showSuperBarcodeResult(`Código ${payload.code} no encontrado. Podés asociarlo a un producto existente.`);
        showSuperBarcodeFeedback("No se encontró un alias activo.");
        setSuperBarcodeAttachEnabled(true);
        setSuperBarcodeRemoveVisible(false);
        applySuperBarcodeHighlight(null);
    } catch (error) {
        showSuperBarcodeFeedback(`No se pudo buscar el código: ${error.message}`, true);
    } finally {
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
    const filters = item?.id ? { itemId: String(item.id), limit: 50 } : { limit: 50 };
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
    document.querySelector("#super-item-objective").value = item.habitualObjective || "";
    document.querySelector("#super-item-quick-quantity").value = item.quickQuantity || "";
    document.querySelector("#super-item-current-stock").value = item.currentStock ?? "";
    document.querySelector("#super-item-notes").value = item.notes || "";
    document.querySelector("#super-item-submit").textContent = "Guardar producto";
    document.querySelector("#super-item-cancel-edit").hidden = false;
    document.querySelector("#super-item-name")?.focus?.();
}

function resetSuperItemForm() {
    editingItemId = null;
    editingItemOriginalStock = null;
    document.querySelector("#super-item-form")?.reset();
    const submit = document.querySelector("#super-item-submit");
    if (submit) {
        submit.textContent = "Crear producto";
    }
    const cancel = document.querySelector("#super-item-cancel-edit");
    if (cancel) {
        cancel.hidden = true;
    }
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
