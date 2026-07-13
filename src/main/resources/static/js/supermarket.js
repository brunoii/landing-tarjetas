import { api } from "./api.js?v=20260712-security-hardening";
import { escapeHtml, setButtonBusy } from "./utils.js";

let supermarketApi = api;
let superItems = [];
let editingItemId = null;
let editingCategoryId = null;
let superCategoryTableCollapsed = true;
let superCategoryCount = 0;

export const SUPER_FIELD_LIMITS = Object.freeze({
    categoryName: 80,
    itemName: 160,
    itemNotes: 500
});

export function setupSupermarket({ apiClient = api } = {}) {
    supermarketApi = apiClient;
    editingCategoryId = null;
    superCategoryTableCollapsed = true;

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
    document.querySelector("#super-category-toggle")?.addEventListener("click", toggleSuperCategoryTable);
    document.querySelector("#super-generate-list")?.addEventListener("click", generateSuperList);
    document.querySelector("#super-copy-list")?.addEventListener("click", copyGeneratedSuperList);
    document.querySelector("#super-download-list")?.addEventListener("click", downloadGeneratedSuperList);
    document.querySelector("#super-whatsapp-list")?.addEventListener("click", shareGeneratedSuperList);
    document.querySelector("#super-uncheck-all")?.addEventListener("click", uncheckAllSuperItems);

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
    return {
        name: String(values.name || "").trim(),
        categoryId: categoryId > 0 ? categoryId : null,
        checked: values.checked === true || values.checked === "true" || values.checked === "on",
        notes: String(values.notes || "").trim()
    };
}

export function validateSuperItemPayload(payload) {
    if (!payload.name) {
        return "El nombre del producto es obligatorio.";
    }
    if (!payload.categoryId) {
        return "La categoría del producto es obligatoria.";
    }
    return "";
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
            const notes = item.notes ? ` — ${item.notes}` : "";
            lines.push(`- ${item.name}${notes}`);
        });
    }
    return lines.join("\n").trim();
}

async function loadSupermarket() {
    try {
        showSuperFeedback("Cargando lista del super...", false, true);
        const [categories, items] = await Promise.all([
            supermarketApi.superCategories(),
            supermarketApi.superItems()
        ]);
        superItems = items;
        renderSuperCategories(categories);
        renderSuperCategoryOptions(categories);
        renderSuperItems(items);
        clearGeneratedSuperList();
        showSuperFeedback(items.length ? "Lista del super cargada." : "Todavía no hay productos cargados.");
    } catch (error) {
        showSuperFeedback(`No se pudo cargar la lista del super: ${error.message}`, true);
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
        groupRow.innerHTML = `<th scope="rowgroup" colspan="5">${escapeHtml(categoryName)}</th>`;
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
        <td data-label="Notas">${item.notes ? escapeHtml(item.notes) : "—"}</td>
        <td data-label="Acciones">
            <div class="row-actions super-item-actions">
                <button type="button" class="secondary-button icon-button" data-super-action="edit" data-super-item-id="${item.id}" aria-label="Editar producto ${escapeHtml(item.name)}" title="Editar">
                    <span aria-hidden="true">✎</span><span class="sr-only">Editar</span>
                </button>
                <button type="button" class="danger-button icon-button" data-super-action="delete" data-super-item-id="${item.id}" aria-label="Eliminar producto ${escapeHtml(item.name)}" title="Eliminar">
                    <span aria-hidden="true">🗑</span><span class="sr-only">Eliminar</span>
                </button>
            </div>
        </td>
    `;
}

async function saveSuperItem() {
    const form = document.querySelector("#super-item-form");
    const button = form?.querySelector("button[type='submit']");
    const payload = superItemPayloadFromValues({
        name: document.querySelector("#super-item-name")?.value,
        categoryId: document.querySelector("#super-item-category")?.value,
        checked: editingItemId ? currentEditingItem()?.checked : false,
        notes: document.querySelector("#super-item-notes")?.value
    });
    const validationMessage = validateSuperItemPayload(payload);
    if (validationMessage) {
        showSuperFeedback(validationMessage, true);
        return;
    }
    try {
        setButtonBusy(button, true, editingItemId ? "Guardando..." : "Creando...");
        if (editingItemId) {
            await supermarketApi.updateSuperItem(editingItemId, payload);
            showSuperFeedback("Producto actualizado.");
        } else {
            await supermarketApi.createSuperItem(payload);
            showSuperFeedback("Producto creado.");
        }
        resetSuperItemForm();
        await loadSupermarket();
    } catch (error) {
        showSuperFeedback(`No se pudo guardar el producto: ${error.message}`, true);
    } finally {
        setButtonBusy(button, false);
    }
}

async function handleSuperItemAction(button) {
    const id = button.dataset.superItemId;
    if (button.dataset.superAction === "edit") {
        openSuperItemEdit(id);
        return;
    }
    if (button.dataset.superAction === "delete") {
        await deleteSuperItem(id, button);
    }
}

function openSuperItemEdit(id) {
    const item = superItems.find((candidate) => String(candidate.id) === String(id));
    if (!item) {
        showSuperFeedback("No se encontró el producto seleccionado para editar.", true);
        return;
    }
    editingItemId = item.id;
    document.querySelector("#super-item-name").value = item.name;
    document.querySelector("#super-item-category").value = String(item.categoryId);
    document.querySelector("#super-item-notes").value = item.notes || "";
    document.querySelector("#super-item-submit").textContent = "Guardar producto";
    document.querySelector("#super-item-cancel-edit").hidden = false;
    document.querySelector("#super-item-name")?.focus?.();
}

function resetSuperItemForm() {
    editingItemId = null;
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

function showSuperFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-feedback", message, isError, isLoading);
}

function showSuperCategoryFeedback(message, isError = false, isLoading = false) {
    showFeedback("#super-category-feedback", message, isError, isLoading);
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
