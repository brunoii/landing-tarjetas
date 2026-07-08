import { escapeHtml, safeHexColor, setButtonBusy } from "./utils.js";

export function renderCategories(categories, handlers) {
    const list = document.querySelector("#category-list");
    list.innerHTML = "";

    if (categories.length === 0) {
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.textContent = "Todavía no hay categorías activas.";
        list.append(empty);
        return;
    }

    categories.forEach((category) => {
        const row = document.createElement("form");
        row.className = "category-row";
        row.innerHTML = `
            <span class="color-swatch" aria-hidden="true"></span>
            <label>
                Nombre
                <input name="name" type="text" maxlength="80" required value="${escapeHtml(category.name)}">
            </label>
            <label>
                Color
                <input name="color" type="text" maxlength="7" pattern="#[0-9A-Fa-f]{6}" value="${escapeHtml(category.color || "")}">
            </label>
            <button type="submit" class="secondary-button">Guardar</button>
            <button type="button" class="danger-button">Eliminar</button>
        `;
        row.querySelector(".color-swatch").style.backgroundColor = safeHexColor(category.color);
        row.addEventListener("submit", (event) => {
            event.preventDefault();
            const formData = new FormData(row);
            const submitButton = row.querySelector('button[type="submit"]');
            setButtonBusy(submitButton, true, "Guardando...");
            handlers.onUpdate(category.id, {
                name: String(formData.get("name") || "").trim(),
                color: String(formData.get("color") || "").trim() || null,
                active: true
            }).finally(() => setButtonBusy(submitButton, false));
        });
        row.querySelector(".danger-button").addEventListener("click", (event) => {
            setButtonBusy(event.currentTarget, true, "Eliminando...");
            handlers.onDelete(category.id).finally(() => setButtonBusy(event.currentTarget, false));
        });
        list.append(row);
    });
}

export function categoryFormPayload() {
    return {
        name: document.querySelector("#category-name").value.trim(),
        color: document.querySelector("#category-color").value.trim() || null,
        active: true
    };
}

export function resetCategoryForm() {
    document.querySelector("#category-form").reset();
}

export function showCategoryFeedback(message, isError = false) {
    const feedback = document.querySelector("#category-feedback");
    feedback.textContent = message;
    feedback.style.color = isError ? "#fecaca" : "#86efac";
}
